package org.springframework.data.marklogic.repository.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.repository.MarkLogicRepository;
import org.springframework.data.marklogic.repository.query.MarkLogicEntityInformation;
import org.springframework.util.Assert;

import java.io.Serializable;

public class SimpleMarkLogicRepository<T, ID extends Serializable> implements MarkLogicRepository<T, ID> {

    private final MarkLogicOperations operations;
    private final MarkLogicEntityInformation<T, ID> entityInformation;

    public SimpleMarkLogicRepository(MarkLogicEntityInformation<T, ID> metadata, MarkLogicOperations operations) {
        Assert.notNull(metadata, "MarkLogicEntityInformation must not be null!");
        Assert.notNull(operations, "MarkLogicOperations must not be null!");

        this.entityInformation = metadata;
        this.operations = operations;
    }

    @Override
    public Iterable<T> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "Entity must not be null!");

        operations.write(entity);

        return entity;
    }

    @Override
    public <S extends T> Iterable<S> save(Iterable<S> entities) {
        return null;
    }

    @Override
    public T findOne(ID id) {
        return null;
    }

    @Override
    public boolean exists(ID id) {
        return false;
    }

    @Override
    public Iterable<T> findAll() {
        return null;
    }

    @Override
    public Iterable<T> findAll(Iterable<ID> ids) {
        return null;
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void delete(ID id) {

    }

    @Override
    public void delete(T entity) {

    }

    @Override
    public void delete(Iterable<? extends T> entities) {

    }

    @Override
    public void deleteAll() {

    }
}
