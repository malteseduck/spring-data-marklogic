package io.github.malteseduck.springframework.data.marklogic.core.query

import com.fasterxml.jackson.databind.JsonNode
import io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinition

abstract class QueryCriteria<T> {
    // Allow support of passing a query string
    var qtext: String? = null

    // Allow support of passing a QBE node
    var query: JsonNode? = null

    // Allow specification of which fields to return in the results.  Specify using dot notation, i.e.
    // "name" for a property at the root and "friends.name" for a property "name" nested under a "friends" property
    var fields: List<String> = emptyList()

    // Allow specifying of specific persisted query options to use when executing the query.  Set the
    // options to the name of the options you have specified in your src/main/resources/db/options directory.
    var options: String? = null

    // Override to do customization of the created CombinedQueryDefinition
    fun configure(query: CombinedQueryDefinition): CombinedQueryDefinition = query
}
