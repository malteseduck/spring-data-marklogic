/*
 * Copyright 2010-2017 the original author or authors.
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
import com.marklogic.client.query.StructuredQueryBuilder.TextIndex;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.marklogic.core.mapping.DocumentFormat;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Collection;
import java.util.Iterator;

import static org.springframework.data.marklogic.core.mapping.DocumentFormat.JSON;
import static org.springframework.data.marklogic.core.mapping.DocumentFormat.XML;

class MarkLogicQueryCreator extends AbstractQueryCreator<StructuredQueryDefinition, StructuredQueryDefinition> {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicQueryCreator.class);
    private final ParameterAccessor accessor;
    private final DocumentFormat format;
    private final MappingContext<?, MarkLogicPersistentProperty> context;
    private final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    public MarkLogicQueryCreator(PartTree tree, ParameterAccessor accessor, MappingContext<?, MarkLogicPersistentProperty> context) {
        this(tree, accessor, context, JSON);
    }

    public MarkLogicQueryCreator(PartTree tree, ParameterAccessor accessor, MappingContext<?, MarkLogicPersistentProperty> context, DocumentFormat format) {
        super(tree, accessor);
        Assert.notNull(context, "MappingContext must not be null!");

        this.accessor = accessor;
        this.context = context;
        this.format = format;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#create(org.springframework.data.repository.query.parser.Part, java.util.Iterator)
     */
    @Override
    protected StructuredQueryDefinition create(Part part, Iterator<Object> iterator) {
        return from(part, iterator);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#and(org.springframework.data.repository.query.parser.Part, java.lang.Object, java.util.Iterator)
     */
    @Override
    protected StructuredQueryDefinition and(Part part, StructuredQueryDefinition base, Iterator<Object> iterator) {
        return qb.and(base, from(part, iterator));
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#or(java.lang.Object, java.lang.Object)
     */
    @Override
    protected StructuredQueryDefinition or(StructuredQueryDefinition base, StructuredQueryDefinition criteria) {
        return qb.or(base, criteria);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.query.parser.AbstractQueryCreator#complete(java.lang.Object, org.springframework.data.domain.Sort)
     */
    @Override
    protected StructuredQueryDefinition complete(StructuredQueryDefinition criteria, Sort sort) {

//        Query query = (criteria == null ? new Query() : new Query(criteria)).with(sort);

//        if (LOG.isDebugEnabled()) {
//            LOG.debug("Created query " + query);
//        }

        return criteria;
    }

    private StructuredQueryDefinition from(Part part, Iterator<Object> parameters) {
        Type type = part.getType();
        PersistentPropertyPath<MarkLogicPersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
        MarkLogicPersistentProperty property = path.getLeafProperty();
        String name = path.toDotPath();

        switch (type) {
            case AFTER:
            case GREATER_THAN:
//                return criteria.(parameters.next());
            case GREATER_THAN_EQUAL:
//                return criteria.gt(parameters.next());
            case BEFORE:
            case LESS_THAN:
//                return criteria.lt(parameters.next());
            case LESS_THAN_EQUAL:
//                return criteria.lte(parameters.next());
            case BETWEEN:
//                return criteria.gt(parameters.next()).lt(parameters.next());
            case IS_NOT_NULL:
//                return criteria.ne(null);
            case IS_NULL:
//                return criteria.is(null);
            case NOT_IN:
//                return criteria.nin(nextAsArray(parameters));
            case IN:
//                return criteria.in(nextAsArray(parameters));
            case LIKE:
            case STARTING_WITH:
            case ENDING_WITH:
            case CONTAINING:
                return createContainingCriteria(name, property, parameters);
            case NOT_LIKE:
//                return createContainingCriteria(part, property, criteria.not(), parameters);
            case NOT_CONTAINING:
//                return createContainingCriteria(part, property, criteria.not(), parameters);
            case REGEX:
//                return criteria.regex(parameters.next().toString());
            case EXISTS:
//                return criteria.exists((Boolean) parameters.next());
            case TRUE:
//                return criteria.is(true);
            case FALSE:
//                return criteria.is(false);
            case NEAR:
            case WITHIN:

//                Object parameter = parameters.next();
//                return criteria.within((Shape) parameter);
            case SIMPLE_PROPERTY:
                return isSimpleComparisionPossible(part) ? createValueCriteria(property, getTextIndex(name), parameters.next())
                        : qb.and();
//
//                return isSimpleComparisionPossible(part) ? criteria.is(parameters.next())
//                        : createLikeRegexCriteriaOrThrow(part, property, criteria, parameters, false);

            case NEGATING_SIMPLE_PROPERTY:
                return qb.and();
//                return isSimpleComparisionPossible(part) ? criteria.ne(parameters.next())
//                        : createLikeRegexCriteriaOrThrow(part, property, criteria, parameters, true);
            default:
                throw new IllegalArgumentException("Unsupported keyword!");
        }
    }

    private TextIndex getTextIndex(String name) {
        return XML.equals(format) ? qb.element(name) : qb.jsonProperty(name);
    }

    private StructuredQueryDefinition createValueCriteria(MarkLogicPersistentProperty property, TextIndex index, Object values) {
        // TODO: Better way to handle types?
        if (values == null) {
            return qb.value(index, (String) null);
        } else if (values.getClass().isArray()) {
            if (values instanceof Boolean[])
                throw new IllegalArgumentException("Condition for property '" + property.getName() + "' can not match multiple boolean values");
            else if (values instanceof Number[])
                return qb.value(index, (Number[]) values);
            else
                return qb.value(index, (String[]) values);
        } else {
            if (values instanceof Boolean)
                return qb.value(index, ((Boolean) values).booleanValue());
            if (values instanceof Number)
                return qb.value(index, ((Number) values).intValue());
            else
                return qb.value(index, String.valueOf(values));
        }
    }



    private boolean isSimpleComparisionPossible(Part part) {
        switch (part.shouldIgnoreCase()) {
            case NEVER:
                return true;
            case WHEN_POSSIBLE:
                return part.getProperty().getType() != String.class;
            case ALWAYS:
                return false;
            default:
                return true;
        }
    }

    /**
     * Creates and extends the given criteria with a like-regex if necessary.
     *
     * @param part
     * @param property
     * @param criteria
     * @param parameters
     * @param shouldNegateExpression
     * @return the criteria extended with the like-regex.
     */
    private StructuredQueryDefinition createLikeRegexCriteriaOrThrow(Part part, MarkLogicPersistentProperty property, StructuredQueryDefinition criteria,
                                                    Iterator<Object> parameters, boolean shouldNegateExpression) {

//        PropertyPath path = part.getProperty().getLeafProperty();
//
//        switch (part.shouldIgnoreCase()) {
//
//            case ALWAYS:
//                if (path.getType() != String.class) {
//                    throw new IllegalArgumentException(
//                            String.format("Part %s must be of type String but was %s", path, path.getType()));
//                }
//                // fall-through
//
//            case WHEN_POSSIBLE:
//
//                if (shouldNegateExpression) {
//                    criteria = criteria.not();
//                }
//
//                return addAppropriateLikeRegexTo(criteria, part, parameters.next().toString());
//
//            case NEVER:
//                // intentional no-op
//        }
//
//        throw new IllegalArgumentException(String.format("part.shouldCaseIgnore must be one of %s, but was %s",
//                Arrays.asList(IgnoreCaseType.ALWAYS, IgnoreCaseType.WHEN_POSSIBLE), part.shouldIgnoreCase()));
        return criteria;
    }

    /**
     * If the target property of the comparison is of type String, then the operator checks for match using regular
     * expression. If the target property of the comparison is a {@link Collection} then the operator evaluates to true if
     * it finds an exact match within any member of the {@link Collection}.
     *
     * @return
     */
    private StructuredQueryDefinition createContainingCriteria(String name, MarkLogicPersistentProperty property, Iterator<Object> parameters) {

        if (property.isCollectionLike()) {
            return createValueCriteria(property, getTextIndex(name), parameters.next());
        }

//        return addAppropriateLikeRegexTo(criteria, part, parameters.next().toString());
        return null;
    }

    /**
     * Creates an appropriate like-regex and appends it to the given criteria.
     *
     * @param criteria
     * @param part
     * @param value
     * @return the criteria extended with the regex.
     */
    private StructuredQueryDefinition addAppropriateLikeRegexTo(StructuredQueryDefinition criteria, Part part, String value) {

//        return criteria.regex(toLikeRegex(createValueCriteria, part), toRegexOptions(part));
        return criteria;
    }

    private String toRegexOptions(Part part) {

        String regexOptions = null;
        switch (part.shouldIgnoreCase()) {
            case WHEN_POSSIBLE:
            case ALWAYS:
                regexOptions = "i";
            case NEVER:
        }
        return regexOptions;
    }

    @SuppressWarnings("unchecked")
    private <T> T nextAs(Iterator<Object> iterator, Class<T> type) {

        Object parameter = iterator.next();

        if (ClassUtils.isAssignable(type, parameter.getClass())) {
            return (T) parameter;
        }

        throw new IllegalArgumentException(
                String.format("Expected parameter type of %s but got %s!", type, parameter.getClass()));
    }

    private Object[] nextAsArray(Iterator<Object> iterator) {

        Object next = iterator.next();

        if (next instanceof Collection) {
            return ((Collection<?>) next).toArray();
        } else if (next != null && next.getClass().isArray()) {
            return (Object[]) next;
        }

        return new Object[] { next };
    }

    private String toLikeRegex(String source, Part part) {
        return null;
    }
}
