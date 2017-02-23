/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.MarkLogicTemplate;
import org.springframework.data.marklogic.core.Person;
import org.springframework.data.marklogic.core.convert.MappingMarkLogicConverter;
import org.springframework.data.marklogic.core.convert.MarkLogicConverter;
import org.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;

import java.lang.reflect.Method;
import java.util.List;

import static com.marklogic.client.query.StructuredQueryBuilder.Operator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.marklogic.repository.query.StubParameterAccessor.getAccessor;

public class MarkLogicQueryCreatorTests {

    MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> context;
    MarkLogicConverter converter;
    MarkLogicOperations operations;
    final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    @Rule
    public ExpectedException expectation = ExpectedException.none();

    @Before
    public void setUp() throws SecurityException, NoSuchMethodException {
        context = new MarkLogicMappingContext();
        converter = new MappingMarkLogicConverter(context);
        operations = new MarkLogicTemplate(null, converter);
    }

    private MarkLogicQueryMethod queryMethod(Class<?> repository, String name, Class<?>... parameters) throws Exception {
        Method method = repository.getMethod(name, parameters);
        ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
        return new MarkLogicQueryMethod(method, new DefaultRepositoryMetadata(repository), factory, context);
    }

    private MarkLogicQueryCreator creator(MarkLogicQueryMethod method, Object... parameters) {
        PartTree tree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());
        return new MarkLogicQueryCreator(tree, getAccessor(parameters), operations, context, method);
    }

    interface PersonRepository extends Repository<Person, String> {
        List<Person> findByName(String name);
        List<Person> findByNameAndAge(String name, int age);
        List<Person> findByNameIsNull();
        List<Person> findByNameNotNull();
        List<Person> findByAgeLessThanEqual(int age);
        List<Person> findByAgeGreaterThanEqual(int age);
    }

    @Test
    public void testSimpleValueQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByName", String.class),
                "Bubba"
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(qb.value(qb.jsonProperty("name"), "Bubba").serialize());
    }

    @Test
    public void testAndValueQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameAndAge", String.class, int.class),
                "Fred", 23
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(qb.and(
                        qb.value(qb.jsonProperty("name"), "Fred"),
                        qb.value(qb.jsonProperty("age"), 23)
                ).serialize());
    }

    @Test
    public void testNullQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameIsNull")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.value(qb.jsonProperty("name"), (String) null).serialize()
                );
    }

    @Test
    public void testNotNullQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByNameNotNull")
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.not(
                                qb.value(qb.jsonProperty("name"), (String) null)
                        ).serialize()
                );
    }

    @Test
    @Ignore("not yet implemented")
    public void testLessThanOrEqualToQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByAgeLessThanEqual", int.class),
                23
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.range(qb.jsonProperty("age"), "xs:integer", (String[]) null, Operator.LE, 23).serialize()
                );
    }

    @Test
    @Ignore("not yet implemented")
    public void testGreaterThanOrEqualToQuery() throws Exception {
        StructuredQueryDefinition query = creator(
                queryMethod(PersonRepository.class, "findByAgeGreaterThanEqual", int.class),
                23
        ).createQuery();
        assertThat(query.serialize())
                .isEqualTo(
                        qb.range(qb.jsonProperty("age"), "xs:integer", (String[]) null, Operator.GE, 23).serialize()
                );
    }

    @Test
    public void testPropertyExists() throws Exception {


    }


    //    @Test // DATAMONGO-338
//    public void createsExistsClauseCorrectly() {
//
//        PartTree tree = new PartTree("findByAgeExists", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, true), context);
//        Query query = query(where("age").exists(true));
//        assertThat(creator.createQuery(), is(query));
//    }
//
//    @Test // DATAMONGO-338
//    public void createsRegexClauseCorrectly() {
//
//        PartTree tree = new PartTree("findByFirstNameRegex", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, ".*"), context);
//        Query query = query(where("firstName").regex(".*"));
//        assertThat(creator.createQuery(), is(query));
//    }
//
//    @Test // DATAMONGO-338
//    public void createsTrueClauseCorrectly() {
//
//        PartTree tree = new PartTree("findByActiveTrue", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter), context);
//        Query query = query(where("active").is(true));
//        assertThat(creator.createQuery(), is(query));
//    }
//
//    @Test // DATAMONGO-338
//    public void createsFalseClauseCorrectly() {
//
//        PartTree tree = new PartTree("findByActiveFalse", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter), context);
//        Query query = query(where("active").is(false));
//        assertThat(creator.createQuery(), is(query));
//    }
//
//    @Test // DATAMONGO-413
//    public void createsOrQueryCorrectly() {
//
//        PartTree tree = new PartTree("findByFirstNameOrAge", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "Dave", 42), context);
//
//        Query query = creator.createQuery();
//        assertThat(query, is(query(new Criteria().orOperator(where("firstName").is("Dave"), where("age").is(42)))));
//    }
//
//    @Test // DATAMONGO-347
//    public void createsQueryReferencingADBRefCorrectly() {
//
//        User user = new User();
//        user.id = new ObjectId();
//
//        PartTree tree = new PartTree("findByCreator", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, user), context);
//        DBObject queryObject = creator.createQuery().getQueryObject();
//
//        assertThat(queryObject.get("creator"), is((Object) user));
//    }
//
//    @Test // DATAMONGO-418
//    public void createsQueryWithStartingWithPredicateCorrectly() {
//
//        PartTree tree = new PartTree("findByUsernameStartingWith", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "Matt"), context);
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("username").regex("^Matt"))));
//    }
//
//    @Test // DATAMONGO-418
//    public void createsQueryWithEndingWithPredicateCorrectly() {
//
//        PartTree tree = new PartTree("findByUsernameEndingWith", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "ews"), context);
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("username").regex("ews$"))));
//    }
//
//    @Test // DATAMONGO-418
//    public void createsQueryWithContainingPredicateCorrectly() {
//
//        PartTree tree = new PartTree("findByUsernameContaining", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "thew"), context);
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("username").regex(".*thew.*"))));
//    }
//
//    private void assertBindsDistanceToQuery(Point point, Distance distance, Query reference) throws Exception {
//
//        PartTree tree = new PartTree("findByLocationNearAndFirstname",
//                org.springframework.data.mongodb.repository.Person.class);
//        Method method = PersonRepository.class.getMethod("findByLocationNearAndFirstname", Point.class, Distance.class,
//                String.class);
//        MongoQueryMethod queryMethod = new MongoQueryMethod(method, new DefaultRepositoryMetadata(PersonRepository.class),
//                new SpelAwareProxyProjectionFactory(), new MongoMappingContext());
//        MongoParameterAccessor accessor = new MongoParametersParameterAccessor(queryMethod,
//                new Object[] { point, distance, "Dave" });
//
//        Query query = new MongoQueryCreator(tree, new ConvertingParameterAccessor(converter, accessor), context)
//                .createQuery();
//        assertThat(query, is(query));
//    }
//
//    @Test // DATAMONGO-770
//    public void createsQueryWithFindByIgnoreCaseCorrectly() {
//
//        PartTree tree = new PartTree("findByfirstNameIgnoreCase", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave"), context);
//
//        Query query = creator.createQuery();
//        assertThat(query, is(query(where("firstName").regex("^dave$", "i"))));
//    }
//
//    @Test // DATAMONGO-770
//    public void createsQueryWithFindByNotIgnoreCaseCorrectly() {
//
//        PartTree tree = new PartTree("findByFirstNameNotIgnoreCase", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave"), context);
//
//        Query query = creator.createQuery();
//        assertThat(query.toString(), is(query(where("firstName").not().regex("^dave$", "i")).toString()));
//    }
//
//    @Test // DATAMONGO-770
//    public void createsQueryWithFindByStartingWithIgnoreCaseCorrectly() {
//
//        PartTree tree = new PartTree("findByFirstNameStartingWithIgnoreCase", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave"), context);
//
//        Query query = creator.createQuery();
//        assertThat(query, is(query(where("firstName").regex("^dave", "i"))));
//    }
//
//    @Test // DATAMONGO-770
//    public void createsQueryWithFindByEndingWithIgnoreCaseCorrectly() {
//
//        PartTree tree = new PartTree("findByFirstNameEndingWithIgnoreCase", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave"), context);
//
//        Query query = creator.createQuery();
//        assertThat(query, is(query(where("firstName").regex("dave$", "i"))));
//    }
//
//    @Test // DATAMONGO-770
//    public void createsQueryWithFindByContainingIgnoreCaseCorrectly() {
//
//        PartTree tree = new PartTree("findByFirstNameContainingIgnoreCase", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave"), context);
//
//        Query query = creator.createQuery();
//        assertThat(query, is(query(where("firstName").regex(".*dave.*", "i"))));
//    }
//
//    @Test // DATAMONGO-770
//    public void shouldThrowExceptionForQueryWithFindByIgnoreCaseOnNonStringProperty() {
//
//        expection.expect(IllegalArgumentException.class);
//        expection.expectMessage("must be of type String");
//
//        PartTree tree = new PartTree("findByFirstNameAndAgeIgnoreCase", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "foo", 42), context);
//
//        creator.createQuery();
//    }
//
//    @Test // DATAMONGO-770
//    public void shouldOnlyGenerateLikeExpressionsForStringPropertiesIfAllIgnoreCase() {
//
//        PartTree tree = new PartTree("findByFirstNameAndAgeAllIgnoreCase", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave", 42), context);
//
//        Query query = creator.createQuery();
//        assertThat(query, is(query(where("firstName").regex("^dave$", "i").and("age").is(42))));
//    }
//
//    @Test // DATAMONGO-566
//    public void shouldCreateDeleteByQueryCorrectly() {
//
//        PartTree tree = new PartTree("deleteByFirstName", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave", 42), context);
//
//        Query query = creator.createQuery();
//
//        assertThat(tree.isDelete(), is(true));
//        assertThat(query, is(query(where("firstName").is("dave"))));
//    }
//
//    @Test // DATAMONGO-566
//    public void shouldCreateDeleteByQueryCorrectlyForMultipleCriteriaAndCaseExpressions() {
//
//        PartTree tree = new PartTree("deleteByFirstNameAndAgeAllIgnoreCase", Person.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave", 42), context);
//
//        Query query = creator.createQuery();
//
//        assertThat(tree.isDelete(), is(true));
//        assertThat(query, is(query(where("firstName").regex("^dave$", "i").and("age").is(42))));
//    }
//
//    @Test // DATAMONGO-1075
//    public void shouldCreateInClauseWhenUsingContainsOnCollectionLikeProperty() {
//
//        PartTree tree = new PartTree("findByEmailAddressesContaining", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave"), context);
//
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("emailAddresses").in("dave"))));
//    }
//
//    @Test // DATAMONGO-1075
//    public void shouldCreateInClauseWhenUsingNotContainsOnCollectionLikeProperty() {
//
//        PartTree tree = new PartTree("findByEmailAddressesNotContaining", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "dave"), context);
//
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("emailAddresses").not().in("dave"))));
//    }
//
//    @Test // DATAMONGO-1075, DATAMONGO-1425
//    public void shouldCreateRegexWhenUsingNotContainsOnStringProperty() {
//
//        PartTree tree = new PartTree("findByUsernameNotContaining", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "thew"), context);
//        Query query = creator.createQuery();
//
//        assertThat(query.getQueryObject(), is(query(where("username").not().regex(".*thew.*")).getQueryObject()));
//    }
//
//    @Test // DATAMONGO-1139
//    public void createsNonSphericalNearForDistanceWithDefaultMetric() {
//
//        Point point = new Point(1.0, 1.0);
//        Distance distance = new Distance(1.0);
//
//        PartTree tree = new PartTree("findByLocationNear", Venue.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, point, distance), context);
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("location").near(point).maxDistance(1.0))));
//    }
//
//    @Test // DATAMONGO-1136
//    public void shouldCreateWithinQueryCorrectly() {
//
//        Point first = new Point(1, 1);
//        Point second = new Point(2, 2);
//        Point third = new Point(3, 3);
//        Shape shape = new Polygon(first, second, third);
//
//        PartTree tree = new PartTree("findByAddress_GeoWithin", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, shape), context);
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("address.geo").within(shape))));
//    }
//
//    @Test // DATAMONGO-1110
//    public void shouldCreateNearSphereQueryForSphericalProperty() {
//
//        Point point = new Point(10, 20);
//
//        PartTree tree = new PartTree("findByAddress2dSphere_GeoNear", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, point), context);
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("address2dSphere.geo").nearSphere(point))));
//    }
//
//    @Test // DATAMONGO-1110
//    public void shouldCreateNearSphereQueryForSphericalPropertyHavingDistanceWithDefaultMetric() {
//
//        Point point = new Point(1.0, 1.0);
//        Distance distance = new Distance(1.0);
//
//        PartTree tree = new PartTree("findByAddress2dSphere_GeoNear", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, point, distance), context);
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("address2dSphere.geo").nearSphere(point).maxDistance(1.0))));
//    }
//
//    @Test // DATAMONGO-1110
//    public void shouldCreateNearQueryForMinMaxDistance() {
//
//        Point point = new Point(10, 20);
//        Range<Distance> range = Distance.between(new Distance(10), new Distance(20));
//
//        PartTree tree = new PartTree("findByAddress_GeoNear", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, point, range), context);
//        Query query = creator.createQuery();
//
//        assertThat(query, is(query(where("address.geo").near(point).minDistance(10D).maxDistance(20D))));
//    }
//
//    @Test // DATAMONGO-1229
//    public void appliesIgnoreCaseToLeafProperty() {
//
//        PartTree tree = new PartTree("findByAddressStreetIgnoreCase", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "Street");
//
//        assertThat(new MongoQueryCreator(tree, accessor, context).createQuery(), is(notNullValue()));
//    }
//
//    @Test // DATAMONGO-1232
//    public void ignoreCaseShouldEscapeSource() {
//
//        PartTree tree = new PartTree("findByUsernameIgnoreCase", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "con.flux+");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//
//        assertThat(query, is(query(where("username").regex("^\\Qcon.flux+\\E$", "i"))));
//    }
//
//    @Test // DATAMONGO-1232
//    public void ignoreCaseShouldEscapeSourceWhenUsedForStartingWith() {
//
//        PartTree tree = new PartTree("findByUsernameStartingWithIgnoreCase", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "dawns.light+");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//
//        assertThat(query, is(query(where("username").regex("^\\Qdawns.light+\\E", "i"))));
//    }
//
//    @Test // DATAMONGO-1232
//    public void ignoreCaseShouldEscapeSourceWhenUsedForEndingWith() {
//
//        PartTree tree = new PartTree("findByUsernameEndingWithIgnoreCase", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "new.ton+");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//
//        assertThat(query, is(query(where("username").regex("\\Qnew.ton+\\E$", "i"))));
//    }
//
//    @Test // DATAMONGO-1232
//    public void likeShouldEscapeSourceWhenUsedWithLeadingAndTrailingWildcard() {
//
//        PartTree tree = new PartTree("findByUsernameLike", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "*fire.fight+*");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//
//        assertThat(query, is(query(where("username").regex(".*\\Qfire.fight+\\E.*"))));
//    }
//
//    @Test // DATAMONGO-1232
//    public void likeShouldEscapeSourceWhenUsedWithLeadingWildcard() {
//
//        PartTree tree = new PartTree("findByUsernameLike", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "*steel.heart+");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//
//        assertThat(query, is(query(where("username").regex(".*\\Qsteel.heart+\\E"))));
//    }
//
//    @Test // DATAMONGO-1232
//    public void likeShouldEscapeSourceWhenUsedWithTrailingWildcard() {
//
//        PartTree tree = new PartTree("findByUsernameLike", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "cala.mity+*");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//        assertThat(query, is(query(where("username").regex("\\Qcala.mity+\\E.*"))));
//    }
//
//    @Test // DATAMONGO-1232
//    public void likeShouldBeTreatedCorrectlyWhenUsedWithWildcardOnly() {
//
//        PartTree tree = new PartTree("findByUsernameLike", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "*");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//        assertThat(query, is(query(where("username").regex(".*"))));
//    }
//
//    @Test // DATAMONGO-1342
//    public void bindsNullValueToContainsClause() {
//
//        PartTree partTree = new PartTree("emailAddressesContains", User.class);
//
//        ConvertingParameterAccessor accessor = getAccessor(converter, new Object[] { null });
//        Query query = new MongoQueryCreator(partTree, accessor, context).createQuery();
//
//        assertThat(query, is(query(where("emailAddresses").in((Object) null))));
//    }
//
//    @Test // DATAMONGO-1424
//    public void notLikeShouldEscapeSourceWhenUsedWithLeadingAndTrailingWildcard() {
//
//        PartTree tree = new PartTree("findByUsernameNotLike", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "*fire.fight+*");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//
//        assertThat(query.getQueryObject(),
//                is(query(where("username").not().regex(".*\\Qfire.fight+\\E.*")).getQueryObject()));
//    }
//
//    @Test // DATAMONGO-1424
//    public void notLikeShouldEscapeSourceWhenUsedWithLeadingWildcard() {
//
//        PartTree tree = new PartTree("findByUsernameNotLike", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "*steel.heart+");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//
//        assertThat(query.getQueryObject(),
//                is(query(where("username").not().regex(".*\\Qsteel.heart+\\E")).getQueryObject()));
//    }
//
//    @Test // DATAMONGO-1424
//    public void notLikeShouldEscapeSourceWhenUsedWithTrailingWildcard() {
//
//        PartTree tree = new PartTree("findByUsernameNotLike", User.class);
//        MongoQueryCreator creator = new MongoQueryCreator(tree, getAccessor(converter, "cala.mity+*"), context);
//        Query query = creator.createQuery();
//
//        assertThat(query.getQueryObject(), is(query(where("username").not().regex("\\Qcala.mity+\\E.*")).getQueryObject()));
//    }
//
//    @Test // DATAMONGO-1424
//    public void notLikeShouldBeTreatedCorrectlyWhenUsedWithWildcardOnly() {
//
//        PartTree tree = new PartTree("findByUsernameNotLike", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, "*");
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//        assertThat(query.getQueryObject(), is(query(where("username").not().regex(".*")).getQueryObject()));
//    }
//
//    @Test // DATAMONGO-1588
//    public void queryShouldAcceptSubclassOfDeclaredArgument() {
//
//        PartTree tree = new PartTree("findByLocationNear", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter, new GeoJsonPoint(-74.044502D, 40.689247D));
//
//        Query query = new MongoQueryCreator(tree, accessor, context).createQuery();
//        assertThat(query.getQueryObject().containsField("location"), is(true));
//    }
//
//    @Test // DATAMONGO-1588
//    public void queryShouldThrowExceptionWhenArgumentDoesNotMatchDeclaration() {
//
//        expection.expect(IllegalArgumentException.class);
//        expection.expectMessage("Expected parameter type of " + Point.class);
//
//        PartTree tree = new PartTree("findByLocationNear", User.class);
//        ConvertingParameterAccessor accessor = getAccessor(converter,
//                new GeoJsonLineString(new Point(-74.044502D, 40.689247D), new Point(-73.997330D, 40.730824D)));
//
//        new MongoQueryCreator(tree, accessor, context).createQuery();
//    }
//
//    interface PersonRepository extends Repository<Person, Long> {
//
//        List<Person> findByLocationNearAndFirstname(Point location, Distance maxDistance, String firstname);
//    }
//
//    class User {
//
//        ObjectId id;
//
//        @Field("foo") String username;
//
//        @DBRef User creator;
//
//        List<String> emailAddresses;
//
//        Address address;
//
//        Address2dSphere address2dSphere;
//
//        Point location;
//    }
//
//    static class Address {
//
//        String street;
//
//        Point geo;
//    }
//
//    static class Address2dSphere {
//
//        String street;
//
//        @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE) Point geo;
//    }

}
