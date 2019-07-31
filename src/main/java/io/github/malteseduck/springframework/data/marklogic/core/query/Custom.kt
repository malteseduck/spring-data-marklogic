package io.github.malteseduck.springframework.data.marklogic.core.query

/**
 * Used to indicate that a criteria property needs custom processing  should be ignored in the default processing.  This
 * allows simple customization in the QueryCriteria#configure( CombinedQueryDefinition ) method.
 */
@Target(AnnotationTarget.PROPERTY)
annotation class Custom
