/*
 * Copyright 2010-2011 the original author or authors.
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
package io.github.malteseduck.springframework.data.marklogic.repository.support;

import io.github.malteseduck.springframework.data.marklogic.core.MarkLogicOperations;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

import java.io.Serializable;

public class MarkLogicRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable>
        extends TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

    private MarkLogicOperations operations;
    private boolean mappingContextConfigured = false;

    public MarkLogicRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    public void setMarkLogicOperations(MarkLogicOperations operations) {
        this.operations = operations;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport#setMappingContext(org.springframework.data.mapping.context.MappingContext)
     */
    @Override
    protected void setMappingContext(MappingContext<?, ?> mappingContext) {
        super.setMappingContext(mappingContext);
        this.mappingContextConfigured = true;
    }

    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {
        return getFactoryInstance(operations);
    }

    protected RepositoryFactorySupport getFactoryInstance(MarkLogicOperations operations) {
        return new MarkLogicRepositoryFactory(operations);
    }

    @Override
    public void setTransactionManager(String transactionManager) {
        super.setTransactionManager(transactionManager);
    }

    /*
         * (non-Javadoc)
         *
         * @see
         * org.springframework.data.repository.support.RepositoryFactoryBeanSupport
         * #afterPropertiesSet()
         */
    @Override
    public void afterPropertiesSet() {

        super.afterPropertiesSet();
        Assert.notNull(operations, "MarkLogicTemplate must not be null!");

        if (!mappingContextConfigured) {
            setMappingContext(operations.getConverter().getMappingContext());
        }
    }
}
