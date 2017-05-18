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
    <version>1.0.0.RELEASE</version>
</dependency>
```

