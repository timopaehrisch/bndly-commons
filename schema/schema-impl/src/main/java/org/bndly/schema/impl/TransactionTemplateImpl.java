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

import org.bndly.schema.api.tx.TransactionCallback;
import org.bndly.schema.api.tx.TransactionStatus;
import org.bndly.schema.api.tx.TransactionTemplate;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.vendor.VendorConfiguration;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TransactionTemplateImpl implements TransactionTemplate {

	private static final Logger LOG = LoggerFactory.getLogger(TransactionTemplateImpl.class);
	private DataSource dataSource;
	private boolean closeConnectionAfterUsage;
	private final ThreadLocal<Boolean> synchronizationLocale = new ThreadLocal<>();
	private final VendorConfiguration vendorConfiguration;
	private final Engine engine;

	public TransactionTemplateImpl(VendorConfiguration vendorConfiguration, Engine engine) {
		this.vendorConfiguration = vendorConfiguration;
		this.engine = engine;
	}

	protected Connection getConnection() throws SchemaException {
		if (dataSource == null) {
			throw new SchemaException("can not get connection, when no datasource is present.");
		}
		Connection c;
		try {
			c = dataSource.getConnection();
			return c;
		} catch (SQLException e) {
			throw vendorConfiguration.getErrorCodeMapper().map("could not obtain connection: " + e.getMessage(), e, engine);
		}
	}

	private <E> E runOnConnection(ConnectionCallback<E> connectionCallback) {
		Connection toClose = null;
		try {
			if (synchronizationLocale.get() != null && synchronizationLocale.get()) {
				throw new IllegalStateException("running on connection in a recursive pattern.");
			}
			synchronizationLocale.set(true);
			Connection connection = getConnection();
			if (closeConnectionAfterUsage) {
				toClose = connection;
			}
			return connectionCallback.doWithConnection(connection);
		} finally {
			if (toClose != null) {
				try {
					toClose.close();
				} catch (SQLException ex) {
					LOG.error("failed to close connection: " + ex.getMessage(), ex);
				}
			}
			synchronizationLocale.remove();
		}
	}

	@Override
	public <E> E doInTransaction(final TransactionCallback<E> transactionCallback) {
		return runOnConnection(new ConnectionCallback<E>() {

			@Override
			public <E> E doWithConnection(final Connection connection) {
				boolean shouldEnableAutoCommit = true;
				try {
					LOG.debug("disabling auto commit");
					shouldEnableAutoCommit = connection.getAutoCommit();
					if (shouldEnableAutoCommit) {
						// if auto commit is on, we will turn it off here.
						connection.setAutoCommit(false);
					}
				} catch (SQLException e) {
					throw vendorConfiguration.getErrorCodeMapper().map("could not disable auto commit on connection", e, engine);
				}
				TransactionStatus tx = new TransactionStatus() {
					private boolean rollbackOnly = false;

					@Override
					public boolean isRollbackOnly() {
						return rollbackOnly;
					}

					@Override
					public Connection getConnection() {
						return connection;
					}

					@Override
					public void setRollbackOnly() {
						rollbackOnly = true;
						try {
							connection.rollback();
						} catch (SQLException ex) {
							throw vendorConfiguration.getErrorCodeMapper().map(ex, engine);
						}
					}
				};
				TemplateImpl template = new TemplateImpl(vendorConfiguration, engine, connection);
				E result = (E) transactionCallback.doInTransaction(tx, template);
				try {
					if (shouldEnableAutoCommit) {
						// only if we disabled the auto commit, we commit here.
						if (connection.getAutoCommit()) {
							throw new SchemaException("can not commit auto commited connection");
						} else {
							LOG.debug("commiting");
							connection.commit();
						}
					}
				} catch (SQLException ex) {
					throw vendorConfiguration.getErrorCodeMapper().map(ex, engine);
				} finally {
					try {
						if (shouldEnableAutoCommit) {
							LOG.debug("enabling auto commit");
							connection.setAutoCommit(true);
						}
					} catch (SQLException ex) {
						throw vendorConfiguration.getErrorCodeMapper().map("failed to re-enable connection auto-commit", ex, engine);
					}
				}
				return result;
			}
		});
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public final void setCloseConnectionAfterUsage(boolean closeConnectionAfterUsage) {
		this.closeConnectionAfterUsage = closeConnectionAfterUsage;
	}

}
