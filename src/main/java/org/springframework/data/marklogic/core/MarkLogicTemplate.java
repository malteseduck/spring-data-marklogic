package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.Transaction;
import com.marklogic.client.document.*;
import com.marklogic.client.impl.DatabaseClientImpl;
import com.marklogic.client.impl.PojoQueryBuilderImpl;
import com.marklogic.client.impl.RESTServices;
import com.marklogic.client.pojo.PojoQueryBuilder;
import com.marklogic.client.query.DeleteQueryDefinition;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.TransactionHolder;
import org.springframework.data.marklogic.core.convert.MappingMarkLogicConverter;
import org.springframework.data.marklogic.core.convert.MarkLogicConverter;
import org.springframework.data.marklogic.core.mapping.DocumentDescriptor;
import org.springframework.data.marklogic.core.mapping.*;
import org.springframework.data.marklogic.domain.ChunkRequest;
import org.springframework.data.marklogic.repository.query.CombinedQueryDefinition;
import org.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder;
import org.springframework.data.marklogic.repository.query.SelectedMode;
import org.springframework.data.marklogic.repository.query.convert.DefaultMarkLogicQueryConversionService;
import org.springframework.data.marklogic.repository.query.convert.QueryConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class MarkLogicTemplate implements MarkLogicOperations, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MarkLogicTemplate.class);

    private ApplicationContext applicationContext;
    private MarkLogicConverter converter;
    private QueryConversionService queryConversionService;
    private PersistenceExceptionTranslator exceptionTranslator;
    private DatabaseClient client;
    private RestTemplate restTemplate;
    private RESTServices services;
    private StructuredQueryBuilder qb = new StructuredQueryBuilder();

    public MarkLogicTemplate() {
        // Default to local host DB on default port if nothing is specified
        this(DatabaseClientFactory.newClient("localhost", 8000), null);
    }

    public MarkLogicTemplate(DatabaseClient client) {
        this(client, null);
    }

    public MarkLogicTemplate(DatabaseClient client, MarkLogicConverter converter) {
        this(client, converter, null);
    }

    public MarkLogicTemplate(DatabaseClient client, MarkLogicConverter converter, QueryConversionService queryConversionService) {
        Assert.notNull(client, "A database client object is required!");
        this.client = client;
        this.converter = converter == null ? getDefaultConverter() : converter;
        this.queryConversionService = queryConversionService == null ? getDefaultQueryConverter() : queryConversionService;
        this.exceptionTranslator = new MarkLogicExceptionTranslator();

        // Create a RestTemplate instance for use directly against the REST API because there are some things that aren't fully supported in the client
        HttpClient httpClient = HttpClientBuilder
                .create()
                .setDefaultCredentialsProvider(provider())
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        this.restTemplate = new RestTemplate(factory);
        services = ((DatabaseClientImpl) client).getServices();
    }

    private CredentialsProvider provider() {
        // Get the username/password from the database client's security context
        DatabaseClientFactory.SecurityContext securityContext = client.getSecurityContext();
        String username;
        String password;
        if (securityContext instanceof DatabaseClientFactory.BasicAuthContext) {
            username = ((DatabaseClientFactory.BasicAuthContext) securityContext).getUser();
            password = ((DatabaseClientFactory.BasicAuthContext) securityContext).getPassword();
        } else if (securityContext instanceof DatabaseClientFactory.DigestAuthContext) {
            username = ((DatabaseClientFactory.DigestAuthContext) securityContext).getUser();
            password = ((DatabaseClientFactory.DigestAuthContext) securityContext).getPassword();
        } else {
            throw new IllegalArgumentException("Currently only BasicAuthContext and DigestAuthContext are supported");
        }

        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
        provider.setCredentials(new AuthScope(client.getHost(), AuthScope.ANY_PORT, AuthScope.ANY_REALM), credentials);
        return provider;
    }

    private static final MarkLogicConverter getDefaultConverter() {
        MappingMarkLogicConverter converter = new MappingMarkLogicConverter(new MarkLogicMappingContext());
        converter.afterPropertiesSet();
        return converter;
    }

    private static final QueryConversionService getDefaultQueryConverter() {
        DefaultMarkLogicQueryConversionService converter = new DefaultMarkLogicQueryConversionService();
        return converter;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private Transaction getCurrentTransaction() {
        TransactionHolder holder = (TransactionHolder) TransactionSynchronizationManager.getResource(client);
        Transaction tx = null;

        if (holder != null) tx = holder.getTransaction();
        return tx;
    }

    @Override
    public <T> T execute(DocumentCallback<T> action) {
        try {
            return action.doWithDocumentManager(client.newDocumentManager(), getCurrentTransaction());
        } catch (RuntimeException e) {
            throw potentiallyConvertRuntimeException(e, exceptionTranslator);
        }
    }

    @Override
    public <T> T executeWithClient(ClientCallback<T> action) {
        try {
            return action.doWithClient(client, getCurrentTransaction());
        } catch (RuntimeException e) {
            throw potentiallyConvertRuntimeException(e, exceptionTranslator);
        }
    }

    @Override
    public <T> T executeQuery(QueryCallback<T> action) {
        try {
            return action.doWithQueryManager(client.newQueryManager(), getCurrentTransaction());
        } catch (RuntimeException e) {
            throw potentiallyConvertRuntimeException(e, exceptionTranslator);
        }
    }

    @Override
    public <T> PojoQueryBuilder<T> qb(Class<T> entityClass) {
        MarkLogicPersistentEntity entity = converter.getMappingContext().getPersistentEntity(entityClass);
        if (entity == null)
            throw new InvalidDataAccessApiUsageException(String.format("Cannot determine entity type from %s", entityClass.getName()));

        // TODO: Actually support this configuration.  Can be used here, in the mapper configuration, and in sorting index query stuff
//        if (entity.getObjectWrapping() == ObjectWrapping.FULL_CLASS)
//            return new PojoQueryBuilderImpl<T>(entityClass, true);
//        else
            return new PojoQueryBuilderImpl<>(entityClass, false);
    }

    @Override
    public StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query) {
        return sortQuery(sort, query, null);
    }

    @Override
    public <T> StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query, Class<T> entityClass) {
        if (sort != null && sort.iterator().hasNext()) {
            CombinedQueryDefinition combined = CombinedQueryDefinitionBuilder.combine(query);

            final MarkLogicPersistentEntity entity = entityClass != null ? converter.getMappingContext().getPersistentEntity(entityClass) : null;

            // TODO: Move this to the combined query builder?
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

                combined.options(options.toString());
            });

            return combined;
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
        return CombinedQueryDefinitionBuilder
                .combine(query)
                .term(term);
    }

    @Override
    public StructuredQueryDefinition limitingQuery(int limit, StructuredQueryDefinition query) {
        return CombinedQueryDefinitionBuilder
                .combine(query)
                .limit(limit);
    }

    @Override
    public StructuredQueryDefinition extractQuery(List<String> paths, SelectedMode mode, StructuredQueryDefinition query) {
        if (paths != null && paths.size() > 0) {
            SelectedMode modeToUse = mode != null ? mode : SelectedMode.HIERARCHICAL;

            return CombinedQueryDefinitionBuilder
                    .combine(query)
                    .extracts(paths, modeToUse);
        } else {
            return query;
        }
    }

    @Override
    public void configure(Resource configuration) throws IOException {
        String json = new String(Files.readAllBytes(Paths.get(configuration.getURI())));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // TODO: Make management port configurable? Or try using services to post?
        // Need to use the management port for configuring the database
        URI configUri = new UriTemplate("http://{host}:{port}/manage/v2/databases/Documents/properties").expand(client.getHost(), 8002);

        restTemplate.exchange(
                configUri,
                HttpMethod.PUT,
                new HttpEntity<>(json, headers),
                Void.class
        );
    }

    @Override
    public <T> T write(T entity) {
        return write(entity, (String[]) null);
    }

    @Override
    public <T> T write(final T entity, String... collections) {
        // TODO: Safer...
        return write(entity, null, collections);
    }

    @Override
    public <T> T write(T entity, ServerTransform transform) {
        return write(entity, transform, (String[]) null);
    }

    @Override
    public <T> T write(final T entity, ServerTransform transform, String... collections) {
        return write(singletonList(entity), transform, collections).get(0);
    }

    @Override
    public <T> List<T> write(List<T> entities) {
        return write(entities, null, (String[]) null);
    }

    @Override
    public <T> List<T> write(List<T> entities, String... collections) {
        return write(entities, null, collections);
    }

    @Override
    public <T> List<T> write(List<T> entities, ServerTransform transform) {
        return write(entities, transform);
    }

    @Override
    public <T> List<T> write(List<T> entities, ServerTransform transform, String... collections) {
        // TODO: Versioning?
        List<DocumentDescriptor> docs =
                entities.stream()
                        .map(entity -> {
                            final DocumentDescriptor doc = new DocumentDescriptor();
                            this.converter.write(entity, doc);
                            return doc;
                        })
                        .collect(Collectors.toList());

        return execute((manager, transaction) -> {
            if (!docs.isEmpty()) {
                DocumentWriteSet writeSet = manager.newWriteSet();
                for (DocumentDescriptor doc : docs) {
                    if (collections != null && collections.length > 0) {
                        // If collections are specified then those are the ones that will be used - we expect things to be how we specify
                        doc.getMetadata().getCollections().clear();
                        doc.setMetadata(doc.getMetadata().withCollections(collections));
                    }

                    if (doc.getUri() != null) {
                        writeSet.add(doc.getUri(), doc.getMetadata(), doc.getContent());
                    } else {
                        // TODO: How to use uri template with a write set?  Is there a way?
                        DocumentUriTemplate template = manager.newDocumentUriTemplate(doc.getFormat().toString());
//                        com.marklogic.client.document.DocumentDescriptor desc = manager.create(template, doc.getMetadata(), doc.getContent());
                        writeSet.add((String) null, doc.getMetadata(), doc.getContent());
                    }
                }
                manager.write(writeSet, transform, transaction);
            }
            return entities;
        });
    }

//    @Override
//    public DocumentRecord read(Object id) {
//        return null;
//    }
//
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

        return execute((manager, transaction) -> {
            manager.setPageLength(uris.size());
            DocumentPage page = manager.read(transaction, uris.toArray(new String[0]));

            if ( page == null || page.hasNext() == false ) {
                throw new DataRetrievalFailureException("Could not find documents of type " + entityClass.getName() + " with ids: " + ids);
            }

            return toEntityList(entityClass, page);
        });
    }

    @Override
    public List<DocumentRecord> search(StructuredQueryDefinition query) {
        DocumentPage page = search(query, 0, -1);
        final List<DocumentRecord> results = new ArrayList<>();
        page.iterator().forEachRemaining(results::add);
        return results;
    }

    @Override
    public <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass) {
        return search(query, 0, -1, entityClass).getContent();
    }

    @Override
    public <T> T searchOne(StructuredQueryDefinition query, Class<T> entityClass) {
        List<T> results = search(query, 0, 1, entityClass).getContent();
        return results.get(0);
    }

    @Override
    public <T> List<T> search(Class<T> entityClass) {
        // TODO: Is this dangerous?
        return search(null, 0, Integer.MAX_VALUE, entityClass).getContent();
    }

    @Override
    public DocumentPage search(StructuredQueryDefinition query, int start) {
        return search(query, start, -1);
    }

    @Override
    public <T> Page<T> search(StructuredQueryDefinition query, int start, Class<T> entityClass) {
        return search(query, start, -1, entityClass);
    }

    @Override
    public DocumentPage search(StructuredQueryDefinition query, int start, int length) {
        return execute((manager, transaction) -> {
            if (length >= 0) manager.setPageLength(length);

            QueryDefinition finalQuery = converter.wrapQuery(query, null);

            return manager.search(finalQuery, start + 1, transaction);
        });
    }

    @Override
    public <T> Page<T> search(StructuredQueryDefinition query, int start, int length, Class<T> entityClass) {
        return execute((manager, transaction) -> {
            if (length >= 0) manager.setPageLength(length);

            QueryDefinition finalQuery = converter.wrapQuery(query, entityClass);
            DocumentPage docPage = manager.search(finalQuery, start + 1, transaction);

            List<T> results = toEntityList(entityClass, docPage);
            return new PageImpl<>(results, new ChunkRequest(start, (int) manager.getPageLength()), docPage.getTotalSize());
        });
    }

    @Override
    public InputStream stream(StructuredQueryDefinition query) {
        return stream(query,0, Integer.MAX_VALUE, null);
    }

    @Override
    public <T> InputStream stream(StructuredQueryDefinition query, Class<T> entityClass) {
        return stream(query,0, Integer.MAX_VALUE, entityClass);
    }

    @Override
    public InputStream stream(StructuredQueryDefinition query, int start) {
        return stream(query,start, Integer.MAX_VALUE, null);
    }

    @Override
    public <T> InputStream stream(StructuredQueryDefinition query, int start, Class<T> entityClass) {
        return stream(query, start, Integer.MAX_VALUE, entityClass);
    }

    @Override
    public InputStream stream(StructuredQueryDefinition query, int start, int length) {
        return stream(query,start, length, null);
    }

    @Override
    public <T> InputStream stream(StructuredQueryDefinition query, int start, int length, Class<T> entityClass) {
        return execute((manager, transaction) -> {
            if (length >= 0) manager.setPageLength(length);

            QueryDefinition finalQuery = converter.wrapQuery(query, entityClass);

            DocumentPage page = manager.search(finalQuery, start + 1, transaction);

            Enumeration<? extends InputStream> results = Collections.enumeration(
                    StreamSupport.stream(page.spliterator(), true)
                            .map(record -> record.getContentAs(InputStream.class))
                            .collect(Collectors.toList()));

            return new SequenceInputStream(results);
        });
    }

    @Override
    public boolean exists(Object id) {
        final List<String> uris = converter.getDocumentUris(singletonList(id));
        return execute((manager, transaction) -> uris.stream().anyMatch(uri -> manager.exists(uri, transaction) != null));
    }

    @Override
    public <T> boolean exists(StructuredQueryDefinition query, Class<T> entityClass) {
        Page<T> page = search(query, 0, 0, entityClass);
        return page.getTotalElements() > 0;
    }

    @Override
    public long count(String... collections) {
        return count(qb.collection(collections));
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
    public <T> void delete(List<T> entities) {
        List<String> uris =
                entities.stream()
                        .map(entity -> {
                            final DocumentDescriptor doc = new DocumentDescriptor();
                            this.converter.write(entity, doc);
                            return doc.getUri();
                        })
                        .collect(Collectors.toList());

        execute((manager, transaction) -> {
            manager.delete(transaction, uris.toArray(new String[0]));
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

        execute((manager, transaction) -> {
            manager.delete(transaction, uris.toArray(new String[0]));
            return null;
        });
    }

    @Override
    public void deleteAll(String... collections) {
        executeQuery((manager, transaction) -> {
            // The REST API only supports deleting one collection at a time, so we need to send a request for each
            asList(collections).forEach(collection -> {
                DeleteQueryDefinition deleteQuery = manager.newDeleteDefinition();
                deleteQuery.setCollections(collection);
                manager.delete(deleteQuery, transaction);
            });
            return null;
        });
    }

    @Override
    public <T> void deleteAll(Class<T> entityClass) {
        // TODO: If no entity class is supplied do we just deleteById directory "/"?  Is that safe/good?
        if (entityClass == null) {
            throw new InvalidDataAccessApiUsageException("Entity class is required to determine scope of deleteById");
        }

        // If the type is not stored in a collection there is no "quick" way to mass deleteById other than directory.
        // Can't easily do it by document properties
        // TODO: Maybe uri lexicon for deleting without a collection, but can't assume it is on, though
        MarkLogicPersistentEntity entity = converter.getMappingContext().getPersistentEntity(entityClass);
        if (entity == null || entity.getTypePersistenceStrategy() != TypePersistenceStrategy.COLLECTION) {
            throw new InvalidDataAccessApiUsageException(String.format("Cannot determine deleteById scope for entity of type %s", entityClass.getName()));
        }

        deleteAll(entityClass.getSimpleName());
    }

    @Override
    public MarkLogicConverter getConverter() {
        return converter;
    }

    public QueryConversionService getQueryConversionService() {
        return queryConversionService;
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
