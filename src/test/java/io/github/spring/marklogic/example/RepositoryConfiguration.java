package io.github.spring.marklogic.example;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.marklogic.client.DatabaseClientFactory.newClient;

@Configuration
public class RepositoryConfiguration {

    @Bean
    public MarkLogicOperations marklogicTemplate() {
        DatabaseClient client = newClient("localhost", 8000, new DigestAuthContext("admin", "admin"));
        return new MarkLogicTemplate(client);
    }
}
