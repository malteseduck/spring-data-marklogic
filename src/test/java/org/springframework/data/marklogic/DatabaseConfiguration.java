package org.springframework.data.marklogic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.data.marklogic.core.MarkLogicOperations;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
@ImportResource("classpath:integration.xml")
public class DatabaseConfiguration {

    @Autowired
    private MarkLogicOperations operations;

    @Value("classpath:database-properties.json")
    private Resource configuration;

    @PostConstruct
    public void configureDatabase() throws IOException {
        operations.configure(configuration);
    }
}
