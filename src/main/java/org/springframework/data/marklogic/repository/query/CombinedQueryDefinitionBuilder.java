package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.impl.AbstractQueryDefinition;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.RawQueryByExampleDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.util.StringUtils;

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
    private List<String> options;
    private String qtext;
    private String sparql;
    private StructuredQueryBuilder qb;

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

    @Override
    public String serialize() {
        StringBuilder search = new StringBuilder();

        // TODO: Switch to use multi-part so we can keep options as XML?  Converting to
        if (isQbe()) {
            search.append("{ search: { ");

            if (qbe != null) {
                search.append("$query: " + qbe.toString());
            }

            if (!options.isEmpty()) {
                search.append(", ").append("options: { ")
                        .append(
                                options.stream()
                                        .map(option -> XML.toJSONObject(option))
                                        .map(option -> {
                                            // Fix path indexes - they don't convert "nicely" like many of the other indexes
                                            // TODO: This doesn't support any additional options that may exist (namespaces?) Does this matter?
                                            String index = (String) option.query("/sort-order/path-index");
                                            if (!StringUtils.isEmpty(index)) {
                                                return new JSONObject()
                                                        .put("sort-order", new JSONObject()
                                                                .put("direction", option.query("/sort-order/direction"))
                                                                .put("path-index", new JSONObject()
                                                                        .put("text", index)
                                                                )
                                                        );
                                            } else {
                                                return option;
                                            }
                                        })
                                        .map(JSONObject::toString)
                                        // The conversion wraps the JSON as an object, but inside the options they are just property names/values
                                        .map(string -> string.substring(1, string.length() - 1))
                                        .collect(Collectors.joining(", "))
                        )
                        .append(" }");
            } else {
                // We need an empty options node or the REST API is unhappy, which makes us unhappy
                search.append(", options: {}");
            }

            search.append(" } }");

        } else {

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
        }

        return search.toString();
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
