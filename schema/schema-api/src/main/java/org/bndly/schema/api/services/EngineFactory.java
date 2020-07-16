package org.bndly.schema.api.services;

/*-
 * #%L
 * Schema API
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface EngineFactory {
	static final String DIALECT_H2 = "h2";
	static final String DIALECT_POSTGRES = "postgres";
	static final String DIALECT_MYSQL = "mysql";
	static final String DIALECT_MYSQL8 = "mysql8";
	static final String DIALECT_MARIADB = "mariadb";
	static final String CONNECTION_POOLED = "pooled";
	static final String CONNECTION_SINGLE = "single";
//	Engine createEngine(String dataSourceName, String schemaName, VendorConfiguration vendorConfiguration);
//	Engine createEngine(String dataSourceName, String schemaName, VendorConfiguration vendorConfiguration, String connectionStrategy);
//	Engine createEngine(String dataSourceName, String schemaName, VendorConfiguration vendorConfiguration, String connectionStrategy, boolean validateOnly);
}
