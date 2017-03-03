package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.impl.AbstractQueryDefinition;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.RawQueryByExampleDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
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
        qbe.withHandle(new StringHandle("{ $query: " + qbe.toString() + " }").withFormat(Format.JSON));
        this.qbe = qbe;
    }

    @Override
    public String serialize() {
        StringBuilder search = new StringBuilder();

        search.append("<search xmlns=\"http://marklogic.com/appservices/search\">");

        if (structuredQuery != null) {
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

    @Override
    public boolean isRaw() {
        return qbe != null;
    }

    @Override
    public RawQueryByExampleDefinition getRaw() {
        return qbe;
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
