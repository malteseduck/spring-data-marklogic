package org.springframework.data.marklogic.core;

import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.document.ServerTransform;
import com.marklogic.client.pojo.PojoQueryBuilder;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.convert.MarkLogicConverter;
import org.springframework.data.marklogic.core.convert.QueryMapper;
import org.springframework.data.marklogic.domain.facets.FacetedPage;
import org.springframework.data.marklogic.repository.query.convert.QueryConversionService;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * The central interface between you and the MarkLogic database.  Helper methods have been created for basic CRUD
 * operations as well as for building some common query constructs.  Additional methods allow you direct access to the
 * different query and document managers, as well as direct access to the database client, so that you can write logic
 * against the database using the full power of the MarkLogic Java Client Library.
 *
 * The main implementation of this is {@link MarkLogicTemplate}, so you will create an instance of that
 * in order to use this interface.
 */
public interface MarkLogicOperations {

    // ========== Query Building =========== //

    /**
     * Get an instance of a {@link com.marklogic.client.pojo.PojoQueryBuilder} for the specified entity class.  This is a simplified interface for
     * building structured queries against JSON objects in the database.  For full control of all query options use
     * the {@link com.marklogic.client.query.StructuredQueryBuilder} instead.
     *
     * @param entityClass Type of Java entity being stored in the database.
     * @param <T> The type of the entity.
     * @return
     */
    <T> PojoQueryBuilder<T> qb(Class<T> entityClass);

    /**
     * Adds sorting configuration to the specified query.
     *
     * @see MarkLogicOperations#sortQuery(Sort, StructuredQueryDefinition, Class)
     */
    StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query);

    /**
     * Add sorting configuration to the specified query.  The default sort algorithm will expect to use a path range
     * index with specified sort properties, i.e. if sorting on "name" then a path index of "/name" should exist.
     *
     * Through use of the {@link org.springframework.data.marklogic.core.mapping.Indexed} annotation you can indicate
     * use of a different type of range index for the property sorting, or specify the full path that should be used in
     * creation of the sort options.  This requires that the entity type is specified.
     *
     * @param sort Sort information for the query, i.e. which properties and which orders.
     * @param query The structured query to "enhance".
     * @param entityClass The entity type.
     *
     * @return The "enhanced" query definition for use in continued building.
     */
    <T> StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query, Class<T> entityClass);

    /**
     * Add a search term to the specified query.  This is used for word searches across your entire document.  For more
     * fine-grained control use {@link com.marklogic.client.query.StructuredQueryBuilder#word(StructuredQueryBuilder.TextIndex, String...)}
     * to specify specific properties or fields into which to scope the word search.
     *
     * @param term A search phrase.
     * @param query The structured query to "enhance".
     *
     * @return The "enhanced" query definition for use in continued building.
     */
    StructuredQueryDefinition termQuery(String term, StructuredQueryDefinition query);

    // ========== Database Configuration =========== //

    /**
     * Configures the connected database using the specified JSON information.  The format of the JSON is outlined at
     * http://docs.marklogic.com/REST/PUT/manage/v2/databases/[id-or-name]/properties.  This requires that the client
     * was created with a user that has "admin" privileges.
     *
     * @param configuration Resource pointing to a JSON document containing database configuration information.
     * @throws IOException
     */
    void configure(Resource configuration) throws IOException;

    // ========== Database Operation Execution =========== //

    /**
     * Executes the specified action using a {@link com.marklogic.client.document.GenericDocumentManager} interface. For
     * all the basic operations this is sufficient.
     *
     * @param action A function to execute using the document manager.  The active transaction is also provided so that
     *               the function can add additional operations to it.
     * @param <T> The result type of the function.
     *
     * @return The results of the document manager interactions (results of a query, etc.).
     */
    <T> T execute(DocumentCallback<T> action);

    /**
     * Executes the specified action using the full {@link com.marklogic.client.DatabaseClient}.  This gives you full
     * control of your interactions with the database with the full power of the MarkLogic Java Client Library, but it
     * also requires you to do a lot of the busy work yourself.  Should mainly be used for operations not supported
     * currently in this template interface either through the helper methods, a {@link com.marklogic.client.document.GenericDocumentManager},
     * or a {@link com.marklogic.client.query.QueryManager}.
     *
     * @param action A function to execute using the database client and active transaction.
     * @param <T> The result type of the function.
     *
     * @return The results of the function.
     */
    <T> T executeWithClient(ClientCallback<T> action);

    /**
     * Executes the specified action using a {@link com.marklogic.client.query.QueryManager}.
     *
     * @param action A function to execute using the query manager and active transaction.
     * @param <T> The result type of the function.
     *
     * @return The results from the query manager.
     */
    <T> T executeQuery(QueryCallback<T> action);

    // ========== Database Writes =========== //

    /**
     * Write a single entity to the database into the default entity collection without any transforms.
     *
     * @see MarkLogicOperations#write(List, ServerTransform, String...)
     */
    <T> T write(T entity);

    /**
     * Write a single entity to the database into the specified collections, without any transforms.
     *
     * @see MarkLogicOperations#write(List, ServerTransform, String...)
     */
    <T> T write(T entity, String... collections);

    /**
     * Write the specified entity to the database into the default entity collection using the specified transform.
     *
     * @see MarkLogicOperations#write(List, ServerTransform, String...)
     */
    <T> T write(T entity, ServerTransform transform);

    /**
     * Write the specified entity to the database into the specified collections with the specified transform.
     *
     * @see MarkLogicOperations#write(List, ServerTransform, String...)
     */
    <T> T write(T entity, ServerTransform transform, String... collections);

    /**
     * Write the specified entities to the database into the default entity collection, without any transforms
     *
     * @see MarkLogicOperations#write(List, ServerTransform, String...)
     */
    <T> List<T> write(List<T> entities);

    /**
     * Write the specified entities to the database into the specified collections, without any transforms.
     *
     * @see MarkLogicOperations#write(List, ServerTransform, String...)
     */
    <T> List<T> write(List<T> entities, String... collections);

    /**
     * Write the specified entities to the database into the default entity collection using the specified transform.
     *
     * @see MarkLogicOperations#write(List, ServerTransform, String...)
     */
    <T> List<T> write(List<T> entities, ServerTransform transform);

    /**
     * Write the specified list of entities to the database.  Using the {@link org.springframework.data.marklogic.core.mapping.Document}
     * annotation you can specify some information as to how it is persisted (i.e. whether or not to store it in a "type"
     * collection, what path, etc.).
     *
     * If a transform is specified then before the entity is saved into the database they transform will be run against
     * each entity.  For more information see http://docs.marklogic.com/guide/java/transforms.
     *
     * @param entities A list of POJO entities you wish to save into the database.
     * @param transform The transform to use before finally persisting to the database.
     * @param collections Additional collections to which you want the document to be a part of inside the database.
     * @param <T> The type of entity
     *
     * @return The list of entities that were saved to the database.  This is mainly for convenience as they are not
     * modified through this process.
     */
    <T> List<T> write(List<T> entities, ServerTransform transform, String... collections);

    // ========== Database Reads =========== //

    /**
     * Reads a list of documents from the database by their URIs instead of ids.  Since type information is not know a
     * "raw" {@link com.marklogic.client.document.DocumentRecord} is returned, and can be handled as desired.
     *
     * @param uris A list of database URIs of documents.
     *
     * @return A list of {@link com.marklogic.client.document.DocumentRecord} objects from the database.
     */
    List<DocumentRecord> read(List<?> uris);

    /**
     * Read a document with the specified ID from the database.
     *
     * @see MarkLogicOperations#read(List, Class)
     */
    <T> T read(Object id, Class<T> entityClass);

    /**
     * Reads a set of documents that have the specified IDs.  The documents must match the specified type or they will
     * not be included in the result set.
     *
     * @param ids A list of IDs of entities.
     * @param entityClass The Java type of the entity.
     * @param <T> The type of the entity.
     *
     * @return A list of all the entities that matched the specified IDs.
     */
    <T> List<T> read(List<?> ids, Class<T> entityClass);

    // ========== Database Queries without Entity  =========== //

    /**
     * Query for a list of documents using the specified structured query.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int)
     */
    List<DocumentRecord> search(StructuredQueryDefinition query);

    /**
     * Query for a page of documents using the specified structured query, starting at the specified item in the
     * result set.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int)
     */
    DocumentPage search(StructuredQueryDefinition query, long start);

    /**
     * Query for a page of documents using the specified structured query.  Specify a start and length to get different
     * "chunks" of data in the result set from the database.  You seldom want to return all results, as this could be a
     * very large number and will degrade the performance of your application.  You instead should query out "pages" of
     * data as needed.
     *
     * @param query A structured query defining the constraints to use to match the desired documents.
     * @param start The start index within the result set that acts as the start of the "page".
     * @param length The number of items to include in the "page"
     *
     * @return A {@link com.marklogic.client.document.DocumentPage} containing the records for this "chunk" of the result
     * set.
     */
    DocumentPage search(StructuredQueryDefinition query, long start, int length);

    /**
     * Same as search with int bounds, but allows paging/sorting based off Spring Pageable.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int, Class)
     */
    DocumentPage search(StructuredQueryDefinition query, Pageable pageable);

    // ========== Database Queries with Entity =========== //

    /**
     * Query for a single entity using the specified structured query.  If for some reason multiple results are matched
     * then only the first is returned.
     *
     * This is a convenience method to get a single item when you "know" that is all there is.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int, Class)
     */
    <T> T searchOne(StructuredQueryDefinition query, Class<T> entityClass);

    /**
     * Query for a list of of documents of the specified type using the specified structured query.  Only the first
     * {@link com.marklogic.client.impl.DocumentManagerImpl#DEFAULT_PAGE_LENGTH} documents are returned.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int, Class)
     */
    <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass);

    /**
     * Query for a page of of documents of the specified type using the specified structured query.  Only a page of
     * documents is returned, starting from the specified start index and containing
     * {@link com.marklogic.client.impl.DocumentManagerImpl#DEFAULT_PAGE_LENGTH} entities.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int, Class)
     */
    <T> Page<T> search(StructuredQueryDefinition query, long start, Class<T> entityClass);

    /**
     * Queries for a page of documents of the specified type, using the specified structured query to constrain which
     * documents are returned.  Only a page is returned, starting from the specified index and containing the specified
     * number of entries.
     *
     * @param query The structured query to use to match documents in the database.
     * @param start The starting index within the result set of matches.
     * @param limit The number of documents to return.
     * @param entityClass The entity type class.
     * @param <T> The type of entity.
     *
     * @return A page of documents matching the specified parameters.
     */
    <T> Page<T> search(StructuredQueryDefinition query, long start, int limit, Class<T> entityClass);

    /**
     * Same as search with int bounds, but allows paging/sorting based off Spring Pageable.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int, Class)
     */
    <T> Page<T> search(StructuredQueryDefinition query, Pageable pageable, Class<T> entityClass);

    /**
     * @see MarkLogicOperations#facetedSearch(StructuredQueryDefinition, long, int, Class)
     */
    <T> FacetedPage<T> facetedSearch(StructuredQueryDefinition query, long start, Class<T> entityClass);

    /**
     * Similar to a normal paged query, except also includes facets for the matched results.  In order for the facets to
     * be generated you need to ensure the specified query has the name of persisted options that describe the facets,
     * or the query is a {@link org.springframework.data.marklogic.repository.query.CombinedQueryDefinition} that includes
     * the ad-hoc options definitions for them.
     *
     * @param query The structured query to use to match documents in the database.
     * @param start The starting index within the result set of matches.
     * @param limit The number of documents to return.
     * @param entityClass The entity type class.
     * @param <T> The type of entity.
     *
     * @return A page of documents matching the specified parameters with the included facets.
     */
    <T> FacetedPage<T> facetedSearch(StructuredQueryDefinition query, long start, int limit, Class<T> entityClass);

    /**
     * Same as faceted search with int bounds, but allows paging/sorting based off Spring Pageable.
     *
     * @see MarkLogicOperations#search(StructuredQueryDefinition, long, int, Class)
     */
    <T> FacetedPage<T> facetedSearch(StructuredQueryDefinition query, Pageable pageable, Class<T> entityClass);

    // ========== Database Queries Streaming Results =========== //

    /**
     * @see MarkLogicOperations#stream(StructuredQueryDefinition, long, int, Class)
     */
    InputStream stream(StructuredQueryDefinition query);

    /**
     * @see MarkLogicOperations#stream(StructuredQueryDefinition, long, int, Class)
     */
    <T> InputStream stream(StructuredQueryDefinition query, Class<T> entityClass);

    /**
     * @see MarkLogicOperations#stream(StructuredQueryDefinition, long, int, Class)
     */
    InputStream stream(StructuredQueryDefinition query, long start);

    /**
     * @see MarkLogicOperations#stream(StructuredQueryDefinition, long, int, Class)
     */
    <T> InputStream stream(StructuredQueryDefinition query, long start, Class<T> entityClass);

    /**
     * @see MarkLogicOperations#stream(StructuredQueryDefinition, long, int, Class)
     */
    InputStream stream(StructuredQueryDefinition query, long start, int length);

    /**
     * Same as method taking int bounds, but allows use of Pagable for paging/sorting.
     *
     * @see MarkLogicOperations#stream(StructuredQueryDefinition, long, int, Class)
     */
    InputStream stream(StructuredQueryDefinition query, Pageable pageable);

    /**
     * Queries just like the {@link MarkLogicOperations#search(StructuredQueryDefinition, long, int, Class)} method does,
     * but instead of a page/list of documents it returns an input stream straight from the results of the REST call
     * against the database.  The purpose of these methods is to skip the serialization/deserialization process of
     * your application layer and just return raw data.
     *
     * If you don't need to access any parts of the document and just need to return the raw
     * JSON/XML then it is easier/faster to just copy the stream from the database into the HTTP response directly.
     *
     * Extractions or server-side transforms can still be used to change how the data is returned in cases where there
     * are properties you don't want returned through a public API, etc.
     *
     * @return An input stream of all the documents that matched the specified parameters.
     */
    <T> InputStream stream(StructuredQueryDefinition query, long start, int length, Class<T> entityClass);

    /**
     * Same as method taking int bounds, but allows use of Pagable for paging/sorting.
     *
     * @see MarkLogicOperations#stream(StructuredQueryDefinition, long, int, Class)
     */
    <T> InputStream stream(StructuredQueryDefinition query, Pageable pageable, Class<T> entityClass);

    // ========== Database Existence Checks =========== //

    /**
     * Check for existence of a document by using the full URI under which the document is stored.
     *
     * @param uri The full URI of the document, i.e. "/Person/my-id.json"
     *
     * @return True if a document with the specified URI exists.
     */
    boolean exists(String uri);

    /**
     * Check for existence of an entity in the database by using it's ID.
     *
     * @param id The ID value of an entity.
     * @param entityClass The type class of an entity.
     * @param <T> The type of an entity.
     *
     * @return True if an entity with the specified ID exists.
     */
    <T> boolean exists(Object id, Class<T> entityClass);

    /**
     * Check for existence of entities matching the specified structured query.  This will not show which ones match,
     * but is a way to do a quick estimate on whether or not any exist in the first place.
     *
     * @param query The structured query to use to match documents in the database.
     * @param entityClass The type class of an entity.
     * @param <T> The type of an entity.
     *
     * @return True if there are any entities that match the query.
     */
    <T> boolean exists(StructuredQueryDefinition query, Class<T> entityClass);

    // ========== Database Counts =========== //

    /**
     * Count the number of documents that exist in the database under the specified collections.
     *
     * @param collections Collection URIs to which to scope the count.
     *
     * @return The number of documents contained under the specified collections.
     */
    long count(String... collections);

    /**
     * Count the number of the specified entity that exist in the database.
     *
     * @param entityClass The type class of an entity.
     * @param <T> The type of an entity.
     *
     * @return The number of the specified entity contained in the database.
     */
    <T> long count(Class<T> entityClass);

    /**
     * Gives a count of the number of documents that would match the specified query.
     *
     * @see MarkLogicOperations#count(StructuredQueryDefinition, Class)
     */
    long count(StructuredQueryDefinition query);

    /**
     * Count the number of entities that would match the specified query.
     *
     * @param query The structured query to use to match documents in the database.
     * @param entityClass The type class of an entity.
     * @param <T> The type of an entity.
     *
     * @return The count of entities that are in the database.
     */
    <T> long count(StructuredQueryDefinition query, Class<T> entityClass);

    // ========== Database Deletion =========== //

    /**
     * Delete a document from the database that is stored under the specified uris
     *
     * @param uris The uris to delete, i.e. "/Person/my-id.json"
     */
    void deleteByUri(String... uris);

    /**
     * Delete a set of documents from the database that are stored under one of the specified URIs
     *
     * @param uris A list of uris to delete.
     */
    void deleteByUris(List<String> uris);

    /**
     * Deletes the entity with the specified ID.
     *
     * @see MarkLogicOperations#deleteByIds(List, Class)
     */
    <T> void deleteById(Object id, Class<T> entityClass);

    /**
     * Deletes the entities which have one of the specified IDs.
     *
     * @param ids A list of entity IDs.
     *
     * @param entityClass The type class of an entity.
     * @param <T> The type of an entity.
     */
    <T> void deleteByIds(List<?> ids, Class<T> entityClass);

    /**
     * Deletes all the entities of the specified type.
     *
     * @param entityClass The type class of an entity.
     * @param <T> The type of an entity.
     */
    <T> void dropCollection(Class<T> entityClass);

    /**
     * Deletes all the entities of the specified types.
     *
     * @param entityClasses The type classes of entities.
     * @param <T> The type of an entity.
     */
    <T> void dropCollections(Class<T>... entityClasses);

    /**
     * Delete documents that are under the specified collection URIs.
     *
     * @param collections Collection URIs containing documents to delete.
     */
    void dropCollections(String... collections);

    /**
     * Delete the specified entities from the database.
     *
     * @param entities A list of entities.
     * @param <T> The type of an entity.
     */
    <T> void delete(List<T> entities);

    /**
     * Delete entities of the specified type that match the specified query.
     *
     * @param query The structured query to use to match documents in the database.
     * @param entityClass The type class of an entity.
     * @param <T> The type of an entity.
     */
    <T> void delete(StructuredQueryDefinition query, Class<T> entityClass);

    // ========== Utility Methods =========== //

    /**
     * Get the entity converter that is used to convert Java POJOs into a form with which they can be stored in the
     * database.
     *
     * @return The converter object.
     */
    MarkLogicConverter getConverter();

    /**
     * The query mapper is responsible for interpreting entity/query annotations and applying the information to a query
     * before it is executed in MarkLogicTemplate.  It also handles converting Query By Example objects into a MarkLogic
     * query.  The query mapper can be used when it is necessary to use the "execute" methods but you still want to take
     * advantage of the annotations and other logic that is part of this framework.
     */
    QueryMapper getQueryMapper();

    /**
     * Get the query object convert service that is used to convert various Java types into their "correct" form for
     * querying in MarkLogic.
     *
     * @return The service object.
     */
    QueryConversionService getQueryConversionService();
}
