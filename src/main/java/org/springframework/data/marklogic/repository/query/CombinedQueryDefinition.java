package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.io.Format;
import com.marklogic.client.query.RawQueryByExampleDefinition;
import com.marklogic.client.query.StructuredQueryDefinition;

import java.util.List;

public interface CombinedQueryDefinition extends StructuredQueryDefinition {

    String serialize();

    boolean isQbe();

    RawQueryByExampleDefinition getRawQbe();

    CombinedQueryDefinition byExample(RawQueryByExampleDefinition qbe);

    CombinedQueryDefinition byExample(RawQueryByExampleDefinition qbe, Format format);

    CombinedQueryDefinition and(StructuredQueryDefinition query);

    CombinedQueryDefinition collections(String... collections);

    CombinedQueryDefinition options(String options);

    CombinedQueryDefinition options(List<String> options);

    CombinedQueryDefinition extracts(List<String> extracts);

    CombinedQueryDefinition extracts(List<String> extracts, SelectedMode mode);

    CombinedQueryDefinition limit(int limit);

    boolean isLimiting();

    int getLimit();

    CombinedQueryDefinition term(String qtext);

    CombinedQueryDefinition sparql(String sparql);
}
