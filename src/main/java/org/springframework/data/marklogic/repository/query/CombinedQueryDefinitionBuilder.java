package org.springframework.data.marklogic.repository.query;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.impl.AbstractQueryDefinition;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.RawQueryByExampleDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allow us to keep a running build of query/options so we can set them throughout the different processing levels.  The
 * CombinedQueryDefinition in the driver library is for a specific purpose and not modifiable, nor is it passable down
 * to the template methods.  Since the structure is well-defined as part of MarkLogic's REST API we can create the appropriate
 * combined query from what we have.
 */
public class CombinedQueryDefinitionBuilder extends AbstractQueryDefinition implements CombinedQueryDefinition {

    // TODO: Support XML QBE?

    private StructuredQueryDefinition structuredQuery;
    private RawQueryByExampleDefinition qbe;
    private Format qbeFormat;
    private List<String> options;
    private List<String> extracts;
    private String qtext;
    private String sparql;
    private String criteria;
    private int limit = -1;
    private StructuredQueryBuilder qb;
    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonNodeCreator factory = JsonNodeFactory.instance;
    private SelectedMode selected;

    public CombinedQueryDefinitionBuilder(StructuredQueryDefinition structuredQuery) {
        this();
        this.structuredQuery = structuredQuery;
        if (structuredQuery != null) {
            setCollections(structuredQuery.getCollections());
            setCriteria(structuredQuery.getCriteria());
            setDirectory(structuredQuery.getDirectory());
            setOptionsName(structuredQuery.getOptionsName());
            setResponseTransform(structuredQuery.getResponseTransform());
        }
    }

    public CombinedQueryDefinitionBuilder() {
        super();
        this.qb = new StructuredQueryBuilder();
        this.options = new ArrayList<>();
        this.extracts = new ArrayList<>();

        // To allow "javascript objects" for the query language
        objectMapper
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    }

    public static CombinedQueryDefinition combine(StructuredQueryDefinition query) {
        if (query instanceof CombinedQueryDefinitionBuilder) {
            return (CombinedQueryDefinition) query;
        } else {
            return new CombinedQueryDefinitionBuilder(query);
        }
    }

    public static CombinedQueryDefinition combine() {
        return new CombinedQueryDefinitionBuilder();
    }

    // Allows us to generate a "random" name for a JSON property so we can support multiple options with the same name
    private String unique(String name) {
        return name + "%" + Math.round(Math.random() * 1000);
    }

    @Override
    public String serialize() {
        List<String> optionsToSerialize = options;

        if (limit >= 0) optionsToSerialize.add(String.format("<page-length>%s</page-length>", limit));

        if (extracts.size() > 0) {
            StringBuilder extract = new StringBuilder();
            extract.append(String.format("<extract-document-data selected='%s'>", selected));

            extracts.forEach(path -> extract.append(String.format("<extract-path>%s</extract-path>", path)));

            extract.append("</extract-document-data>");
            optionsToSerialize.add(extract.toString());
        }

        if (isQbe() && qbeFormat == Format.JSON) {

            ObjectNode search = factory.objectNode();

            if (qbe != null) {
                try {
                    search.set("$query", objectMapper.readTree(qbe.toString()));
                } catch (IOException ex) {
                    throw new IllegalArgumentException(qbe.toString());
                }
            }

            if (!optionsToSerialize.isEmpty()) {
                ObjectNode optionsJson = factory.objectNode();
                optionsToSerialize.stream()
                        .map(XML::toJSONObject)
                        .map(JSONObject::toString)
                        .map(option -> {
                            try {
                                return objectMapper.readTree(option);
                            } catch (IOException ex) {
                                throw new IllegalArgumentException(option);
                            }
                        })
                        .forEachOrdered(option -> {
                            // Fix path indexes - they don't convert "nicely" like many of the other indexes
                            String index = (String) option.at("/sort-order/path-index").asText();
                            if (!StringUtils.isEmpty(index)) {
                                optionsJson
                                        .set(unique("sort-order"), factory.objectNode()
                                                .put("direction", option.at("/sort-order/direction").asText())
                                                .set("path-index", factory.objectNode()
                                                        .put("text", index)
                                                )
                                        );
                            } else {
                                option.fieldNames().forEachRemaining(key -> optionsJson.set(unique(key), option.get(key)));
                            }
                        });

                search.set("options", optionsJson);
            } else {
                // We need an empty options node or the REST API is unhappy, which makes us unhappy
                search.set("options", factory.objectNode());
            }

            // Make sure it is "proper" JSON so that MarkLogic is happy
            return factory.objectNode().set("search", search).toString()
                    .replaceAll("%[0-9]*\":", "\":"); // get rid of the "unique" identifiers for properties

        } else {
            StringBuilder search = new StringBuilder();

            search.append("<search xmlns=\"http://marklogic.com/appservices/search\">");

            if (isQbe() && qbeFormat == Format.XML) {
                // TODO: Can we put the structured query in "additional-query" if the "main" query is a QBE?
                // TODO: Support "true" XML QBE instead of converting the JSON to XML?
                search.append("<q:qbe xmlns:q=\"http://marklogic.com/appservices/querybyexample\">");
                JSONObject json = new JSONObject(qbe.toString());
                // TODO: Somehow support namespaces?
                search.append(XML.toString(json, "q:query").replace("<q:query", "<q:query xmlns=\"\""));
                search.append("</q:qbe>");
            } else if (structuredQuery != null) {
                search.append(structuredQuery.serialize());
            }

            if (!optionsToSerialize.isEmpty())
                search.append("<options>")
                        .append(optionsToSerialize.stream().collect(Collectors.joining()))
                        .append("</options>");

            if (!StringUtils.isEmpty(qtext))
                search.append("<qtext>")
                        .append(qtext)
                        .append("</qtext>");

            if (!StringUtils.isEmpty(sparql))
                search.append("<sparql>")
                        .append(qtext)
                        .append("</sparql>");

            search.append("</search>");
            return search.toString();
        }
    }

    @Override
    public boolean isQbe() {
        return qbe != null;
    }

    @Override
    public RawQueryByExampleDefinition getQbe() {
        if (qbe != null) {
            // Copy all the configuration from the combined query object so we don't loose any information as we pass back just the raw query object
            qbe.setCollections(getCollections());
            qbe.setDirectory(getDirectory());
            qbe.setResponseTransform(getResponseTransform());
            qbe.setOptionsName(getOptionsName());
            qbe.setHandle(new StringHandle(serialize()).withFormat(qbeFormat));
        }
        return qbe;
    }

    @Override
    public CombinedQueryDefinition byExample(RawQueryByExampleDefinition qbe) {
        return byExample(qbe, Format.JSON);
    }

    @Override
    public CombinedQueryDefinition byExample(RawQueryByExampleDefinition qbe, Format format) {
        if (qbe != null) {
            qbe.withHandle(new StringHandle(qbe.toString()).withFormat(format));
            this.qbeFormat = format;
            this.qbe = qbe;
        }
        return this;
    }

    @Override
    public CombinedQueryDefinition and(StructuredQueryDefinition query) {
        if (structuredQuery != null)
            structuredQuery = qb.and(structuredQuery, query);
        else
            structuredQuery = query;
        return this;
    }

    @Override
    public CombinedQueryDefinition collections(String... collections) {
        return and(qb.collection(collections));
    }

    @Override
    public CombinedQueryDefinition options(String options) {
        this.options.add(options);
        return this;
    }

    // TODO: Support passing JSON options like this: "{ 'sort-order': { direction : 'ascending', 'path-index': { text: '/name' } } }"?
    @Override
    public CombinedQueryDefinition options(List<String> options) {
        this.options.addAll(options);
        return this;
    }

    @Override
    public CombinedQueryDefinition extracts(List<String> extracts) {
        return extracts(extracts, SelectedMode.HIERARCHICAL);
    }

    @Override
    public CombinedQueryDefinition extracts(List<String> extracts, SelectedMode mode) {
        this.extracts = extracts;
        this.selected = mode;
        return this;
    }

    @Override
    public CombinedQueryDefinition limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public boolean isLimiting() {
        return limit >= 0;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public CombinedQueryDefinition term(String qtext) {
        this.qtext = qtext;
        return this;
    }

    @Override
    public CombinedQueryDefinition sparql(String sparql) {
        this.sparql = sparql;
        return this;
    }

//    @Override
    public String getCriteria()
    {
        return criteria;
    }

//    @Override
    public void setCriteria(String criteria)
    {
        this.criteria = criteria;
    }

//    @Override
    public CombinedQueryDefinition withCriteria(String criteria)
    {
        this.criteria = criteria;
        return this;
    }

    public Format getQbeFormat() {
        return qbeFormat;
    }

    public StructuredQueryDefinition getStructuredQuery() {
        return structuredQuery;
    }

    public List<String> getOptions() {
        return options;
    }

    public List<String> getExtracts() {
        return extracts;
    }

    public String getQtext() {
        return qtext;
    }

    public String getSparql() {
        return sparql;
    }

    public SelectedMode getSelected() {
        return selected;
    }
}
