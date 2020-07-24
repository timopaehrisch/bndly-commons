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
package org.bndly.rest.testfixture.resources;

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

import org.bndly.rest.api.ContentType;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.api.Context;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.PUT;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.beans.testfixture.TestFixtureDumpRestBean;
import org.bndly.rest.beans.testfixture.TestFixtureRestBean;
import org.bndly.rest.beans.testfixture.TestFixtureStatusRestBean;
import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.api.CacheTransactionFactory;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.schema.fixtures.api.FixtureDeploymentException;
import org.bndly.testfixture.api.TestFixtureService;
import org.bndly.testfixture.impl.TestFixtureException;
import org.bndly.testfixture.model.TestFixture;
import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * Resource Controller to let a DBUnit enabled Backend Service setup Test
 * fixtures from remote integration tests. Only enabled if
 * -Dspring.profiles.active is set to integration-test
 */
@Component(
		service = TestFixtureResourceImpl.class,
		immediate = true
)
@Designate(ocd = TestFixtureResourceImpl.Configuration.class)
@Path("testfixtures")
public class TestFixtureResourceImpl {

	@ObjectClassDefinition(
		name = "Test Fixture Resource",
		description = "The Test Fixture Resource should be used to retrieve or install fixed sets of test data in a schema. The data set is defined in a JSON format."
	)
	public @interface Configuration {
		@AttributeDefinition(
				name = "Buffer size",
				description = "Number of bytes to buffer when dumping a schema to a stream."
		)
		int bufferSize() default 512;
	}
	
	@Reference
	private TestFixtureService testFixtureService;
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	@Reference
	private CacheTransactionFactory cacheTransactionFactory;
	private int bufferSize;

	@Activate
	public void activate(Configuration configuration) {
		bufferSize = configuration.bufferSize();
		controllerResourceRegistry.deploy(this);
	}
	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}
	
	@GET
	@AtomLinks({
		@AtomLink(rel = "testFixtures", target = Services.class),
		@AtomLink(rel = "self", target = TestFixtureStatusRestBean.class)
	})
	public Response status() {
		TestFixtureStatusRestBean s = new TestFixtureStatusRestBean();
		return Response.ok(s);
	}

	@POST
	@AtomLinks({
		@AtomLink(rel = "dump", target = TestFixtureStatusRestBean.class)
	})
	public Response dump(TestFixtureDumpRestBean bean, @Meta Context context) {
		String schemaName = bean.getSchemaName();
		if (schemaName == null) {
			return Response.status(404);
		}
		ResourceURIBuilder builder = context.createURIBuilder();
		String uri = builder
				.pathElement("testfixtures")
				.pathElement("database")
				.pathElement(schemaName)
				.build().asString();
		return Response.seeOther(uri);
	}

	@PUT
	@Path("database")
	@AtomLink(rel = "update", target = TestFixtureStatusRestBean.class)
	public Response update(TestFixtureRestBean testFixtureRestBean) {
		TestFixture testFixture = new TestFixture();
		testFixture.setName(testFixtureRestBean.getName());
		testFixture.setPurpose(testFixtureRestBean.getPurpose());
		testFixture.setOrigin(testFixtureRestBean.getOrigin());
		testFixture.setFixture(testFixtureRestBean.getDataSetContent());
		testFixture.setSchemaName(testFixtureRestBean.getSchemaName());
		try {
			testFixtureService.doEstablishTestFixture(testFixture);
			if (testFixture.getSchemaName() != null) {
				try (CacheTransaction cacheTransaction = cacheTransactionFactory.createCacheTransaction()) {
					cacheTransaction.flush("/testfixtures/database/" + testFixture.getSchemaName());
				}
			}
			return Response.ok();
		} catch (TestFixtureException e) {
			throw e;
		} catch (Exception e) {
			throw new TestFixtureException(testFixture, "failed to setup test fixture '" + testFixture.getName() + "'", e);
		}
	}

	@GET
	@Path("database/{schemaName}")
	public Response get(@PathParam("schemaName") String schemaName) throws FixtureDeploymentException {
		try {
			TestFixtureRestBean testFixtureRestBean = new TestFixtureRestBean();
			testFixtureRestBean.setName("Export_" + new Date());
			testFixtureRestBean.setPurpose("unspecified");
			testFixtureRestBean.setOrigin("server");
			testFixtureRestBean.setSchemaName(schemaName);
			String dataSetXml = testFixtureService.getCurrentTestFixture(testFixtureRestBean.getSchemaName());
			testFixtureRestBean.setDataSetContent(dataSetXml);
			return Response.ok(testFixtureRestBean);
		} catch (FixtureDeploymentException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@GET
	@Path("database/{schemaName}.json")
	public Response getPlainJson(@PathParam("schemaName") String schemaName, @Meta Context context) throws FixtureDeploymentException {
		context.setOutputContentType(ContentType.JSON, "UTF-8");
		try (OutputStream os = context.getOutputStream()) {
			BufferedOutputStream bos = new BufferedOutputStream(os, bufferSize);
			Writer writer = new OutputStreamWriter(bos, "UTF-8");
			testFixtureService.writeCurrentTestFixture(schemaName, writer);
			writer.flush();
			bos.flush();
			return Response.ok();
		} catch (FixtureDeploymentException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setTestFixtureService(TestFixtureService testFixtureService) {
		this.testFixtureService = testFixtureService;
	}

}
