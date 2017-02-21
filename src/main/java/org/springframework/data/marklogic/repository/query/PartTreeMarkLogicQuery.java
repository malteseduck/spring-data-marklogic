package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.mapping.DocumentFormat;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;

public class PartTreeMarkLogicQuery extends AbstractMarkLogicQuery {

    private final PartTree tree;
    private final MappingContext<?, MarkLogicPersistentProperty> context;
    private final ResultProcessor processor;
    private final DocumentFormat format;

    public PartTreeMarkLogicQuery(MarkLogicQueryMethod method, MarkLogicOperations operations) {
        super(method, operations);

        this.processor = method.getResultProcessor();
        this.tree = new PartTree(method.getName(), processor.getReturnedType().getDomainType());
        this.context = operations.getConverter().getMappingContext();
        this.format = method.getFormat();
    }

    @Override
    protected StructuredQueryDefinition createQuery(ParameterAccessor accessor) {
        MarkLogicQueryCreator creator = new MarkLogicQueryCreator(tree, accessor, context, format);
        StructuredQueryDefinition query = creator.createQuery();

        if (tree.isLimiting()) {
            // TODO: Use a combined query to specify page length?
//            query.limit(tree.getMaxResults());
        }

        // TODO: Add sorting here as well?
        return query;

//        TextCriteria textCriteria = accessor.getFullText();
//        if (textCriteria != null) {
//            query.addCriteria(textCriteria);
//        }

//        String fieldSpec = this.getQueryMethod().getFieldSpecification();
//
//        if (!StringUtils.hasText(fieldSpec)) {
//
//            ReturnedType returnedType = processor.withDynamicProjection(accessor).getReturnedType();
//
//            if (returnedType.isProjecting()) {
//
//                Field fields = query.fields();
//
//                for (String field : returnedType.getInputProperties()) {
//                    fields.include(field);
//                }
//            }
//
//            return query;
//        }


    }

}
