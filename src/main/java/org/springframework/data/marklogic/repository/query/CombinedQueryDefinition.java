package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;

import java.util.List;

public interface CombinedQueryDefinition extends StructuredQueryDefinition {

    String serialize();

    CombinedQueryDefinition and(StructuredQueryDefinition query);

    CombinedQueryDefinition with(String options);

    CombinedQueryDefinition with(List<String> options);

    CombinedQueryDefinition term(String qtext);

    CombinedQueryDefinition sparql(String sparql);
}
