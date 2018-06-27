package io.github.malteseduck.springframework.data.marklogic.repository.support

import io.github.malteseduck.springframework.data.marklogic.core.query.QueryCriteria
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

interface QueryCriteriaExecutor<T> {

    fun findAll(criteria: QueryCriteria<T>): List<T>

    fun findAll(criteria: QueryCriteria<T>, sort: Sort): List<T>

    fun findAll(criteria: QueryCriteria<T>, pageable: Pageable): Page<T>

    fun deleteAll(criteria: QueryCriteria<T>)

    fun count(criteria: QueryCriteria<T>): Long

    fun exists(criteria: QueryCriteria<T>): Boolean
}
