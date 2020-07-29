/*
 * Copyright (c) 2012, cyber:con GmbH, Bonn.
 *
 * All rights reserved. This source file is provided to you for
 * documentation purposes only. No part of this file may be
 * reproduced or copied in any form without the written
 * permission of cyber:con GmbH. No liability can be accepted
 * for errors in the program or in the documentation or for damages
 * which arise through using the program. If an error is discovered,
 * cyber:con GmbH will endeavour to correct it as quickly as possible.
 * The use of the program occurs exclusively under the conditions
 * of the licence contract with cyber:con GmbH.
 */
package org.bndly.testfixture.impl;

/*-
 * #%L
 * Test Fixture Resource
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

import org.bndly.schema.fixtures.api.FixtureDeployer;
import org.bndly.schema.fixtures.api.FixtureDeploymentException;
import org.bndly.testfixture.api.TestFixtureDao;
import org.bndly.testfixture.model.TestFixture;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * DAO to setup a Test Fixture.
 */
@Component(service = TestFixtureDao.class, immediate = true)
public class TestFixtureDaoImpl implements TestFixtureDao {

	@Reference
	private FixtureDeployer fixtureDeployer;

	@Override
	public void doEstablishTestFixture(TestFixture testFixture) throws FixtureDeploymentException {
		if (testFixture == null || testFixture.getSchemaName() == null) {
			throw new TestFixtureException(testFixture, "testFixture or schemaName of testfixture was null");
		}
		fixtureDeployer.deploy(testFixture.getSchemaName(), new StringReader(testFixture.getFixture()));
	}

	@Override
	public String getCurrentTestFixture(String schemaName) throws FixtureDeploymentException {
		StringWriter sw = new StringWriter();
		writeCurrentTestFixture(schemaName, sw);
		sw.flush();
		String s = sw.getBuffer().toString();
		return s;
	}

	@Override
	public void writeCurrentTestFixture(String schemaName, Writer writer) throws FixtureDeploymentException {
		fixtureDeployer.dumpFixture(schemaName, writer);
	}

}
