package org.malteseduck.springframework.data.marklogic.core;

import org.springframework.data.annotation.Id;
import org.malteseduck.springframework.data.marklogic.core.mapping.IndexType;
import org.malteseduck.springframework.data.marklogic.core.mapping.Indexed;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Person {

    @Id
    private String id;
    private String name;
    private int age;
    private boolean active = true;
    private String gender;
    private String occupation;
    @Indexed(type = IndexType.ELEMENT)
    private String description;
    @Indexed(type = IndexType.ELEMENT)
    private Instant birthtime;
    private List<Integer> rankings;
    private List<String> hobbies;
    private List<Pet> pets;

    public Person() {
        this.id = UUID.randomUUID().toString();
    }

    public Person(String name) {
        this(name, 0, "female", "", "", Instant.now(), new ArrayList<>(), new ArrayList<>());
    }

    public Person(String name, int age, String gender, String occupation, String description, Instant birthtime) {
        this(name, age, gender, occupation, description, birthtime, new ArrayList<>());
    }

    public Person(String name, int age, String gender, String occupation, String description, Instant birthtime, List<String> hobbies) {
        this(name, age, gender, occupation, description, birthtime, hobbies, new ArrayList<>());
    }

    public Person(String name, int age, String gender, String occupation, String description, Instant birthtime, List<String> hobbies, List<Pet> pets) {
        this();
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.occupation = occupation;
        this.description = description;
        this.birthtime = birthtime;
        this.rankings = new ArrayList<>();
        this.hobbies = hobbies;
        this.pets = pets;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public List<Integer> getRankings() {
        return rankings;
    }

    public void setRankings(List<Integer> rankings) {
        this.rankings = rankings;
    }

    public List<String> getHobbies() {
        return hobbies;
    }

    public void setHobbies(List<String> hobbies) {
        this.hobbies = hobbies;
    }

    public List<Pet> getPets() {
        return pets;
    }

    public void setPets(List<Pet> pets) {
        this.pets = pets;
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
