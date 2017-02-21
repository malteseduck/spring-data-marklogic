package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryBuilder.FragmentScope;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = InfrastructureConfiguration.class)
public class BasicXmlQueryTests {

    private MarkLogicTemplate template;
    private StructuredQueryBuilder qb;

    private PersonXml bobby;
    private PersonXml george;
    private PersonXml jane;

    @Autowired
    public void setClient(DatabaseClient client) {
        template = new MarkLogicTemplate(client);
        qb = template.queryBuilder();
    }

    @Before
    public void setUp() {
        cleanDb();

        bobby = new PersonXml("Bobby", 23, "male", "dentist", "", Instant.parse("2016-01-01T00:00:00Z"));
        george = new PersonXml("George", 12, "male", "engineer", "The guy wo works at the gas station, he is your friend", Instant.parse("2016-01-01T00:00:00Z"));
        jane = new PersonXml("Jane", 52, "female", "doctor", "A nice lady that is a friend of george", Instant.parse("2016-01-01T00:00:00Z"));

        template.write(asList(bobby, george, jane));
    }

    @After
    public void tearDown() {
        cleanDb();
    }

    private void cleanDb() {
        template.deleteAll(PersonXml.class);
    }

    @Test
    public void testSaveWithXmlLang() {
        PersonXml jenny = new PersonXml("Jenny", 28, "mujer", "programador", "Ella no te quiere", Instant.parse("2016-01-01T00:00:00Z"));
        jenny.setLang("spa");

        template.write(jenny);

        List<PersonXml> people = template.search(
            qb.word(qb.element("gender"), FragmentScope.DOCUMENTS, new String[]{"lang=spa"}, 1.0, "mujer"),
            PersonXml.class
        );

        assertThat(people).containsExactly(jenny);
    }

    @Test
    public void testQueryByValue() {
        List<PersonXml> people = template.search(
            qb.value(qb.element("gender"), "male"),
            PersonXml.class
        );

        assertThat(people).hasSize(2);
    }

    @Test
    public void testQueryByValueWithLimit() {
        Page<PersonXml> people = template.search(
            qb.value(qb.element("gender"), "male"),
            0,
            1,
            PersonXml.class
        );

        assertThat(people).hasSize(1);
        assertThat(people.getTotalPages()).isEqualTo(2);
    }
}
