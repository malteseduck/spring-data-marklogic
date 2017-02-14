package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentWriteSet;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.marklogic.core.convert.MappingMarkLogicConverter;
import org.springframework.data.marklogic.core.convert.MarkLogicConverter;
import org.springframework.data.marklogic.core.mapping.DocumentDescriptor;
import org.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

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

    <T> T execute(MarkLogicCallback<T> action) {
        // TODO: Transactional stuff?
        // TODO: Error handling?

        try {
            // TODO: Which document manager - should this be decided somewhere else?
            // TODO: Do we "cache" these managers?  The problem is with setting pagelength and other attributes, though, so maybe only if there is a need to optimize
            // TODO: For cache, can store "defaults" for settable properties, and reset them back?
            return action.doInMarkLogic(this.client.newJSONDocumentManager());
        } catch (RuntimeException e) {
            throw potentiallyConvertRuntimeException(e, exceptionTranslator);
        }
    }

    @Override
    public Object write(Object entity) {
        return write(entity, null);
    }

    @Override
    public Object write(final Object entity, String... collections) {
        // TODO: Safer...
        return write(asList(entity), collections).get(0);
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
    public Object read(Object id) {
        // TODO: With no entity class do we just try for both XML and JSON uris, then use _class to get type?
        return null;
    }

    @Override
    public <T> T read(Object id, Class<T> entityClass) {
        return read(asList(id), entityClass).get(0);
    }

    @Override
    public List<?> read(List<? extends Object> ids) {
        // TODO: With no entity class do we just try for both XML and JSON uris, then use _class to get type?
        return null;
    }

    @Override
    public <T> List<T> read(List<? extends Object> ids, Class<T> entityClass) {
        final List<String> uris = ids.stream()
                .map(id -> converter.getDocumentUri(id, entityClass))
                .collect(Collectors.toList());

        return execute((manager) -> {
            manager.setPageLength(uris.size());
            DocumentPage page = manager.read(uris.toArray(new String[0]));

            if ( page == null || page.hasNext() == false ) {
                throw new DataRetrievalFailureException("Could not find documents of type " + entityClass.getName() + " with ids: " + ids);
            }

            final List<T> results = new ArrayList<>();
            page.iterator().forEachRemaining(item -> {
                results.add(converter.read(entityClass, new DocumentDescriptor(item)));
            });
            return results;
        });
    }

    @Override
    public List<?> search(StructuredQueryDefinition query) {
        return null;
    }

    @Override
    public <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass) {
        return null;
    }

    @Override
    public boolean exists(Object id) {
        return false;
    }

    @Override
    public long count(String... collections) {
        return 0;
    }

    @Override
    public <T> long count(Class<T> entityClass) {
        return 0;
    }

    @Override
    public <T> long count(StructuredQueryDefinition query) {
        return 0;
    }

    @Override
    public <T> long count(StructuredQueryDefinition query, Class<T> entityClass) {
        return 0;
    }

    @Override
    public void delete(Object id) {
    }

    @Override
    public void delete(List<? extends Object> ids) {
    }

    @Override
    public void deleteAll(String collection) {
    }

    @Override
    public <T> void deleteAll(Class<T> entityClass) {
    }

    private static RuntimeException potentiallyConvertRuntimeException(RuntimeException ex,
                                                                       PersistenceExceptionTranslator exceptionTranslator) {
        RuntimeException resolved = exceptionTranslator.translateExceptionIfPossible(ex);
        return resolved == null ? ex : resolved;
    }

}
