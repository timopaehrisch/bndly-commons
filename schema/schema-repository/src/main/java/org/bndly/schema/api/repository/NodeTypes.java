package org.bndly.schema.api.repository;

/*-
 * #%L
 * Schema Repository
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
public interface NodeTypes {
	public static final String ROOT = "cy:root";
	public static final String BEAN = "cy:bean";
	public static final String BEAN_DEFINITION = "cy:beanDef";
	public static final String BEAN_PROPERTY_DEFINITION = "cy:beanPropertyDef";
	public static final String ENTITY_REFERENCE = "cy:entityRef";
	public static final String UNSTRUCTURED = "cy:unstructured";
	public static final String ARRAY = "cy:array";
	public static final String FORM_CONFIG = "cy:formConfig";
	public static final String FOLDER = "fs:folder";
	public static final String FILE = "fs:file";
}
