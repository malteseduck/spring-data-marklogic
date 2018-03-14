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
package org.malteseduck.springframework.data.marklogic.repository.query;

import com.marklogic.client.query.StructuredQueryDefinition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import org.malteseduck.springframework.data.marklogic.core.MarkLogicTemplate;
import org.malteseduck.springframework.data.marklogic.core.Pet;
import org.malteseduck.springframework.data.marklogic.core.convert.JacksonMarkLogicConverter;
import org.malteseduck.springframework.data.marklogic.core.mapping.MarkLogicMappingContext;
import org.malteseduck.springframework.data.marklogic.repository.PersonRepository;
import org.malteseduck.springframework.data.marklogic.repository.PersonXmlRepository;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.malteseduck.springframework.data.marklogic.repository.query.QueryTestUtils.*;

/**
 * Tests all the QBE queries.  You may notice that some of the properties in the "expected" JSON contain an '%' character.  These will be
 * stripped out during the comparison and are there so "valid" MarkLogic JSON can be created from a structure that may contain duplicate keys.
 * Duplicate keys are allowed in MarkLogic, but not in many JSON creators/parsers.  Single quotes are converted to double quotes as well.  This was
 * all done so the tests are more readable.
 */
public class StringMarkLogicQueryTests {

	SpelExpressionParser PARSER = new SpelExpressionParser();

	MarkLogicOperations operations;

	@Before
	public void setUp() {
		operations = new MarkLogicTemplate(client(), new JacksonMarkLogicConverter(new MarkLogicMappingContext()));
	}

	@Test
	public void bindsSimplePropertyCorrectly() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByName", String.class),
				"Bubba"
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { name: 'Bubba' }, options: {} } }"));
	}

	@Test
	public void bindsSimplePropertyWithPageable() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByGenderWithPageable", String.class, Pageable.class),
				"female", PageRequest.of(0, 2, ASC, "name")
		);
		// TODO: Should this be a multipart string?
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { gender: 'female' }, options: { 'sort-order': { direction : 'ascending', 'path-index': { text: '/name' } } } } }"));
	}

	@Test
	public void bindsSimplePropertyWithPageableElementSort() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByGenderWithPageable", String.class, Pageable.class),
				"female", PageRequest.of(0, 2, ASC, "description")
		);
		// TODO: Should this be a multipart string?
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { gender: 'female' }, options: { 'sort-order': { direction: 'ascending', element: { ns: '', name: 'description' } } } } }"));
	}

	@Test
	public void bindsSimplePropertyWithPageableMultipleSort() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByGenderWithPageable", String.class, Pageable.class),
				"female", PageRequest.of(0, 2, Sort.by(ASC, "name").and(Sort.by(DESC, "age")))
		);
		// TODO: Should this be a multipart string?
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { gender: 'female'}, options: { 'sort-order': { direction: 'ascending', 'path-index': { text: '/name' } }, 'sort-order%': { direction: 'descending', path-index: { text: '/age' } } } } }"));
	}

	@Test
	public void bindsComplexPropertyCorrectly() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByPet", Pet.class),
				new Pet("Fluffy", "cat")
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { pets: { name: 'Fluffy', type: 'cat', immunizations: null } }, options: {} } }"));
	}

	@Test
	public void testConvertsToProperJSON() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByName", String.class),
				"Bubba"
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { name: 'Bubba' }, options: {} } }"));
	}


	@Test
	public void testConvertsToProperXML() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonXmlRepository.class, "qbeFindByName", String.class),
				"Bubba"
		);
		assertThat(query.serialize())
				.isEqualTo("<search xmlns=\"http://marklogic.com/appservices/search\"><q:qbe xmlns:q=\"http://marklogic.com/appservices/querybyexample\"><q:query xmlns=\"\"><name>Bubba</name></q:query></q:qbe></search>");
	}

	@Test
	public void testBindsSimplePropertyAlreadyQuotedCorrectly() throws Exception {
		StructuredQueryDefinition query = stringQuery(
				queryMethod(PersonRepository.class, "qbeFindByNameQuoted", String.class),
				"Bubba"
		);
		assertThat(query.serialize())
				.isEqualTo(jsonQuery("{ search: { $query: { name: 'Bubba' }, options: {} } }"));

	}

//
//	@Test
//	public void bindsMultipleParametersCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameAndAddress", String.class, Address.class);
//
//		Address address = new Address("Foo", "0123", "Bar");
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "Matthews", address);
//
//		DBObject addressDbObject = new BasicDBObject();
//		converter.write(address, addressDbObject);
//		addressDbObject.removeField(DefaultMongoTypeMapper.DEFAULT_TYPE_KEY);
//
//		DBObject reference = new BasicDBObject("address", addressDbObject);
//		reference.put("lastname", "Matthews");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(), is(reference));
//	}
//
//	@Test
//	public void bindsNullParametersCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByAddress", Address.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, new Object[] { null });
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject().containsField("address"), is(true));
//		assertThat(query.getQueryObject().get("address"), is(nullValue()));
//	}
//
//	@Test 
//	public void bindsDbrefCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByHavingSizeFansNotZero");
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(), is(new BasicQuery("{ fans : { $not : { $size : 0 } } }").getQueryObject()));
//	}
//
//	@Test 
//	public void constructsDeleteQueryCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("removeByLastname", String.class);
//		assertThat(mongoQuery.isDeleteQuery(), is(true));
//	}
//
//	@Test(expected = IllegalArgumentException.class) 
//	public void preventsDeleteAndCountFlagAtTheSameTime() throws Exception {
//		createQueryForMethod("invalidMethod", String.class);
//	}
//
//	@Test 
//	public void shouldSupportFindByParameterizedCriteriaAndFields() throws Exception {
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter,
//				new BasicDBObject("firstname", "first").append("lastname", "last"), Collections.singletonMap("lastname", 1));
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByParameterizedCriteriaAndFields", DBObject.class,
//				Map.class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//
//		assertThat(query.getQueryObject(),
//				is(new BasicQuery("{ 'firstname': 'first', 'lastname': 'last'}").getQueryObject()));
//		assertThat(query.getFieldsObject(), is(new BasicQuery(null, "{ 'lastname': 1}").getFieldsObject()));
//	}
//
//	@Test 
//	public void shouldSupportRespectExistingQuotingInFindByTitleBeginsWithExplicitQuoting() throws Exception {
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "fun");
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByTitleBeginsWithExplicitQuoting", String.class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//
//		assertThat(query.getQueryObject(), is(new BasicQuery("{title: {$regex: '^fun', $options: 'i'}}").getQueryObject()));
//	}
//
//	@Test 
//	public void shouldParseQueryWithParametersInExpression() throws Exception {
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, 1, 2, 3, 4);
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByQueryWithParametersInExpression", int.class,
//				int.class, int.class, int.class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//
//		assertThat(query.getQueryObject(),
//				is(new BasicQuery("{$where: 'return this.date.getUTCMonth() == 3 && this.date.getUTCDay() == 4;'}")
//						.getQueryObject()));
//	}
//

//
//	@Test , DATAMONGO-420
//	public void bindsSimplePropertyAlreadyQuotedWithRegexCorrectly() throws Exception {
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "^Mat.*");
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameQuoted", String.class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		org.springframework.data.mongodb.core.query.Query reference = new BasicQuery("{'lastname' : '^Mat.*'}");
//
//		assertThat(query.getQueryObject(), is(reference.getQueryObject()));
//	}
//
//	@Test , DATAMONGO-420
//	public void bindsSimplePropertyWithRegexCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastname", String.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "^Mat.*");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		org.springframework.data.mongodb.core.query.Query reference = new BasicQuery("{'lastname' : '^Mat.*'}");
//
//		assertThat(query.getQueryObject(), is(reference.getQueryObject()));
//	}
//
//	@Test 
//	public void parsesDbRefDeclarationsCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("methodWithManuallyDefinedDbRef", String.class);
//		ConvertingParameterAccessor parameterAccessor = StubParameterAccessor.getAccessor(converter, "myid");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(parameterAccessor);
//
//		DBRef dbRef = DBObjectTestUtils.getTypedValue(query.getQueryObject(), "reference", DBRef.class);
//		assertThat(dbRef.getId(), is((Object) "myid"));
//		assertThat(dbRef.getCollectionName(), is("reference"));
//	}
//
//	@Test 
//	public void shouldParseJsonKeyReplacementCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("methodWithPlaceholderInKeyOfJsonStructure", String.class,
//				String.class);
//		ConvertingParameterAccessor parameterAccessor = StubParameterAccessor.getAccessor(converter, "key", "value");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(parameterAccessor);
//
//		assertThat(query.getQueryObject(), is(new BasicDBObjectBuilder().add("key", "value").get()));
//	}
//
//	@Test 
//	public void shouldSupportExpressionsInCustomQueries() throws Exception {
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "Matthews");
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByQueryWithExpression", String.class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		org.springframework.data.mongodb.core.query.Query reference = new BasicQuery("{'lastname' : 'Matthews'}");
//
//		assertThat(query.getQueryObject(), is(reference.getQueryObject()));
//	}
//
//	@Test 
//	public void shouldSupportExpressionsInCustomQueriesWithNestedObject() throws Exception {
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, true, "param1", "param2");
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByQueryWithExpressionAndNestedObject", boolean.class,
//				String.class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		org.springframework.data.mongodb.core.query.Query reference = new BasicQuery("{ 'id' : { '$exists' : true}}");
//
//		assertThat(query.getQueryObject(), is(reference.getQueryObject()));
//	}
//
//	@Test 
//	public void shouldSupportExpressionsInCustomQueriesWithMultipleNestedObjects() throws Exception {
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, true, "param1", "param2");
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByQueryWithExpressionAndMultipleNestedObjects",
//				boolean.class, String.class, String.class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		org.springframework.data.mongodb.core.query.Query reference = new BasicQuery(
//				"{ 'id' : { '$exists' : true} , 'foo' : 42 , 'bar' : { '$exists' : false}}");
//
//		assertThat(query.getQueryObject(), is(reference.getQueryObject()));
//	}
//
//	@Test 
//	public void shouldSupportNonQuotedBinaryDataReplacement() throws Exception {
//
//		byte[] binaryData = "Matthews".getBytes("UTF-8");
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, binaryData);
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameAsBinary", byte[].class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		org.springframework.data.mongodb.core.query.Query reference = new BasicQuery("{'lastname' : { '$binary' : '"
//				+ DatatypeConverter.printBase64Binary(binaryData) + "', '$type' : " + BSON.B_GENERAL + "}}");
//
//		assertThat(query.getQueryObject(), is(reference.getQueryObject()));
//	}
//
//	@Test 
//	public void shouldSupportExistsProjection() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("existsByLastname", String.class);
//
//		assertThat(mongoQuery.isExistsQuery(), is(true));
//	}
//
//	@Test 
//	public void bindsPropertyReferenceMultipleTimesCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByAgeQuotedAndUnquoted", Integer.TYPE);
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, 3);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		BasicDBList or = new BasicDBList();
//		or.add(new BasicDBObject("age", 3));
//		or.add(new BasicDBObject("displayAge", "3"));
//		BasicDBObject queryObject = new BasicDBObject("$or", or);
//		org.springframework.data.mongodb.core.query.Query reference = new BasicQuery(queryObject);
//
//		assertThat(query.getQueryObject(), is(reference.getQueryObject()));
//	}
//
//	@Test 
//	public void shouldIgnorePlaceholderPatternInReplacementValue() throws Exception {
//
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "argWith?1andText",
//				"nothing-special");
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByStringWithWildcardChar", String.class, String.class);
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(),
//				is(JSON.parse("{ 'arg0' : 'argWith?1andText' , 'arg1' : 'nothing-special'}")));
//	}
//
//	@Test 
//	public void shouldQuoteStringReplacementCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameQuoted", String.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "Matthews', password: 'foo");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(),
//				is(not(new BasicDBObjectBuilder().add("lastname", "Matthews").add("password", "foo").get())));
//		assertThat(query.getQueryObject(), is((DBObject) new BasicDBObject("lastname", "Matthews', password: 'foo")));
//	}
//
//	@Test 
//	public void shouldQuoteStringReplacementContainingQuotesCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameQuoted", String.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "Matthews', password: 'foo");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(),
//				is(not(new BasicDBObjectBuilder().add("lastname", "Matthews").add("password", "foo").get())));
//		assertThat(query.getQueryObject(), is((DBObject) new BasicDBObject("lastname", "Matthews', password: 'foo")));
//	}
//
//	@Test 
//	public void shouldQuoteStringReplacementWithQuotationsCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameQuoted", String.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter,
//				"'Dave Matthews', password: 'foo");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(),
//				is((DBObject) new BasicDBObject("lastname", "'Dave Matthews', password: 'foo")));
//	}
//
//	@Test , DATAMONGO-1575
//	public void shouldQuoteComplexQueryStringCorrectly() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameQuoted", String.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "{ $ne : 'calamity' }");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(), is((DBObject) new BasicDBObject("lastname", "{ $ne : 'calamity' }")));
//	}
//
//	@Test , DATAMONGO-1575
//	public void shouldQuotationInQuotedComplexQueryString() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameQuoted", String.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter,
//				"{ $ne : '\\'calamity\\'' }");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(), is((DBObject) new BasicDBObject("lastname", "{ $ne : '\\'calamity\\'' }")));
//	}
//
//	@Test 
//	public void shouldTakeBsonParameterAsIs() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByWithBsonArgument", DBObject.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter,
//				new BasicDBObject("$regex", "^calamity$"));
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(), is((DBObject) new BasicDBObject("arg0", Pattern.compile("^calamity$"))));
//	}
//
//	@Test 
//	public void shouldReplaceParametersInInQuotedExpressionOfNestedQueryOperator() throws Exception {
//
//		StringBasedMongoQuery mongoQuery = createQueryForMethod("findByLastnameRegex", String.class);
//		ConvertingParameterAccessor accessor = StubParameterAccessor.getAccessor(converter, "calamity");
//
//		org.springframework.data.mongodb.core.query.Query query = mongoQuery.createQuery(accessor);
//		assertThat(query.getQueryObject(), is((DBObject) new BasicDBObject("lastname", Pattern.compile("^(calamity)"))));
//	}
//
}
