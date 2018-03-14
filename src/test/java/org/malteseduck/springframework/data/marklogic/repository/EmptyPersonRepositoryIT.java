package org.malteseduck.springframework.data.marklogic.repository;


import com.marklogic.client.io.DocumentMetadataHandle;
import com.marklogic.client.io.Format;
import com.marklogic.client.io.StringHandle;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.malteseduck.springframework.data.marklogic.DatabaseConfiguration;
import org.malteseduck.springframework.data.marklogic.core.Immunization;
import org.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import org.malteseduck.springframework.data.marklogic.core.Person;
import org.malteseduck.springframework.data.marklogic.core.PersonXml;
import org.malteseduck.springframework.data.marklogic.core.Pet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.malteseduck.springframework.data.marklogic.repository.query.QueryTestUtils.stream;
import static org.malteseduck.springframework.data.marklogic.repository.query.QueryTestUtils.streamXml;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("classpath:integration.xml"),
        @ContextConfiguration(classes = DatabaseConfiguration.class),
        @ContextConfiguration(classes = PersonRepositoryIntegrationConfiguration.class)
})
public class EmptyPersonRepositoryIT {

    @Autowired
    private PersonRepository repository;

    @Autowired
    private PersonXmlRepository xmlRepository;

    @Autowired
    private PersonStreamRepository streamRepository;

    @Autowired
    private PersonTransformingRepository transRepository;

    @Autowired
    private MarkLogicOperations operations;

    private Person bobby, george, jane, jenny, andrea, henry, freddy;
    private PersonXml jimmy;

    List<Person> all;
    List<PersonXml> allXml;

    @Before
    public void init() {
        cleanDb();

        Pet fluffy = new Pet("Fluffy", "cat");
        fluffy.setImmunizations(asList(new Immunization("flu", "shot")));
        andrea = new Person("Andrea", 17, "female", "food prep", "There isn't much to say", Instant.parse("2016-04-01T00:00:00Z"), asList("sewing", "karate"), asList(fluffy));
        bobby = new Person("Bobby", 23, "male", "dentist", "", Instant.parse("2016-01-01T00:00:00Z"), asList("running", "origami"), asList(new Pet("Bowwow", "dog")));
        george = new Person("George", 12, "male", "engineer", "The guy who works at the gas station, he is your friend", Instant.parse("2016-02-01T00:00:00Z"), asList("fishing", "hunting", "sewing"), asList(new Pet("Hazelnut", "snake"), new Pet("Snoopy", "dog")));
        henry = new Person("Henry", 32, "male", "construction", "He built my house", Instant.parse("2016-05-01T00:00:00Z"), asList("carpentry", "gardening"));
        jane = new Person("Jane", 52, "female", "doctor", "A nice lady that is a friend of george", Instant.parse("2016-03-01T00:00:00Z"), asList("fencing", "archery", "running"));
        jenny = new Person("Jenny", 41, "female", "dentist", "", Instant.parse("2016-06-01T00:00:00Z"), asList("gymnastics"), asList(new Pet("Powderkeg", "wolverine")));

        henry.setRankings(asList(1, 2, 3));

        all = emptyList();

        freddy = new Person("Freddy", 27, "male", "policeman", "", Instant.parse("2016-08-01T00:00:00Z"), asList("gaming"));
        //operations.write(freddy, "OtherPeople");

        jimmy = new PersonXml("Jimmy", 15, "male", "student", "Lives next door", Instant.parse("2016-12-01T00:00:00Z"));

        allXml = emptyList();
    }

    @After
    public void clean() {
        cleanDb();
    }

    private void cleanDb() {
        repository.deleteAll();
        xmlRepository.deleteAll();
        operations.dropCollections("OtherPeople");
    }

    @Test
    public void testFindsPersonByIdThatDoesNotExist() throws Exception {
        Optional<Person> found = repository.findById("does-not-exist");
        assertThat(found).isNotPresent();
    }

    @Test
    public void testFindsAllPeople() throws Exception {
        List<Person> people = repository.findAll();
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindsAllPeopleOrderedByName() throws Exception {
        List<Person> people = repository.findAll(Sort.by("name"));
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindsAllWithGivenIds() {
        Iterable<Person> people = repository.findAllById(asList("", bobby.getId()));
        assertThat(people).isEmpty();
    }

    @Test
    public void testDeletesPersonCorrectly() throws Exception {
        assertThat(repository.findAll()).isEmpty();
        repository.delete(george);
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void testDeleteByIdCorrectly() throws Exception {
        assertThat(repository.findAll()).isEmpty();
        repository.deleteById(george.getId());
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void testDeletesPersonByIdCorrectly() {
        assertThat(repository.findAll()).isEmpty();
        repository.deleteById(bobby.getId());
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void testFindsPersonsOrderedByName() throws Exception {
        List<Person> people = repository.findAllByOrderByNameAsc();
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindsPersonsByName() throws Exception {
        List<Person> people = repository.findByName("Jane");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindsPersonsByNameOrderedByAge() throws Exception {
        List<Person> people = repository.findByGenderOrderByAge("female");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindPersonsByOccupationOrderedByName() throws Exception {
        List<Person> people = repository.findByOccupationOrderByNameAsc("dentist");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindsPersonsByNameIn() throws Exception {
        List<Person> people = repository.findByNameIn("Jane", "George");
        assertThat(people).isEmpty();
    }

    @Test
    public void findsPersonsByNameNull() throws Exception {
        List<Person> people = repository.findByName(null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void testFindsPersonsByNameInNull() throws Exception {
        List<Person> people = repository.findByNameIn((String[]) null);
        assertThat(people).isNullOrEmpty();
    }

    @Test
    public void testFindsPersonsByAge() throws Exception {
        List<Person> people = repository.findByAge(23);
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindsPersonByBirthtime() throws Exception {
        Person person = repository.findByBirthtime(Instant.parse("2016-01-01T00:00:00Z"));
        assertThat(person).isNull();
    }

    @Test
    public void testFindsPersonsByGenderLike() throws Exception {
        List<Person> people = repository.findByGenderLike("ma*");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindsPersonsByNameNotLike() throws Exception {
        List<Person> people = repository.findByNameNotLike("Bo*");
        assertThat(people).doesNotContain(bobby);
    }

    @Test
    public void testFindsPagedPersonsOrderedByName() throws Exception {
        Page<Person> page = repository.findAll(PageRequest.of(1, 2, Sort.Direction.ASC, "name"));
        assertEmpty(page);
    }

    @Test
    public void testHandlesNullPageable() throws Exception {
        Throwable thrown = catchThrowable(() -> repository.findAll((Pageable) null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The given Pageable must not be null");
    }

    @Test
    public void testExecutesPagedFinderCorrectly() throws Exception {
        Page<Person> page = repository.findByGenderLike("fem*",
                PageRequest.of(0, 2, Sort.Direction.ASC, "name"));
        assertEmpty(page);
    }

    @Test
    public void testExistsWorksCorrectly() {
        assertThat(repository.existsById(bobby.getId())).isFalse();
    }

    @Test
    public void testFindsPeopleUsingNotPredicate() {
        List<Person> people = repository.findByNameNot("Andrea");

        assertThat(people).isEmpty();
    }

    @Test
    public void tesetExecutesAndQueryCorrectly() {
        List<Person> people = repository.findByNameAndAge("Bobby", 23);

        assertThat(people).isEmpty();
    }

    @Test
    public void testExecutesOrQueryCorrectly() {
        List<Person> people = repository.findByNameOrAge("Bobby", 23);

        assertThat(people).isEmpty();
    }

    @Test
    public void testExecutesDerivedCountProjection() {
        assertThat(repository.countByName("George")).isEqualTo(0);
    }

    @Test
    public void testExecutesDerivedExistsProjectionToBoolean() {
        assertThat(repository.existsByName("Jane")).as("does exist").isFalse();
        assertThat(repository.existsByName("Brunhilda")).as("doesn't exist").isFalse();
    }

    @Test
    public void testExecutesDerivedStartsWithQueryCorrectly() {
        List<Person> people = repository.findByNameStartsWith("J");

        assertThat(people).isEmpty();
    }

    @Test
    public void testFxecutesDerivedEndsWithQueryCorrectly() {
        List<Person> people = repository.findByNameEndsWith("nny");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByNameIgnoreCase() {
        List<Person> people = repository.findByNameIgnoreCase("george");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByNameNotIgnoreCase() {
        List<Person> people = repository.findByNameNotIgnoreCase("george");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByNameStartingWithIgnoreCase() {
        List<Person> people = repository.findByNameStartingWithIgnoreCase("ge");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByHobbiesContains() throws Exception {
        List<Person> people = repository.findByHobbiesContains(asList("running"));
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByHobbiesNotContains() throws Exception {
        List<Person> people = repository.findByHobbiesNotContaining(asList("running"));
        assertThat(people).doesNotContain(bobby, jane);
    }

    @Test
    public void testFindByPet() throws Exception {
        List<Person> people = repository.findByPets(new Pet("Powderkeg", "wolverine"));
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByPetImmunizationsType() throws Exception {
        List<Person> people = repository.findByPetsImmunizationsType("shot");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByOccupationUsingExtract() {
        List<Person> people = repository.findByOccupation("engineer");
        assertThat(people).isEmpty();
    }

    // ===== Range Queries

    @Test
    public void testFindByAgeBetween() {
        List<Person> people = repository.findByAgeBetween(20, 40);
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByAgeGreaterThanEqual() {
        List<Person> people = repository.findByAgeGreaterThanEqual(50);
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByBirthtimeGreaterThan() {
        List<Person> people = repository.findByBirthtimeGreaterThan(Instant.parse("2016-05-02T00:00:00Z"));
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByGenderUsingForcedRangeQuery() {
        List<Person> people = repository.findByGender("female");
        assertThat(people).isEmpty();
    }

    // ===== Limiting Queries

    @Test
    public void testFindFirstByName() {
        Person person = repository.findFirstByName("Bobby");
        assertThat(person).isNull();
    }

    @Test
    public void testFindTop2ByOrderByName() {
        List<Person> people = repository.findTop2ByOrderByName();
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindFirst2OrderByName() {
        Page<Person> page = repository.findFirst2ByOrderByName(PageRequest.of(0, 10, Sort.Direction.ASC, "name"));
        assertEmpty(page);
    }

    // ===== Query By Example

    @Test
    public void testFindAllQBE() throws Exception {
        List<Person> people = repository.qbeFindAll();
        assertThat(people).containsAll(all);
    }

    @Test
    public void testFindAllWithPageableQBE() throws Exception {
        Page<Person> page = repository.qbeFindAllWithPageable(
                PageRequest.of(0, 1, Sort.Direction.ASC, "name")
        );
        assertEmpty(page);
    }

    @Test
    public void testFindByNameQBE() throws Exception {
        Person person = repository.qbeFindByName("Bobby");
        assertThat(person).isNull();
    }

    @Test
    public void testFindByNameExtractingQBE() throws Exception {
        Person person = repository.qbeFindByNameExtractingNameAndAge("Bobby");
        assertThat(person).isNull();
    }

    @Test
    public void testFindListByNameQBE() throws Exception {
        List<Person> person = repository.qbeFindByNameList("Bobby");
        assertThat(person).isEmpty();
    }

    @Test
    public void testFindBobby() throws Exception {
        Person person = repository.qbeFindBobby();
        assertThat(person).isNull();
    }

    @Test
    public void testFindByNameQBEQuoted() throws Exception {
        Person person = repository.qbeFindByNameQuoted("Bobby");
        assertThat(person).isNull();
    }

    @Test
    public void testFindByPetQBE() throws Exception {
        List<Person> people = repository.qbeFindByPet(new Pet("Snoopy", "dog"));
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByGenderWithPageableQBE() throws Exception {
        Page<Person> page = repository.qbeFindByGenderWithPageable(
                "female",
                PageRequest.of(0, 2, Sort.Direction.ASC, "name")
        );
        assertEmpty(page);
    }

    @Test
    public void testFindByComplicatedQBE() throws Exception {
        List<Person> people = repository.qbeFindByComplicated("fish");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindByGenderQBEHonorsCollections() throws Exception {
        Page<Person> page = repository.qbeFindByGenderWithPageable(
                "male",
                PageRequest.of(0, 20, Sort.Direction.ASC, "name")
        );
        assertEmpty(page);
    }

    // ===== XML Query By Example

    @Test
    public void testFindXmlByNameQBE() throws Exception {
        List<PersonXml> people = xmlRepository.qbeFindByName("Jimmy");
        assertThat(people).isEmpty();
    }

    @Test
    public void testFindXmlByNameQBEWithoutSpecifyingFormat() throws Exception {
        List<PersonXml> people = xmlRepository.qbeFindByNameWithoutSpecifyingFormat("Jimmy");
        assertThat(people).isEmpty();
    }

    // ===== Various forms of streaming

    @Test
    public void testFindByNameReturningDocumentStream() {
        InputStream people = streamRepository.findAllByName("Bobby");
        assertThat(people).hasSameContentAs(stream());
    }

    @Test
    public void testFindByNameReturningInputStream() {
        InputStream people = streamRepository.findAllByNameUsingGeneric("Bobby");
        assertThat(people).hasSameContentAs(stream());
    }

    @Test
    public void testFindAllByOrderByNameReturningDocumentStream() {
        InputStream people = streamRepository.findAllByOrderByName();
        assertThat(people).hasSameContentAs(stream());
    }

    @Test
    public void testFindAllByOrderByPetsNameReturningDocumentStream() {
        InputStream people = streamRepository.findAllByOrderByPetsNameAscNameAsc();
        assertThat(people).hasSameContentAs(stream());
    }

    @Test
    public void testFindAllByGenderReturningDocumentStream() {
        InputStream people = streamRepository.findAllByGenderOrderByName("female");
        assertThat(people).hasSameContentAs(stream());
    }

    @Test
    @Ignore("not yet fully implemented?")
    public void testFindAllXmlReturningStream() {
        InputStream people = xmlRepository.findAllByName("Jimmy");
        assertThat(people).hasSameContentAs(streamXml(jimmy));
    }

    // ====== Transforming Queries =====

    @Test // spring-data-marklogic/issues/8
    public void testFindByPersonNameAsElementValueQuery() {
        boolean exists = transRepository.existsByName("Bubba");
        assertThat(exists).isFalse();
    }

    @Test
    public void testFindPersonAndTransform() {
        Person person = transRepository.findByNameTransforming("Bobby");
        assertThat(person).isNull();
    }

    @Test
    public void testFindAllOverriddenWithTransform() {
        Page<Person> page = transRepository.findAllBy(PageRequest.of(0, 1));
        assertEmpty(page);
    }

    //    @Ignore("Maybe not possible?")
    @Test
    public void testDefaultImplementationFind() {
        Page<Person> page = transRepository.findAllBy(PageRequest.of(0, 1));
        assertEmpty(page);
    }

    @Test
    public void testFindPersonWithStructuredQueryAndTransform() {
        Person person = transRepository.findFirstByOccupation("construction");
        assertThat(person).isNull();
    }

    private static void assertEmpty(Page<Person> page) {
        assertThat(page).isEmpty();
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isTrue();
        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(1);
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getNumberOfElements()).isEqualTo(0);
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getSize()).isEqualTo(0);
        assertThat(page.getSort()).isEqualTo(Sort.unsorted());
    }
}
