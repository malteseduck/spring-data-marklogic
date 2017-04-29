package org.springframework.data.marklogic.core;

import com.marklogic.client.DatabaseClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.annotation.Id;
import org.springframework.data.marklogic.core.mapping.Document;
import org.springframework.data.marklogic.core.mapping.TypePersistenceStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:integration.xml")
public class TemplateCrudIT {

    private MarkLogicTemplate template;

    @Autowired
    public void setClient(DatabaseClient client) {
        template = new MarkLogicTemplate(client);
    }

    @Before
    public void init() {
        cleanDb();
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        template.deleteAll(Person.class);
        template.deleteAll(InstantPerson.class);
        template.deleteAll(IntPerson.class);
        template.deleteAll(asList("badfred"), BadPerson.class);
    }

    @Test
    public void testDeleteById() {
        Person bob = new Person("Bob");
        Person george = new Person("George");

        template.write(asList(bob, george));

        template.deleteById(bob.getId());
        assertThat(template.exists(bob.getId())).as("deleted by id").isFalse();

        template.deleteById(george.getId(), Person.class);
        assertThat(template.exists(george.getId())).as("deleted by id and type").isFalse();
    }

    @Test
    public void testDeleteByIds() throws Exception {
        Person bob = new Person("Bob");
        Person george = new Person("George");

        template.write(asList(bob, george));

        template.deleteAll(asList(bob.getId()));
        assertThat(template.exists(bob.getId())).as("without type").isFalse();

        template.deleteAll(asList(george.getId()), Person.class);
        assertThat(template.exists(george.getId())).as("options type").isFalse();
    }

    @Test
    public void testDeleteEntities() throws Exception {
        Person bob = new Person("Bob");
        Person george = new Person("George");

        template.write(asList(bob, george));
        template.delete(asList(bob, george));
        assertThat(template.exists(asList(bob.getId(), george.getId()))).isFalse();
    }

    @Test
    public void testSimpleWrite() {
        Person person = new Person("Bob");

        template.write(person);

        Person saved = template.read(person.getId(), Person.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(person.getId());
    }

    @Test
    @Ignore("not yet implemented because not supported in WriteSet")
    public void testSimpleWriteAutoId() {
        Person person = new Person("Bob");
        person.setId(null);

        template.write(person);

        List<Person> people = template.search(Person.class);
        assertThat(people).hasSize(1);
        assertThat(people.get(0).getId())
                .isNotNull()
                .isNotEqualTo("null");
    }

    @Test
    public void testBatchWrite() {
        Person bob = new Person("bob");
        Person fred = new Person("fred");

        template.write(asList(bob, fred));

        Person saved = template.read(fred.getId(), Person.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(fred.getId());
    }

    @Test
    public void testBatchRead() {
        Person bob = new Person("bob");
        Person fred = new Person("fred");

        template.write(asList(bob, fred));

        List<Person> saved = template.read(asList(fred.getId(), bob.getId()), Person.class);
        assertThat(saved).extracting(Person::getName)
            .containsExactlyInAnyOrder("bob", "fred");
    }

    @Test
    public void testBatchReadByEntity() {
        Person bob = new Person("bob");
        Person fred = new Person("fred");

        template.write(asList(bob, fred));

        List<Person> people = template.search(Person.class);
        assertThat(people).extracting(Person::getName)
                .containsExactlyInAnyOrder("bob", "fred");
    }

    @Test
    public void testNoIdAnnotationFailure() throws Exception {
        Object person = new Object() {};

        Throwable thrown = catchThrowable(() -> template.write(person));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not have a method or field annotated with org.springframework.data.annotation.Id");
    }

    @Document(typeStrategy = TypePersistenceStrategy.NONE)
    private static class BadPerson extends Person {
        public BadPerson(String name) { super(name); }
    }

    @Test
    public void testNoCollectionDeleteFailure() throws Exception {
        Throwable thrown = catchThrowable(() -> template.deleteAll(BadPerson.class));

        assertThat(thrown).isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("Cannot determine deleteById scope for entity of type org.springframework.data.marklogic.core.TemplateCrudIT$BadPerson");
    }

    @Test
    public void testSearchConstrainedToCollection() {
        Person bob = new Person("bob");
        BadPerson fred = new BadPerson("fred");
        fred.setId("badfred");

        template.write(asList(bob, fred));
        List<Person> people = template.search(Person.class);
        assertThat(people).containsExactly(bob);
    }

    @Test
    public void testNoClassDeleteFailure() throws Exception {
        Throwable thrown = catchThrowable(() -> template.deleteAll((Class) null));

        assertThat(thrown).isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessage("Entity class is required to determine scope of deleteById");
    }

    public static class IntPerson {
        @Id
        private int id = 23;
        public int getId() { return id; }
    }

    @Test
    public void testIntIdWrite() throws Exception {
        IntPerson person = new IntPerson();

        template.write(person);

        IntPerson saved = template.read(23, IntPerson.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(23);
    }

    public static class InstantPerson {
        @Id
        private Instant id = Instant.parse("2007-07-07T07:07:07Z");
        public Instant getId() { return id; }
    }

    @Test
    public void testInstantIdWrite() throws Exception {
        InstantPerson person = new InstantPerson();

        template.write(person);

        InstantPerson saved = template.read(Instant.parse("2007-07-07T07:07:07Z"), InstantPerson.class);
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isEqualTo(Instant.parse("2007-07-07T07:07:07Z"));
    }

    public static class ColPerson {
        @Id
        private List<String> id = asList("23");
        public List<String> getId() { return id; }
    }

    @Test
    public void testCollectionIdWriteFailure() throws Exception {
        ColPerson person = new ColPerson();

        Throwable thrown = catchThrowable(() -> template.write(person));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Collection types not supported as entity id");
    }
}
