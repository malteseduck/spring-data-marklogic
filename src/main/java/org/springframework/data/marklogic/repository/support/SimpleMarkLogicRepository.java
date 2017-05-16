package org.springframework.data.marklogic.repository.support;

import com.marklogic.client.pojo.PojoQueryBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.repository.MarkLogicRepository;
import org.springframework.data.marklogic.repository.query.MarkLogicEntityInformation;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

public class SimpleMarkLogicRepository<T, ID extends Serializable> implements MarkLogicRepository<T, ID> {

    private final MarkLogicOperations operations;
    private final MarkLogicEntityInformation<T, ID> entityInformation;
    private final PojoQueryBuilder<T> qb;

    public SimpleMarkLogicRepository(MarkLogicEntityInformation<T, ID> metadata, MarkLogicOperations operations) {
        Assert.notNull(metadata, "MarkLogicEntityInformation must not be null!");
        Assert.notNull(operations, "MarkLogicOperations must not be null!");

        this.entityInformation = metadata;
        this.operations = operations;
        this.qb = operations.qb(entityInformation.getJavaType());
    }

    @Override
    public List<T> findAll(Sort sort) {
        return operations.search(
                operations.sortQuery(sort, null, entityInformation.getJavaType()),
                entityInformation.getJavaType()
        );
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return operations.search(
                operations.sortQuery(pageable.getSort(), null, entityInformation.getJavaType()),
                pageable.getOffset(),
                pageable.getPageSize(),
                entityInformation.getJavaType()
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "Entity must not be null");
        Method calling = SimpleMarkLogicRepository.class.getEnclosingMethod();
        return (S) operations.write(entity);
    }

    @Override
    public <S extends T> List<S> save(Iterable<S> entities) {
        Assert.notNull(entities, "The given Iterable of entities must not be null");
        return operations.write(convertIterableToList(entities));
    }

    @Override
    public T findOne(ID id) {
        Assert.notNull(id, "The given id must not be null");
        return operations.read(id, entityInformation.getJavaType());
    }

    @Override
    public boolean exists(ID id) {
        Assert.notNull(id, "The given id must not be null");
        return operations.exists(id);
    }

    @Override
    public List<T> findAll() {
        return operations.search(entityInformation.getJavaType());
    }

    @Override
    public Iterable<T> findAll(Iterable<ID> ids) {
        Assert.notNull(ids, "The given Iterable of ids must not be null");
        return operations.read(convertIterableToList(ids), entityInformation.getJavaType());
    }

    @Override
    public long count() {
        return operations.count(entityInformation.getJavaType());
    }

    @Override
    public void delete(ID id) {
        Assert.notNull(id, "The given id must not be null");
        operations.deleteById(id, entityInformation.getJavaType());
    }

    @Override
    public void delete(T entity) {
        Assert.notNull(entity, "The given entity must not be null");
        operations.delete(singletonList(entity));
    }

    @Override
    public void delete(Iterable<? extends T> entities) {
        Assert.notNull(entities, "The given Iterable of entities must not be null");
        operations.deleteById(entities, entityInformation.getJavaType());
    }

    @Override
    public void deleteAll() {
        operations.deleteAll(entityInformation.getJavaType());
    }

    private static <T> List<T> convertIterableToList(Iterable<T> entities) {
        if (entities instanceof List) return (List<T>) entities;
        List<T> list = new ArrayList<>();
        entities.iterator().forEachRemaining(list::add);
        return list;
    }
}
