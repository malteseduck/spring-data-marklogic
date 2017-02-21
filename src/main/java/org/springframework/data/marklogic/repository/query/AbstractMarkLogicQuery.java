package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

public abstract class AbstractMarkLogicQuery implements RepositoryQuery {

    private final MarkLogicQueryMethod method;
    private final MarkLogicOperations operations;
    private final EntityInstantiators instantiators;

    public AbstractMarkLogicQuery(MarkLogicQueryMethod method, MarkLogicOperations operations) {
        Assert.notNull(operations, "MarkLogicOperations must not be null!");
        Assert.notNull(method, "MarkLogicQueryMethod must not be null!");

        this.method = method;
        this.operations = operations;
        this.instantiators = new EntityInstantiators();
    }

    @Override
    public Object execute(Object[] values) {
        StructuredQueryDefinition query = createQuery(new ParametersParameterAccessor(method.getParameters(), values));

        // TODO: Need results processing
        return operations.search(query, method.getReturnedObjectType());
    }

    @Override
    public QueryMethod getQueryMethod() {
        return method;
    }

    protected abstract StructuredQueryDefinition createQuery(ParameterAccessor accessor);
}
