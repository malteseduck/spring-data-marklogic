package org.springframework.data.marklogic.core.mapping;

import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;

public interface MarkLogicPersistentProperty extends PersistentProperty<MarkLogicPersistentProperty> {

    /**
     * Returns the name of the field a property is persisted to.
     *
     * @return
     */
    String getFieldName();

    /**
     * Returns whether the property is explicitly marked as an identifier property of the owning {@link PersistentEntity}.
     * A property is an explicit id property if it is annotated options @see {@link Id}.
     *
     * @return
     */
    boolean isExplicitIdProperty();

    String getPath();
}
