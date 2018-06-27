package io.github.malteseduck.springframework.data.marklogic.repository.support

import com.marklogic.client.impl.ValueConverter
import com.marklogic.client.io.Format
import com.marklogic.client.io.Format.JSON
import com.marklogic.client.query.StructuredQueryBuilder
import com.marklogic.client.query.StructuredQueryDefinition
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicOperations
import io.github.malteseduck.springframework.data.marklogic.core.mapping.DocumentFormat
import io.github.malteseduck.springframework.data.marklogic.core.query.QueryCriteria
import io.github.malteseduck.springframework.data.marklogic.core.query.Range
import io.github.malteseduck.springframework.data.marklogic.core.query.Value
import io.github.malteseduck.springframework.data.marklogic.core.query.Word
import io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinition
import io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine
import io.github.malteseduck.springframework.data.marklogic.repository.query.MarkLogicEntityInformation
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.io.Serializable
import java.time.temporal.Temporal
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

open class QueryCriteriaMarkLogicExecutor<T, ID : Serializable>(
    private val metadata: MarkLogicEntityInformation<T, ID>,
    private val ops: MarkLogicOperations
) : QueryCriteriaExecutor<T> {


    override fun findAll(criteria: QueryCriteria<T>): List<T> =
        ops.search(convert(criteria), 0, 10, metadata.javaType).content as List<T>

    override fun findAll(criteria: QueryCriteria<T>, sort: Sort): List<T> =
        ops.search(
            combine(convert(criteria)).type(metadata.javaType).sort(sort), 0, 10, metadata.javaType
        ).content as List<T>

    override fun findAll(criteria: QueryCriteria<T>, pageable: Pageable): Page<T> =
        ops.search(
            combine(convert(criteria)).type(metadata.javaType).sort(pageable.sort),
            pageable.offset,
            pageable.pageSize,
            metadata.javaType
        ) as Page<T>

    override fun deleteAll(criteria: QueryCriteria<T>) {
        ops.delete(convert(criteria), metadata.javaType)
    }

    override fun count(criteria: QueryCriteria<T>): Long =
        ops.count(convert(criteria), metadata.javaType)

    override fun exists(criteria: QueryCriteria<T>): Boolean =
        ops.exists(convert(criteria), metadata.javaType)

    companion object {
        private val qb = StructuredQueryBuilder()
        private val coreCriteria = arrayOf(
            QueryCriteria<*>::qtext.name,
            QueryCriteria<*>::options.name,
            QueryCriteria<*>::fields.name,
            QueryCriteria<*>::query.name
        )
    }

    fun convert(criteria: QueryCriteria<T>): StructuredQueryDefinition = build(criteria)

    private var format: Format = JSON

    /**
     * Builds the structured query from all the predicate constraints.
     *
     * @return A structured query.
     */
    fun build(criteria: QueryCriteria<T>): StructuredQueryDefinition {
        // Use the correct document format if it is not the default
        ops.converter.mappingContext.getPersistentEntity(criteria::class.java)?.let { entity ->
            if (entity.documentFormat != JSON) format = entity.documentFormat
        }

        var queryDef: CombinedQueryDefinition = combine(qb.and(*queries(criteria).toTypedArray()))

        return criteria.run {
            if (qtext?.isNotBlank() == true) {
                queryDef = combine(queryDef).term(qtext)
            }

            if (fields.isNotEmpty()) {
                queryDef = combine(queryDef)
                    .extracts(
                        // Add a "/" prefix to each field name to make it a path from the root
                        fields.map { "/${it.replace('.', '/')}" }.toList()
                    )
            }

            configure(queryDef)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <Q : QueryCriteria<T>> queries(criteria: Q): List<StructuredQueryDefinition> =
        (criteria::class as KClass<Q>).declaredMemberProperties
            .filter { !coreCriteria.contains(it.name) }
            .mapNotNull { property ->
                val annotation: Annotation? = property.annotations.firstOrNull()

                property.get(criteria)?.let { value ->
                    when (annotation) {
                        is Word -> words(annotation, property, value)
                        is Range -> range(annotation, property, value)
                        is Value -> value(annotation, property, value)
                        else -> value(property.name, value)
                    }
                }
            }

    /**
     * Based on the configured format creates the correct element query for either XML or JSON
     *
     * @param field The name of the field
     */
    private fun element(field: String): StructuredQueryBuilder.Element =
        when (format) {
            DocumentFormat.XML -> qb.element(field)
            else -> qb.jsonProperty(field)
        }

    /**
     * Break out the string(s) into "terms".  To allow for partial matches surround each term with wildcards.  This
     * potentially will not with other languages, as the words separator may not be a space.  The search will be
     * case-insensitive always, regardless of what case is submitted.  For a full search grammar set the qtext param
     * in the Criteria instead.
     *
     * @param value The search text that may contain one or more terms
     */
    private fun terms(value: Any): List<String> {
        val stringValue: String =
            (value as? List<*>)?.joinToString(" ") ?: value.toString()
        return stringValue
            .toLowerCase() // Making all the terms lower-case will make the search case-insensitive
            .split(" ")
            .filter(String::isNotBlank)
            .map {
                when {
                    it.contains("*") -> it
                    else -> "*$it*"
                }
            }
    }

    private fun <Q : QueryCriteria<T>> words(annotation: Word, property: KProperty1<Q, *>, value: Any): StructuredQueryDefinition {
        val fieldName = if (annotation.field.isNotBlank()) annotation.field else property.name
        return wrapScope(fieldName) {
            qb.and(
                *terms(value).map { term ->
                    qb.word(element(it), null, annotation.options, annotation.weight, term)
                }.toTypedArray()
            )
        }
    }

    private fun <Q : QueryCriteria<T>> range(annotation: Range, property: KProperty1<Q, *>, value: Any): StructuredQueryDefinition {
        val fieldName = if (annotation.field.isNotBlank()) annotation.field else property.name
        var type: String = annotation.type

        if (type.isEmpty()) {
            if (value is Temporal) type = "xs:dateTime"
            else ValueConverter.convertFromJava(value, { _, stringType, _ -> type = stringType })
        }

        return wrapScope(fieldName, { qb.range(element(it), type, annotation.options, annotation.operator, value) })
    }

    private fun <Q : QueryCriteria<T>> value(annotation: Value, property: KProperty1<Q, *>, value: Any): StructuredQueryDefinition {
        val fieldName = if  (annotation.field.isNotBlank()) annotation.field else property.name
        return value(fieldName, value, annotation.options, annotation.weight)
    }

    private fun value(fieldName: String, value: Any, options: Array<String>? = arrayOf("exact"), weight: Double = 1.0): StructuredQueryDefinition {

        return wrapScope(fieldName) {
            when (value) {
                is Number -> qb.value(element(it), null, options, weight, value)
                is Boolean -> qb.value(element(it), null, options, weight, value)
                else -> qb.value(element(it), null, options, weight, value.toString())
            }
        }
    }

    private fun wrapScope(name: String, query: (String) -> StructuredQueryDefinition): StructuredQueryDefinition =
        if (name.contains('.')) {
            val parts = name.split('.')
            parts
                .take(parts.size - 1)
                .foldRight(query(parts.last())) { s, acc ->
                    qb.containerQuery(qb.jsonProperty(s), acc)
                }
        } else query(name)
}
