package org.springframework.data.marklogic.core.mapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

public class BasicMarkLogicPersistentProperty extends AnnotationBasedPersistentProperty<MarkLogicPersistentProperty> implements MarkLogicPersistentProperty {

    private static final Logger LOG = LoggerFactory.getLogger(BasicMarkLogicPersistentProperty.class);
    private static final String ID_FIELD_NAME = "id";

    public BasicMarkLogicPersistentProperty(Field field, PropertyDescriptor propertyDescriptor, PersistentEntity<?, MarkLogicPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
        super(field, propertyDescriptor, owner, simpleTypeHolder);
    }

    @Override
    protected Association<MarkLogicPersistentProperty> createAssociation() {
        return null;
    }

    @Override
    public String getFieldName() {
        return null;
    }

    @Override
    public boolean isExplicitIdProperty() {
        return false;
    }
}
