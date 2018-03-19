package io.github.malteseduck.springframework.data.marklogic.core.convert;

import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.StructuredQueryDefinition;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.DocumentDescriptor;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.TypePersistenceStrategy;
import io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

public abstract class AbstractMarkLogicConverter implements MarkLogicConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMarkLogicConverter.class);

    private static final Pattern formats = Pattern.compile("\\.(json|xml)");
    private static final ConversionService converter = new DefaultConversionService();

    private MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext;

    public AbstractMarkLogicConverter(MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext) {
        this.mappingContext = mappingContext;
    }

    @Override
    public MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> getMappingContext() {
        return mappingContext;
    }

    @Override
    public ConversionService getConversionService() {
        return converter;
    }

    @Override
    public void write(Object source, DocumentDescriptor doc) {
        final MarkLogicPersistentEntity<?> entity = getMappingContext().getPersistentEntity(source.getClass());

        if (entity != null && entity.hasIdProperty()) {
            PersistentProperty idProperty = entity.getPersistentProperty(entity.getIdProperty().getName());

            if (Collection.class.isAssignableFrom(idProperty.getType()) || Map.class.isAssignableFrom(idProperty.getType()))
                throw new IllegalArgumentException("Collection types not supported as entity id");

            try {
                // TODO: Better way to do this?
                // TODO: Support document URI templates if the ID value is null
                Object id = idProperty.getGetter().invoke(source);
                doc.setUri(getDocumentUris(asList(id), entity.getType()).get(0));
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to access value of @Id from " + idProperty.getName());
            }
        } else {
            throw new IllegalArgumentException("Your entity of type " + source.getClass().getName() + " does not have a method or field annotated with org.springframework.data.annotation.Id");
        }

        if (entity.getTypePersistenceStrategy() == TypePersistenceStrategy.COLLECTION) {
            if (doc.getMetadata() == null) doc.setMetadata(new DocumentMetadataHandle());
            doc.setMetadata(doc.getMetadata().withCollections(entity.getTypeName()));
        } else {
            doc.setMetadata(new DocumentMetadataHandle());
        }

        doc.setFormat(entity.getDocumentFormat());
        doWrite(source, doc);
    }

    @Override
    public <R> R read(Class<R> clazz, DocumentDescriptor doc) {
        final MarkLogicPersistentEntity<?> entity = getMappingContext().getPersistentEntity(clazz);

        R mapped = doRead(clazz, doc);

        // We assume that the ID from the database is the correct one, so update the property with the @Id annotation with the "correct" ID
        if (entity != null && entity.hasIdProperty() && mapped != null) {
            PersistentProperty idProperty = entity.getPersistentProperty(entity.getIdProperty().getName());
            try {
                // TODO: Better way to do this?
                Method setter = idProperty.getSetter();
                if (setter != null) setter.invoke(mapped, uriToId(doc.getUri(), entity.getDocumentFormat(), entity.getBaseUri(), idProperty.getType()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to set value of @Id from " + idProperty.getName());
            }
        }

        return mapped;
    }

    @Override
    public List<String> getDocumentUris(List<?> ids) {
        return getDocumentUris(ids, null);
    }

    @Override
    public <T> List<String> getDocumentUris(List<?> ids, Class<T> entityClass) {

        final List<String> uris = ids.stream()
                .flatMap(id -> {
                    if (entityClass != null) {
                        final MarkLogicPersistentEntity<?> entity = getMappingContext().getPersistentEntity(entityClass);
                        return Stream.of(idToUri(id, entity.getDocumentFormat(), entity.getBaseUri()));
                    } else {
                        // Just from the ID we don't know the type, or can't infer it, so we need to "try" both.  The potential downside
                        // is if they have both JSON/XML for the same id - might get "odd" results?
                        return Stream.of(
                                idToUri(id, Format.JSON, "/"),
                                idToUri(id, Format.XML, "/")
                        );
                    }
                })
                .collect(Collectors.toList());

        return uris;
    }

    private Object uriToId(String uri, Format format, String baseUri, Class<?> idType) {
        String id = formats
                .matcher(uri.substring(baseUri.length()))
                .replaceAll("");
        return converter.convert(id, idType);
    }

    private String idToUri(Object id, Format format, String baseUri) {
        return baseUri + String.valueOf(id) + "." + format.toString().toLowerCase();
    }

    @Override
    public <T> QueryDefinition wrapQuery(StructuredQueryDefinition query, Class<T> entityClass) {
        boolean isRaw = query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition) query).isQbe();
        CombinedQueryDefinition combined = combine(query).type(entityClass);
        if (isRaw) {
            return combined.getRawQbe();
        } else {
            return combined;
        }
    }

    @Override
    public abstract <R> R doRead(Class<R> type, DocumentDescriptor source);

    @Override
    public abstract void doWrite(Object source, DocumentDescriptor sink);

}
