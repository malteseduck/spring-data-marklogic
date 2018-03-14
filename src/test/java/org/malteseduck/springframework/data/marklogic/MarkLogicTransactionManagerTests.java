/*
 * Copyright 2002-2017 the original author or authors.
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

package org.malteseduck.springframework.data.marklogic;

import com.marklogic.client.DatabaseClient;
import com.marklogic.client.Transaction;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.*;


public class MarkLogicTransactionManagerTests  {

	private DatabaseClient client;
	private Transaction transaction;

	private MarkLogicTransactionManager tm;

	@Before
	public void init() throws Exception {
		// We don't want this to actually hit MarkLogic so we have to mock the client even though it is not "ours"
		this.client = mock(DatabaseClient.class);
		this.transaction = mock(Transaction.class);
		when(client.openTransaction(any(), anyInt()))
				.thenReturn(this.transaction);
	}

	public static void verifyTransactionSynchronizationManagerState() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}

	@After
	public void clear() {
		if (TransactionSynchronizationManager.isSynchronizationActive())
			TransactionSynchronizationManager.clear();
	}

	@Test
	public void testTransactionCommit() throws Exception {
		doTestTransactionCommit(false);
		verifyTransactionSynchronizationManagerState();
	}

	@Test
	public void testTransactionCommitWithCreate() throws Exception {
		doTestTransactionCommit(true);
		verifyTransactionSynchronizationManagerState();
	}

	private void doTestTransactionCommit(final boolean createStatement) throws Exception {

		tm = new MarkLogicTransactionManager(client);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(client)).as("Hasn't thread transaction").isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization not active").isFalse();

		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
				assertThat(TransactionSynchronizationManager.hasResource(client)).as("Has thread transaction").isTrue();
				assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
				assertThat(status.isNewTransaction()).as("Is a new transaction").isTrue();
				assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
				assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
				// TODO: Test read-only or not?
				// TODO: Test run "create"?
			}
		});

		assertThat(TransactionSynchronizationManager.hasResource(client)).as("Hasn't thread transaction").isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization not active").isFalse();

		verify(transaction).commit();
	}

	@Test
	public void testTransactionRollback() throws Exception  {
		doTestTransactionRollback(false);
		verifyTransactionSynchronizationManagerState();
	}

	@Test
	public void testTransactionRollbackWithCreate() throws Exception  {
		doTestTransactionRollback(true);
		verifyTransactionSynchronizationManagerState();
	}

	private void doTestTransactionRollback(final boolean createStatement) throws Exception {
		tm = new MarkLogicTransactionManager(client);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(client)).as("Hasn't thread transaction").isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization not active").isFalse();

		final RuntimeException ex = new RuntimeException("Application exception");
		assertThatExceptionOfType(RuntimeException.class)
				.isThrownBy(() -> {
					tt.execute(new TransactionCallbackWithoutResult() {
						@Override
						protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
							assertThat(TransactionSynchronizationManager.hasResource(client)).as("Has thread transaction").isTrue();
							assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization active").isTrue();
							assertThat(status.isNewTransaction()).as("Is a new transaction").isTrue();
							throw ex;
						}
					});
				})
				.isEqualTo(ex);

		assertThat(TransactionSynchronizationManager.hasResource(client)).as("Hasn't thread transaction").isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization not active").isFalse();

		verify(transaction).rollback();
	}

	@Test
	public void testTransactionRollbackOnly() throws Exception {
		tm = new MarkLogicTransactionManager(client);
		tm.setTransactionSynchronization(MarkLogicTransactionManager.SYNCHRONIZATION_NEVER);
		TransactionTemplate tt = new TransactionTemplate(tm);
		assertThat(TransactionSynchronizationManager.hasResource(client)).as("Hasn't thread transaction").isFalse();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization not active").isFalse();

		TransactionHolder holder = new TransactionHolder(client);
		holder.begin();
		TransactionSynchronizationManager.bindResource(client, holder);

		final RuntimeException ex = new RuntimeException("Application exception");
		try {
			assertThatExceptionOfType(RuntimeException.class)
					.isThrownBy(() -> {
						tt.execute(new TransactionCallbackWithoutResult() {
							protected void doInTransactionWithoutResult(TransactionStatus status) throws RuntimeException {
								assertThat(TransactionSynchronizationManager.hasResource(client)).as("Has thread transaction").isTrue();
								assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization not active").isFalse();
								assertThat(status.isNewTransaction()).as("Is existing transaction").isFalse();
								throw ex;
							}
						});
					})
					.isEqualTo(ex);
		} finally {
			TransactionSynchronizationManager.unbindResourceIfPossible(client);
		}

		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).as("Synchronization not active").isFalse();
		assertThat(TransactionSynchronizationManager.hasResource(client)).as("Hasn't thread transaction").isFalse();

		verifyTransactionSynchronizationManagerState();
	}

	// TODO: MarkLogic's transactions are pretty simple, but what use cases are we missing for testing?
}
