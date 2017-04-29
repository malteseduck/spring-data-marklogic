package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.ValuesHandle;
import com.marklogic.client.query.QueryManager;
import com.marklogic.client.query.StructuredQueryDefinition;
import com.marklogic.client.query.ValuesDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

interface MarkLogicQueryExecution {

    // TODO: Is this approach overly complicated for one-line calls to the operations?
    // TODO: Add support for values queries (FacetResults)?

    Object execute(StructuredQueryDefinition query, Class<?> type);

    final class PagedExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;
        private final Pageable pageable;

        PagedExecution(MarkLogicOperations operations, Pageable pageable) {
            Assert.notNull(operations, "MarkLogicOperations must not be null!");
            Assert.notNull(pageable, "Need a Pageable in order to page");
            this.operations = operations;
            this.pageable = pageable;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.repository.query.AbstractMongoQuery.Execution#execute(org.springframework.data.mongodb.core.query.Query, java.lang.Class, java.lang.String)
         */
        @Override
        public Object execute(final StructuredQueryDefinition query, final Class<?> type) {
            if (query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition)query).isLimiting()) {
                return operations.search(query, 0, ((CombinedQueryDefinition)query).getLimit(), type);
            } else {
                return operations.search(query, pageable.getOffset(), pageable.getPageSize(), type);
            }
        }
    }

    final class EntityListExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;

        EntityListExecution(MarkLogicOperations operations) {
            Assert.notNull(operations);
            this.operations = operations;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.repository.query.AbstractMongoQuery.Execution#execute(org.springframework.data.mongodb.core.query.Query, java.lang.Class, java.lang.String)
         */
        @Override
        public Object execute(StructuredQueryDefinition query, Class<?> type) {
            if (query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition)query).isLimiting()) {
                return operations.search(query, 0, ((CombinedQueryDefinition)query).getLimit(), type).getContent();
            } else {
                return operations.search(query, type);
            }
        }
    }

    final class SingleEntityExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;

        SingleEntityExecution(MarkLogicOperations operations) {
            Assert.notNull(operations, "MarkLogicOperations must not be null!");
            this.operations = operations;
        }

        @Override
        public Object execute(StructuredQueryDefinition query, Class<?> type) {
            Page<?> page = operations.search(query, 0, 1, type);
            return page.hasContent() ? page.iterator().next() : null;
        }
    }

    final class StreamingExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;
        private final Pageable pageable;

        StreamingExecution(MarkLogicOperations operations, Pageable pageable) {
            Assert.notNull(operations, "MarkLogicOperations must not be null!");
            this.operations = operations;
            this.pageable = pageable;
        }

        @Override
        public Object execute(StructuredQueryDefinition query, Class<?> type) {
            if (pageable != null) {
                if (query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition) query).isLimiting()) {
                    return operations.stream(query, 0, ((CombinedQueryDefinition) query).getLimit(), type);
                } else {
                    return operations.stream(query, pageable.getOffset(), pageable.getPageSize(), type);
                }
            } else {
                return operations.stream(query, type);
            }
        }
    }

    final class CountExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;

        CountExecution(MarkLogicOperations operations) {
            Assert.notNull(operations, "MarkLogicOperations must not be null!");
            this.operations = operations;
        }

        @Override
        public Object execute(StructuredQueryDefinition query, Class<?> type) {
            return operations.count(query, type);
        }
    }

    final class ExistsExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;

        ExistsExecution(MarkLogicOperations operations) {
            Assert.notNull(operations, "MarkLogicOperations must not be null!");
            this.operations = operations;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.repository.query.AbstractMongoQuery.Execution#execute(org.springframework.data.mongodb.core.query.Query, java.lang.Class, java.lang.String)
         */
        @Override
        public Object execute(StructuredQueryDefinition query, Class<?> type) {
            return operations.exists(query, type);
        }
    }

    final class DeleteExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;

        DeleteExecution(MarkLogicOperations operations) {
            Assert.notNull(operations, "MarkLogicOperations must not be null!");
            this.operations = operations;
        }

        @Override
        public Object execute(StructuredQueryDefinition query, Class<?> type) {
            return operations.executeWithClient((client, transaction) -> {
                QueryManager qryMgr = client.newQueryManager();
                CombinedQueryDefinition combined = new CombinedQueryDefinitionBuilder(query)
                        .options("<values name='uris'><uri/></values>");

                combined = (CombinedQueryDefinition) operations.getConverter().wrapQuery(combined, type);
                ValuesDefinition valDef = qryMgr.newValuesDefinition("uris");
                valDef.setQueryDefinition(
                        qryMgr.newRawCombinedQueryDefinition(new StringHandle(combined.serialize()).withFormat(Format.XML))
                );

                ValuesHandle results = qryMgr.values(valDef, new ValuesHandle(), transaction);

                // Convert the facets to just a list of the ids so we can filter them down
                List<String> uris = Arrays.stream(results.getValues())
                        .map(value -> value.get("xs:string", String.class))
                        .collect(Collectors.toList());

                if (!uris.isEmpty()) {
                    client.newDocumentManager().delete(transaction, uris.toArray(new String[0]));
                }

                return null;
            });
        }
    }

}
