package org.springframework.data.marklogic.repository.support;

import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.repository.query.MarkLogicEntityInformation;
import org.springframework.data.repository.core.support.PersistentEntityInformation;

import java.io.Serializable;

public class MappingMarkLogicEntityInformation <T, ID extends Serializable> extends PersistentEntityInformation<T, ID>
		implements MarkLogicEntityInformation<T, ID> {

    private final MarkLogicPersistentEntity<T> entityMetadata;
    private final Class<ID> fallbackIdType;

    public MappingMarkLogicEntityInformation(MarkLogicPersistentEntity<T> entityMetadata) {
        this(entityMetadata, null);
    }

    public MappingMarkLogicEntityInformation(MarkLogicPersistentEntity<T> entityMetadata, Class<ID> idType) {
        super(entityMetadata);
        this.entityMetadata = entityMetadata;
        this.fallbackIdType =  idType != null ? idType : (Class<ID>) String.class;
    }


    @Override
    public String getCollectionName() {
        return entityMetadata.getCollection();
    }

    @Override
    public String getIdAttribute() {
        return entityMetadata.getIdProperty().getName();
    }

    @Override
    public Class<ID> getIdType() {
        if (this.entityMetadata.hasIdProperty()) {
            return super.getIdType();
        }

        return fallbackIdType != null ? fallbackIdType : (Class<ID>) String.class;
    }
}
