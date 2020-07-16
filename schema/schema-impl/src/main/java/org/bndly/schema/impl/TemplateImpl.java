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

import org.bndly.schema.api.tx.Key;
import org.bndly.schema.api.tx.PreparedStatementCreator;
import org.bndly.schema.api.tx.PreparedStatementCallback;
import org.bndly.schema.api.tx.KeyHolder;
import org.bndly.schema.api.tx.Template;
import org.bndly.schema.api.PreparedStatementArgumentSetter;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.api.mapper.RowMapper;
import org.bndly.schema.vendor.VendorConfiguration;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TemplateImpl implements Template {

	private static final Logger LOG = LoggerFactory.getLogger(TemplateImpl.class);
	
	private final VendorConfiguration vendorConfiguration;
	private final Engine engine;
	private final Connection connection;

	public TemplateImpl(VendorConfiguration vendorConfiguration, Engine engine, Connection connection) {
		this.vendorConfiguration = vendorConfiguration;
		this.engine = engine;
		this.connection = connection;
	}
	
	private <E> E runOnConnection(ConnectionCallback<E> connectionCallback) {
		return connectionCallback.doWithConnection(connection);
	}
	
	@Override
	public <E> List<E> query(final String sql, final Object[] arguments, final RowMapper<E> mapper) {
		return query(sql, wrapObjectArrayAsArgumentSetters(arguments), mapper);
	}
	
	@Override
	public <E> List<E> query(final String sql, final PreparedStatementArgumentSetter[] arguments, final RowMapper<E> mapper) {
		return runOnConnection(new ConnectionCallback<List<E>>() {

			@Override
			public List<E> doWithConnection(Connection connection) {
				PreparedStatement ps = prepareStatementOnConnection(connection, sql);
				setParametersInPreparedStatement(ps, arguments);
				boolean hasResultSet = executePreparedStatement(ps, sql);
				if (hasResultSet) {
					ResultSet rs = retrieveResultSet(ps, sql);
					final List<E> l = new ArrayList<>();
					iterate(rs, ps, new ResultSetIterator() {

						@Override
						public void onRow(ResultSet rs, int rowIndex) throws SQLException {
							E row;
							row = mapper.mapRow(rs, rowIndex);
							l.add(row);
						}
					}, sql);
					releaseResources(rs,ps);
					return l;
				} else {
					// not a result set
					releaseResources(ps);
					return null;
				}
			}
		});
	}

	private PreparedStatement prepareStatementOnConnection(Connection connection, String sql) {
		PreparedStatement ps;
		try {
			ps = connection.prepareStatement(sql);
			return ps;
		} catch (SQLException e) {
			throw vendorConfiguration.getErrorCodeMapper().map(e, engine);
		}
	}

	@Override
	public <E> List<E> query(String sql, RowMapper<E> mapper) {
		return query(sql, null, mapper);
	}

	private void setParametersInPreparedStatement(PreparedStatement ps, PreparedStatementArgumentSetter[] arguments) {
		if (arguments != null) {
			for (int i = 1; i <= arguments.length; i++) {
				PreparedStatementArgumentSetter argumentSetter = arguments[i - 1];
				try {
					argumentSetter.set(i, ps);
				} catch (SQLException e) {
					silentlyClose(ps);
					throw vendorConfiguration.getErrorCodeMapper().map("could not set parameters in prepared statement", e, engine);
				}
			}
		}
	}
	
	private boolean executePreparedStatement(PreparedStatement ps, String sql) {
		if (sql != null) {
			LOG.debug(sql);
		}
		boolean hasResultSet;
		try {
			hasResultSet = ps.execute();
		} catch (SQLException ex) {
			silentlyClose(ps);
			throw vendorConfiguration.getErrorCodeMapper().map("failed to execute statement: " + sql, ex, engine);
		}
		return hasResultSet;
	}
	
	private ResultSet retrieveResultSet(PreparedStatement ps, String sql) {
		ResultSet rs;
		try {
			rs = ps.getResultSet();
		} catch (SQLException ex) {
			silentlyClose(ps);
			throw vendorConfiguration.getErrorCodeMapper().map("failed to execute statement: " + sql, ex, engine);
		}
		return rs;
	}
	
	private PreparedStatementArgumentSetter[] wrapObjectArrayAsArgumentSetters(Object[] arguments) {
		if (arguments == null) {
			return null;
		}
		PreparedStatementArgumentSetter[] res = new PreparedStatementArgumentSetter[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			final Object argument = arguments[i];
			res[i] = new PreparedStatementArgumentSetter() {

				@Override
				public void set(int index, PreparedStatement ps) throws SQLException {
					ps.setObject(index, argument);
				}
			};
		}
		return res;
	}
	
	@Override
	public <E> E queryForObject(final String sql, final Object[] arguments, final RowMapper<E> mapper) {
		return queryForObject(sql, wrapObjectArrayAsArgumentSetters(arguments), mapper);
	}
	
	@Override
	public <E> E queryForObject(final String sql, final PreparedStatementArgumentSetter[] arguments, final RowMapper<E> mapper) {
		return runOnConnection(new ConnectionCallback<E>() {

			@Override
			public <E> E doWithConnection(Connection connection) {
				PreparedStatement ps;
				try {
					ps = connection.prepareStatement(sql);
				} catch (SQLException e) {
					throw vendorConfiguration.getErrorCodeMapper().map("could not create prepared statement", e, engine);
				}
				setParametersInPreparedStatement(ps, arguments);
				boolean hasResultSet = executePreparedStatement(ps, sql);
				if (hasResultSet) {
					ResultSet rs = retrieveResultSet(ps, sql);
					final ObjectHolder<E> holder = new ObjectHolder<>();
					iterate(rs, ps, new ResultSetIterator() {

						@Override
						public void onRow(ResultSet rs, int rowIndex) throws SQLException {
							if (rowIndex == 0) {
								holder.setObject((E) mapper.mapRow(rs, rowIndex));
							}
						}
					}, sql);
					releaseResources(ps, rs);
					return holder.getObject();
				} else {
					// not a result set
					releaseResources(ps);
					return null;
				}
			}
		});
	}
	
	@Override
	public <E> E queryForObject(String sql, Object[] arguments, Class<E> type) {
		return (E) queryForObject(sql, arguments, new SimpleTypeRowMapper(type));
	}

	@Override
	public <E> E queryForObject(String sql, PreparedStatementArgumentSetter[] arguments, Class<E> type) {
		return (E) queryForObject(sql, arguments, new SimpleTypeRowMapper(type));
	}

	@Override
	public void execute(final String sql) {
		execute(sql, (PreparedStatementCallback) null);
	}

	private void invokeCallbackOnPreparedStatement(PreparedStatement ps, PreparedStatementCallback callback, String sql) {
		if (callback != null) {
			try {
				callback.setValues(ps);
			} catch (Exception ex) {
				silentlyClose(ps);
				throw new SchemaException("failed to invoke callback on prepared statement: " + sql, ex);
			}
		}
	}
	
	@Override
	public void execute(final String sql, final PreparedStatementCallback callback) {
		runOnConnection(new ConnectionCallback<Object>() {

			@Override
			public Object doWithConnection(Connection connection) {
				PreparedStatement ps = prepareStatementOnConnection(connection, sql);
				invokeCallbackOnPreparedStatement(ps, callback, sql);
				boolean hasResultSet = executePreparedStatement(ps, sql);
				if (hasResultSet) {
					// result set
				} else {
					// update count or no result
				}
				releaseResources(ps);
				return null;
			}
		});
	}
	
	private PreparedStatement invokeCallbackOnConnection(PreparedStatementCreator statementCreator, Connection connection) {
		PreparedStatement preparedStatement;
		try {
			preparedStatement = statementCreator.createPreparedStatement(connection);
		} catch (SQLException e) {
			throw vendorConfiguration.getErrorCodeMapper().map("could not created prepared statement", e, engine);
		}
		return preparedStatement;
	}
	
	private ResultSet retrieveGeneratedKeys(PreparedStatement ps) {
		try {
			final ResultSet generatedKeys = ps.getGeneratedKeys();
			return generatedKeys;
		} catch (SQLException ex) {
			silentlyClose(ps);
			throw vendorConfiguration.getErrorCodeMapper().map("failed to retrieve generated keys from statement", ex, engine);
		}
	}

	private void pushGeneratedKeyToKeyHolder(PreparedStatement ps, final KeyHolder keyHolder) {
		if (keyHolder != null) {
			ResultSet rs = retrieveGeneratedKeys(ps);
			iterate(rs, ps, new ResultSetIterator() {

				boolean didSetKey = false;

				@Override
				public void onRow(ResultSet rs, int rowIndex) throws SQLException {
					if (!didSetKey) {
						didSetKey = true;
						final long id = rs.getLong(1);
						keyHolder.setKey(new Key() {

							@Override
							public long longValue() {
								return id;
							}
						});
					}
				}
			});
			releaseResources(rs);
		}
		
	}
	
	@Override
	public void update(final PreparedStatementCreator statementCreator, final KeyHolder keyHolder) {
		runOnConnection(new ConnectionCallback() {

			@Override
			public Object doWithConnection(Connection connection) {
				PreparedStatement ps = invokeCallbackOnConnection(statementCreator, connection);
				boolean hasResultSet = executePreparedStatement(ps, null);
				if (!hasResultSet) {
					// update count or no result
					pushGeneratedKeyToKeyHolder(ps, keyHolder);
				}
				releaseResources(ps);
				return null;
			}
		});
	}

	@Override
	public void update(PreparedStatementCreator statementCreator) {
		update(statementCreator, null);
	}

	
	private void releaseResources(AutoCloseable... closeables) {
		if (closeables == null) {
			return;
		}
		for (AutoCloseable autoCloseable : closeables) {
			silentlyClose(autoCloseable);
		}
	}
	
	private void silentlyClose(AutoCloseable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception ex) {
			// ignore
		}
	}
	
	
	private void iterate(ResultSet rs, PreparedStatement ps, ResultSetIterator iter) {
		iterate(rs, ps, iter, null);
	}
	
	private void iterate(ResultSet rs, PreparedStatement ps, ResultSetIterator iter, String sql) {
		if (rs == null) {
			return;
		}
		int rowIndex = 0;
		boolean hasRows;
		do {
			try {
				hasRows = rs.next();
			} catch (SQLException ex) {
				silentlyClose(rs);
				silentlyClose(ps);
				throw vendorConfiguration.getErrorCodeMapper().map("failed to iterate result set of statement in row " + rowIndex + " : " + sql, ex, engine);
			}
			if (hasRows) {
				try {
					iter.onRow(rs, rowIndex);
				} catch (Exception ex) {
					silentlyClose(rs);
					silentlyClose(ps);
					throw new SchemaException("failed to handle row " + rowIndex + " of result set for statement: " + sql, ex);
				}
				rowIndex++;
			}
		} while (hasRows);
	}
	
	private static interface ResultSetIterator {
		void onRow(ResultSet rs, int rowIndex) throws SQLException;
	}
}
