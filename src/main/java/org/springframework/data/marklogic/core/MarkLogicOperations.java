package org.springframework.data.marklogic.core;

import com.marklogic.client.pojo.PojoQueryBuilder;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.marklogic.core.convert.MarkLogicConverter;

import java.util.List;

public interface MarkLogicOperations {

    StructuredQueryBuilder queryBuilder();

    StructuredQueryBuilder queryBuilder(String options);

    <T> PojoQueryBuilder<T> queryBuilder(Class<T> entityClass);

    Object write(Object entity);

    Object write(Object entity, String... collections);

    <T> List<T> write(List<T> entities);

    <T> List<T> write(List<T> entities, String... collections);

    Object read(Object id);

    <T> T read(Object id, Class<T> entityClass);

    List<?> read(List<?> ids);

    <T> List<T> read(List<?> ids, Class<T> entityClass);

    List<?> search(StructuredQueryDefinition query);

    <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass);

    Page<?> search(StructuredQueryDefinition query, int start);

    <T> Page<T> search(StructuredQueryDefinition query, int start, Class<T> entityClass);

    <T> Page<T> search(StructuredQueryDefinition query, int start, int length);

    <T> Page<T> search(StructuredQueryDefinition query, int start, int length, Class<T> entityClass);

    boolean exists(Object id);

    long count(String... collections);

    <T> long count(Class<T> entityClass);

    <T> long count(StructuredQueryDefinition query);

    <T> long count(StructuredQueryDefinition query, Class<T> entityClass);

    void delete(Object id);

    <T> void delete(Object id, Class<T> entityClass);

    void delete(List<?> ids);

    <T> void delete(List<?> ids, Class<T> entityClass);

    void deleteAll(String... collections);

    <T> void deleteAll(Class<T> entityClass);

    MarkLogicConverter getConverter();

//    void clear();
}
