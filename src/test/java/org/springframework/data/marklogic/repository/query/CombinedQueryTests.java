package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryBuilder;
import org.junit.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

public class CombinedQueryTests {

    private static final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    @Test
    public void testSerialize() {
        String serialized = combine(qb.and()).serialize();

        assertThat(serialized)
                .contains("<search xmlns=\"http://marklogic.com/appservices/search\">")
                .contains("<query")
                .contains("<and-query/>");
    }

    @Test
    public void testSorted() throws Exception {
        String serialized = combine().sort(new Sort("name")).serialize();
        assertThat(serialized)
                .contains("<search xmlns=\"http://marklogic.com/appservices/search\">")
                .contains("<sort-order direction='ascending'>")
                .contains("<path-index>/name</path-index>");
    }
}
