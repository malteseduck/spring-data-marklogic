package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.util.Assert;

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
            if (query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition) query).isLimiting()) {
                return operations.search(query, 0, ((CombinedQueryDefinition) query).getLimit(), type);
            } else {
                return operations.search(query, Math.toIntExact(pageable.getOffset()), pageable.getPageSize(), type);
            }
        }
    }

    final class FacetedPageExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;
        private final Pageable pageable;

        FacetedPageExecution(MarkLogicOperations operations, Pageable pageable) {
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
            if (query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition) query).isLimiting()) {
                return operations.facetedSearch(query, 0, ((CombinedQueryDefinition) query).getLimit(), type);
            } else {
                return operations.facetedSearch(query, Math.toIntExact(pageable.getOffset()), pageable.getPageSize(), type);
            }
        }
    }

    final class EntityListExecution implements MarkLogicQueryExecution {

        private final MarkLogicOperations operations;

        EntityListExecution(MarkLogicOperations operations) {
            Assert.notNull(operations, "MarkLogicOperations cannot be null");
            this.operations = operations;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.mongodb.repository.query.AbstractMongoQuery.Execution#execute(org.springframework.data.mongodb.core.query.Query, java.lang.Class, java.lang.String)
         */
        @Override
        public Object execute(StructuredQueryDefinition query, Class<?> type) {
            if (query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition) query).isLimiting()) {
                return operations.search(query, 0, ((CombinedQueryDefinition) query).getLimit(), type).getContent();
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
            if (query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition) query).isLimiting()) {
                return operations.stream(query, 0, ((CombinedQueryDefinition) query).getLimit(), type);
            } else if (pageable.isPaged()) {
                return operations.stream(query, pageable.getOffset(), pageable.getPageSize(), type);

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
            operations.delete(query, type);
            return null;
        }
    }

}
