package org.springframework.data.marklogic.core.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

public class BasicMarkLogicPersistentProperty extends AnnotationBasedPersistentProperty<MarkLogicPersistentProperty> implements MarkLogicPersistentProperty {

    private static final Logger LOG = LoggerFactory.getLogger(BasicMarkLogicPersistentProperty.class);
    private final String path;

    public BasicMarkLogicPersistentProperty(Field field, PropertyDescriptor propertyDescriptor, PersistentEntity<?, MarkLogicPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
        super(field, propertyDescriptor, owner, simpleTypeHolder);

        Indexed indexed = this.findAnnotation(Indexed.class);
        JsonProperty jsonProperty = this.findAnnotation(JsonProperty.class);

        String name = field != null ? field.getName() : "";

        if (jsonProperty != null && !StringUtils.isEmpty(jsonProperty.value())) {
            name = jsonProperty.value();
        }

        if (indexed != null) {
            if (!StringUtils.isEmpty(indexed.path())) {
                this.path = indexed.path() + "/" + name;
            } else if (indexed.type() == IndexType.PATH) {
                path = "/" + name;
            } else {
                path = null;
            }
        } else {
            path = "/" + name;
        }
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

    @Override
    public String getPath() {
        return path;
    }
}
