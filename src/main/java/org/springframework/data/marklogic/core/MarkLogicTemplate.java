package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.impl.PojoQueryBuilderImpl;
import com.marklogic.client.pojo.PojoQueryBuilder;
import com.marklogic.client.query.DeleteQueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.convert.MappingMarkLogicConverter;
import org.springframework.data.marklogic.core.convert.MarkLogicConverter;
import org.springframework.data.marklogic.core.mapping.*;
import org.springframework.data.marklogic.domain.ChunkRequest;
import org.springframework.data.marklogic.repository.query.CombinedQueryDefinition;
import org.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class MarkLogicTemplate implements MarkLogicOperations, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private MarkLogicConverter converter;
    private PersistenceExceptionTranslator exceptionTranslator;
    private DatabaseClient client;

    public MarkLogicTemplate(DatabaseClient client) {
        this(client, null);
    }

    public MarkLogicTemplate(DatabaseClient client, MarkLogicConverter converter) {
        this.client = client;
        this.converter = converter == null ? getDefaultConverter() : converter;
        this.exceptionTranslator = new MarkLogicExceptionTranslator();
    }

    private static final MarkLogicConverter getDefaultConverter() {
        MappingMarkLogicConverter converter = new MappingMarkLogicConverter(new MarkLogicMappingContext());
        converter.afterPropertiesSet();
        return converter;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    <T> T execute(DocumentCallback<T> action) {
        // TODO: Transactional stuff?

        try {
            return action.doInMarkLogic(client.newDocumentManager());
        } catch (RuntimeException e) {
            throw potentiallyConvertRuntimeException(e, exceptionTranslator);
        }
    }

    <T> T executeQuery(QueryCallback<T> action) {
        try {
            return action.doInMarkLogic(client.newQueryManager());
        } catch (RuntimeException e) {
            throw potentiallyConvertRuntimeException(e, exceptionTranslator);
        }
    }

    @Override
    public StructuredQueryBuilder queryBuilder() {
        return queryBuilder((String) null);
    }

    @Override
    public StructuredQueryBuilder queryBuilder(String options) {
        return new StructuredQueryBuilder(options);
    }

    @Override
    public <T> PojoQueryBuilder<T> queryBuilder(Class<T> entityClass) {
        // TODO: If wrapping is configured then we want to wrap the queries
        return new PojoQueryBuilderImpl<>(entityClass, false);
    }

    @Override
    public StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query) {
        return sortQuery(sort, query, null);
    }

    @Override
    public <T> StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query, Class<T> entityClass) {
        if (sort != null && sort.iterator().hasNext()) {
            CombinedQueryDefinition queryDefinition;

            if (query instanceof CombinedQueryDefinition)
                queryDefinition = (CombinedQueryDefinition) query;
            else
                queryDefinition = new CombinedQueryDefinitionBuilder(query);

            final MarkLogicPersistentEntity entity = entityClass != null ? converter.getMappingContext().getPersistentEntity(entityClass) : null;

            sort.forEach(order -> {
                StringBuilder options = new StringBuilder();
                String propertyName = order.getProperty();
                String direction = asMLSort(order.getDirection());

                options.append("<sort-order direction='").append(direction).append("'>");

                StringBuilder path = new StringBuilder();
                // If there is an entity then we can determine the configuration of the index from it, otherwise we just
                // default to a path index. An error will be thrown by the database if the index is not created, though.
                if (entity != null) {
                    MarkLogicPersistentProperty property = (MarkLogicPersistentProperty) entity.getPersistentProperty(propertyName);
                    if (!StringUtils.isEmpty(property.getPath())) path.append(property.getPath());
                } else {
                    path.append("/").append(order.getProperty());
                }

                if (path.length() > 0) {
                    options.append("<path-index>").append(path).append("</path-index>");
                } else {
                    options.append("<element ns='' name='").append(propertyName).append("'/>");
                }
                options.append("</sort-order>");

                queryDefinition.with(options.toString());
            });

            return queryDefinition;
        } else {
            return query;
        }
    }

    private String asMLSort(Sort.Direction direction) {
        if (Sort.Direction.DESC.equals(direction)) {
            return "descending";
        } else {
            return "ascending";
        }
    }

    @Override
    public StructuredQueryDefinition termQuery(String term, StructuredQueryDefinition query) {
        if (query instanceof CombinedQueryDefinition)
            return ((CombinedQueryDefinition) query).term(term);
        else
            return new CombinedQueryDefinitionBuilder(query).term(term);
    }

    @Override
    public Object write(Object entity) {
        return write(entity, null);
    }

    @Override
    public Object write(final Object entity, String... collections) {
        // TODO: Safer...
        return write(singletonList(entity), collections).get(0);
    }

    @Override
    public <T> List<T> write(List<T> entities) {
        return write(entities, null);
    }

    @Override
    public <T> List<T> write(List<T> entities, String... collections) {

        // TODO: Versioning?
        // TODO: ID generation/updating?

        List<DocumentDescriptor> docs =
                entities.stream()
                        .map(entity -> {
                            final DocumentDescriptor doc = new DocumentDescriptor();
                            this.converter.write(entity, doc);
                            return doc;
                        })
                        .collect(Collectors.toList());

        return execute((manager) -> {
            if (!docs.isEmpty()) {
                DocumentWriteSet writeSet = manager.newWriteSet();
                for (DocumentDescriptor doc : docs) {
                    if (collections != null && collections.length > 0) {
                        doc.setMetadata(doc.getMetadata().withCollections(collections));
                    }

                    writeSet.add(doc.getUri(), doc.getMetadata(), doc.getContent());
                }
                manager.write(writeSet);
            }
            return entities;
        });
    }

    @Override
    public DocumentRecord read(Object id) {
        return null;
    }

    @Override
    public <T> T read(Object id, Class<T> entityClass) {
        return read(singletonList(id), entityClass).get(0);
    }

    @Override
    public List<DocumentRecord> read(List<?> ids) {
        return null;
    }

    @Override
    public <T> List<T> read(List<?> ids, Class<T> entityClass) {
        final List<String> uris = converter.getDocumentUris(ids, entityClass);

        return execute((manager) -> {
            manager.setPageLength(uris.size());
            DocumentPage page = manager.read(uris.toArray(new String[0]));

            if ( page == null || page.hasNext() == false ) {
                throw new DataRetrievalFailureException("Could not find documents of type " + entityClass.getName() + " with ids: " + ids);
            }

            return toEntityList(entityClass, page);
        });
    }

    @Override
    public List<DocumentRecord> search(StructuredQueryDefinition query) {
        return null;
    }

    @Override
    public <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass) {
        return search(query, 0, -1, entityClass).getContent();
    }

    @Override
    public <T> List<T> search(Class<T> entityClass) {
        // TODO: Is this dangerous?
        return search(null, 0, Integer.MAX_VALUE, entityClass).getContent();
    }

    @Override
    public Page<DocumentRecord> search(StructuredQueryDefinition query, int start) {
        return null;
    }

    @Override
    public <T> Page<T> search(StructuredQueryDefinition query, int start, Class<T> entityClass) {
        return search(query, start, -1, entityClass);
    }

    @Override
    public Page<DocumentRecord> search(StructuredQueryDefinition query, int start, int length) {
        return null;
    }

    @Override
    public <T> Page<T> search(StructuredQueryDefinition query, int start, int length, Class<T> entityClass) {
        // TODO: Sorting here?
        return execute((manager) -> {
            if (length >= 0) manager.setPageLength(length);

            // TODO: Is here the best place to check this?
            StructuredQueryDefinition finalQuery;
            if (query != null) finalQuery = query; else finalQuery = queryBuilder().and();

            // MarkLogic uses 1-based indexing, whereas the rest of us use 0-based, so convert and query
            DocumentPage docPage = manager.search(converter.wrapQuery(finalQuery, entityClass), start + 1);
            List<T> results = toEntityList(entityClass, docPage);

            return new PageImpl<>(results, new ChunkRequest(start, (int) manager.getPageLength()), docPage.getTotalSize());
        });
    }

    @Override
    public boolean exists(Object id) {
        final List<String> uris = converter.getDocumentUris(singletonList(id));
        return execute((manager) -> uris.stream().anyMatch(uri -> manager.exists(uri) != null));
    }

    @Override
    public <T> boolean exists(StructuredQueryDefinition query, Class<T> entityClass) {
        Page<T> page = search(query, 0, 0, entityClass);
        return page.getTotalElements() > 0;
    }

    @Override
    public long count(String... collections) {
        return count(queryBuilder().collection(collections));
    }

    @Override
    public <T> long count(Class<T> entityClass) {
        return count(null, entityClass);
    }

    @Override
    public long count(StructuredQueryDefinition query) {
        return count(query, null);
    }

    @Override
    public <T> long count(StructuredQueryDefinition query, Class<T> entityClass) {
        return search(query, 0, 0, entityClass).getTotalElements();
    }

    @Override
    public void deleteById(Object id) {
        deleteAll(singletonList(id), null);
    }

    @Override
    public <T> void deleteById(Object id, Class<T> entityClass) {
        deleteAll(singletonList(id), entityClass);
    }

    @Override
    public <T> void delete(List<T> entities, Class<T> entityClass) {
        List<String> uris =
                entities.stream()
                        .map(entity -> {
                            final DocumentDescriptor doc = new DocumentDescriptor();
                            this.converter.write(entity, doc);
                            return doc.getUri();
                        })
                        .collect(Collectors.toList());

        execute((manager) -> {
            manager.delete(uris.toArray(new String[0]));
            return null;
        });
    }

    @Override
    public void deleteAll(List<?> ids) {
        deleteAll(ids, null);
    }

    @Override
    public <T> void deleteAll(List<?> ids, Class<T> entityClass) {
        final List<String> uris = converter.getDocumentUris(ids, entityClass);

        execute((manager) -> {
            manager.delete(uris.toArray(new String[0]));
            return null;
        });
    }

    @Override
    public void deleteAll(String... collections) {
        executeQuery((manager) -> {
            DeleteQueryDefinition deleteQuery = manager.newDeleteDefinition();
            deleteQuery.setCollections(collections);
            manager.delete(deleteQuery);
            return null;
        });
    }

    @Override
    public <T> void deleteAll(Class<T> entityClass) {
        // TODO: If no entity class is supplied do we just deleteById directory "/"?  Is that safe/good?
        if (entityClass == null)
            throw new InvalidDataAccessApiUsageException("Entity class is required to determine scope of deleteById");

        // If the type is not stored in a collection there is no "quick" way to mass deleteById other than directory.  Can't easily do it by document properties
        // TODO: Maybe uri lexicon for deleting without a collection, but can't assume it is on, though
        MarkLogicPersistentEntity entity = converter.getMappingContext().getPersistentEntity(entityClass);
        if (entity == null || entity.getTypePersistenceStrategy() != TypePersistenceStrategy.COLLECTION)
            throw new InvalidDataAccessApiUsageException(String.format("Cannot determine deleteById scope for entity of type %s", entityClass.getName()));

        deleteAll(entityClass.getSimpleName());
    }

    @Override
    public MarkLogicConverter getConverter() {
        return converter;
    }

    private static RuntimeException potentiallyConvertRuntimeException(RuntimeException ex,
                                                                       PersistenceExceptionTranslator exceptionTranslator) {
        RuntimeException resolved = exceptionTranslator.translateExceptionIfPossible(ex);
        return resolved == null ? ex : resolved;
    }

    private <T> List<T> toEntityList(Class<T> entityClass, DocumentPage page) {
        final List<T> results = new ArrayList<>();
        page.iterator().forEachRemaining(item -> {
            results.add(converter.read(entityClass, new DocumentDescriptor(item)));
        });
        return results;
    }
}
