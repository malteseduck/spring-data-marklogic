package org.springframework.data.marklogic.repository.query;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.StringUtils;

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
    private List<String> options;
    private String qtext;
    private String sparql;
    private StructuredQueryBuilder qb;
    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonNodeCreator factory = JsonNodeFactory.instance;


    public CombinedQueryDefinitionBuilder() {
        super();
        this.qb = new StructuredQueryBuilder();
        this.options = new ArrayList<>();
    }

    public CombinedQueryDefinitionBuilder(StructuredQueryDefinition structuredQuery) {
        this();
        this.structuredQuery = structuredQuery;
    }

    public CombinedQueryDefinitionBuilder(RawQueryByExampleDefinition qbe) {
        this();
        qbe.withHandle(new StringHandle(qbe.toString()).withFormat(Format.JSON));
        this.qbe = qbe;
    }

    // Allows us to generate a "random" name for a JSON property so we can support multiple options with the same name
    private String unique(String name) {
        return name + "%" + Math.round(Math.random() * 1000);
    }

    @Override
    public String serialize() {
        // TODO: Switch to use multi-part so we can keep options as XML?
        // TODO: Use Jackson library
        if (isQbe()) {
            ObjectNode search = factory.objectNode();

            if (!options.isEmpty()) {
                ObjectNode optionsJson = factory.objectNode();
                options.stream()
                    .map(option -> {
                        try {
                            return objectMapper.readTree(option);
                        } catch (IOException ex) {
                            throw new IllegalArgumentException(option);
                        }
                    })
                    .forEach(option -> {
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

            if (qbe != null) {
                try {
                    search.set("$query", objectMapper.readTree(qbe.toString()));
                } catch (IOException ex) {
                    throw new IllegalArgumentException(qbe.toString());
                }
            }

            // Make sure it is "proper" JSON so that MarkLogic is happy
            return factory.objectNode().set("search", search).toString()
                    .replaceAll("%[0-9]*\":", "\":"); // get rid of the "unique" identifiers for properties
        } else {
            StringBuilder search = new StringBuilder();

            search.append("<search xmlns=\"http://marklogic.com/appservices/search\">");

            // TODO: Can we put the structured query in "additional-query" if the "main" query is a QBE?
            if (structuredQuery != null && !isQbe()) {
                search.append(structuredQuery.serialize());
            }

            if (!options.isEmpty())
                search.append("<options>")
                        .append(options.stream().collect(Collectors.joining()))
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
            qbe.setHandle(new StringHandle(serialize()).withFormat(Format.JSON));
        }
        return qbe;
    }

    @Override
    public CombinedQueryDefinition byExample(RawQueryByExampleDefinition qbe) {
        this.qbe = qbe;
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
    public CombinedQueryDefinition withCollections(String... collections) {
        return and(qb.collection(collections));
    }

    @Override
    public CombinedQueryDefinition withOptions(String options) {
        this.options.add(options);
        return this;
    }

    @Override
    public CombinedQueryDefinition withOptions(List<String> options) {
        this.options.addAll(options);
        return this;
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
}
