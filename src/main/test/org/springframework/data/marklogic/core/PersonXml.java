package org.springframework.data.marklogic.core;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.springframework.data.annotation.Id;
import org.springframework.data.marklogic.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.marklogic.core.mapping.DocumentFormat.XML;

@Document(format = XML)
@JacksonXmlRootElement(localName = "person")
public class PersonXml {

    @Id
    private String id;
    private String lang;
    private String name;
    private int age;
    private String gender;
    private String occupation;
    private String description;
    private Instant birthtime;
    private List<String> hobbies;

    public PersonXml() {
        this.id = UUID.randomUUID().toString();
    }

    public PersonXml(String name) {
        this();
        this.name = name;
        this.lang = "eng";
        this.birthtime = Instant.now();
    }

    public PersonXml(String name, int age, String gender, String occupation, String description, Instant birthtime) {
        this(name, age, gender, occupation, description, birthtime, new ArrayList<>());
    }

    public PersonXml(String name, int age, String gender, String occupation, String description, Instant birthtime, List<String> hobbies) {
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

    @JacksonXmlProperty(isAttribute = true, localName = "xml:lang")
    public String getLang() {
        return lang;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "lang")
    public void setLang(String lang) {
        this.lang = lang;
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

        PersonXml personXml = (PersonXml) o;

        return id != null ? id.equals(personXml.id) : personXml.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
