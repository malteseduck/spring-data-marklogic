package org.springframework.data.marklogic.core.convert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.JacksonDatabindHandle;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.mapping.*;
import org.springframework.data.marklogic.repository.query.CombinedQueryDefinition;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.springframework.data.marklogic.core.mapping.DocumentFormat.JSON;
import static org.springframework.data.marklogic.core.mapping.DocumentFormat.XML;

public class MappingMarkLogicConverter implements MarkLogicConverter, InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(MappingMarkLogicConverter.class);

    public static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static SimpleDateFormat simpleDateFormat8601 = new SimpleDateFormat(ISO_8601_FORMAT);
    static { simpleDateFormat8601.setTimeZone(TimeZone.getTimeZone("UTC")); }

    private MappingContext mappingContext;
    private ObjectMapper objectMapper;
    private ObjectMapper xmlMapper;

    public MappingMarkLogicConverter(MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext) {
        this.mappingContext = mappingContext;
    }

    @Override
    public MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> getMappingContext() {
        return mappingContext;
    }

    @Override
    public ConversionService getConversionService() {
        return null;
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
            doc.setMetadata(doc.getMetadata().withCollections(getTypeName(entity)));
        } else {
            doc.setMetadata(new DocumentMetadataHandle());
        }

        JacksonDatabindHandle contentHandle = new JacksonDatabindHandle<>(source);
        if (mapAsXml(entity)) {
            contentHandle.setMapper(xmlMapper);
        } else {
            contentHandle.setMapper(objectMapper);
        }

        doc.setFormat(entity.getDocumentFormat().toFormat());
        doc.setContent(contentHandle);
    }

    @Override
    public <R> R read(Class<R> clazz, DocumentDescriptor doc) {
        final MarkLogicPersistentEntity<?> entity = getMappingContext().getPersistentEntity(clazz);

        JacksonDatabindHandle<R> handle = new JacksonDatabindHandle<>(clazz);
        if (mapAsXml(entity)) {
            handle.setMapper(xmlMapper);
        } else {
            handle.setMapper(objectMapper);
        }

        R mapped = doc.getRecord().getContent(handle).get();

        // We assume that the ID from the database is the correct one, so update the property with the @Id annotation with the "correct" ID
        if (entity != null && entity.hasIdProperty() && mapped != null) {
            PersistentProperty idProperty = entity.getPersistentProperty(entity.getIdProperty().getName());
            try {
                // TODO: Better way to do this?
                Method setter = idProperty.getSetter();
                if (setter != null) setter.invoke(mapped, uriToId(doc.getUri(), entity.getDocumentFormat()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to set value of @Id from " + idProperty.getName());
            }
        }

        return mapped;
    }

    @Override
    public <T> List<String> getDocumentUris(List<?> ids, Class<T> entityClass) {

        final List<String> uris = ids.stream()
                .flatMap(id -> {
                    if (entityClass != null) {
                        final MarkLogicPersistentEntity<?> entity = getMappingContext().getPersistentEntity(entityClass);
                        return Stream.of(idToUri(id, entity.getDocumentFormat()));
                    } else {
                        // Just from the ID we don't know the type, or can't infer it, so we need to "try" both.  The potential downside
                        // is if they have both JSON/XML for the same id - might get "odd" results?
                        return Stream.of(
                                idToUri(id, JSON),
                                idToUri(id, XML)
                        );
                    }
                })
                .collect(Collectors.toList());

        return uris;
    }

    @Override
    public boolean mapAsXml(MarkLogicPersistentEntity entity) {
        return entity != null && entity.getDocumentFormat() == XML && xmlMapper != null;
    }

    @Override
    public List<String> getDocumentUris(List<? extends Object> ids) {
        return getDocumentUris(ids, null);
    }

    private Object uriToId(String uri, DocumentFormat format) {
        // TODO: For other types other than String what do we do?
        return uri.substring(1, uri.indexOf("." + format.toString().toLowerCase()));
    }

    private String idToUri(Object id, DocumentFormat format) {
        return "/" + String.valueOf(id) + "." + format.toString().toLowerCase();
    }

    @Override
    public <T> QueryDefinition wrapQuery(StructuredQueryDefinition query, Class<T> entityClass) {
        if (query == null) {
            query = new StructuredQueryBuilder().and();
        }
        if (entityClass != null) {
            MarkLogicPersistentEntity entity = getMappingContext().getPersistentEntity(entityClass);

            // TODO: If type information is configured on a property then add the value clause here
            if (entity != null && entity.getTypePersistenceStrategy() == TypePersistenceStrategy.COLLECTION) {
                List<String> collections = new ArrayList<>();
                Collections.addAll(collections, query.getCollections());
                collections.add(getTypeName(entity));
                query.setCollections(collections.toArray(new String[0]));
            }
        }
        if (query instanceof CombinedQueryDefinition && ((CombinedQueryDefinition) query).isQbe())
            return ((CombinedQueryDefinition) query).getQbe();
        else
            return query;
    }

    @Override
    public void afterPropertiesSet() {
        objectMapper = new ObjectMapper()
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .setDateFormat(simpleDateFormat8601)
                .registerModule(new JavaTimeModule())
                // Since we don't configure to "wrap" in the class name we can't do "type scoped" path range indexes - could be a problem withOptions larger data sets
                .disableDefaultTyping();

        try {
            // TODO: Is it just easier/better to include the dumb library?  It will cause the default behavior to change for Spring Web stuff
            Class mapperClass = Class.forName("com.fasterxml.jackson.dataformat.xml.XmlMapper", false, this.getClass().getClassLoader());
            xmlMapper = ((ObjectMapper) mapperClass.newInstance())
                    .configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false)
                    .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                    .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .setDateFormat(MappingMarkLogicConverter.simpleDateFormat8601)
                    .registerModule(new JavaTimeModule())
                    .disableDefaultTyping();
        } catch (ClassNotFoundException e) {
            LOG.warn("com.fasterxml.jackson.dataformat:jackson-dataformat-xml needs to be included in order to use Java->XML conversion");
        } catch (IllegalAccessException e) {
            LOG.warn("Unable to instantiate XmlMapper instance in order to use Java->XML conversion");
        } catch (InstantiationException e) {
            LOG.warn("Unable to instantiate XmlMapper instance in order to use Java->XML conversion");
        }

    }

    public String getTypeName(MarkLogicPersistentEntity entity) {
        // TODO: Add support for full class name for collection
        return entity.getType().getSimpleName();
    }
}
