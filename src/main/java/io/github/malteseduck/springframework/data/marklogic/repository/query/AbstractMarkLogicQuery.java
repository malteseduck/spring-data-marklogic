package io.github.malteseduck.springframework.data.marklogic.repository.query;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.query.StructuredQueryDefinition;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import io.github.malteseduck.springframework.data.marklogic.core.convert.ServerTransformer;
import io.github.malteseduck.springframework.data.marklogic.repository.Query;
import io.github.malteseduck.springframework.data.marklogic.repository.query.MarkLogicQueryExecution.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.query.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;

import static io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

public abstract class AbstractMarkLogicQuery implements RepositoryQuery {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractMarkLogicQuery.class);
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
        // TODO: projections are not enabled until we can add extracts to limit properties returned (based on projection interface)
        ParameterAccessor accessor = new ParametersParameterAccessor(method.getParameters(), values);
        StructuredQueryDefinition query = createQuery(accessor);

//        ResultProcessor processor = method.getResultProcessor().withDynamicProjection(accessor);
        ResultProcessor processor = method.getResultProcessor();
        ReturnedType returnedType = processor.getReturnedType();
        Class typeToRead = returnedType.getDomainType();

        // Add transforms and extracts to the query, if they are in the annotations
        query = transform(query, typeToRead, accessor);
        query = extracts(query);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing query " + query.serialize());
        }

//        return processor.processResult(getExecution(accessor).execute(query, typeToRead));
        return getExecution(accessor).execute(query, typeToRead);
    }

    @Override
    public QueryMethod getQueryMethod() {
        return method;
    }

    /**
     * Add a server transform to the created query, if one is specified.
     */
    private StructuredQueryDefinition transform(StructuredQueryDefinition query, Class typeToRead, ParameterAccessor accessor) {
        Query queryAnnotation = method.getQueryAnnotation();
        if (queryAnnotation != null) {
            if (queryAnnotation.transformer() != ServerTransformer.class) {
                ServerTransform reader = operations.getQueryMapper()
                        .getTransformer(queryAnnotation.transformer())
                        .reader(operations.getConverter().getMappingContext().getPersistentEntity(typeToRead), accessor);
                query.setResponseTransform(reader);
            } else if (StringUtils.hasText(queryAnnotation.transform())) {
                query.setResponseTransform(new ServerTransform(queryAnnotation.transform()));
            }
        }
        return query;
    }

    /**
     * Add extracts to the created query, if any are specified.
     *
     * @param query
     * @return
     */
    private StructuredQueryDefinition extracts(StructuredQueryDefinition query) {
        String[] extracts = method.getExtracts();
        if (extracts != null && extracts.length > 0) {
            query = combine(query).extracts(Arrays.asList(extracts));
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
            if (method.isFacetedQuery()) {
                return new FacetedPageExecution(operations, accessor.getPageable());
            } else {
                return new PagedExecution(operations, accessor.getPageable());
            }
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
