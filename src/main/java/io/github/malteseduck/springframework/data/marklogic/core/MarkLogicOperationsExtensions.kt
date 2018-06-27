package io.github.malteseduck.springframework.data.marklogic.core

import com.marklogic.client.query.StructuredQueryDefinition
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.io.InputStream

inline fun <reified T : Any> MarkLogicOperations.stream(query: StructuredQueryDefinition, start: Long = 0, length: Int = 10): InputStream =
    stream(query, start, length, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.stream(query: StructuredQueryDefinition, pageable: Pageable): InputStream =
    stream(query, pageable, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.search(query: StructuredQueryDefinition): List<T> =
    search(query, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.search(query: StructuredQueryDefinition, pageable: Pageable): Page<T> =
    search(query, pageable, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.search(query: StructuredQueryDefinition, start: Long = 0, length: Int = 10): Page<T> =
    search(query, start, length, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.facetedSearch(query: StructuredQueryDefinition, pageable: Pageable): Page<T> =
    facetedSearch(query, pageable, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.facetedSearch(query: StructuredQueryDefinition, start: Long = 0, length: Int = 10): Page<T> =
    facetedSearch(query, start, length, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.searchOne(query: StructuredQueryDefinition): T? =
    searchOne(query, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.exists(query: StructuredQueryDefinition): Boolean =
    exists(query, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.exists(id: Any): Boolean =
    exists(id, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.read(id: Any): T? =
    read(id, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.read(ids: MutableList<Any>): MutableList<T>? =
    read(ids, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.count(query: StructuredQueryDefinition): Long =
    count(query, T::class.java)

inline fun <reified T : Any> MarkLogicOperations.count(): Long =
    count(T::class.java)

inline fun <reified T : Any> MarkLogicOperations.dropCollection() {
    dropCollection(T::class.java)
}

inline fun <reified T : Any> MarkLogicOperations.deleteById(id: Any) {
    deleteById(id, T::class.java)
}

inline fun <reified T : Any> MarkLogicOperations.deleteByIds(ids: MutableList<Any>) {
    deleteByIds(ids, T::class.java)
}

inline fun <reified T : Any> MarkLogicOperations.delete(query: StructuredQueryDefinition) {
    delete(query, T::class.java)
}
