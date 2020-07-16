package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.schema.api.services.Engine;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.json.beans.JSONSchemaBeanFactory;
import org.bndly.schema.model.Schema;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface SchemaAdapter {
	public String getName();
	public Schema getSchema();
	public String getSchemaRestBeanPackage();
	public String getSchemaBeanPackage();
	public ClassLoader getSchemaRestBeanClassLoader();
	public ClassLoader getSchemaBeanClassLoader();
	public SchemaBeanFactory getSchemaBeanFactory();
	public JSONSchemaBeanFactory getJSONSchemaBeanFactory();
	public Engine getEngine();
}
