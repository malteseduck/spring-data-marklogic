package io.github.malteseduck.springframework.data.marklogic.repository.query;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeCreator;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.impl.AbstractQueryDefinition;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.RawQueryByExampleDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.*;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.springframework.util.StringUtils.hasText;

/**
 * Allow us to keep a running build of query/options so we can set them throughout the different processing levels.  The
 * CombinedQueryDefinition in the driver library is for a specific purpose and not modifiable, nor is it passable down
 * to the template methods.  Since the structure is well-defined as part of MarkLogic's REST API we can create the appropriate
 * combined query from what we have.
 */
public class CombinedQueryDefinitionBuilder extends AbstractQueryDefinition implements CombinedQueryDefinition {

    private StructuredQueryDefinition structuredQuery;
    private MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext;
    private RawQueryByExampleDefinition qbe;
    private Class entityClass;
    private Format qbeFormat;
    private List<String> options;
    private List<String> extracts;
    private String qtext;
    private String sparql;
    private String criteria;
    private SelectedMode selected;
    private int limit = -1;
    private StructuredQueryBuilder qb;
    private ObjectMapper objectMapper = new ObjectMapper();
    private JsonNodeCreator factory = JsonNodeFactory.instance;

    public static CombinedQueryDefinition combine() {
        return new CombinedQueryDefinitionBuilder();
    }

    public static CombinedQueryDefinition combine(StructuredQueryDefinition query) {
        return new CombinedQueryDefinitionBuilder(query);
    }

    private CombinedQueryDefinitionBuilder(StructuredQueryDefinition query) {
        this();
        if (query instanceof CombinedQueryDefinitionBuilder) {
            CombinedQueryDefinitionBuilder builder = (CombinedQueryDefinitionBuilder) query;
            this.structuredQuery = builder.getStructuredQuery();
            this.qbe = builder.getQbe();
            this.extracts = builder.getExtracts();
            this.qtext = builder.getQtext();
            this.options = builder.getOptions();
            this.qbeFormat = builder.getQbeFormat();
            this.criteria = builder.getCriteria();
            this.limit = builder.getLimit();
            this.selected = builder.getSelected();
            this.entityClass = builder.getEntityClass();
            this.mappingContext = builder.getMappingContext();
        } else {
            this.structuredQuery = query;
        }

        if (query != null) {
            collections(query.getCollections());
            directory(query.getDirectory());
            optionsName(query.getOptionsName());
            transform(query.getResponseTransform());
            setCriteria(query.getCriteria());
        }
    }

    private CombinedQueryDefinitionBuilder() {
        super();
        this.mappingContext = new MarkLogicMappingContext();
        this.qb = new StructuredQueryBuilder();
        this.options = new ArrayList<>();
        this.extracts = new ArrayList<>();

        // To allow "javascript objects" for the query language
        objectMapper
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    }

    // Allows us to generate a "random" name for a JSON property so we can support multiple options with the same name
    private String unique(String name) {
        return name + "%" + Math.round(Math.random() * 1000);
    }

    @Override
    public String serialize() {
        List<String> optionsToSerialize = new ArrayList<>(options);

        if (limit >= 0) optionsToSerialize.add(format("<page-length>%s</page-length>", limit));

        if (extracts.size() > 0) {
            StringBuilder extract = new StringBuilder();
            extract.append(format("<extract-document-data selected='%s'>", selected));

            extracts.forEach(path -> extract.append(format("<extract-path>%s</extract-path>", path)));

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

        } else if (isQbe() || structuredQuery != null ||
                !optionsToSerialize.isEmpty() ||
                hasText(qtext) ||
                hasText(sparql)) {
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
        return "";
    }

    @Override
    public boolean isQbe() {
        return qbe != null;
    }

    @Override
    public RawQueryByExampleDefinition getRawQbe() {
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

    public RawQueryByExampleDefinition getQbe() {
        return qbe;
    }

    public void setQbe(RawQueryByExampleDefinition qbe) {
        this.qbe = qbe;
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
    public CombinedQueryDefinition and(StructuredQueryDefinition... queries) {
        if (queries != null && queries.length > 0) {
            this.structuredQuery = qb.and(collect(queries));
        }
        return this;
    }

    @Override
    public CombinedQueryDefinition or(StructuredQueryDefinition... queries) {
        if (queries != null && queries.length > 0) {
            this.structuredQuery = qb.or(collect(queries));
        }
        return this;
    }

    /**
     * Create an array of queries, starting with the current structured queries and adding the specified ones.
     *
     * @param queries Variable number of structured query definitions.
     *
     * @return An array of structured query definitions that can be sent to and() and or(), or whatever takes varargs.
     */
    private StructuredQueryDefinition[] collect(StructuredQueryDefinition... queries) {
        Set<StructuredQueryDefinition> current = structuredQuery != null
                ? new HashSet<>(singletonList(structuredQuery))
                : new HashSet<>();
        Collections.addAll(current, queries);
        return current.toArray(new StructuredQueryDefinition[0]);
    }

    @Override
    public CombinedQueryDefinition collections(String... collections) {
        // Preserve any current collections set on the query so this can be called multiple times without destroying anything
        Set<String> current = new HashSet<>(asList(getCollections()));
        Collections.addAll(current, collections);
        setCollections(current.toArray(new String[0]));
        return this;
    }

    @Override
    public CombinedQueryDefinition directory(String directory) {
        setDirectory(directory);
        return this;
    }

    @Override
    public CombinedQueryDefinition optionsName(String name) {
        setOptionsName(name);
        return this;
    }

    // TODO: Support passing JSON options like this: "{ 'sort-order': { direction : 'ascending', 'path-index': { text: '/name' } } }"?
    @Override
    public CombinedQueryDefinition options(String... options) {
        Collections.addAll(this.options, options);
        return this;
    }

    @Override
    public CombinedQueryDefinition sort(Sort sort) {
        if (sort != null && sort.iterator().hasNext()) {
            final MarkLogicPersistentEntity entity = entityClass != null ? mappingContext.getPersistentEntity(entityClass) : null;

            sort.forEach(order -> {
                String propertyName = order.getProperty();
                String direction = asMLSort(order.getDirection());

                String path = null;
                // If there is an entity then we can determine the configuration of the index from it, otherwise we just
                // default to a path index. An error will be thrown by the database if the index is not created, though.
                if (entity != null) {
                    MarkLogicPersistentProperty property = (MarkLogicPersistentProperty) entity.getPersistentProperty(propertyName);
                    if (property != null) {
                        // If the user specified type of "PATH" (default), or a path was specified, do we set it.  If
                        // they specify type of "ELEMENT" then we don't set a path and let it fall through into the
                        // logic of creating an element sort
                        if (property.getIndexType() == IndexType.PATH && !StringUtils.isEmpty(property.getPath())) {
                            path = property.getPath();
                        }
                    } else {
                        // If the property was not found (user probably specified a path) then we default to path index
                        path = "/"+ order.getProperty();
                    }
                } else {
                    path = "/"+ order.getProperty();
                }

                // If any of the conditions above made it seem like this needs to be an element sort, then do so,
                // otherwise default to a path index sort
                if (!hasText(path)) {
                    sort(propertyName, direction, IndexType.ELEMENT);
                } else {
                    sort(path, direction, IndexType.PATH);
                }
            });
        }

        return this;
    }

    @Override
    public CombinedQueryDefinition sort(Sort sort, IndexType type) {
        if (sort != null) {
            sort.forEach(order -> sort(order.getProperty(), asMLSort(order.getDirection()), type));
        }
        return this;
    }

    @Override
    public CombinedQueryDefinition sort(String propertyName, String order, IndexType type) {
        if (hasText(propertyName) && hasText(order) && type != null) {
            if (type == IndexType.PATH) {
                options(format("" +
                        "<sort-order direction='%s'>" +
                        "   <path-index>%s</path-index>" +
                        "</sort-order>", order, propertyName));
            } else {
                options(format("" +
                        "<sort-order direction='%s'>" +
                        "   <element ns='' name='%s'/>" +
                        "</sort-order>", order, propertyName));
            }
        }

        return this;
    }

    private String asMLSort(Sort.Direction direction) {
        if (Sort.Direction.DESC.equals(direction)) {
            return "descending";
        } else {
            return "ascending";
        }
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
    public CombinedQueryDefinition transform(ServerTransform transform) {
        setResponseTransform(transform);
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

    @Override
    public CombinedQueryDefinition type(Class entityClass) {
        this.entityClass = entityClass;

        if (entityClass != null) {
            MarkLogicPersistentEntity entity = mappingContext.getPersistentEntity(entityClass);

            if (entity != null && entity.getTypePersistenceStrategy() == TypePersistenceStrategy.COLLECTION) {
                collections(entity.getTypeName());
            }
        }

        return this;
    }

    @Override
    public CombinedQueryDefinition context(MappingContext mappingContext) {
        this.mappingContext = mappingContext;
        return this;
    }

    @Override
    public String getCriteria()
    {
        return criteria;
    }

    @Override
    public void setCriteria(String criteria)
    {
        this.criteria = criteria;
    }

    @Override
    public CombinedQueryDefinition withCriteria(String criteria)
    {
        this.criteria = criteria;
        return this;
    }

    private CombinedQueryDefinition and(StructuredQueryDefinition query) {
        if (structuredQuery != null)
            structuredQuery = qb.and(structuredQuery, query);
        else
            structuredQuery = query;
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

    public Class getEntityClass() {
        return entityClass;
    }

    public MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> getMappingContext() {
        return mappingContext;
    }
}
