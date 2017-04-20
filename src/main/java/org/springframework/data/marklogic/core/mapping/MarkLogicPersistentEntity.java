package org.springframework.data.marklogic.core.mapping;

import com.marklogic.client.io.Format;
import org.springframework.data.mapping.PersistentEntity;

public interface MarkLogicPersistentEntity<T> extends PersistentEntity<T, MarkLogicPersistentProperty> {

    /**
     * Returns the base URI of the entity.
     */
    String getBaseUri();

    /**
     * Returns the collection the entity shall be persisted to.
     */
    String getCollection();

    /**
     * Returns the strategy for determining how to preserve "type" information about a document.
     */
    TypePersistenceStrategy getTypePersistenceStrategy();

    /**
     * Determines the serialization format for the entity, i.e. JSON, XML, or binary formats
     */
    Format getDocumentFormat();
}
