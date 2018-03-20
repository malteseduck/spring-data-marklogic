package io.github.malteseduck.springframework.data.marklogic.repository.custom;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.Document;
import org.springframework.data.annotation.Id;

import java.util.UUID;

import static io.github.malteseduck.springframework.data.marklogic.core.mapping.TypePersistenceStrategy.NONE;
import static io.github.malteseduck.springframework.data.marklogic.repository.custom.Person.CONTACT;
import static io.github.malteseduck.springframework.data.marklogic.repository.custom.Person.EMPLOYEE;

@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, include= JsonTypeInfo.As.PROPERTY, property="type")
@JsonSubTypes({
    @JsonSubTypes.Type(value=Employee.class, name=EMPLOYEE),
    @JsonSubTypes.Type(value=Contact.class, name=CONTACT),
})
@Document(typeStrategy = NONE)
public abstract class Person {

    public static final String EMPLOYEE = "employee";
    public static final String CONTACT = "contact";

    @Id
    String id;
    String type;
    String name;

    public Person(String name) {
        this(name, "person");
    }

    public Person(String name, String type) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.name = name;
    }

    public Person() {}

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
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

        Person person = (Person) o;

        return id != null ? id.equals(person.id) : person.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Person(name=" + name + ", id=" + id + ")";
    }

}
