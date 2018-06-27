package io.github.malteseduck.springframework.data.marklogic.core.query

/**
 * Used to indicate that a criteria property should create a value query.  This is the default if nothing is specified,
 * so this should only really be used to override the default query parameters for a value query.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Value(
    // Specify any word query options you want to use, http://docs.marklogic.com/cts.jsonPropertyValueQuery for
    // available options.
    val options: Array<String> = ["exact"],

    // Boost relevance for this criteria property
    val weight: Double = 1.0,

    // Use dot notation to specify the field to which this refers if it is not the same name as the criteria property.
    val field: String = ""
)
