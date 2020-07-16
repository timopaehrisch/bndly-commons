package org.bndly.schema.fixtures.impl;

/*-
 * #%L
 * Schema Fixtures
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

import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.schema.api.Record;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.fixtures.api.FixtureDeploymentException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ConditionEvaluator {
	public static interface ConditionContext {

		public SchemaBeanFactory getSchemaBeanFactory();
		String getInstanceType();
		JSValue getDefinition();
		JSObject getEntry();
		void onFoundRecord(Record foundRecord) throws FixtureDeploymentException;
		JSObject getEntry(String key) throws FixtureDeploymentException;
		
	}
	boolean shouldBePersisted(ConditionContext conditionContext) throws FixtureDeploymentException;
}
