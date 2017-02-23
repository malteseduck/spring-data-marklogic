package org.springframework.data.marklogic;

import com.marklogic.client.DatabaseClientFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.data.marklogic.core.MarkLogicClientFactoryBean;
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

public class InfrastructureConfiguration {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 8000;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    @Bean
    public MarkLogicClientFactoryBean client() {
        MarkLogicClientFactoryBean client = new MarkLogicClientFactoryBean();
        client.setHost(HOST);
        client.setPort(PORT);
        client.setUser(USERNAME);
        client.setPassword(PASSWORD);
        client.setType(DatabaseClientFactory.Authentication.DIGEST);
        return client;
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
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(USERNAME, PASSWORD);
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
                new UriTemplate("http://{host}:{port}/manage/v2/databases/Documents/properties").expand(HOST, PORT),
                HttpMethod.PUT,
                new HttpEntity<>(json, headers),
                Void.class
        );
    }
}
