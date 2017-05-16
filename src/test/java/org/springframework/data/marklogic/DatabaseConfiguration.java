package org.springframework.data.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.admin.TransformExtensionsManager;
import com.marklogic.client.io.StringHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.data.marklogic.core.MarkLogicOperations;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
@ImportResource("classpath:integration.xml")
public class DatabaseConfiguration {

    @Autowired
    private MarkLogicOperations operations;

    @Autowired
    private DatabaseClient client;

    @Value("classpath:database-properties.json")
    private Resource configuration;

    @Value("classpath:transforms/query-transform.sjs")
    private Resource queryTransform;

    @Value("classpath:transforms/write-transform.sjs")
    private Resource writeTransform;

    @PostConstruct
    public void configureDatabase() throws IOException {
        operations.configure(configuration);

        TransformExtensionsManager transMgr = client.newServerConfigManager().newTransformExtensionsManager();

        String theQueryTransform = new String(Files.readAllBytes(Paths.get(queryTransform.getURI())));
        transMgr.writeJavascriptTransform("query-transform", new StringHandle(theQueryTransform));

        String theWriteTransform = new String(Files.readAllBytes(Paths.get(writeTransform.getURI())));
        transMgr.writeJavascriptTransform("write-transform", new StringHandle(theWriteTransform));

    }
}
