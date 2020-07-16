package org.bndly.common.service.setup;

/*-
 * #%L
 * Service Shared Client Setup
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
public final class SchemaServiceConstructionGuide {
	private final String schemaName;
	private final String serviceImplementationPackage;
	private final String serviceApiPackage;
	
	private final String customServicePrefix;
	private final String defaultServicePrefix;
	private final String serviceImplementationSuffix;
	private final String serviceSuffix;

	public SchemaServiceConstructionGuide(
			String schemaName, 
			String serviceImplementationPackage, 
			String serviceApiPackage
	) {
		this(schemaName, serviceImplementationPackage, serviceApiPackage, "Custom", "Default", "ServiceImpl", "Service");
	}
	
	public SchemaServiceConstructionGuide(
			String schemaName, 
			String serviceImplementationPackage, 
			String serviceApiPackage, 
			String customServicePrefix, 
			String defaultServicePrefix, 
			String serviceImplementationSuffix, 
			String serviceSuffix
	) {
		if (schemaName == null) {
			throw new IllegalArgumentException("schemaName is not allowed to be null");
		}
		this.schemaName = schemaName;
		if (serviceImplementationPackage == null) {
			throw new IllegalArgumentException("serviceImplementationPackage is not allowed to be null");
		}
		this.serviceImplementationPackage = serviceImplementationPackage;
		if (serviceApiPackage == null) {
			throw new IllegalArgumentException("serviceApiPackage is not allowed to be null");
		}
		this.serviceApiPackage = serviceApiPackage;
		if (customServicePrefix == null) {
			throw new IllegalArgumentException("customServicePrefix is not allowed to be null");
		}
		this.customServicePrefix = customServicePrefix;
		if (defaultServicePrefix == null) {
			throw new IllegalArgumentException("defaultServicePrefix is not allowed to be null");
		}
		this.defaultServicePrefix = defaultServicePrefix;
		if (serviceImplementationSuffix == null) {
			throw new IllegalArgumentException("serviceImplementationSuffix is not allowed to be null");
		}
		this.serviceImplementationSuffix = serviceImplementationSuffix;
		if (serviceSuffix == null) {
			throw new IllegalArgumentException("serviceSuffix is not allowed to be null");
		}
		this.serviceSuffix = serviceSuffix;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getServiceImplementationPackage() {
		return serviceImplementationPackage;
	}

	public String getServiceApiPackage() {
		return serviceApiPackage;
	}

	public String getCustomServicePrefix() {
		return customServicePrefix;
	}

	public String getDefaultServicePrefix() {
		return defaultServicePrefix;
	}

	public String getServiceImplementationSuffix() {
		return serviceImplementationSuffix;
	}

	public String getServiceSuffix() {
		return serviceSuffix;
	}
	
}
