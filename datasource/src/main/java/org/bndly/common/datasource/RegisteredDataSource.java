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

import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import javax.sql.DataSource;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class RegisteredDataSource {

	private final DataSourceConfiguration configuration;
	private final BundleContext bundleContext;
	private final String name;
	private final DataSource dataSource;
	private final String databaseSchemaName;
	private final RegisteredDriver registeredDriver;
	private ServiceRegistration registration;

	public RegisteredDataSource(
			BundleContext bundleContext, 
			String name, 
			DataSourceConfiguration configuration, 
			RegisteredDriver registeredDriver, 
			DataSource dataSource, 
			String databaseSchemaName
	) {
		if (bundleContext == null) {
			throw new IllegalArgumentException("bundleContext is not allowed to be null");
		}
		this.bundleContext = bundleContext;
		if (name == null) {
			throw new IllegalArgumentException("name is not allowed to be null");
		}
		this.name = name;
		if (configuration == null) {
			throw new IllegalArgumentException("configuration is not allowed to be null");
		}
		this.configuration = configuration;
		if (registeredDriver == null) {
			throw new IllegalArgumentException("registeredDriver is not allowed to be null");
		}
		this.registeredDriver = registeredDriver;
		if (dataSource == null) {
			throw new IllegalArgumentException("dataSource is not allowed to be null");
		}
		this.dataSource = dataSource;
		this.databaseSchemaName = databaseSchemaName;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public String getDatabaseSchemaName() {
		return databaseSchemaName;
	}

	public RegisteredDriver getRegisteredDriver() {
		return registeredDriver;
	}

	public String getName() {
		return name;
	}

	public DataSourceConfiguration getConfiguration() {
		return configuration;
	}

	public void init() {
		registration = ServiceRegistrationBuilder.newInstance(DataSource.class, dataSource)
				.pid(DataSource.class.getName() + "." + name)
				.register(bundleContext);
	}

	public void destroy() {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}
}
