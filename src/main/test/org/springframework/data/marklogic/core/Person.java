package org.springframework.data.marklogic.core;

import org.springframework.data.annotation.Id;

import java.util.UUID;

public class Person {

    @Id
    private String id;
    private String name;

    public Person() {}

    public Person(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
