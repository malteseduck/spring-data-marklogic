package org.springframework.data.marklogic.core;

import com.marklogic.client.document.DocumentPage;
import com.marklogic.client.document.DocumentRecord;
import com.marklogic.client.pojo.PojoQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.convert.MarkLogicConverter;
import org.springframework.data.marklogic.repository.query.convert.QueryConversionService;

import java.io.IOException;
import java.util.List;

public interface MarkLogicOperations {

    // Build a query builder for the specified entity type using any configuration in @Document
    <T> PojoQueryBuilder<T> qb(Class<T> entityClass);

    // Query-building helpers that are commonly needed but somewhat complicated to implement
    StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query);

    <T> StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query, Class<T> entityClass);

    StructuredQueryDefinition termQuery(String term, StructuredQueryDefinition query);

    // Database configuration - requires the "admin" role in the user account
    void configure(Resource configuration) throws IOException;

    // Callback methods for accessing different low-level APIs available on the client
    // TODO: Add additional callbacks for different managers and other stuff on the client?  Add one for access to the client?
    <T> T execute(DocumentCallback<T> action);

    Object executeWithClient(ClientCallback action);

    <T> T executeQuery(QueryCallback<T> action);

    // Entity peristence - no need to specify the class since it can be determined from the object

    Object write(Object entity);

    Object write(Object entity, String... collections);

    <T> List<T> write(List<T> entities);

    <T> List<T> write(List<T> entities, String... collections);

    // If the entity class is not specified return the "raw" result so that the client can convert to whatever they need

    DocumentRecord read(Object id);

    List<DocumentRecord> read(List<?> ids);

    List<DocumentRecord> search(StructuredQueryDefinition query);

    // If an entity class is specified then the template can convert all the entities before returning them

    <T> T read(Object id, Class<T> entityClass);

    <T> List<T> read(List<?> ids, Class<T> entityClass);

    <T> List<T> search(Class<T> entityClass);

    <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass);

    <T> T findOne(StructuredQueryDefinition query, Class<T> entityClass);

    // Using Spring Page so we don't lose the paging information from the database - only if they "care" about pages (specifying start/stop, etc)
    // Also this is done so there doesn't have to be another conversion "down the line" to get it into a Spring Page.

    DocumentPage search(StructuredQueryDefinition query, int start);

    DocumentPage search(StructuredQueryDefinition query, int start, int length);

    <T> Page<T> search(StructuredQueryDefinition query, int start, Class<T> entityClass);

    <T> Page<T> search(StructuredQueryDefinition query, int start, int length, Class<T> entityClass);

    // Document existence

    boolean exists(Object id);

    <T> boolean exists(StructuredQueryDefinition query, Class<T> entityClass);

    // Counting

    long count(String... collections);

    <T> long count(Class<T> entityClass);

    long count(StructuredQueryDefinition query);

    <T> long count(StructuredQueryDefinition query, Class<T> entityClass);

    // Document/collection deletes

    void deleteById(Object id);

    <T> void deleteById(Object id, Class<T> entityClass);

    <T> void delete(List<T> entities);

    void deleteAll(List<?> ids);

    <T> void deleteAll(List<?> ids, Class<T> entityClass);

    void deleteAll(String... collections);

    <T> void deleteAll(Class<T> entityClass);

    // TODO: Implement streaming functions

    // TODO: Implement patch functions

    // TODO: Implement some functions that make it easy to get values from range indexes

    MarkLogicConverter getConverter();

    QueryConversionService getQueryConversionService();
}
