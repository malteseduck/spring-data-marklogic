package io.github.malteseduck.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.query.StructuredQueryBuilder;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.malteseduck.springframework.data.marklogic.DatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:integration.xml"),
        @ContextConfiguration(classes = DatabaseConfiguration.class)
})
public class TemplateTransformerIT {


    private MarkLogicTemplate template;
    private StructuredQueryBuilder qb = new StructuredQueryBuilder();

    private TransformedPerson fred;

    @Autowired
    public void setClient(DatabaseClient client) {
        template = new MarkLogicTemplate(client);
    }

    @Before
    public void init() {
        cleanDb();

        fred = new TransformedPerson("Fred");
        template.write(asList(fred));
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        template.dropCollection(TransformedPerson.class);
    }

    @Test
    public void testReadDocumentIsTransformed() {
        List<TransformedPerson> people = template.read(
                singletonList(fred.getId()),
                TransformedPerson.class
        );

        Assertions.assertThat(people).hasSize(1);
        assertThat(people.get(0).getName()).isEqualTo("Override Master Read");
    }

    @Test
    public void testQueryByValue() {
        List<TransformedPerson> people = template.search(
                qb.value(qb.jsonProperty("name"), "Override Master Write"),
                TransformedPerson.class
        );

        Assertions.assertThat(people).hasSize(1);
        assertThat(people.get(0).getName()).isEqualTo("Override Master Read");
    }

    @Test
    public void testSearchOne() {
        TransformedPerson person = template.searchOne(
                qb.value(qb.jsonProperty("name"), "Override Master Write"),
                TransformedPerson.class
        );

        assertThat(person).isEqualTo(fred);
        assertThat(person.getName()).isEqualTo("Override Master Read");
    }

}
