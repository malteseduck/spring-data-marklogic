package org.springframework.data.marklogic.core.mapping;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;

import java.util.Comparator;

public class BasicMarkLogicPersistentEntity<T> extends BasicPersistentEntity<T, MarkLogicPersistentProperty> implements
        MarkLogicPersistentEntity<T>, ApplicationContextAware {

    private TypePersistenceStrategy typePersistenceStrategy;
    private DocumentFormat documentFormat;

    public BasicMarkLogicPersistentEntity(TypeInformation<T> information) {
        this(information, null);
    }

    public BasicMarkLogicPersistentEntity(TypeInformation<T> information, Comparator<MarkLogicPersistentProperty> comparator) {
        super(information, comparator);

        Document document = this.findAnnotation(Document.class);
        TypePersistenceStrategy strategyFallback = TypePersistenceStrategy.COLLECTION;
        DocumentFormat formatFallback = DocumentFormat.JSON;

        if (document != null) {
            this.documentFormat = document.format() != null ? document.format() : formatFallback;
            this.typePersistenceStrategy = document.strategy() != null ? document.strategy() : strategyFallback;
        } else {
            this.typePersistenceStrategy = strategyFallback;
            this.documentFormat = formatFallback;
        }
    }

    @Override
    public String getCollection() {
        return null;
    }

    public TypePersistenceStrategy getTypePersistenceStrategy() {
        return this.typePersistenceStrategy;
    }

    @Override
    public DocumentFormat getDocumentFormat() {
        return this.documentFormat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }
}
