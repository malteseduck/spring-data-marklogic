package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.RawQueryByExampleDefinition;
import com.marklogic.client.query.StructuredQueryDefinition;

import java.util.List;

public interface CombinedQueryDefinition extends StructuredQueryDefinition {

    String serialize();

    boolean isRaw();

    RawQueryByExampleDefinition getRaw();

    CombinedQueryDefinition and(StructuredQueryDefinition query);

    CombinedQueryDefinition withOptions(String options);

    CombinedQueryDefinition withOptions(List<String> options);

    CombinedQueryDefinition term(String qtext);

    CombinedQueryDefinition sparql(String sparql);
}
