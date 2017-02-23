package org.springframework.data.marklogic.core;

import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Person {

    @Id
    private String id;
    private String name;
    private int age;
    private String gender;
    private String occupation;
    private String description;
    private Instant birthtime;
    private List<String> hobbies;

    public Person() {
        this.id = UUID.randomUUID().toString();
    }

    public Person(String name) {
        this();
        this.name = name;
        this.birthtime = Instant.now();
    }

    public Person(String name, int age, String gender, String occupation, String description, Instant birthtime) {
        this(name, age, gender, occupation, description, birthtime, new ArrayList<>());
    }

    public Person(String name, int age, String gender, String occupation, String description, Instant birthtime, List<String> hobbies) {
        this();
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.occupation = occupation;
        this.description = description;
        this.birthtime = birthtime;
        this.hobbies = hobbies;
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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getBirthtime() {
        return birthtime;
    }

    public void setBirthtime(Instant birthtime) {
        this.birthtime = birthtime;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public void setHobbies(List<String> hobbies) {
        this.hobbies = hobbies;
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
