package io.github.malteseduck.springframework.data.marklogic.core.query

import com.marklogic.client.query.StructuredQueryBuilder.Operator
import io.github.malteseduck.springframework.data.marklogic.core.mapping.IndexType

/**
 * Used to indicate that a criteria property should create a range query.  It is assumed that a path range index is
 * created for the field, or an element range index.  Various parameters allow changing behavior from the defaults.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Range(
    val operator: Operator = Operator.EQ,

    // Type will be determined by the type of the property, unless overridden with "type"
    val type: String = "",

    // Specify any word query options you want to use, http://docs.marklogic.com/cts.rangeQuery for available options.
    val options: Array<String> = [],

    // Use dot notation to specify the field to which this refers if it is not the same name as the criteria property.
    val field: String = "",

    // If the path range index is more complicated than can be inferred by the property name, or than can be specified
    // in the "field" param, you can use this param to specify it.
    val pathIndex: String = "",

    // Can specify either "PATH" OR "ELEMENT" for the type of range index that was created and will be used
    val indexType: IndexType = IndexType.PATH
)
