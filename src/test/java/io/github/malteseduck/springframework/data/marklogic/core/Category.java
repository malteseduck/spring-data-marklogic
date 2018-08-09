package io.github.malteseduck.springframework.data.marklogic.core;

import io.github.malteseduck.springframework.data.marklogic.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.util.UUID;

import static io.github.malteseduck.springframework.data.marklogic.core.mapping.TypePersistenceStrategy.URI;

@Document(uri = "/test/categories/", typeStrategy = URI)
public class Category {
    @Id
    private UUID id = UUID.randomUUID();
    private String title;

    public UUID getId() {
        return id;
    }

    public Category setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Category setTitle(String title) {
        this.title = title;
        return this;
    }
}
