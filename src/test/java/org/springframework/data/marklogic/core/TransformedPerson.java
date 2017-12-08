package org.springframework.data.marklogic.core;

import org.springframework.data.annotation.Id;
import org.springframework.data.marklogic.core.mapping.Document;

import java.util.UUID;

@Document(transformer = SimplePersonTransformer.class)
public class TransformedPerson {

    @Id
    private String id;
    private String name;


    public TransformedPerson() {
        this.id = UUID.randomUUID().toString();
    }

    public TransformedPerson(String name) {
        this();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TransformedPerson that = (TransformedPerson) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TransformedPerson{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
