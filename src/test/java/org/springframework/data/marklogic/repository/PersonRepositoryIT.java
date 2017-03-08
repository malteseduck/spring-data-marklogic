package org.springframework.data.marklogic.repository;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.marklogic.DatabaseConfiguration;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.Person;
import org.springframework.data.marklogic.core.Pet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:integration.xml"),
        @ContextConfiguration(classes = DatabaseConfiguration.class),
        @ContextConfiguration(classes = PersonRepositoryIntegrationConfiguration.class)
})
public class PersonRepositoryIT {

    @Autowired
    private PersonRepository repository;

    @Autowired
    MarkLogicOperations operations;

    private Person bobby, george, jane, jenny, andrea, henry;

    List<Person> all;

    @Before
    public void init() {
        cleanDb();

        andrea = new Person("Andrea", 17, "female", "food prep", "There isn't much to say", Instant.parse("2016-04-01T00:00:00Z"), asList("sewing", "karate"), asList(new Pet("Fluffy", "cat")));
        bobby = new Person("Bobby", 23, "male", "dentist", "", Instant.parse("2016-01-01T00:00:00Z"), asList("running", "origami"), asList(new Pet("Bowwow", "dog")));
        george = new Person("George", 12, "male", "engineer", "The guy wo works at the gas station, he is your friend", Instant.parse("2016-02-01T00:00:00Z"), asList("fishing", "hunting", "sewing"), asList(new Pet("Hazelnut", "snake"), new Pet("Snoopy", "dog")));
        henry = new Person("Henry", 32, "male", "construction", "He built my house", Instant.parse("2016-05-01T00:00:00Z"), asList("carpentry", "gardening"));
        jane = new Person("Jane", 52, "female", "doctor", "A nice lady that is a friend of george", Instant.parse("2016-03-01T00:00:00Z"), asList("fencing", "archery", "running"));
        jenny = new Person("Jenny", 41, "female", "dentist", "", Instant.parse("2016-06-01T00:00:00Z"), asList("gymnastics"), asList(new Pet("Powderkeg", "wolverine")));

        all = repository.save(asList(jenny, bobby, george, jane, andrea, henry));
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        repository.deleteAll();
    }

    @Test
    public void findsPersonById() throws Exception {
        Person found = repository.findOne(bobby.getId());
        assertThat(found).isEqualTo(bobby);
    }

    @Test
    public void findsAllPeople() throws Exception {
        List<Person> people = repository.findAll();
        assertThat(people).containsAll(all);
    }

    @Test
    public void findsAllPeopleOrderedByName() throws Exception {
        List<Person> people = repository.findAll(new Sort("name"));
        assertThat(people).containsExactly(andrea, bobby, george, henry, jane, jenny);
    }

    @Test
    public void findsAllWithGivenIds() {
        Iterable<Person> people = repository.findAll(asList(george.getId(), bobby.getId()));
        assertThat(people).containsExactlyInAnyOrder(george, bobby);
    }

    @Test
    public void deletesPersonCorrectly() throws Exception {
        repository.delete(george);

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(george);
    }

    @Test
    public void deletesPersonByIdCorrectly() {
        repository.delete(bobby.getId());

        List<Person> people = repository.findAll();
        assertThat(people).hasSize(all.size() - 1);
        assertThat(people).doesNotContain(bobby);
    }

    @Test
    public void findsPersonsOrderedByName() throws Exception {
        List<Person> people = repository.findAllByOrderByNameAsc();
        assertThat(people).containsExactly(andrea, bobby, george, henry, jane, jenny);
    }

    @Test
    public void findsPersonsByName() throws Exception {
        List<Person> people = repository.findByName("Jane");
        assertThat(people).containsExactly(jane);
    }

    @Test
    public void findsPersonsByNameOrderedByAge() throws Exception {
        List<Person> people = repository.findByGenderOrderByAge("female");
        assertThat(people).containsExactly(andrea, jenny, jane);
    }

    @Test
    public void findPersonsByOccupationOrderedByName() throws Exception {
        List<Person> people = repository.findByOccupationOrderByNameAsc("dentist");
        assertThat(people).containsExactly(bobby, jenny);
    }

    @Test
    public void findsPersonsByNameIn() throws Exception {
        List<Person> people = repository.findByNameIn("Jane", "George");
        assertThat(people).containsExactlyInAnyOrder(jane, george);
    }

    @Test
    public void findsPersonsByNameNull() throws Exception {
        List<Person> people = repository.findByName(null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void findsPersonsByNameInNull() throws Exception {
        List<Person> people = repository.findByNameIn(null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void findsPersonsByAge() throws Exception {
        List<Person> people = repository.findByAge(23);
        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void findsPersonsByGenderLike() throws Exception {
        List<Person> people = repository.findByGenderLike("ma*");
        assertThat(people).containsExactlyInAnyOrder(bobby, george, henry);
    }

    @Test
    public void findsPersonsByNameNotLike() throws Exception {
        List<Person> people = repository.findByNameNotLike("Bo*");
        assertThat(people).doesNotContain(bobby);
    }

    @Test
    public void findsPagedPersonsOrderedByName() throws Exception {
        Page<Person> page = repository.findAll(new PageRequest(1, 2, Sort.Direction.ASC, "name"));
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
        assertThat(page).containsExactly(george, henry);
    }

    @Test
    public void executesPagedFinderCorrectly() throws Exception {
        Page<Person> page = repository.findByGenderLike("fem*",
                new PageRequest(0, 2, Sort.Direction.ASC, "name"));
        
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
        assertThat(page.getNumberOfElements()).isEqualTo(2);
        assertThat(page).containsExactly(andrea, jane);

        // Wildcard index required for result total to be correct
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    public void existsWorksCorrectly() {
        assertThat(repository.exists(bobby.getId())).isTrue();
    }

    @Test
    public void findsPeopleUsingNotPredicate() {
        List<Person> people = repository.findByNameNot("Andrea");
        
        assertThat(people)
                .doesNotContain(andrea)
                .hasSize(all.size() - 1);
    }

    @Test
    public void executesAndQueryCorrectly() {
        List<Person> people = repository.findByNameAndAge("Bobby", 23);

        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void executesOrQueryCorrectly() {
        List<Person> people = repository.findByNameOrAge("Bobby", 23);

        assertThat(people).containsExactly(bobby);
    }

    @Test
    public void executesDerivedCountProjection() {
        assertThat(repository.countByName("George")).isEqualTo(1);
    }

    @Test
    public void executesDerivedExistsProjectionToBoolean() {
        assertThat(repository.existsByName("Jane")).as("does exist").isTrue();
        assertThat(repository.existsByName("Brunhilda")).as("doesn't exist").isFalse();
    }

    @Test
    public void executesDerivedStartsWithQueryCorrectly() {
        List<Person> people = repository.findByNameStartsWith("J");
        
        assertThat(people).containsExactlyInAnyOrder(jenny, jane);
    }

    @Test
    public void executesDerivedEndsWithQueryCorrectly() {
        List<Person> people = repository.findByNameEndsWith("nny");
        assertThat(people).containsExactly(jenny);
    }
    
    @Test
    public void findByNameIgnoreCase() {
        List<Person> people = repository.findByNameIgnoreCase("george");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void findByNameNotIgnoreCase() {
        List<Person> people = repository.findByNameNotIgnoreCase("george");
        assertThat(people)
                .hasSize(all.size()-1)
                .doesNotContain(george);
    }

    @Test
    public void findByNameStartingWithIgnoreCase() {
        List<Person> people = repository.findByNameStartingWithIgnoreCase("ge");
        assertThat(people).containsExactly(george);
    }

    @Test
    public void findByHobbiesContains() throws Exception {
        List<Person> people = repository.findByHobbiesContains(asList("running"));
        assertThat(people).containsExactlyInAnyOrder(bobby, jane);
    }

    @Test
    public void findByHobbiesNotContains() throws Exception {
        List<Person> people = repository.findByHobbiesNotContaining(asList("running"));
        assertThat(people).doesNotContain(bobby, jane);
    }

    @Test
    public void testFindByNameQBE() throws Exception {
        Person person = repository.qbeFindByName("Bobby");
        assertThat(person).isEqualTo(bobby);
    }

    @Test
    public void testFindByPetQBE() throws Exception {
        List<Person> people = repository.qbeFindByPet(new Pet("Fluffy", "cat"));
        assertThat(people).containsExactly(andrea);
    }

    @Test
    public void testFindByGenderWithPageableQBE() throws Exception {
        Page<Person> people = repository.qbeFindByGenderWithPageable(
                "female",
                new PageRequest(0, 2, Sort.Direction.ASC, "name")
        );
        assertThat(people).containsExactly(andrea, jane);
    }
}
