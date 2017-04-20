package org.springframework.data.marklogic.core.mapping;

import com.marklogic.client.io.Format;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;

import java.util.Comparator;

import static org.springframework.util.StringUtils.hasText;

public class BasicMarkLogicPersistentEntity<T> extends BasicPersistentEntity<T, MarkLogicPersistentProperty> implements
        MarkLogicPersistentEntity<T>, ApplicationContextAware {

    private TypePersistenceStrategy typePersistenceStrategy;
    private Format documentFormat;
    private String baseUri;

    public BasicMarkLogicPersistentEntity(TypeInformation<T> information) {
        this(information, null);
    }

    public BasicMarkLogicPersistentEntity(TypeInformation<T> information, Comparator<MarkLogicPersistentProperty> comparator) {
        super(information, comparator);

        Document document = this.findAnnotation(Document.class);
        TypePersistenceStrategy defaultTypeStrategy = TypePersistenceStrategy.COLLECTION;
        Format defaultFormat = Format.JSON;
        String defaultUri = "/";

        if (document != null) {
            this.baseUri = normalize(coalesce(document.uri(), document.value(), defaultUri));
            this.documentFormat = document.format().toFormat();
            this.typePersistenceStrategy = document.typeStrategy();
        } else {
            this.baseUri = defaultUri;
            this.typePersistenceStrategy = defaultTypeStrategy;
            this.documentFormat = defaultFormat;
        }
    }

    @Override
    public String getBaseUri() {
        return baseUri;
    }

    @Override
    public String getCollection() {
        return null;
    }

    public TypePersistenceStrategy getTypePersistenceStrategy() {
        return this.typePersistenceStrategy;
    }

    @Override
    public Format getDocumentFormat() {
        return this.documentFormat;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }

    private String normalize(String uri) {
        String result = uri;
        if (!result.startsWith("/")) {
            result = "/" + result;
        }
        if (!result.endsWith("/")) {
            result = result + "/";
        }
        return result;
    }

    private String coalesce(String... candidates) {
        for (String value : candidates) {
            if (hasText(value)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No valid candidate arguments");
    }
}
