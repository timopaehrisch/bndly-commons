package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
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
 * #L%
 */

import org.bndly.schema.api.tx.PreparedStatementCreator;
import org.bndly.schema.api.tx.PreparedStatementCallback;
import org.bndly.schema.api.tx.KeyHolder;
import org.bndly.schema.api.tx.Template;
import org.bndly.schema.api.tx.TransactionCallback;
import org.bndly.schema.api.tx.TransactionStatus;
import org.bndly.schema.api.tx.TransactionTemplate;
import org.bndly.schema.api.CommitHandler;
import org.bndly.schema.api.ExceptionHandlingTransactionalQuery;
import org.bndly.schema.api.Logic;
import org.bndly.schema.api.ObjectReference;
import org.bndly.schema.api.PreparedStatementArgumentSetter;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RollbackHandler;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.TransactionalQuery;
import org.bndly.schema.api.TransactionalQueryRunner;
import org.bndly.schema.api.exception.ConstraintViolationException;
import org.bndly.schema.api.exception.IntegrityException;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.query.Query;
import org.bndly.schema.api.services.TransactionFactory;
import org.bndly.schema.impl.events.PersistenceEventBuilder;
import org.bndly.schema.impl.events.PersistenceEventTransaction;
import org.bndly.schema.api.mapper.RowMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionFactoryImpl implements TransactionFactory {
	private static final Logger LOG = LoggerFactory.getLogger(TransactionFactoryImpl.class);
	
	private EngineImpl engineImpl;
	private TransactionTemplate transactionTemplate;

	private PreparedStatementCreator buildPreparedStatementCreatorFromQuery(final Query q, final String... columnsToReturn) {
		return new PreparedStatementCreator() {
			@Override
			public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
				PreparedStatement ps = connection.prepareStatement(q.getSql(), columnsToReturn);

				for (int i = 0; i < q.getArgumentSetters().length; i++) {
					PreparedStatementArgumentSetter preparedStatementArgumentSetter = q.getArgumentSetters()[i];
					preparedStatementArgumentSetter.set(i + 1, ps);
				}

				return ps;
			}
		};
	}

	public void runInTransaction(final Query... queries) {
		Transaction tx = createTransaction();
		TransactionalQueryRunner runner = tx.getQueryRunner();
		for (Query query : queries) {
			runner.run(query);
		}
		tx.commit();
	}

	@Override
	public PersistenceEventTransaction createTransaction() {
		LOG.debug("created transaction");
		return new PersistenceEventTransaction() {
			private final PersistenceEventBuilder persistenceEventBuilder = new PersistenceEventBuilder(engineImpl);
			private List<Logic> logicElements = new ArrayList<>();
			private List<CommitHandler> afterCommit = new ArrayList<>();
			private List<RollbackHandler> afterRollback = new ArrayList<>();
			private Boolean didRollBack;
			private final TransactionalQueryRunner queryRunner = new TransactionalQueryRunner() {

				@Override
				public ObjectReference<Record> single(final Query q) {
					final ObjectReference<Record> result = new ObjectReference<>();
					final RowMapper<Record> mapper = (RowMapper<Record>) q.getMapper();
					if (mapper == null) {
						throw new IllegalStateException("mapper for single record query was not build.");
					}
					add(new AbstractTemplateBasedTransactionalQueryImpl("single") {
						
						@Override
						public void execute(Transaction transaction) {
							result.set(template.queryForObject(q.getSql(), q.getArgumentSetters(), mapper));
						}
					});
					return result;
				}

				@Override
				public ObjectReference<List<Record>> list(final Query q) {
					final ObjectReference<List<Record>> result = new ObjectReference<>();
					final RowMapper<Record> mapper = (RowMapper<Record>) q.getMapper();
					if (mapper == null) {
						throw new IllegalStateException("mapper for single record query was not build.");
					}
					add(new AbstractTemplateBasedTransactionalQueryImpl("list") {
						
						@Override
						public void execute(Transaction transaction) {
							result.set(template.query(q.getSql(), q.getArgumentSetters(), mapper));
						}
					});
					return result;
				}

				@Override
				public ObjectReference<Long> number(final Query q, final String primaryKeyFieldName) {
					if (!q.asUpdate() || primaryKeyFieldName == null) {
						throw new SchemaException("provided query has to be an update and the primaryKeyFieldName has to be non-null");
					}
					final ObjectReference<Long> result = new ObjectReference<>();
					add(new AbstractTemplateBasedTransactionalQueryImpl("number: " + q.getSql()) {

						@Override
						public void execute(Transaction transaction) {
							KeyHolder keyHolder = new GeneratedKeyHolder();
							template.update(buildPreparedStatementCreatorFromQuery(q, primaryKeyFieldName), keyHolder);
							result.set(keyHolder.getKey().longValue());
						}
					});
					return result;
				}

				
				@Override
				public ObjectReference<Long> number(final Query q) {
					if (q.asUpdate()) {
						throw new SchemaException("provided query has to be a non-update (INSERT or UPDATE)");
					}
					final ObjectReference<Long> result = new ObjectReference<>();
					add(new AbstractTemplateBasedTransactionalQueryImpl("number: " + q.getSql()) {
	
						@Override
						public void execute(Transaction transaction) {
							result.set(template.queryForObject(q.getSql(), q.getArgumentSetters(), Long.class));
						}
					});
					return result;
				}

				@Override
				public void run(final Query q) {
					add(new AbstractTemplateBasedTransactionalQueryImpl("run") {
						
						@Override
						public void execute(Transaction transaction) {
							template.update(buildPreparedStatementCreatorFromQuery(q));
						}
					});
				}

				@Override
				public void uploadBlob(final Query q) {
					add(new AbstractTemplateBasedTransactionalQueryImpl("upload blob") {
						
						@Override
						public void execute(Transaction transaction) {
							template.execute(q.getSql(), new PreparedStatementCallback() {
								@Override
								public void setValues(PreparedStatement ps) throws SQLException {
									for (int i = 0; i < q.getArgumentSetters().length; i++) {
										PreparedStatementArgumentSetter preparedStatementArgumentSetter = q.getArgumentSetters()[i];
										int index = i + 1;
										preparedStatementArgumentSetter.set(index, ps);
									}
								}
							});
						}
					});
				}
			};
			private boolean isCommiting;

			@Override
			public PersistenceEventBuilder getPersistenceEventBuilder() {
				return persistenceEventBuilder;
			}

			@Override
			public TransactionalQueryRunner getQueryRunner() {
				return queryRunner;
			}

			@Override
			public void add(Logic logic) {
				if (logic != null) {
					if (isCommiting) {
						throw new SchemaException("can not add further logic while transaction is already commiting");
					}
					logicElements.add(logic);
				}
			}

			@Override
			public void commit() {
				LOG.debug("commiting transaction");
				if (persistenceEventBuilder.hasEventsScheduled()) {
					persistenceEventBuilder.attachToTransaction(this);
				}
				isCommiting = true;
				if (logicElements.isEmpty()) {
					return;
				}
				final ObjectHolder<RuntimeException> exceptionHolder = new ObjectHolder<>();
				final Transaction _this = this;

				transactionTemplate.doInTransaction(new TransactionCallback<Object>() {

					@Override
					public Object doInTransaction(TransactionStatus status, Template template) {
						ExceptionHandlingTransactionalQuery ehq = null;
						TransactionalQuery currentQuery = null;
						Logic currentLogic = null;
						try {
							for (Logic logic : logicElements) {
								currentLogic = logic;
								if (TemplateBasedTransactionalQuery.class.isInstance(logic)) {
									((TemplateBasedTransactionalQuery) logic).setTemplate(template);
								}
								if (TransactionalQuery.class.isInstance(logic)) {
									currentQuery = ((TransactionalQuery) logic);
								}
								if (ExceptionHandlingTransactionalQuery.class.isInstance(logic)) {
									ehq = (ExceptionHandlingTransactionalQuery) logic;
								} else {
									ehq = null;
								}
								logic.execute(_this);
							}
							currentQuery = null;
						} catch (RuntimeException e) {
							if (!ConstraintViolationException.class.isInstance(e) && !IntegrityException.class.isInstance(e)) {
								LOG.error("rolling back transaction: " + e.getMessage(), e);
							}
							status.setRollbackOnly();
							didRollBack = true;
							if (ehq != null) {
								Exception re = ehq.handleException(e);
								if (RuntimeException.class.isInstance(re)) {
									e = (RuntimeException) re;
								}
							}
							exceptionHolder.setObject(e);
							return null;
						}
						didRollBack = status.isRollbackOnly();
						return null;
					}
				});

				if (didRollBack) {
					LOG.debug("did rollback transaction");
					for (RollbackHandler rollbackHandler : afterRollback) {
						rollbackHandler.didRollback(_this);
					}
					if (exceptionHolder.getObject() != null) {
						throw exceptionHolder.getObject();
					}
				} else {
					LOG.debug("did commit transaction");
					for (CommitHandler commitHandler : afterCommit) {
						commitHandler.didCommit(_this);
					}
				}
			}

			@Override
			public boolean didRollBack() {
				if (didRollBack == null) {
					throw new IllegalStateException("transaction has not been tried to be commited.");
				}
				return didRollBack;
			}

			@Override
			public void afterCommit(CommitHandler commitHandler) {
				afterCommit.add(commitHandler);
			}

			@Override
			public void afterRollback(RollbackHandler rollbackHandler) {
				afterRollback.add(rollbackHandler);
			}

		};
	}

	public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
		this.transactionTemplate = transactionTemplate;
	}

	public void setEngineImpl(EngineImpl engineImpl) {
		this.engineImpl = engineImpl;
	}

}
