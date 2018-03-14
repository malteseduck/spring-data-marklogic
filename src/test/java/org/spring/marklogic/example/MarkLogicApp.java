package org.spring.marklogic.example;

import com.marklogic.client.DatabaseClientFactory.DigestAuthContext;
import com.marklogic.client.query.StructuredQueryBuilder;
import org.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import org.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;

import static com.marklogic.client.DatabaseClientFactory.newClient;

public class MarkLogicApp {

    private static final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    public static void main(String[] args) throws Exception {

        MarkLogicOperations ops = new MarkLogicTemplate(newClient("localhost", 8000, new DigestAuthContext("admin", "admin")));
        ops.write(new Person(1234, "Bobby", 23));

        Person bobby = ops.searchOne(qb.value(qb.jsonProperty("name"), "Bobby"), Person.class);
        System.out.println(bobby);

        ops.dropCollections("Person");
    }

}
