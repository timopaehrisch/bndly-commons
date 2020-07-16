package org.bndly.common.datasource;

/*-
 * #%L
 * Data Source
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

import org.bndly.common.osgi.util.DictionaryAdapter;
import java.util.Collection;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(
		service = DataSourceConfiguration.class,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(factory = true, ocd = DataSourceConfiguration.Configuration.class)
public class DataSourceConfiguration {
	
	@ObjectClassDefinition(
			name = "Data Source Configuration",
			description = "This configuration defines a data source that should be made available as an OSGI service."
	)
	public @interface Configuration {
		@AttributeDefinition(
				name = "Name",
				description = "Natural name of this datasource"
		)
		String name() default "datasource";
		
		@AttributeDefinition(
				name = "Driver",
				description = "Driver class name"
		)
		String driverClass();
		
		@AttributeDefinition(
				name = "JDBC URL",
				description = "The connection JDBC URL"
		)
		String jdbcUrl();
		
		@AttributeDefinition(
				name = "Driver Version",
				description = "The version of the driver class"
		)
		String driverClassVersion();
		
		@AttributeDefinition(
				name = "DB Schema",
				description = "Name of the database schema"
		)
		String schemaName();

		@AttributeDefinition(
				name = "User",
				description = "The DB user"
		)
		String user();
		
		@AttributeDefinition(
				name = "Password",
				description = "The password of the DB user",
				type = AttributeType.PASSWORD
		)
		String password();
		
		@AttributeDefinition(
				name = "Validation query",
				description = "Query to validate connections"
		)
		String connection_validationQuery();
		
		@AttributeDefinition(
				name = "Validation query timeout",
				description = "Seconds to wait for connection validation"
		)
		String connection_validationQueryTimeout();

		@AttributeDefinition(
				name = "Connection init SQL statements",
				description = "SQL statements to execute on a new connection"
		)
		String connection_connectionInitSqls();

		@AttributeDefinition(
				name = "Read only",
				description = "Set a connection to read only"
		)
		String connection_defaultReadOnly();

		@AttributeDefinition(
				name = "Auto commit",
				description = "Set the default auto commit setting on a connection"
		)
		String connection_defaultAutoCommit();

		@AttributeDefinition(
				name = "Transaction isolation",
				description = "Set the transaction isolation level",
				options = {
					@Option(value = "", label = "undefined"),
					@Option(value = "0", label = "TRANSACTION_NONE"),
					@Option(value = "1", label = "TRANSACTION_READ_UNCOMMITTED"),
					@Option(value = "2", label = "TRANSACTION_READ_COMMITTED"),
					@Option(value = "4", label = "TRANSACTION_REPEATABLE_READ"),
					@Option(value = "8", label = "TRANSACTION_SERIALIZABLE")
				}
		)
		String connection_defaultTransactionIsolation();

		@AttributeDefinition(
				name = "Default catalog",
				description = "The default catalog on a connection"
		)
		String connection_defaultCatalog();
		
		@AttributeDefinition(
				name = "Pool statements",
				description = "Enable pooling of statements"
		)
		String connection_poolStatements();
		
		@AttributeDefinition(
				name = "Max open prepared statements",
				description = "Max number of open prepared statements"
		)
		String connection_maxOpenPreparedStatements();
		
		@AttributeDefinition(
				name = "Max connection lifetime",
				description = "Max lifetime of a connection in milliseconds"
		)
		String connection_maxConnLifetimeMillis();
		
		@AttributeDefinition(
				name = "Auto commit on connection return",
				description = "Commit on return of connection"
		)
		String connection_enableAutoCommitOnReturn();
		
		@AttributeDefinition(
				name = "Auto rollback on connection return",
				description = "Rollback on return of connection"
		)
		String connection_rollbackOnReturn();
		
		@AttributeDefinition(
				name = "Default query timeout",
				description = "Default query timeout"
		)
		String connection_defaultQueryTimeout();
		
		@AttributeDefinition(
				name = "SQL codes for disconnection",
				description = "disconnectionSqlCodes"
		)
		String connection_disconnectionSqlCodes();
		
		@AttributeDefinition(
				name = "fastFailValidation"
		)
		String connection_fastFailValidation();
		
		@AttributeDefinition(
				name = "Min idle connections",
				description = "Minimum idle connection from this datasource"
		)
		String pool_minIdle();
		
		@AttributeDefinition(
				name = "Max idle connections",
				description = "Maximum idle connections from this datasource"
		)
		String pool_maxIdle();
		
		@AttributeDefinition(
				name = "Max total connections",
				description = "Maximum number of connections from this datasource"
		)
		String pool_maxTotal();
		
		@AttributeDefinition(
				name = "Block on exhaustion",
				description = "Block the pooling when the connection pool is exhausted"
		)
		String pool_blockWhenExhausted();
		
		@AttributeDefinition(
				name = "Max wait",
				description = "Maximum time to wait for a connection"
		)
		String pool_maxWaitMillis();
		
		@AttributeDefinition(
				name = "Lifo",
				description = "Use Lifo based pooling"
		)
		String pool_lifo();
		
		@AttributeDefinition(
				name = "Test on create",
				description = "Test connection health on creation"
		)
		String pool_testOnCreate();
		
		@AttributeDefinition(
				name = "Test on borrow",
				description = "Test connection health on borrow of a connection"
		)
		String pool_testOnBorrow();
		
		@AttributeDefinition(
				name = "Test on return",
				description = "Test connection health on return of a connection"
		)
		String pool_testOnReturn();
		
		@AttributeDefinition(
				name = "Test while idle",
				description = "Test connection while a connection is idle"
		)
		String pool_testWhileIdle();
		
		@AttributeDefinition(
				name = "Eviction period",
				description = "Milliseconds between an eviction run of connections"
		)
		String pool_timeBetweenEvictionRunsMillis();
		
		@AttributeDefinition(
				name = "Eviction tests",
				description = "Number of tests per eviction run"
		)
		String pool_numTestsPerEvictionRun();
		
		@AttributeDefinition(
				name = "Eviction min idle",
				description = "Minimum idle time to evict an idle connection"
		)
		String pool_minEvictableIdleTimeMillis();
		
		@AttributeDefinition
		String pool_softMinEvictableIdleTimeMillis();
		
		@AttributeDefinition(
				name = "Eviction policy class name",
				description = "A class name of an eviction policy. NOTE: The class should be made available as a fragment bundle to this bundle."
		)
		String pool_evictionPolicyClassName();
	}
	
	private DictionaryAdapter adapter;
	
	@Activate
	public void activate(ComponentContext componentContext) {
		this.adapter = new DictionaryAdapter(componentContext.getProperties()).emptyStringAsNull();
	}

	public DictionaryAdapter getAdapter() {
		return adapter;
	}
	public String getName() { return adapter.getString("name", "datasource"); }
	public String getDriverClass() { return adapter.getString("driverClass"); }
	public String getJdbcUrl() { return adapter.getString("jdbcUrl"); }
	public String getDriverClassVersion() { return adapter.getString("driverClassVersion"); }
	public String getSchemaName() { return adapter.getString("schemaName"); }
	public String getUser() { return adapter.getString("schemaName"); }
	public String getPassword() { return adapter.getString("schemaName"); }
	public String getConnectionValidationQuery() { return adapter.getString("connection.validationQuery"); }
	public Integer getConnectionValidationQueryTimeout() { return adapter.getInteger("connection.validationQueryTimeout"); }
	public Collection<String> getConnectionInitSqls() { return adapter.getStringCollection("connection.connectionInitSqls"); }
	public Boolean getConnectionDefaultReadOnly() { return adapter.getBoolean("connection.defaultReadOnly"); }
	public Boolean getConnectionDefaultAutoCommit() { return adapter.getBoolean("connection.defaultAutoCommit"); }
	public Integer getConnectionDefaultTransactionIsolation() { return adapter.getInteger("connection.defaultTransactionIsolation"); }
	public String getConnectionDefaultCatalog() { return adapter.getString("connection.defaultCatalog"); }
	public Boolean getConnectionPoolStatements() { return adapter.getBoolean("connection.poolStatements"); }
	public Integer getConnectionMaxOpenPreparedStatements() { return adapter.getInteger("connection.maxOpenPreparedStatements"); }
	public Long getConnectionMaxConnLifetimeMillis() { return adapter.getLong("connection.maxConnLifetimeMillis"); }
	public Boolean getConnectionEnableAutoCommitOnReturn() { return adapter.getBoolean("connection.enableAutoCommitOnReturn"); }
	public Boolean getConnectionRollbackOnReturn() { return adapter.getBoolean("connection.rollbackOnReturn"); }
	public Integer getConnectionDefaultQueryTimeout() { return adapter.getInteger("connection.defaultQueryTimeout"); }
	public Collection<String> getConnectionDisconnectionSqlCodes() { return adapter.getStringCollection("connection.disconnectionSqlCodes"); }
	public Boolean getConnectionFastFailValidation() { return adapter.getBoolean("connection.fastFailValidation"); }
	public Integer getPoolMinIdle() { return adapter.getInteger("pool.minIdle"); }
	public Integer getPoolMaxTotal() { return adapter.getInteger("pool.maxTotal"); }
	public Boolean getPoolBlockWhenExhausted() { return adapter.getBoolean("pool.blockWhenExhausted"); }
	public Long getPoolMaxWaitMillis() { return adapter.getLong("pool.maxWaitMillis"); }
	public Boolean getPoolLifo() { return adapter.getBoolean("pool.lifo"); }
	public Boolean getPoolTestOnCreate() { return adapter.getBoolean("pool.testOnCreate"); }
	public Boolean getPoolTestOnBorrow() { return adapter.getBoolean("pool.testOnBorrow"); }
	public Boolean getPoolTestOnReturn() { return adapter.getBoolean("pool.testOnReturn"); }
	public Boolean getPoolTestWhileIdle() { return adapter.getBoolean("pool.testWhileIdle"); }
	public Long getPoolTimeBetweenEvictionRunsMillis() { return adapter.getLong("pool.timeBetweenEvictionRunsMillis"); }
	public Integer getPoolNumTestsPerEvictionRun() { return adapter.getInteger("pool.numTestsPerEvictionRun"); }
	public Long getPoolMinEvictableIdleTimeMillis() { return adapter.getLong("pool.minEvictableIdleTimeMillis"); }
	public Long getPoolSoftMinEvictableIdleTimeMillis() { return adapter.getLong("pool.softMinEvictableIdleTimeMillis"); }
	public String getPoolEvictionPolicyClassName() { return adapter.getString("pool.evictionPolicyClassName"); }
	public Integer getPoolMaxIdle() { return adapter.getInteger("pool.maxIdle"); }
}
