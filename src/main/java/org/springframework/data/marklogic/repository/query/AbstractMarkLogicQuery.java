package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.repository.Transform;
import org.springframework.data.marklogic.repository.query.MarkLogicQueryExecution.*;
import org.springframework.data.repository.query.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public abstract class AbstractMarkLogicQuery implements RepositoryQuery {

    private final MarkLogicQueryMethod method;
    private final MarkLogicOperations operations;

    public AbstractMarkLogicQuery(MarkLogicQueryMethod method, MarkLogicOperations operations) {
        Assert.notNull(operations, "MarkLogicOperations must not be null!");
        Assert.notNull(method, "MarkLogicQueryMethod must not be null!");

        this.method = method;
        this.operations = operations;
    }

    @Override
    public Object execute(Object[] values) {
        ParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), values);
        StructuredQueryDefinition query = transform(createQuery(accessor));

        // TODO: This currently uses the type specified in the repository, it should use the return type of the method.
        // TODO: Do we need a "special" type like DocumentStream<T> to better signify return type once convert exists?
        // TODO: What is required to support projections?
        ResultProcessor processor = method.getResultProcessor();
        return getExecution(accessor).execute(query, processor.getReturnedType().getDomainType());
    }

    @Override
    public QueryMethod getQueryMethod() {
        return method;
    }

    /**
     * Add a server transform to the created query, if one is specified.
     *
     * @param query
     * @return
     */
    private StructuredQueryDefinition transform(StructuredQueryDefinition query) {
        Transform transform = method.getTransform();
        if (transform != null && StringUtils.hasText(transform.value())) {
            query.setResponseTransform(new ServerTransform(transform.value()));
        }
        return query;
    }

    private MarkLogicQueryExecution getExecution(ParameterAccessor accessor) {
        if (isDeleteQuery()) {
            return new DeleteExecution(operations);
        } else if (isExistsQuery()) {
            return new ExistsExecution(operations);
        } else if (isCountQuery()) {
            return new CountExecution(operations);
        } else if (method.isStreamingQuery()) {
            return new StreamingExecution(operations, accessor.getPageable());
        } else if (method.isSliceQuery() || method.isPageQuery()) {
            // TODO: Do we need to support slice differently?  A page is a slice...
            return new PagedExecution(operations, accessor.getPageable());
        } else if (method.isCollectionQuery()) {
            return new EntityListExecution(operations);
        } else {
            return new SingleEntityExecution(operations);
        }
    }

    protected abstract StructuredQueryDefinition createQuery(ParameterAccessor accessor);

    protected abstract boolean isCountQuery();
    protected abstract boolean isExistsQuery();
    protected abstract boolean isDeleteQuery();
}
