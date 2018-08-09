package io.github.malteseduck.springframework.data.marklogic.core.mapping;

public enum TypePersistenceStrategy {
    /**
     * Store and query documents by the "type" collection
     */
    COLLECTION,
    /**
     * Store and query documents under a base URI, defaults to "/TYPE_NAME/"
     */
    URI,
    /**
     * Don't scope queries using any type information
     */
    NONE
}
