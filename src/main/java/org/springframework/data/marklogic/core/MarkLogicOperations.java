package org.springframework.data.marklogic.core;

import com.marklogic.client.query.StructuredQueryDefinition;

import java.util.List;

public interface MarkLogicOperations {

    Object write(Object entity);

    Object write(Object entity, String... collections);

    <T> List<T> write(List<T> entities);

    <T> List<T> write(List<T> entities, String... collections);

    Object read(Object id);

    <T> T read(Object id, Class<T> entityClass);

    List<?> read(List<? extends Object> ids);

    <T> List<T> read(List<? extends Object> ids, Class<T> entityClass);

    List<?> search(StructuredQueryDefinition query);

    <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass);

    boolean exists(Object id);

    long count(String... collections);

    <T> long count(Class<T> entityClass);

    <T> long count(StructuredQueryDefinition query);

    <T> long count(StructuredQueryDefinition query, Class<T> entityClass);

    void delete(Object id);

    void delete(List<? extends Object> ids);

    void deleteAll(String collection);

    <T> void deleteAll(Class<T> entityClass);

//    void clear();
}
