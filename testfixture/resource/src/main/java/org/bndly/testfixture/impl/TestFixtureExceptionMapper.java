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

import org.bndly.rest.controller.api.ExceptionMapper;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.rest.common.beans.util.ExceptionMessageUtil;
import org.bndly.testfixture.model.TestFixture;
import org.osgi.service.component.annotations.Component;

@Component(service = ExceptionMapper.class, immediate = true)
public class TestFixtureExceptionMapper implements ExceptionMapper<TestFixtureException> {

	@Override
	public Response toResponse(TestFixtureException e) {
		ErrorRestBean msg = new ErrorRestBean();
		msg.setMessage(e.getMessage());
		msg.setName("FatalError");
		TestFixture fixture = e.getTestFixture();
		if (fixture != null) {
			String name = fixture.getName();
			if (name != null) {
				ExceptionMessageUtil.createKeyValue(msg, "name", name);
			}
			String origin = fixture.getOrigin();
			if (origin != null) {
				ExceptionMessageUtil.createKeyValue(msg, "origin", origin);
			}
			String purpose = fixture.getPurpose();
			if (purpose != null) {
				ExceptionMessageUtil.createKeyValue(msg, "purpose", purpose);
			}
			String schemaName = fixture.getSchemaName();
			if (schemaName != null) {
				ExceptionMessageUtil.createKeyValue(msg, "schemaName", schemaName);
			}
		}
		return Response.status(400).entity(msg);
	}

}
