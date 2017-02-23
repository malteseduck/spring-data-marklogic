package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.repository.query.MarkLogicQueryExecution.*;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

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
        StructuredQueryDefinition query = createQuery(accessor);

        // TODO: Need results processing of any kind?
        // TODO: What is required to support projections?
        return getExecution(accessor).execute(query, method.getEntityInformation().getJavaType());
    }

    @Override
    public QueryMethod getQueryMethod() {
        return method;
    }

    private MarkLogicQueryExecution getExecution(ParameterAccessor accessor) {
        if (isExistsQuery()) {
            return new ExistsExecution(operations);
        } else if (isCountQuery()) {
            return new CountExecution(operations);
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
}
