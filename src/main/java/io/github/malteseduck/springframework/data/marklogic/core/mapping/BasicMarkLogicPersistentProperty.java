package io.github.malteseduck.springframework.data.marklogic.core.mapping;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.StringUtils;

public class BasicMarkLogicPersistentProperty extends AnnotationBasedPersistentProperty<MarkLogicPersistentProperty> implements MarkLogicPersistentProperty {

    private static final Logger LOG = LoggerFactory.getLogger(BasicMarkLogicPersistentProperty.class);
    private final String path;
    private final IndexType indexType;

    public BasicMarkLogicPersistentProperty(Property property, PersistentEntity<?, MarkLogicPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
        super(property, owner, simpleTypeHolder);

        Indexed indexed = this.findAnnotation(Indexed.class);
        JsonProperty jsonProperty = this.findAnnotation(JsonProperty.class);

        String name = property.getName();

        if (jsonProperty != null && StringUtils.hasText(jsonProperty.value())) {
            name = jsonProperty.value();
        }

        if (indexed != null) {
            indexType = indexed.type();
            if (StringUtils.hasText(indexed.path())) {
                path = indexed.path();
            } else if (indexed.type() == IndexType.PATH) {
                path = "/" + name;
            } else {
                path = null;
            }
        } else {
            indexType = IndexType.PATH;
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

    @Override
    public IndexType getIndexType() {
        return indexType;
    }
}
