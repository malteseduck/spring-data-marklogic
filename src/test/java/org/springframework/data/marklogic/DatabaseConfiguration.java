package org.springframework.data.marklogic;

import com.marklogic.client.DatabaseClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;

@Configuration
@ImportResource("classpath:integration.xml")
public class DatabaseConfiguration {

    private DatabaseClient client;

    @Autowired
    public void setClient(DatabaseClient client) {
        this.client = client;
    }

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        HttpClient client = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(provider())
                .build();
        factory.setHttpClient(client);
        return new RestTemplate(factory);
    }

    private CredentialsProvider provider() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(client.getUser(), client.getPassword());
        provider.setCredentials(AuthScope.ANY, credentials);
        return provider;
    }

    @Value("classpath:database-properties.json")
    private Resource configuration;

    @PostConstruct
    public void configureDatabase() throws IOException {
        String json = new String(Files.readAllBytes(configuration.getFile().toPath()));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        restTemplate().exchange(
                // Need to use the management port for configuring the database
                new UriTemplate("http://{host}:{port}/manage/v2/databases/Documents/properties").expand(client.getHost(), 8002),
                HttpMethod.PUT,
                new HttpEntity<>(json, headers),
                Void.class
        );
    }
}
