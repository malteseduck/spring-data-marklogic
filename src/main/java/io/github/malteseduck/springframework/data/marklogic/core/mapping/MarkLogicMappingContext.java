package io.github.malteseduck.springframework.data.marklogic.core.mapping;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

public class MarkLogicMappingContext extends AbstractMappingContext<BasicMarkLogicPersistentEntity<?>, MarkLogicPersistentProperty>
        implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    protected <T> BasicMarkLogicPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {
        return new BasicMarkLogicPersistentEntity<>(typeInformation);
    }

    @Override
    protected MarkLogicPersistentProperty createPersistentProperty(Property property, BasicMarkLogicPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new BasicMarkLogicPersistentProperty(property, owner, simpleTypeHolder);
    }
}
