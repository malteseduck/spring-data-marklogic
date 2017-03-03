package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.MarkLogicTemplate;
import org.springframework.data.marklogic.core.convert.MappingMarkLogicConverter;
import org.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;

import static org.springframework.data.marklogic.repository.query.StubParameterAccessor.getAccessor;

public class QueryTestUtils {

    private static final DatabaseClient client = DatabaseClientFactory.newClient("nowhere", 23);

    public static DatabaseClient client() {
        return client;
    }

    public static MarkLogicQueryMethod queryMethod(Class<?> repository, String name, Class<?>... parameters) throws Exception {
        Method method = repository.getMethod(name, parameters);
        ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
        return new MarkLogicQueryMethod(method, new DefaultRepositoryMetadata(repository), factory, new MarkLogicMappingContext());
    }

    public static MarkLogicQueryCreator creator(MarkLogicQueryMethod method, Object... parameters) {
        MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> context = new MarkLogicMappingContext();
        PartTree tree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());
        return new MarkLogicQueryCreator(tree, getAccessor(parameters), new MarkLogicTemplate(client(), new MappingMarkLogicConverter(context)), context, method);
    }
}
