package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

public class PartTreeMarkLogicQuery extends AbstractMarkLogicQuery {

    private final PartTree tree;
    private final MappingContext<?, MarkLogicPersistentProperty> context;
    private final MarkLogicOperations operations;
    private final ResultProcessor processor;

    public PartTreeMarkLogicQuery(MarkLogicQueryMethod method, MarkLogicOperations operations) {
        super(method, operations);

        this.processor = method.getResultProcessor();
        this.tree = new PartTree(method.getName(), processor.getReturnedType().getDomainType());
        this.operations = operations;
        this.context = operations.getConverter().getMappingContext();
    }

    @Override
    protected StructuredQueryDefinition createQuery(ParameterAccessor accessor) {
        MarkLogicQueryCreator creator = new MarkLogicQueryCreator(tree, accessor, operations, context, (MarkLogicQueryMethod) getQueryMethod());
        StructuredQueryDefinition query = creator.createQuery();

        if (tree.isLimiting()) {
            query = operations.limitingQuery(tree.getMaxResults(), query);
        }

        return query;
    }

    @Override
    protected boolean isCountQuery() {
        return tree.isCountProjection();
    }

    @Override
    protected boolean isExistsQuery() {
        return tree.isExistsProjection();
    }

    @Override
    protected boolean isDeleteQuery() {
        return tree.isDelete();
    }

}
