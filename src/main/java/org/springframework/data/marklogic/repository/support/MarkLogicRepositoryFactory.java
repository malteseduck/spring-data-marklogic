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
package org.springframework.data.marklogic.repository.support;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentEntity;
import org.springframework.data.marklogic.core.mapping.MarkLogicPersistentProperty;
import org.springframework.data.marklogic.repository.query.MarkLogicEntityInformation;
import org.springframework.data.marklogic.repository.query.MarkLogicQueryMethod;
import org.springframework.data.marklogic.repository.query.StringMarkLogicQuery;
import org.springframework.data.marklogic.repository.query.PartTreeMarkLogicQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;

public class MarkLogicRepositoryFactory extends RepositoryFactorySupport {

    private static final SpelExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

    private final MarkLogicOperations operations;
    private final MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext;

    /**
     * Creates a new {@link MarkLogicRepositoryFactory} withOptions the given {@link MarkLogicOperations}.
     *
     * @param operations must not be {@literal null}.
     */
    public MarkLogicRepositoryFactory(MarkLogicOperations operations) {

        Assert.notNull(operations, "MarkLogicOperations must not be null!");

        this.operations = operations;
        this.mappingContext = operations.getConverter().getMappingContext();
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getRepositoryBaseClass(org.springframework.data.repository.core.RepositoryMetadata)
     */
    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleMarkLogicRepository.class;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryInformation)
     */
    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        MarkLogicEntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());
        return getTargetRepositoryViaReflection(information, entityInformation, operations);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getQueryLookupStrategy(org.springframework.data.repository.query.QueryLookupStrategy.Key, org.springframework.data.repository.query.EvaluationContextProvider)
     */
    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
        return new MarkLogicQueryLookupStrategy(operations, evaluationContextProvider, mappingContext);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getEntityInformation(java.lang.Class)
     */

    @SuppressWarnings("unchecked")
    public <T, ID extends Serializable> MarkLogicEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        MarkLogicPersistentEntity<?> entity = mappingContext.getPersistentEntity(domainClass);

        if (entity == null) {
            throw new MappingException(
                    String.format("Could not lookup mapping metadata for domain class %s!", domainClass.getName()));
        }

        return new MappingMarkLogicEntityInformation(entity, entity.getIdProperty().getType());
    }

    private static class MarkLogicQueryLookupStrategy implements QueryLookupStrategy {

        private final MarkLogicOperations operations;
        private final EvaluationContextProvider evaluationContextProvider;
        MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext;

        public MarkLogicQueryLookupStrategy(MarkLogicOperations operations, EvaluationContextProvider evaluationContextProvider,
                                        MappingContext<? extends MarkLogicPersistentEntity<?>, MarkLogicPersistentProperty> mappingContext) {

            this.operations = operations;
            this.evaluationContextProvider = evaluationContextProvider;
            this.mappingContext = mappingContext;
        }

        /*
         * (non-Javadoc)
         * @see org.springframework.data.repository.query.QueryLookupStrategy#resolveQuery(java.lang.reflect.Method, org.springframework.data.repository.core.RepositoryMetadata, org.springframework.data.projection.ProjectionFactory, org.springframework.data.repository.core.NamedQueries)
         */
        @Override
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata, ProjectionFactory factory,
                                            NamedQueries namedQueries) {
            MarkLogicQueryMethod queryMethod = new MarkLogicQueryMethod(method, metadata, factory, mappingContext);

            if (queryMethod.hasAnnotatedQuery())
                return new StringMarkLogicQuery(queryMethod, operations, EXPRESSION_PARSER, evaluationContextProvider);
            else
                return new PartTreeMarkLogicQuery(queryMethod, operations);
        }
    }
}
