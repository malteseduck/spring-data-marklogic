package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.junit.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.MarkLogicTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.marklogic.repository.query.QueryTestUtils.client;

public class CombinedQueryTests {

    private static final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    @Test
    public void testSerialize() {
        CombinedQueryDefinition query = new CombinedQueryDefinitionBuilder(qb.and());
        String serialized = query.serialize();

        assertThat(serialized)
                .contains("<search xmlns=\"http://marklogic.com/appservices/search\">")
                .contains("<query")
                .contains("<and-query/>");
    }

    @Test
    public void testSorted() throws Exception {
        StructuredQueryDefinition query =
                new MarkLogicTemplate(client())
                        .sortQuery(new Sort("name"), null, null);

        String serialized = query.serialize();
        assertThat(serialized)
                .contains("<search xmlns=\"http://marklogic.com/appservices/search\">")
                .contains("<sort-order direction='ascending'>")
                .contains("<path-index>/name</path-index>");
    }
}
