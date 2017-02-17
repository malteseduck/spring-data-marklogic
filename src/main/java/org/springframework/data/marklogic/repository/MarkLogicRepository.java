package org.springframework.data.marklogic.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.io.Serializable;

public interface MarkLogicRepository<T, ID extends Serializable>
        extends PagingAndSortingRepository<T, ID> {
}
