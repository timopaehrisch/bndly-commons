package org.bndly.testfixture.client;

/*-
 * #%L
 * Test Fixture Test
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

import org.bndly.rest.beans.testfixture.TestFixtureDumpRestBean;
import org.bndly.rest.beans.testfixture.TestFixtureRestBean;
import org.bndly.rest.beans.testfixture.TestFixtureStatusRestBean;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.ServiceFactory;
import org.bndly.rest.client.exception.ClientException;
import org.bndly.testfixture.test.api.FixtureSetupException;

public class TestFixtureClient {

	private ServiceFactory serviceFactory;
	private HATEOASClient<TestFixtureStatusRestBean> client;

	public TestFixtureRestBean get(String schemaName) throws FixtureSetupException {
		TestFixtureDumpRestBean dumpDesc = new TestFixtureDumpRestBean();
		dumpDesc.setSchemaName(schemaName);
		try {
			TestFixtureRestBean dumpedFixture = client.follow("dump").execute(dumpDesc, TestFixtureRestBean.class);
			return dumpedFixture;
		} catch (ClientException e) {
			throw new FixtureSetupException("failed to dump fixture for schema " + schemaName, e);
		}
	}

	public void update(TestFixtureRestBean testFixtureRestBean) throws FixtureSetupException {
		try {
			client.update().execute(testFixtureRestBean);
		} catch (ClientException e) {
			throw new FixtureSetupException("failed to install fixture for schema " + testFixtureRestBean.getSchemaName(), e);
		}
	}

	public void init() throws FixtureSetupException {
		try {
			this.client = serviceFactory.getServiceClient("testFixtures", TestFixtureStatusRestBean.class);
		} catch (ClientException e) {
			throw new FixtureSetupException("failed to init test fixture client", e);
		}
	}

	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

}
