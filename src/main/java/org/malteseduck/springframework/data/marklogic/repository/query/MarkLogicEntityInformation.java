package org.malteseduck.springframework.data.marklogic.repository.query;

import org.springframework.data.repository.core.EntityInformation;

import java.io.Serializable;

public interface MarkLogicEntityInformation <T, ID> extends EntityInformation<T, ID> {

    /**
     * Returns the name of the collection the entity shall be persisted to.
     *
     * @return
     */
    String getCollectionName();

    /**
     * Returns the attribute that the id will be persisted to.
     *
     * @return
     */
    String getIdAttribute();
}