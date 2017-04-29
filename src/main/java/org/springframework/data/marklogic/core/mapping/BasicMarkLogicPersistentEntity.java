package org.springframework.data.marklogic.core.mapping;

import com.marklogic.client.io.Format;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;

import java.util.Comparator;

import static org.springframework.data.marklogic.core.Util.coalesce;

public class BasicMarkLogicPersistentEntity<T> extends BasicPersistentEntity<T, MarkLogicPersistentProperty> implements
        MarkLogicPersistentEntity<T>, ApplicationContextAware {

    private TypePersistenceStrategy typePersistenceStrategy;
    private Format documentFormat;
    private String baseUri;
    private String typeName;

    public BasicMarkLogicPersistentEntity(TypeInformation<T> information) {
        this(information, null);
    }

    public BasicMarkLogicPersistentEntity(TypeInformation<T> information, Comparator<MarkLogicPersistentProperty> comparator) {
        super(information, comparator);

        Document document = this.findAnnotation(Document.class);
        TypePersistenceStrategy defaultTypeStrategy = TypePersistenceStrategy.COLLECTION;
        String defaultTypeName = information.getType().getSimpleName();
        Format defaultFormat = Format.JSON;
        String defaultUri = "/";

        if (document != null) {
            this.baseUri = normalize(coalesce(document.uri(), document.value(), defaultUri));
            this.documentFormat = document.format().toFormat();
            this.typePersistenceStrategy = document.typeStrategy();
            // TODO: if configuration says use full name instead of simple name, let that be the default
            this.typeName = coalesce(document.type(), defaultTypeName);
        } else {
            this.baseUri = defaultUri;
            this.typePersistenceStrategy = defaultTypeStrategy;
            this.documentFormat = defaultFormat;
            this.typeName = defaultTypeName;
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

    @Override
    public String getTypeName() {
        return typeName;
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
}
