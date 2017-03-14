# Spring Data MarkLogic

An implementation of the Spring Data interfaces for the MarkLogic NoSQL database that supports the following features:

- Spring configuration support using Java based @Configuration classes
- MarkLogicTemplate helper class for common MarkLogic operations. Includes integrated object mapping between documents and POJOs (using Jackson by Default).
- Exception translation into Spring’s portable Data Access Exception hierarchy
- Automatic implementation of Repository interfaces including support for custom finder methods.
- Annotated queries for QBE queries

## Getting Started

Spring MarkLogic support requires MarkLogic 8 or above and Java 8 or above. You will need a running MarkLogic server in order to get started.  For more information see [Installing MarkLogic Server](http://docs.marklogic.com/guide/installation/procedures#id_28962).

Include the following dependency in your project's `pom.xml` dependencies section:

```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-marklogic</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

To start out create a simple Person class you can save into the database:

```java
package org.spring.marklogic.example;

public class Person {

  @Id
  private String id;
  private String name;
  private int age;

  public Person() {}

  public Person(String id, String name, int age) {
    this.id = id;
    this.name = name;
    this.age = age;
  }

  public String getId() {
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
    return "Person(id=" + id + ", name=" + name + ", age=" + age + ")";
  }
}
```

and a main application to run:

```java
package org.spring.marklogic.example;

import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.MarkLogicTemplate;

import com.mongodb.Mongo;

public class MongoApp {

  private static final Log log = LogFactory.getLog(MongoApp.class);

  public static void main(String[] args) throws Exception {

    MongoOperations mongoOps = new MongoTemplate(new Mongo(), "database");
    mongoOps.insert(new Person("Joe", 34));

    log.info(mongoOps.findOne(new Query(where("name").is("Joe")), Person.class));

    mongoOps.dropCollection("person");
  }
}
```