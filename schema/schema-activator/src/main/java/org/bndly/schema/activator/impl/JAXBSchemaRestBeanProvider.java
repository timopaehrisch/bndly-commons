package org.bndly.schema.activator.impl;

/*-
 * #%L
 * Schema Activator
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

import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import org.bndly.schema.api.SchemaRestBeanProvider;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class JAXBSchemaRestBeanProvider implements SchemaRestBeanProvider {
	private final JAXBMessageClassProvider messageClassProvider;
	private final String schemaName;
	private final String schemaRestBeanPackage;

	public JAXBSchemaRestBeanProvider(JAXBMessageClassProvider messageClassProvider, String schemaName, String schemaRestBeanPackage) {
		if (messageClassProvider == null) {
			throw new IllegalArgumentException("messageClassProvider is not allowed to be null");
		}
		this.messageClassProvider = messageClassProvider;
		if (schemaName == null) {
			throw new IllegalArgumentException("schemaName is not allowed to be null");
		}
		this.schemaName = schemaName;
		if (schemaRestBeanPackage == null) {
			throw new IllegalArgumentException("schemaRestBeanPackage is not allowed to be null");
		}
		this.schemaRestBeanPackage = schemaRestBeanPackage;
	}
	
	@Override
	public String getSchemaName() {
		return schemaName;
	}

	@Override
	public String getSchemaRestBeanPackage() {
		return schemaRestBeanPackage;
	}

	@Override
	public ClassLoader getSchemaRestBeanClassLoader() {
		return messageClassProvider.getClass().getClassLoader();
	}

	public JAXBMessageClassProvider getMessageClassProvider() {
		return messageClassProvider;
	}
	
	
}
