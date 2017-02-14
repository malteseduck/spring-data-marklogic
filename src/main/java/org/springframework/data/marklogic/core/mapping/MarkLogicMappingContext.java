package org.springframework.data.marklogic.core.mapping;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

public class MarkLogicMappingContext extends AbstractMappingContext<BasicMarkLogicPersistentEntity<?>, MarkLogicPersistentProperty>
        implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    protected <T> BasicMarkLogicPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
        // TODO: Do we determine here which document manager needs to be used?
        return new BasicMarkLogicPersistentEntity<>(typeInformation);
    }

    @Override
    protected MarkLogicPersistentProperty createPersistentProperty(Field field, PropertyDescriptor propertyDescriptor, BasicMarkLogicPersistentEntity<?> basicMarkLogicPersistentEntity, SimpleTypeHolder simpleTypeHolder) {
        return new BasicMarkLogicPersistentProperty(field, propertyDescriptor, basicMarkLogicPersistentEntity, simpleTypeHolder);
    }
}
