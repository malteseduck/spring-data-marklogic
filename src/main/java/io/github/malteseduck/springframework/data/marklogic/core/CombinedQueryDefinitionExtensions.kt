package io.github.malteseduck.springframework.data.marklogic.core

import io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinition

inline fun <reified T : Any> CombinedQueryDefinition.type(): CombinedQueryDefinition =
    type(T::class.java)
