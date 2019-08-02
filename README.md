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
    <groupId>io.github.malteseduck.springframework.data</groupId>
    <artifactId>spring-data-marklogic</artifactId>
    <version>2.1.0.RELEASE</version>
</dependency>
```

Create a simple Person class that you can use to save person information:

```java
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
```

And an application:

```java
package org.spring.marklogic.example;

import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import com.marklogic.client.query.StructuredQueryBuilder;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;

import static com.marklogic.client.DatabaseClientFactory.newClient;

public class MarkLogicApp {

    private static final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    public static void main(String[] args) throws Exception {

        MarkLogicOperations ops = new MarkLogicTemplate(newClient("localhost", 8000, new DigestAuthContext("admin", "admin")));
        ops.write(new Person("Bobby", 23));

        Person bobby = ops.searchOne(qb.value(qb.jsonProperty("name"), "Bobby"), Person.class);
        System.out.println(bobby);

        ops.deleteAll("Person");
    }

}
```

This will produce output similar to the following:

```bash
Person [id=1234, name=Bobby, age=23]
```

There are a few things of note in this example:

- By default this uses Jackson to serialize your POJOs for persistence in the database, and deserialize them when pulling data out of the database.  There is an empty default constructor because Jackson creates the POJO instance first before setting the fields from the data
- An `@Id` annotation is required to identify the field that will hold the document identifier.  Internally documents are stored under a URI, so a person with an id of "1234" would be saved under the URI "/Person/1234.json"
- You can create the template class for MarkLogic, `MarkLogicTemplate`, by passing in a `DatabaseClient` that you get from the `DatabaseClientFactory`


### Connecting to MarkLogic with Spring
The simplest way to connect to MarkLogic is to create a @Bean that creates a template object:

```java
package org.spring.marklogic.example;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;

import static com.marklogic.client.DatabaseClientFactory.newClient;

@Configuration
public class RepositoryConfiguration {

    @Bean
    public MarkLogicOperations marklogicTemplate() {
        DatabaseClient client = newClient("localhost", 8000, new DigestAuthContext("admin", "admin"));
        return new MarkLogicTemplate(client);
    }
}
```

This template will automatically be wired into any of your `@Repository` annotated classes (or ones that extend `MarkLogicRepository`).  You can also explicitly auto-wired it in your other Spring classes:

```java
@Autowired
private MarkLogicOperations ops;
```

## Introduction to `MarkLogicTemplate`
The `MarkLogicTemplate` class is the central class you can use to interact with a MarkLogic database.  It provides a simple interface to do the main CRUD operations against a database, as well as additional helper methods to help you build your queries.

When using the template it is recommended that you interact with [`MarkLogicOperations`](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/MarkLogicOperations.html) instead of with `MarkLogicTemplate` directly.

The naming conventions for `MarkLogicOperations` is patterned after MarkLogic's [`PojoRepository`](http://docs.marklogic.com/javadoc/client/index.html), so you will find `read`, `write`, `search`, `count`, `exists`, and `delete` methods, as well as some variants from those.

### Entity Mapping

The mapping between Java entity classes and documents in MarkLogic is handled by the `MarkLogicConverter` interface.  By default the template uses the `JacksonMarkLogicConverter` which uses MarkLogic's `JacksonDatabindHandle` (and Jackson internally) to do the mapping.  You can implement your own converter using the `MarkLogicConverter` interface and specify at template creation time to use it.

#### @Document Annotation

The [@Document](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/mapping/Document.html) annotation is main way to configure how your entities get mapped/transformed/etc. into documents is through the use of this annotation.  By default the converter will convert your entity into a JSON document, under a "directory" with the name of the entity type (i.e. a "Person" entity will be saved under a "/Person/" uri). 

There are various options available to configure this, though, which we will explain in this section.

#### `uri`

The base URI for documents of the annotated type.  If the type persistence strategy is set to "URI" then will scope all queries to limit to only documents under this URI.  Defaults to "/TYPE_NAME/".

#### `format`

The serialization format to use for documents of the annotated type, either "JSON" or "XML".

#### `type`

The name to use for the type of document which will be persisted into the database.  This overrides the default of using the class simple name (or full class name, depending on configuration).

#### `typeStrategy`

 Set to scope queries to a "type" as defined by the configured strategy.  Can be either "COLLECTION" - which means that the documents will be stored in a collection with the type name (in addition to under a uri of that name), and queries scoped to that collection, "URI" - which means queries will be scoped to the entity uri, or "NONE" - where queries will not be scoped by type by default.

#### `transformer`

The configured transformer class to use for the entity for server read/write transforms.  An implementation of the [ServerTransformer](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/convert/ServerTransformer.html) interface.

### Constructors
There are three different constructors you can use to create an instance of `MarkLogicTemplate`:

```java
// Create a template interface using the specified database client and the default entity converter and query conversion service.
MarkLogicTemplate(com.marklogic.client.DatabaseClient client)

// Create a template interface using the specified database client and the default query conversion service.
MarkLogicTemplate(com.marklogic.client.DatabaseClient client, MarkLogicConverter converter)

// Create a template interface using the specified database client, converter, and conversion service.
MarkLogicTemplate(com.marklogic.client.DatabaseClient client, MarkLogicConverter converter, QueryConversionService queryConversionService)
```

Usually the defaults are sufficient so you can create you template just like the example at the beginning of this document.

### CRUD Operations
The template provides many functions that allow you to read, write, and delete documents from the database.  The methods you can use to write are:

```java
    <T> T write(T entity);

    <T> T write(T entity, String... collections);

    <T> T write(T entity, ServerTransform transform);

    <T> T write(T entity, ServerTransform transform, String... collections);

    <T> List<T> write(List<T> entities);

    <T> List<T> write(List<T> entities, String... collections);

    <T> List<T> write(List<T> entities, ServerTransform transform);

    <T> List<T> write(List<T> entities, ServerTransform transform, String... collections);
```

Here are the ones for simple reads:

```java
    List<DocumentRecord> read(List<?> uris);

    <T> T read(Object id, Class<T> entityClass);

    <T> List<T> read(List<?> ids, Class<T> entityClass);
```

And finally, here are ones you can use to remove documents from the database:

```java
    void deleteByUri(String... uris);

    void deleteByUris(List<String> uris);

    <T> void deleteById(Object id, Class<T> entityClass);

    <T> void deleteByIds(List<?> ids, Class<T> entityClass);

    <T> void dropCollection(Class<T> entityClass);

    <T> void dropCollections(Class<T>... entityClasses);

    void dropCollections(String... collections);

    <T> void delete(List<T> entities);

    <T> void delete(StructuredQueryDefinition query, Class<T> entityClass);
```

For more details on what each function does, as well as additional functions for "count" and "exists" operations, see the javadocs for [`MarkLogicOperations`](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/MarkLogicOperations.html) .

### Notes on "@Id"
All your entities need to have a single field specified as the "ID" of the entity that will be used to create the URI (primary key) of the document in the database.  By default the URI consists of the type of entity with id value and type extensions added on.  For example, a `Person` entity with an `id` field with the value of `1234` would by default get saved under the URI `/Person/1234.json` in the database.

This behavior can be modified through use of the [`@Document`](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/mapping/Document.html) annotation.  You can change the default path under which documents get saved, as well as change the type (so XML documents get saved with ".xml").  In order to have more control over how the URIs are generated you can override the `MarkLogicConverter` [`getDocumentUris()`](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/convert/MarkLogicConverter.html#getDocumentUris-java.util.List-java.lang.Class-) methods.

## Building Queries
There are four main ways to build your queries:

- "Finder" queries defined in repositories
- Query By Example string queries, defined with the `@Query` annotation
- Structured queries created with a `StructuredQueryBuilder` interface
- Using the MarkLogic 'DocumentManager` and `QueryManager` interfaces

If all else fails there is an [`executeWithClient`](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/MarkLogicOperations.html#executeWithClient-org.malteseduck.springframework.data.marklogic.core.ClientCallback-) method you can use in `MarkLogicTemplate` that allows you to construct whatever you need to using the full capabilities of the MarkLogic Java Client API.

We will now go into more detail about each of these approaches.

### Repositories

The simplest approach to creating queries is to define a Repository interface and create query methods using Spring Data patterns, as described here: http://docs.spring.io/spring-data/data-commons/docs/current/reference/html/#repositories. Spring Data MarkLogic has an `@EnableMarkLogicRepositories` annotation that allows you to customize how your repositories are created.  There is also a `MarkLogicRepository` interface you can extend to get a few additional methods inherited for your repository, if you need.

This implementation does not currently implement the functionality to return Java 8 Stream objects or Async query results, mainly only what is available through the `PagingAndSortingRepository` interface.

For more details on query method patterns see the [Spring Data Commons](http://docs.spring.io/spring-data/data-commons/docs/current/reference/html/#repositories) documentation.

### @Query Annotation
As your queries get more complicated you will find that your query method names may start to get unreadable.  You may also want to be able to test your queries in a query console of sorts and you really can't paste a method name and get it to run.  This is where the [`@Query`](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/repository/Query.html) annotation comes in.  This allows you to specify a [Query By Example](http://docs.marklogic.com/guide/search-dev/qbe) query in order to match documents in the database.

One of the nice things about the QBE query is that you can run simple code in MarkLogic's QConsole to test it out.

The `@Query` annotation isn't just for specifying queries, you can also specify query options - you don't even need to specify a query if all you want to do is tweak the query options for one of your query methods.

#### `format`
With this you can specify the format of the documents against which you are querying.  Typically they match the type of document as you have defined with [`@Document`](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/mapping/Document.html), but depending on what transforms you use or other process you have, it may not.

#### `transform`
This allows you to specify the name of a transform to use when reading the documents from the database.  For more information on using transforms see http://docs.marklogic.com/guide/java/transforms.

#### `extract`/`selected`
These are used to specify which properties are included/excluded from the result documents before they return from the database. The `extract` field is for specifying the XPath of each property this effects, and the `selected` allows you whether to include or exclude those fields.

For more information on extracts see [Extracting a Portion of Matching Documents](http://docs.marklogic.com/guide/java/searches#id_90087).

#### `optionsName`
You can persist query configuration into your MarkLogic database and reference that configuration in your queries.  This is a more optimize approach to using options (as opposed to creating ad-hoc ones at query time).  This option allows you to specify the name of one of your persisted options.  These options will be used as part of the annotated query that is run.

#### `searchOptions`
To configure search query options like "faceted", "unchecked", etc.  To see all the available options see the documentation on [cts:search](http://docs.marklogic.com/cts:search)  

### Structured Query Building
If you need more fine-grained control over query options, or need to use features not available with MarkLogic's Query By Example, then you will need to customize your repositories and use `MarkLogicOperations` directly.  When you do this the main way to build your queries is to use a `StructuredQueryBuilder` or `PojoQueryBuilder` interface and pass it into one of the various `search` methods in the template.

For more details on how to use the builders see [Search Documents Using Structured Query Definition](http://docs.marklogic.com/guide/java/searches#id_70572).

Once you have build your query constraints you can pass it into one of the following "query" methods:

```java
    List<DocumentRecord> search(StructuredQueryDefinition query);

    DocumentPage search(StructuredQueryDefinition query, int start);

    DocumentPage search(StructuredQueryDefinition query, int start, int length);

    <T> T searchOne(StructuredQueryDefinition query, Class<T> entityClass);

    <T> List<T> search(StructuredQueryDefinition query, Class<T> entityClass);

    <T> Page<T> search(StructuredQueryDefinition query, int start, Class<T> entityClass);

    <T> Page<T> search(StructuredQueryDefinition query, int start, int length, Class<T> entityClass);
```

Each of these methods also have a companion `stream` method that returns an `InputStream` instead of entities.  This allows you to stream the data out through your application layer without incurring the cost of de-serialization and serialization.  You would only do this if you wanted to return the raw documents without interaction with them in the application.

For more information on the details of each of these methods, see the [javadocs](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/MarkLogicOperations.html).

### Facets

Sometimes when doing searches it is helpful to give aggregates of "category" values that allow a user to either narrow down their search or give useful information about the composition of their data.  These values are called "facets".  For more information about the specifics of what facets are and how they can be used see [Generating Search Facets](http://docs.marklogic.com/guide/rest-dev/search#id_27983).

In order to use facets you must create configuration for the facets and perist them to the database, or create ad-hoc configuration with the `CombinedQueryDefinitionBuilder` (see below).  If you are using repository queries you will need to persist the options.  Then you can use the `@Query` annotation to specify the options name that have your facets configured, and set the return type of your method to `FacetedPage` and you will get those facets returned back as part of your page object, for example:

```java
    @Query(optionsName = "facet-options")
    FacetedPage<Person> findByNameAndGenderAndPetsName(String name, String gender, String petName);
```

### MarkLogic `DocumentManager` and `QueryManager`
If you need to do more than just construct a structured query you can get "access" to the document manager, query manager, or database client objects to build your queries directly with the MarkLogic Java Client Library.

To access these objects from your template object you can use these methods:

```java
    <T> T execute(DocumentCallback<T> action);

    <T> T executeQuery(QueryCallback<T> action);

    <T> T executeWithClient(ClientCallback<T> action);
```

Just pass in a function to execute using these objects.  The current transaction is also passed into your function so that you can tie your operations in.

You can get more information on how
to use these you can see the [Java Application Developer's Guide](http://docs.marklogic.com/guide/java) .

### Sorting
For many of your queries you will want to order the results by various properties in the document.  By default to do any sorting you need to create range indexes on the properties by which you wish to sort.  You can read more about that in [Range Indexes and Lexicons](http://docs.marklogic.com/guide/admin/range_index).

Typically a path range index is most flexible and you should create those for the properties on which you wish to sort.  Then you can construct a Spring Data `Sort` object that contains the properties to use in sorting.

Once you have that you can "enhance" your structured query by calling one of these functions:

```java
    StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query);

    <T> StructuredQueryDefinition sortQuery(Sort sort, StructuredQueryDefinition query, Class<T> entityClass);
```

This adds the necessary query option configuration to the query so that it will sort as you have specified.  As an alternative to this template method you can also use the `CombinedQueryDefinitionBuilder` to add sort configuration, and it is described below.

For more details on how this works see the javadocs for [`sortQuery()`](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/core/MarkLogicOperations.html#sortQuery-org.springframework.data.domain.Sort-com.marklogic.client.query.StructuredQueryDefinition-java.lang.Class-).

### `CombinedQueryDefinitionBuilder`
The `CombinedQueryDefinitionBuilder` class is a query-building helper class that allows you to more easily create combined queries.  "Combined" queries combine structured queries with ad-hoc query options configuration.  This was build to make things easier in implementing various features of this library but can be useful to you as well.  It has some helper methods that allow easy addition of extracts and sorting configuration to your query.

To create an instance of the builder you can do one of the following:

```java
CombinedQueryDefinitionBuilder.combine(myStructuredQuery);
CombinedQueryDefinitionBuilder.combine();
```

The first you pass in your query, then you can leverage the various builder functions to construct the options.  The second is for if you don't have a specific query and are just sending some options to query against "everything".

This builder implements the `CombinedQueryDefinition` interface, so you can look at the [javadocs](https://malteseduck.github.io/spring-data-marklogic/io/github/malteseduck/springframework/data/marklogic/repository/query/CombinedQueryDefinition.html) for more details on the available methods.
