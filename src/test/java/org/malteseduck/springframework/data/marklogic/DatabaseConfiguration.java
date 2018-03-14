package org.malteseduck.springframework.data.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory;
import com.marklogic.client.admin.QueryOptionsManager;
import com.marklogic.client.admin.ServerConfigurationManager;
import com.marklogic.client.admin.TransformExtensionsManager;
import com.marklogic.client.extra.okhttpclient.OkHttpClientConfigurator;
import com.marklogic.client.io.StringHandle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
@ImportResource("classpath:integration.xml")
public class DatabaseConfiguration {

    // Uncomment this in order to proxy calls to the database for debugging, etc.
//    static {
//        DatabaseClientFactory.addConfigurator((OkHttpClientConfigurator) client -> {
//            if (client != null) {
//                client.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888)));
//            }
//        });
//    }

    private MarkLogicOperations operations;
    private DatabaseClient client;
    private Resource configuration;
    private Resource queryTransform;
    private Resource writeTransform;
    private Resource facetOptions;

    public DatabaseConfiguration(
            MarkLogicOperations operations,
            DatabaseClient client,
            @Value("classpath:database-properties.json")
                    Resource configuration,
            @Value("classpath:transforms/query-transform.sjs")
                    Resource queryTransform,
            @Value("classpath:transforms/write-transform.sjs")
                    Resource writeTransform,
            @Value("classpath:options/facets.xml")
                    Resource facetOptions
    ) throws IOException {
        this.operations = operations;
        this.client = client;
        this.configuration = configuration;
        this.queryTransform = queryTransform;
        this.writeTransform = writeTransform;
        this.facetOptions = facetOptions;
        init();
    }

    public void init() throws IOException {
        operations.configure(configuration);

        ServerConfigurationManager configMgr = client.newServerConfigManager();
        TransformExtensionsManager transMgr = configMgr.newTransformExtensionsManager();

        String theQueryTransform = new String(Files.readAllBytes(Paths.get(queryTransform.getURI())));
        transMgr.writeJavascriptTransform("query-transform", new StringHandle(theQueryTransform));

        String theWriteTransform = new String(Files.readAllBytes(Paths.get(writeTransform.getURI())));
        transMgr.writeJavascriptTransform("write-transform", new StringHandle(theWriteTransform));

        QueryOptionsManager optionsMgr = configMgr.newQueryOptionsManager();
        String theFacetOptions = new String(Files.readAllBytes(Paths.get(facetOptions.getURI())));
        optionsMgr.writeOptions("facets", new StringHandle(theFacetOptions));
    }
}
