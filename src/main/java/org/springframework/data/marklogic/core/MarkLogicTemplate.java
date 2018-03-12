package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.Transaction;
import com.marklogic.client.document.*;
import com.marklogic.client.impl.DatabaseClientImpl;
import com.marklogic.client.impl.PojoQueryBuilderImpl;
import com.marklogic.client.impl.RESTServices;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.SearchHandle;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.io.ValuesHandle;
import com.marklogic.client.pojo.PojoQueryBuilder;
import com.marklogic.client.query.*;
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
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.TransactionHolder;
import org.springframework.data.marklogic.core.convert.JacksonMarkLogicConverter;
import org.springframework.data.marklogic.core.convert.MarkLogicConverter;
import org.springframework.data.marklogic.core.convert.QueryMapper;
import org.springframework.data.marklogic.core.mapping.DocumentDescriptor;
import org.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.TypePersistenceStrategy;
import org.springframework.data.marklogic.domain.ChunkRequest;
import org.springframework.data.marklogic.domain.facets.FacetedPage;
import org.springframework.data.marklogic.repository.query.CombinedQueryDefinition;
import org.springframework.data.marklogic.repository.query.convert.DefaultMarkLogicQueryConversionService;
import org.springframework.data.marklogic.repository.query.convert.QueryConversionService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

public class MarkLogicTemplate implements MarkLogicOperations, ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(MarkLogicTemplate.class);

    private ApplicationContext applicationContext;
    private MarkLogicConverter converter;
    private QueryConversionService queryConversionService;
    private PersistenceExceptionTranslator exceptionTranslator;
    private DatabaseClient client;
    private RestTemplate restTemplate;
    private RESTServices services;
    private QueryMapper queryMapper;
    private StructuredQueryBuilder qb = new StructuredQueryBuilder();

    /**
     * Create a template interface using the specified database client and the default entity converter and query conversion
     * service.
     *
     * @param client The MarkLogic database client created from @see {@link DatabaseClientFactory}
     */
    public MarkLogicTemplate(DatabaseClient client) {
        this(client, null);
    }

    /**
     * Create a template interface using the specified database client and the default query conversion service.
     *
     * @param client The MarkLogic database client created from DatabaseClientFactory.
     * @param converter A custom entity converter.
     */
    public MarkLogicTemplate(DatabaseClient client, MarkLogicConverter converter) {
        this(client, converter, null);
    }

    /**
     * Create a template interface using the specified database client.  Also provide a custom convert that handles the
     * translation of entities between Java and MarkLogic.  You can also provide a custom query conversion service that will
     * handle the conversion of different types used in finder queries and @Query queries.
     *
     * @param client The MarkLogic database client created from @see {@link DatabaseClientFactory}.
     * @param converter A custom entity converter.
     * @param queryConversionService A custom query object conversion service.
     */
    public MarkLogicTemplate(DatabaseClient client, MarkLogicConverter converter, QueryConversionService queryConversionService) {
        Assert.notNull(client, "A database client object is required!");
        this.client = client;
        this.converter = converter == null ? getDefaultConverter() : converter;
        this.queryConversionService = queryConversionService == null ? getDefaultQueryConverter() : queryConversionService;
        this.exceptionTranslator = new MarkLogicExceptionTranslator();
        this.queryMapper = new QueryMapper(this.converter);

        // Create a RestTemplate instance for use directly against the REST API because there are some things that
        // aren't fully supported in the client
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

    private static MarkLogicConverter getDefaultConverter() {
        JacksonMarkLogicConverter converter = new JacksonMarkLogicConverter(new MarkLogicMappingContext());
        converter.afterPropertiesSet();
        return converter;
    }

    private static QueryConversionService getDefaultQueryConverter() {
        return new DefaultMarkLogicQueryConversionService();
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
        return new PojoQueryBuilderImpl<>(entityClass, false);
    }

    @Override
    public StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query) {
        return sortQuery(sort, query, null);
    }

    @Override
    public <T> StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query, Class<T> entityClass) {
        if (sort != null && sort.iterator().hasNext()) {
            return combine(query)
                    .type(entityClass)
                    .sort(sort);
        } else {
            return query;
        }
    }

    @Override
    public StructuredQueryDefinition termQuery(String term, StructuredQueryDefinition query) {
        return combine(query).term(term);
    }

    private String asMLSort(Sort.Direction direction) {
        if (Sort.Direction.DESC.equals(direction)) {
            return "descending";
        } else {
            return "ascending";
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
        return write(entities, transform, (String[]) null);
    }

    @Override
    public <T> List<T> write(List<T> entities, ServerTransform transform, String... collections) {
        ServerTransform writeTransform = !entities.isEmpty() && transform == null
                ? queryMapper.getWriteTransform(entities.get(0).getClass())
                : transform;

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
                // TODO: Do we have a case where we are saving entities of different types all in the same operation?
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
                        DocumentUriTemplate template = manager.newDocumentUriTemplate(doc.getFormat().toString());
                        writeSet.add((String) null, doc.getMetadata(), doc.getContent());
                    }
                }
                manager.write(writeSet, writeTransform, transaction);
            }
            return entities;
        });
    }

    @Override
    public <T> T read(Object id, Class<T> entityClass) {
        List<T> results = read(singletonList(id), entityClass);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<DocumentRecord> read(List<?> uris) {
        return execute((manager, transaction) -> {
            manager.setPageLength(uris.size());
            DocumentPage page = manager.read(transaction, uris.toArray(new String[0]));
            return toRecordList(page);
        });
    }

    @Override
    public <T> List<T> read(List<?> ids, Class<T> entityClass) {
        // TODO: Do we have a case where we are saving entities of different types all in the same operation?
        ServerTransform readTransform = queryMapper.getReadTransform(entityClass);
        final List<String> uris = converter.getDocumentUris(ids, entityClass);

        return execute((manager, transaction) -> {
            manager.setPageLength(uris.size());
            DocumentPage page = manager.read(readTransform, transaction, uris.toArray(new String[0]));
            return toEntityList(entityClass, page);
        });
    }

    @Override
    public List<DocumentRecord> search(StructuredQueryDefinition query) {
        DocumentPage page = search(query, 0, Integer.MAX_VALUE);
        final List<DocumentRecord> results = new ArrayList<>();
        page.iterator().forEachRemaining(results::add);
        return results;
    }

    @Override
    public <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass) {
        return search(query, 0, Integer.MAX_VALUE, entityClass)
                .getContent();
    }

    @Override
    public <T> T searchOne(StructuredQueryDefinition query, Class<T> entityClass) {
        List<T> results = search(query, 0, 1, entityClass)
                .getContent();
        return results == null || results.isEmpty() ? null : results.get(0);
    }

    /**
     * Query for a list of of documents of the specified type.  As this returns all the documents for the type it can
     * potentially take a long time to resolve pulling all of them back from the database.  The query will be fast, but
     * transmitting all the records could time out if the result set is sufficiently large.  It is recommended that you
     * page your results.
     *
     * This method is mainly created to accommodate creating an implementation for the
     * {@link org.springframework.data.repository.CrudRepository#findAll()} method.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int, Class)
     */
    public <T> List<T> search(Class<T> entityClass) {
        return search(qb.and(), 0, Integer.MAX_VALUE, entityClass)
                .getContent();
    }

    @Override
    public DocumentPage search(StructuredQueryDefinition query, long start) {
        return search(query, start, Integer.MAX_VALUE);
    }

    @Override
    public <T> Page<T> search(StructuredQueryDefinition query, long start, Class<T> entityClass) {
        return search(query, start, Integer.MAX_VALUE, entityClass);
    }

    @Override
    public DocumentPage search(StructuredQueryDefinition query, long start, int length) {
        return execute((manager, transaction) -> {
            if (length >= 0) manager.setPageLength(length);

            QueryDefinition finalQuery = converter.wrapQuery(query, null);

            return manager.search(finalQuery, start + 1, transaction);
        });
    }

    @Override
    public DocumentPage search(StructuredQueryDefinition query, Pageable pageable) {
        return search(
                combine(query).sort(pageable.getSort()),
                pageable.getOffset(),
                pageable.getPageSize());
    }

    @Override
    public <T> Page<T> search(StructuredQueryDefinition query, long start, int limit, Class<T> entityClass) {
        return execute((manager, transaction) -> {
            if (limit >= 0) manager.setPageLength(limit);

            QueryDefinition finalQuery = queryMapper.getMappedQuery(query, entityClass);
            DocumentPage docPage = manager.search(finalQuery, start + 1, transaction);

            List<T> results = toEntityList(entityClass, docPage);
            int length = (int) Math.min(manager.getPageLength(), docPage.getTotalSize());
            return new PageImpl<>(results, ChunkRequest.of(start, length), docPage.getTotalSize());
        });
    }

    @Override
    public <T> Page<T> search(StructuredQueryDefinition query, Pageable pageable, Class<T> entityClass) {
        return search(
                combine(query).sort(pageable.getSort()),
                pageable.getOffset(),
                pageable.getPageSize(),
                entityClass);
    }

    @Override
    public <T> FacetedPage<T> facetedSearch(StructuredQueryDefinition query, long start, Class<T> entityClass) {
        return facetedSearch(query, start, -1, entityClass);
    }

    @Override
    public <T> FacetedPage<T> facetedSearch(StructuredQueryDefinition query, long start, int limit, Class<T> entityClass) {
        return execute((manager, transaction) -> {
            if (limit >= 0) manager.setPageLength(limit);

            manager.setSearchView(QueryManager.QueryView.FACETS);
            SearchHandle results = new SearchHandle();
            QueryDefinition finalQuery = queryMapper.getMappedQuery(query, entityClass);

            DocumentPage docPage = manager.search(finalQuery, start + 1, results, transaction);

            List<T> entities = toEntityList(entityClass, docPage);
            int length = (int) Math.min(manager.getPageLength(), docPage.getTotalSize());
            return new FacetedPage<>(entities, ChunkRequest.of(start, length), docPage.getTotalSize(), results.getFacetResults());
        });
    }

    @Override
    public <T> FacetedPage<T> facetedSearch(StructuredQueryDefinition query, Pageable pageable, Class<T> entityClass) {
        return facetedSearch(
                combine(query).sort(pageable.getSort()),
                pageable.getOffset(),
                pageable.getPageSize(),
                entityClass);
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
    public InputStream stream(StructuredQueryDefinition query, long start) {
        return stream(query,start, Integer.MAX_VALUE, null);
    }

    @Override
    public <T> InputStream stream(StructuredQueryDefinition query, long start, Class<T> entityClass) {
        return stream(query, start, Integer.MAX_VALUE, entityClass);
    }

    @Override
    public InputStream stream(StructuredQueryDefinition query, long start, int length) {
        return stream(query,start, length, null);
    }

    @Override
    public InputStream stream(StructuredQueryDefinition query, Pageable pageable) {
        return stream(
                combine(query).sort(pageable.getSort()),
                pageable.getOffset(),
                pageable.getPageSize());
    }

    @Override
    public <T> InputStream stream(StructuredQueryDefinition query, long start, int length, Class<T> entityClass) {
        return execute((manager, transaction) -> {
            if (length >= 0) manager.setPageLength(length);

            QueryDefinition finalQuery = queryMapper.getMappedQuery(query, entityClass);

            DocumentPage page = manager.search(finalQuery, start + 1, transaction);

            Enumeration<? extends InputStream> results = Collections.enumeration(
                    StreamSupport.stream(page.spliterator(), true)
                            .map(record -> record.getContentAs(InputStream.class))
                            .collect(Collectors.toList()));

            return new SequenceInputStream(results);
        });
    }

    @Override
    public <T> InputStream stream(StructuredQueryDefinition query, Pageable pageable, Class<T> entityClass) {
        return stream(
                combine(query).sort(pageable.getSort()),
                pageable.getOffset(),
                pageable.getPageSize(),
                entityClass);
    }

    @Override
    public boolean exists(String uri) {
        return execute((manager, transaction) -> singletonList(String.valueOf(uri))
                .stream()
                .anyMatch(item -> manager.exists(item, transaction) != null));
    }

    @Override
    public <T> boolean exists(Object id, Class<T> entityClass) {
        final List<String> uris = converter.getDocumentUris(singletonList(id), entityClass);
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
    public void deleteByUri(String... uris) {
        deleteByUris(Arrays.asList(uris));
    }

    @Override
    public void deleteByUris(List<String> uris) {
        execute((manager, transaction) -> {
            manager.delete(transaction, uris.toArray(new String[0]));
            return null;
        });
    }

    @Override
    public <T> void deleteById(Object id, Class<T> entityClass) {
        deleteByIds(singletonList(id), entityClass);
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
    public <T> void delete(StructuredQueryDefinition query, Class<T> entityClass) {
        executeWithClient((client, transaction) -> {
            QueryManager qryMgr = client.newQueryManager();
            CombinedQueryDefinition combined =
                    combine(query).options("<values name='uris'><uri/></values>");

            combined = (CombinedQueryDefinition) queryMapper.getMappedQuery(combined, entityClass);
            ValuesDefinition valDef = qryMgr.newValuesDefinition("uris");
            valDef.setQueryDefinition(
                    qryMgr.newRawCombinedQueryDefinition(new StringHandle(combined.serialize()).withFormat(Format.XML))
            );

            ValuesHandle results = qryMgr.values(valDef, new ValuesHandle(), transaction);

            // Convert the facets to just a list of the ids
            List<String> uris = Arrays.stream(results.getValues())
                    .map(value -> value.get("xs:string", String.class))
                    .collect(Collectors.toList());

            if (!uris.isEmpty()) {
                client.newDocumentManager().delete(transaction, uris.toArray(new String[0]));
            }

            return null;
        });
    }

    @Override
    public <T> void deleteByIds(List<?> ids, Class<T> entityClass) {
        final List<String> uris = converter.getDocumentUris(ids, entityClass);

        execute((manager, transaction) -> {
            manager.delete(transaction, uris.toArray(new String[0]));
            return null;
        });
    }

    @Override
    public void dropCollections(String... collections) {
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
    public <T> void dropCollection(Class<T> entityClass) {
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

        dropCollections(entityClass.getSimpleName());
    }

    @Override
    public <T> void dropCollections(Class<T>... entityClasses) {
        String[] collections = Arrays.stream(entityClasses)
                .map(entityClass -> {
                    MarkLogicPersistentEntity entity = converter.getMappingContext().getPersistentEntity(entityClass);
                    if (entity == null || entity.getTypePersistenceStrategy() != TypePersistenceStrategy.COLLECTION) {
                        throw new InvalidDataAccessApiUsageException(String.format("Cannot determine deleteById scope for entity of type %s", entityClass.getName()));
                    }

                    return entityClass.getSimpleName();
                }).toArray(String[]::new);

        dropCollections(collections);
    }

    @Override
    public MarkLogicConverter getConverter() {
        return converter;
    }

    @Override
    public QueryMapper getQueryMapper() {
        return queryMapper;
    }

    public QueryConversionService getQueryConversionService() {
        return queryConversionService;
    }

    private static RuntimeException potentiallyConvertRuntimeException(RuntimeException ex,
                                                                       PersistenceExceptionTranslator exceptionTranslator) {
        RuntimeException resolved = exceptionTranslator.translateExceptionIfPossible(ex);
        return resolved == null ? ex : resolved;
    }

    protected  <T> List<T> toEntityList(Class<T> entityClass, DocumentPage page) {
        final List<T> results = new ArrayList<>();
        page.iterator().forEachRemaining(item -> results.add(converter.read(entityClass, new DocumentDescriptor(item))));
        return results;
    }

    protected List<DocumentRecord> toRecordList(DocumentPage page) {
        final List<DocumentRecord> results = new ArrayList<>();
        page.iterator().forEachRemaining(results::add);
        return results;
    }
}
