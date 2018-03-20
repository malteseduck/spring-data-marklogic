package io.github.malteseduck.springframework.data.marklogic.repository.support;

import com.marklogic.client.pojo.PojoQueryBuilder;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import io.github.malteseduck.springframework.data.marklogic.repository.MarkLogicRepository;
import io.github.malteseduck.springframework.data.marklogic.repository.query.MarkLogicEntityInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

public class SimpleMarkLogicRepository<T, ID extends Serializable> implements MarkLogicRepository<T, ID> {

    private static final Logger log = LoggerFactory.getLogger(SimpleMarkLogicRepository.class);

    private final MarkLogicOperations operations;
    private final MarkLogicEntityInformation<T, ID> entityInformation;
    private final PojoQueryBuilder<T> qb;

    private final static int DEFAULT_START = 0;
    private final static int DEFAULT_LIMIT = Integer.MAX_VALUE;

    public SimpleMarkLogicRepository(MarkLogicEntityInformation<T, ID> metadata, MarkLogicOperations operations) {
        Assert.notNull(metadata, "MarkLogicEntityInformation must not be null!");
        Assert.notNull(operations, "MarkLogicOperations must not be null!");

        this.entityInformation = metadata;
        this.operations = operations;
        this.qb = operations.qb(entityInformation.getJavaType());
    }

    @Override
    public List<T> findAll() {
        return operations.search(
                qb.and(),
                DEFAULT_START,
                DEFAULT_LIMIT,
                entityInformation.getJavaType())
            .getContent();
    }

    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        Assert.notNull(ids, "The given Iterable of ids must not be null");
        return operations.read(convertIterableToList(ids), entityInformation.getJavaType());
    }

    @Override
    public List<T> findAll(Sort sort) {
        Assert.notNull(sort, "The given Sort must not be null");
        return operations.search(
                combine()
                    .type(entityInformation.getJavaType())
                    .sort(sort),
                DEFAULT_START,
                DEFAULT_LIMIT,
                entityInformation.getJavaType())
            .getContent();
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        Assert.notNull(pageable, "The given Pageable must not be null");
        return operations.search(
                combine()
                        .type(entityInformation.getJavaType())
                        .sort(pageable.getSort()),
                Math.toIntExact(pageable.getOffset()),
                pageable.getPageSize(),
                entityInformation.getJavaType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "Entity must not be null");
        Method calling = SimpleMarkLogicRepository.class.getEnclosingMethod(); // TODO What does this do?
        return operations.write(entity);
    }

    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        Assert.notNull(entities, "The given Iterable of entities must not be null");
        return operations.write(convertIterableToList(entities));
    }

    @Override
    public Optional<T> findById(ID id) {
        Assert.notNull(id, "The given id must not be null");
        return Optional.ofNullable(operations.read(id, entityInformation.getJavaType()));
    }

    @Override
    public boolean existsById(ID id) {
        Assert.notNull(id, "The given id must not be null");
        return operations.exists(id, entityInformation.getJavaType());
    }

    @Override
    public long count() {
        return operations.count(entityInformation.getJavaType());
    }

    @Override
    public void deleteById(ID id) {
        Assert.notNull(id, "The given id must not be null");
        operations.deleteById(id, entityInformation.getJavaType());
    }

    @Override
    public void delete(T entity) {
        Assert.notNull(entity, "The given entity must not be null");
        operations.delete(singletonList(entity));
    }

    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities, "The given Iterable of entities must not be null");
        operations.delete((List<T>) entities);
    }

    @Override
    public void deleteAll() {
        operations.dropCollection(entityInformation.getJavaType());
    }

    private static <T> List<T> convertIterableToList(Iterable<T> entities) {
        if (entities instanceof List) return (List<T>) entities;
        List<T> list = new ArrayList<>();
        entities.iterator().forEachRemaining(list::add);
        return list;
    }
}
