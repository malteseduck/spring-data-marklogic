package org.malteseduck.springframework.data.marklogic.core.convert;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.JacksonDatabindHandle;
import com.marklogic.client.query.QueryDefinition;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.malteseduck.springframework.data.marklogic.core.mapping.DocumentDescriptor;
import org.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.malteseduck.springframework.data.marklogic.core.mapping.TypePersistenceStrategy;
import org.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinition;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;

public class JacksonMarkLogicConverter extends AbstractMarkLogicConverter implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(JacksonMarkLogicConverter.class);

    public static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    public static SimpleDateFormat simpleDateFormat8601 = new SimpleDateFormat(ISO_8601_FORMAT);
    static { simpleDateFormat8601.setTimeZone(TimeZone.getTimeZone("UTC")); }

    private ObjectMapper objectMapper;
    private ObjectMapper xmlMapper;

    public JacksonMarkLogicConverter(MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext) {
        super(mappingContext);
    }

    @Override
    public void doWrite(Object source, DocumentDescriptor doc) {
        final MarkLogicPersistentEntity<?> entity = getMappingContext().getPersistentEntity(source.getClass());

        JacksonDatabindHandle contentHandle = new JacksonDatabindHandle<>(source);
        if (mapAsXml(entity)) {
            contentHandle.setMapper(xmlMapper);
        } else {
            contentHandle.setMapper(objectMapper);
        }

        doc.setContent(contentHandle);
    }

    @Override
    public <R> R doRead(Class<R> clazz, DocumentDescriptor doc) {
        final MarkLogicPersistentEntity<?> entity = getMappingContext().getPersistentEntity(clazz);

        JacksonDatabindHandle<R> handle = new JacksonDatabindHandle<>(clazz);
        if (mapAsXml(entity)) {
            handle.setMapper(xmlMapper);
        } else {
            handle.setMapper(objectMapper);
        }

        return doc.getRecord().getContent(handle).get();
    }

    private boolean mapAsXml(MarkLogicPersistentEntity entity) {
        return entity != null && entity.getDocumentFormat() == Format.XML && xmlMapper != null;
    }

    @Override
    public void afterPropertiesSet() {
        objectMapper = new ObjectMapper()
                .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .setDateFormat(simpleDateFormat8601)
                .registerModule(new JavaTimeModule())
                // Since we don't configure to "wrap" in the class name we can't do "type scoped" path range indexes - could be a problem options larger data sets
                .disableDefaultTyping();

        try {
            // TODO: Is it just easier/better to include the dumb library?  It will cause the default behavior to change for Spring Web stuff
            Class mapperClass = Class.forName("com.fasterxml.jackson.dataformat.xml.XmlMapper", false, this.getClass().getClassLoader());
            xmlMapper = ((ObjectMapper) mapperClass.newInstance())
                    .configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false)
                    .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
                    .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
                    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                    .setDateFormat(JacksonMarkLogicConverter.simpleDateFormat8601)
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
}
