package org.malteseduck.springframework.data.marklogic.repository.custom;

import org.malteseduck.springframework.data.marklogic.core.mapping.Document;
import org.malteseduck.springframework.data.marklogic.core.mapping.TypePersistenceStrategy;

@Document(typeStrategy = TypePersistenceStrategy.NONE)
public class Contact extends Person {
    String phone;

    public Contact() {
        super();
    }

    public Contact(String name, String phone) {
        super(name, "contact");
        this.phone = phone;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
