package org.springframework.data.marklogic.core.convert;

import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.domain.Example;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.repository.query.CombinedQueryDefinition;
import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

/**
 * Helper class for last-minute tweaks to the query, or for last minute updates to read/write operations
 */
public class QueryMapper {

    private final MarkLogicConverter converter;
    private final ConcurrentHashMap<Class, ServerTransformer> transformers = new ConcurrentHashMap<>();

    public QueryMapper(MarkLogicConverter converter) {
        Assert.notNull(converter, "MarkLogicConverter must not be null!");
        this.converter = converter;
    }

    public QueryDefinition getMappedQuery(StructuredQueryDefinition query, Class entityClass) {
        boolean isRaw = query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition) query).isQbe();
        CombinedQueryDefinition combined = combine(query).type(entityClass);

        // If no server transform is already set on the query then we can see if there is a entity-configured transform
        if (entityClass != null && combined.getResponseTransform() == null) {
            combined.setResponseTransform(getReadTransform(entityClass));
        }

        if (isRaw) {
            return combined.getRawQbe();
        } else {
            return combined;
        }
    }

    public <T> QueryDefinition getExampleQuery(Example<T> example) {
        return null;
    }

    public ServerTransform getReadTransform(Class entityClass) {
        MarkLogicPersistentEntity<?> entity = converter.getMappingContext().getPersistentEntity(entityClass);
        ServerTransformer transformer = findTransformer(entity);
        return transformer != null
                ? transformer.reader(entity, null)
                : null;
    }

    public ServerTransform getWriteTransform(Class entityClass) {
        MarkLogicPersistentEntity<?> entity = converter.getMappingContext().getPersistentEntity(entityClass);
        ServerTransformer transformer = findTransformer(entity);
        return transformer != null
                ? transformer.writer(entity)
                : null;
    }

    public ServerTransformer getTransformer(Class<? extends ServerTransformer> transformerClass) {
        ServerTransformer transformer = transformers.get(transformerClass);

        if (transformer == null) {
            try {
                transformer = transformerClass.newInstance();
                transformers.put(transformerClass, transformer);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return transformer;
    }

    protected ServerTransformer findTransformer(MarkLogicPersistentEntity<?> pEntity) {
        ServerTransformer transformer = transformers.get(pEntity);

        if (transformer == null) {
            if (pEntity.getTransformer() != null && pEntity.getTransformer() != ServerTransformer.class) {
                transformer = getTransformer(pEntity.getTransformer());
            }
        }

        return transformer;
    }
}
