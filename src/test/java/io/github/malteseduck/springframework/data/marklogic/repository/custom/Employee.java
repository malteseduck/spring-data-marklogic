package io.github.malteseduck.springframework.data.marklogic.repository.custom;

import io.github.malteseduck.springframework.data.marklogic.core.mapping.Document;
import io.github.malteseduck.springframework.data.marklogic.core.mapping.TypePersistenceStrategy;

@Document(typeStrategy = TypePersistenceStrategy.NONE)
public class Employee extends Person {
    String title;

    public Employee() {
        super();
    }

    public Employee(String name, String title) {
        super(name, "employee");
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
