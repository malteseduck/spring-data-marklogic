package org.spring.marklogic.example;

import org.springframework.data.annotation.Id;

public class Person {

    @Id
    private int id;
    private String name;
    private int age;

    public Person() {}

    public Person(int id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public String toString() {
        return "Person [id=" + id + ", name=" + name + ", age=" + age + "]";
    }
}