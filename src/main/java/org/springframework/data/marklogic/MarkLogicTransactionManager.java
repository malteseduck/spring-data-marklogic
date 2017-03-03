/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.data.marklogic;

import com.marklogic.client.DatabaseClient;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public class MarkLogicTransactionManager extends AbstractPlatformTransactionManager {

	protected DatabaseClient client;

	public MarkLogicTransactionManager(DatabaseClient client) {
		Assert.notNull(client, "Need a database client in order to manage transactions");
		this.client = client;
	}

	public DatabaseClient getClient() {
		return client;
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		TransactionHolder holder = (TransactionHolder) TransactionSynchronizationManager.getResource(getClient());
		if (holder != null) {
			holder.setNewTransaction(false);
			return holder;
		} else {
			return new TransactionHolder(getClient());
		}
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		TransactionHolder holder = (TransactionHolder) transaction;
		return holder != null && holder.isTransactionActive();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		TransactionHolder holder = (TransactionHolder) transaction;

		holder.setName(definition.getName());

		int timeout = determineTimeout(definition);
		if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
			holder.setTimeoutInSeconds(timeout);
		}

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException("MarkLogicTransactionManager is not allowed to support custom isolation levels");
		}

		// TODO: Support read-only?

		holder.begin();

		if (holder.isNewTransaction()) {
			holder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.bindResource(getClient(), holder);
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		TransactionHolder holder = (TransactionHolder) status.getTransaction();
		if (holder != null && holder.isTransactionActive()) holder.getTransaction().commit();
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
		TransactionHolder holder = (TransactionHolder) status.getTransaction();
		if (holder != null && holder.isTransactionActive()) holder.getTransaction().rollback();
	}

	@Override
	protected Object doSuspend(Object transaction) throws TransactionException {
		TransactionHolder holder = (TransactionHolder) transaction;
		holder.setTransaction(null);
		return TransactionSynchronizationManager.unbindResource(getClient());
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) throws TransactionException {
		TransactionSynchronizationManager.bindResource(getClient(), suspendedResources);
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
		TransactionHolder holder = (TransactionHolder) status.getTransaction();
		if (holder != null) holder.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		TransactionHolder holder = (TransactionHolder)  transaction;
		TransactionSynchronizationManager.unbindResourceIfPossible(getClient());
	}
}
