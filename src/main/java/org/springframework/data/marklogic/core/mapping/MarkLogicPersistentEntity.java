package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.mapping.PersistentEntity;

public interface MarkLogicPersistentEntity<T> extends PersistentEntity<T, MarkLogicPersistentProperty> {

    /**
     * Returns the collection the entity shall be persisted to?
     *
     * @return
     */
    String getCollection();

    /**
     * Gets the typeStrategy defined for determining how to save "type" information about a document, to save the name of the
     * class in a Collection so queries can "auto" filter based on type, or left to the discretion of the client.
     * @return
     */
    TypePersistenceStrategy getTypePersistenceStrategy();

    /**
     * Determines the model type for the entity, i.e. JSON, XML, or binary formats
     * @return
     */
    DocumentFormat getDocumentFormat();
}
