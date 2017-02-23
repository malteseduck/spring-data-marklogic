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
import com.marklogic.client.query.StructuredQueryBuilder.ContainerIndex;
import com.marklogic.client.query.StructuredQueryBuilder.TextIndex;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.springframework.data.marklogic.core.mapping.DocumentFormat.XML;
import static org.springframework.data.repository.query.parser.Part.IgnoreCaseType.ALWAYS;
import static org.springframework.data.repository.query.parser.Part.IgnoreCaseType.WHEN_POSSIBLE;

class MarkLogicQueryCreator extends AbstractQueryCreator<StructuredQueryDefinition, StructuredQueryDefinition> {

    private static final Logger LOG = LoggerFactory.getLogger(MarkLogicQueryCreator.class);
    private final MarkLogicOperations operations;
    private final MarkLogicQueryMethod method;
    private final MappingContext<?, MarkLogicPersistentProperty> context;
    private final StructuredQueryBuilder qb = new StructuredQueryBuilder();

    public MarkLogicQueryCreator(PartTree tree, ParameterAccessor accessor, MarkLogicOperations operations, MappingContext<?, MarkLogicPersistentProperty> context, MarkLogicQueryMethod method) {
        super(tree, accessor);
        Assert.notNull(context, "MappingContext must not be null!");
        Assert.notNull(operations, "MarkLogicOperations must not be null!");
        Assert.notNull(method, "MarkLogicQueryMethod must not be null!");

        this.operations = operations;
        this.context = context;
        this.method = method;
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
        // TODO: Always create a CombinedQueryDefinition so we can merge in more options?  Need a "modified" implementation that allows modifying an existing combined
        StructuredQueryDefinition query = operations.sortQuery(sort, criteria, method.getEntityInformation().getJavaType());

        if (LOG.isDebugEnabled()) {
            LOG.debug("Created query " + query);
        }

        return query;
    }

    private StructuredQueryDefinition from(Part part, Iterator<Object> parameters) {
        Type type = part.getType();
        PersistentPropertyPath<MarkLogicPersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
        MarkLogicPersistentProperty property = path.getLeafProperty();
        String name = path.toDotPath();
        // TODO: Simplify?  Pass part to functions?  Pass parameters instead of parameters.next()?  Do getTextIndex() in single place?

        switch (type) {
            //
            case AFTER:
            case GREATER_THAN:
            case GREATER_THAN_EQUAL:
            case BEFORE:
            case LESS_THAN:
            case LESS_THAN_EQUAL:
            case BETWEEN:
                // TODO: Support range queries
                throw new IllegalArgumentException("Unsupported keyword!");
            case IS_NOT_NULL:
                return qb.not(createValueCriteria(property, getTextIndex(name), null, shouldIgnoreCase(part)));
            case IS_NULL:
                return createValueCriteria(property, getTextIndex(name), null, shouldIgnoreCase(part));
            case NOT_IN:
                return qb.not(createValueCriteria(property, getTextIndex(name), parameters.next(), shouldIgnoreCase(part)));
            case IN:
                return createValueCriteria(property, getTextIndex(name), parameters.next(), shouldIgnoreCase(part));
            case LIKE:
            case STARTING_WITH:
                return createWordCriteria(property, getTextIndex(name), formatWords(parameters.next(), "%s*"), shouldIgnoreCase(part));
            case ENDING_WITH:
                return createWordCriteria(property, getTextIndex(name), formatWords(parameters.next(), "*%s"), shouldIgnoreCase(part));
            case CONTAINING:
                return createContainingCriteria(name, property, parameters, shouldIgnoreCase(part));
            case NOT_LIKE:
                return qb.not(createContainingCriteria(name, property, parameters, shouldIgnoreCase(part)));
            case NOT_CONTAINING:
                return qb.not(createContainingCriteria(name, property, parameters, shouldIgnoreCase(part)));
            case REGEX:
                // TODO: What types of regex is passed?  Can this really be supported
                return createWordCriteria(property, getTextIndex(name), formatWords(parameters.next(), "%s"), shouldIgnoreCase(part));
            case EXISTS:
                return qb.containerQuery((ContainerIndex) getTextIndex(name), qb.and());
            case TRUE:
                return createValueCriteria(property, getTextIndex(name), true, shouldIgnoreCase(part));
            case FALSE:
                return createValueCriteria(property, getTextIndex(name), false, shouldIgnoreCase(part));
            case NEAR:
            case WITHIN:
                // TODO: Support near queries
                // TODO: Support geo queries?
                throw new IllegalArgumentException("Unsupported keyword!");
            case SIMPLE_PROPERTY:
                return createValueCriteria(property, getTextIndex(name), parameters.next(), shouldIgnoreCase(part));
            case NEGATING_SIMPLE_PROPERTY:
                // TODO: is a not() query really the same thing as "negating simple"?
                return qb.not(createValueCriteria(property, getTextIndex(name), parameters.next(), shouldIgnoreCase(part)));
            default:
                throw new IllegalArgumentException("Unsupported keyword!");
        }
    }

    private TextIndex getTextIndex(String name) {
        return XML.equals(method.getFormat()) ? qb.element(name) : qb.jsonProperty(name);
    }

    private String[] formatWords(Object values, String format) {
        List<String> words = asList(asArray(values, String[].class));

        return words.stream()
                .map(word -> {
                    if (word.contains("*")) return word;
                    else return String.format(format, word);
                })
                .toArray(String[]::new);
    }

    private StructuredQueryDefinition createValueCriteria(MarkLogicPersistentProperty property, TextIndex index, Object values, boolean ignoreCase) {
        List<String> options = new ArrayList<>();

        if (ignoreCase) options.add("case-insensitive");

        // TODO: Is there a better way to do this logic?

        if (values == null) {
            if (!options.isEmpty())
                return qb.value(index, null, options.toArray(new String[0]), 1.0, (String) null);
            else
                return qb.value(index, (String) null);
        } else if (values.getClass().isArray() || property.isCollectionLike()) {
            if (values instanceof Boolean[]) {
                throw new IllegalArgumentException("Condition for property '" + property.getName() + "' can not match multiple boolean values");
            } else if (values instanceof Number[]) {
                if (!options.isEmpty())
                    return qb.value(index, null, options.toArray(new String[0]), 1.0, asArray(values, Number[].class));
                else
                    return qb.value(index, asArray(values, Number[].class));
            } else {
                if (!options.isEmpty())
                    return qb.value(index, null, options.toArray(new String[0]), 1.0, asArray(values, String[].class));
                else
                    return qb.value(index, asArray(values, String[].class));
            }
        } else {
            if (values instanceof Boolean) {
                if (!options.isEmpty())
                    return qb.value(index, null, options.toArray(new String[0]), 1.0, as(values, Boolean.class));
                else
                    return qb.value(index, as(values, Boolean.class));
            } else if (values instanceof Number) {
                if (!options.isEmpty())
                    return qb.value(index, null, options.toArray(new String[0]), 1.0, as(values, Number.class));
                else
                    return qb.value(index, as(values, Number.class));
            } else {
                if (!options.isEmpty())
                    return qb.value(index, null, options.toArray(new String[0]), 1.0, as(values, String.class));
                else
                    return qb.value(index, as(values, String.class));
            }
        }
    }

    private StructuredQueryDefinition createWordCriteria(MarkLogicPersistentProperty property, TextIndex index, String[] words) {
        return createWordCriteria(property, index, words, false);
    }

    private StructuredQueryDefinition createWordCriteria(MarkLogicPersistentProperty property, TextIndex index, String[] words, boolean ignoreCase) {
        List<String> options = new ArrayList<>();

        // If there are any wild cards we need to specify the "wildcarded" options so it processes correctly
        if (Stream.of(words).anyMatch(word ->  word.contains("*"))) options.add("wildcarded");

        if (ignoreCase) options.add("case-insensitive");

        if (!options.isEmpty())
            return qb.word(index, null, options.toArray(new String[0]), 1.0, words);
        else
            return qb.word(index, words);
    }

    private boolean shouldIgnoreCase(Part part) {
        return part.shouldIgnoreCase() == WHEN_POSSIBLE || part.shouldIgnoreCase() == ALWAYS;
    }

    private StructuredQueryDefinition createContainingCriteria(String name, MarkLogicPersistentProperty property, Iterator<Object> parameters, boolean ignoreCase) {

        if (property.isCollectionLike()) {
            return createValueCriteria(property, getTextIndex(name), parameters.next(), ignoreCase);
        }

        return createWordCriteria(property, getTextIndex(name), formatWords(parameters.next(), "*%s*"), ignoreCase);
    }


    @SuppressWarnings("unchecked")
    private <T> T as(Object value, Class<T> type) {

        if (ClassUtils.isAssignable(type, value.getClass())) {
            return (T) value;
        }

        throw new IllegalArgumentException(
                String.format("Expected parameter type of %s but got %s!", type, value.getClass()));
    }

    @SuppressWarnings("unchecked")
    private <T> T[] asArray(Object values, Class<T[]> type) {

        if (values instanceof Collection) {
            return (T[]) ((Collection<T>) values).toArray();
        } else if (values != null && values.getClass().isArray()) {
            return (T[]) values;
        }

        return Arrays.copyOf(new Object[] { values }, 1, type);
    }
}
