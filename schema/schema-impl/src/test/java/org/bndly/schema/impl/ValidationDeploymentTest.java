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

import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ValidationDeploymentTest extends AbstractSchemaTest {
	
	@Test
	public void testValidationAfterRollout() {
		DeployerImpl deployer = (DeployerImpl) engine.getDeployer();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb.type("Something")
			.attribute("name", StringAttribute.class);
		
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		deployer.reset();
		
		deployer.setValidateOnly(true);
		deployer.setValidationErrorIgnored(false);

		// this should pass, because we already rolled out
		deployer.deploy(schema);
		
		deployer.reset();
	}
	
	@Test
	public void testValidationWithoutRollout() {
		DeployerImpl deployer = (DeployerImpl) engine.getDeployer();
		deployer.setValidateOnly(true);
		deployer.setValidationErrorIgnored(false);
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb.type("Something")
			.attribute("name", StringAttribute.class);
		
		Schema schema = sb.getSchema();
		try {
			deployer.deploy(schema);
			Assert.fail("expected a schema exception, because we are in validate only mode");
		} catch (SchemaException e) {
		}

		deployer.reset();
		deployer.setValidateOnly(true);
		deployer.setValidationErrorIgnored(true);
		
		// this should pass, because we ignore errors
		deployer.deploy(schema);
	}
}
