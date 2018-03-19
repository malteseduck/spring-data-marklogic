package io.github.malteseduck.springframework.data.marklogic.repository.query;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.io.StringHandle;
import com.marklogic.client.query.StructuredQueryDefinition;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;
import io.github.malteseduck.springframework.data.marklogic.core.convert.JacksonMarkLogicConverter;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.DefaultEvaluationContextProvider;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

import static io.github.malteseduck.springframework.data.marklogic.core.convert.JacksonMarkLogicConverter.simpleDateFormat8601;
import static io.github.malteseduck.springframework.data.marklogic.repository.query.CombinedQueryDefinitionBuilder.combine;
import static io.github.malteseduck.springframework.data.marklogic.repository.query.StubParameterAccessor.getAccessor;

public class QueryTestUtils {

    static SpelExpressionParser PARSER = new SpelExpressionParser();

    private static ObjectMapper xmlMapper = new XmlMapper()
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setDateFormat(simpleDateFormat8601)
            .registerModule(new JavaTimeModule())
            .disableDefaultTyping();


    private static ObjectMapper objectMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setDateFormat(simpleDateFormat8601)
            .registerModule(new JavaTimeModule())
            // Since we don't configure to "wrap" in the class name we can't do "type scoped" path range indexes - could be a problem options larger data sets
            .disableDefaultTyping();

    private static final DatabaseClient client = DatabaseClientFactory.newClient("nowhere", 23,
            new DatabaseClientFactory.DigestAuthContext("nobody", "nothing"));

    public static String stringify(Object json) {
        try {
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static InputStream stream(Object... json) {
        Enumeration<? extends InputStream> results = Collections.enumeration(
                Arrays.stream(json)
                        .map(record -> {
                            InputStream stream = null;
                            try {
                                stream = new ByteArrayInputStream(
                                        // Do the string replace because the streams that come back from the server are more liberal with spaces than Jackson is
                                        new String(objectMapper.writeValueAsBytes(record))
                                                .replace(",", ", ")
                                                .replace(",  ", ", ")
                                                .getBytes()
                                );
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                            return stream;
                        })
                        .collect(Collectors.toList()));

        return new SequenceInputStream(results);
    }

    public static InputStream streamXml(Object... xml) {
        Enumeration<? extends InputStream> results = Collections.enumeration(
                Arrays.stream(xml)
                        .map(record -> {
                            InputStream stream = null;
                            try {
                                stream = new ByteArrayInputStream(xmlMapper.writeValueAsBytes(record));
                            } catch (JsonProcessingException e) {
                                e.printStackTrace();
                            }
                            return stream;
                        })
                        .collect(Collectors.toList()));

        return new SequenceInputStream(results);
    }

    public static DatabaseClient client() {
        return client;
    }

    public static String rawQuery(String qbe) {
        return combine()
                .byExample(client().newQueryManager().newRawQueryByExampleDefinition(new StringHandle(qbe))
        ).serialize();
    }

    public static String jsonQuery(String qbe) throws IOException {
        return objectMapper.readTree(qbe).toString().replace("%", "");
    }

    public static MarkLogicQueryMethod queryMethod(Class<?> repository, String name, Class<?>... parameters) throws Exception {
        Method method = repository.getMethod(name, parameters);
        ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
        return new MarkLogicQueryMethod(method, new DefaultRepositoryMetadata(repository), factory, new MarkLogicMappingContext());
    }

    public static StructuredQueryDefinition stringQuery(MarkLogicQueryMethod method, Object... parameters) throws Exception {
        MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> context = new MarkLogicMappingContext();
        return new StringMarkLogicQuery(method, new MarkLogicTemplate(client(), new JacksonMarkLogicConverter(context)), PARSER, DefaultEvaluationContextProvider.INSTANCE).createQuery(getAccessor(parameters));
    }

    public static PartTreeMarkLogicQuery tree(MarkLogicQueryMethod method) {
        MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> context = new MarkLogicMappingContext();
        return new PartTreeMarkLogicQuery(method, new MarkLogicTemplate(client(), new JacksonMarkLogicConverter(context)));
    }

    public static MarkLogicQueryCreator creator(MarkLogicQueryMethod method, Object... parameters) {
        MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> context = new MarkLogicMappingContext();
        PartTree tree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());
        return new MarkLogicQueryCreator(tree, getAccessor(parameters), new MarkLogicTemplate(client(), new JacksonMarkLogicConverter(context)), context, method);
    }
}
