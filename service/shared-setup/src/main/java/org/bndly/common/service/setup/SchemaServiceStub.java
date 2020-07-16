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
public class SchemaServiceStub {
	private String schemaName;
	private Object customService;
	private Object genericService;
	private Object fullApi;
	private Class fullApiClass;
	private Class genericServiceClass;
	private Class customServiceClass;

	public String getSchemaName() {
		return schemaName;
	}

	public Object getCustomService() {
		return customService;
	}

	public Object getGenericService() {
		return genericService;
	}

	public Class getFullApiClass() {
		return fullApiClass;
	}

	public Class getGenericServiceClass() {
		return genericServiceClass;
	}

	public Class getCustomServiceClass() {
		return customServiceClass;
	}

	public Object getFullApi() {
		return fullApi;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public void setFullApi(Object fullApi) {
		this.fullApi = fullApi;
	}
	
	public void setCustomServiceClassName(String customServiceClassName) throws ClassNotFoundException {
		setCustomServiceClass(loadClass(customServiceClassName));
	}
	
	public void setCustomServiceClass(Class customServiceClass) {
		this.customServiceClass = customServiceClass;
	}

	public void setGenericServiceClassName(String genericServiceClassName) throws ClassNotFoundException {
		setGenericServiceClass(loadClass(genericServiceClassName));
	}
	
	public void setGenericServiceClass(Class genericServiceClass) {
		this.genericServiceClass = genericServiceClass;
	}

	public void setFullApiClassName(String fullApiClassName) throws ClassNotFoundException {
		setFullApiClass(loadClass(fullApiClassName));
	}
	
	public void setFullApiClass(Class fullApiClass) {
		this.fullApiClass = fullApiClass;
	}

	public void setCustomService(Object customService) {
		this.customService = customService;
	}

	public void setGenericService(Object genericService) {
		this.genericService = genericService;
	}
	
	protected Class loadClass(String className) throws ClassNotFoundException {
		try {
			return getClass().getClassLoader().loadClass(className);
		} catch (ClassNotFoundException e) {
			ClassLoader ccl = Thread.currentThread().getContextClassLoader();
			if (ccl != null) {
				return ccl.loadClass(className);
			} else {
				throw e;
			}
		}
	}

}
