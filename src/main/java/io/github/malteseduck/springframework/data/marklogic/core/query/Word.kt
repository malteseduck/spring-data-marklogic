package io.github.malteseduck.springframework.data.marklogic.core.query

/**
 * Used to indicate that a criteria property should create a word query with the specified parameters.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Word(
    // Specify any word query options you want to use, see http://docs.marklogic.com/cts.wordQuery for available
    // options.
    val options: Array<String> = [],

    // Boost relevance for this criteria property
    val weight: Double = 1.0,

    // Use dot notation to specify the field to which this refers if it is not the same name as the criteria property.
    val field: String = ""
)
