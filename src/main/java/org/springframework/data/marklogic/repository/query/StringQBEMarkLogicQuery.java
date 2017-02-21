package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.repository.query.ParameterAccessor;

public class StringQBEMarkLogicQuery extends AbstractMarkLogicQuery {

    public StringQBEMarkLogicQuery(MarkLogicQueryMethod method, MarkLogicOperations operations) {
        super(method, operations);
    }

    @Override
    protected StructuredQueryDefinition createQuery(ParameterAccessor accessor) {
        return null;
    }
}
