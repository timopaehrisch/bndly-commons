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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.SchemaBeanProvider;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.fixtures.api.FixtureDeploymentException;
import org.bndly.schema.impl.AccessorImpl;
import org.bndly.schema.impl.json.RecordJsonConverterImpl;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaBuilder;
import org.bndly.schema.model.StringAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FixtureDeploymentTest extends AbstractSchemaTest {

	private Deployer deployer;
	private AccessorImpl accessor;
	private FixtureDeployerImpl fixtureDeployerImpl;
	private SchemaBeanFactory sbf;
	
	@Test
	public void testDeployment() throws FixtureDeploymentException, IOException {
		bootstrap();
		
		{
			StringWriter sw = new StringWriter();
			fixtureDeployerImpl.dumpFixture("test", sw);
			sw.flush();
			String dumpedSchema = sw.getBuffer().toString();
			Assert.assertEquals(dumpedSchema, "{\"items\":[{\"type\":\"Foo\",\"entries\":[]},{\"type\":\"Bar\",\"entries\":[]}]}");
		}
		
		try (InputStream is = Files.newInputStream(Paths.get("src", "test", "resources", "onetoonefixture.json"), StandardOpenOption.READ)) {
			fixtureDeployerImpl.deploy("test", new InputStreamReader(is, "UTF-8"));
			performAssertsForOneToOne();
			
			// now test, if dumping still works
			StringWriter dumpedAfterDeployment = new StringWriter();
			fixtureDeployerImpl.dumpFixture("test", dumpedAfterDeployment);
			dumpedAfterDeployment.flush();
			String dump = dumpedAfterDeployment.getBuffer().toString();
			Assert.assertNotNull(dump);
		}
	}
	
	@Test
	public void testDeploymentOfDump() throws FixtureDeploymentException, IOException {
		bootstrap();
		try (InputStream is = Files.newInputStream(Paths.get("src", "test", "resources", "onetoonefixture_after_deployment.json"), StandardOpenOption.READ)) {
			fixtureDeployerImpl.deploy("test", new InputStreamReader(is, "UTF-8"));
			// check if there is two foo instances
			// check if there is one bar instance
			performAssertsForOneToOne();
		}
	}
	
	private void bootstrap() {
		this.deployer = engine.getDeployer();
		this.accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
		.type("Foo")
			.attribute("name", StringAttribute.class)
			.typeAttribute("myBar", "Bar")
		
		.type("Bar")
			.typeAttribute("myFoo", "Foo")
				.mandatory()
				.toOneAttribute("myBar")
			.attribute("name", StringAttribute.class);
		Schema schema = sb.getSchema();
		deployer.deploy(schema);
		
		// create a schema bean factory
		final ClassLoader classloader = getClass().getClassLoader();
		this.sbf = new SchemaBeanFactory(new SchemaBeanProvider() {
			@Override
			public String getSchemaName() {
				return "test";
			}

			@Override
			public String getSchemaBeanPackage() {
				return "org.bndly.schema.fixtures.impl";
			}

			@Override
			public ClassLoader getSchemaBeanClassLoader() {
				return classloader;
			}
		});
		sbf.registerTypeBinding("Foo", Foo.class);
		sbf.registerTypeBinding("Bar", Bar.class);
		sbf.setEngine(engine);
		
		// create a FixtureDeployerImpl
		this.fixtureDeployerImpl = new FixtureDeployerImpl();
		fixtureDeployerImpl.setBase64Service(base64Service);
		fixtureDeployerImpl.setRecordJsonConverter(new RecordJsonConverterImpl());
		
		fixtureDeployerImpl.addSchemaBeanFactory(sbf);
	}

	private void performAssertsForOneToOne() {
		// check if there is two foo instances
		// check if there is one bar instance
		Iterator<Record> lonesome = accessor.query("PICK Foo f IF f.name=?", "lonesome foo");
		Assert.assertTrue(lonesome.hasNext());
		Foo lonesomeFoo = sbf.getSchemaBean(Foo.class, lonesome.next());
		Assert.assertEquals(lonesomeFoo.getName(), "lonesome foo");
		Assert.assertNull(lonesomeFoo.getMyBar());
		
		Iterator<Record> fooWithReferenceToBar = accessor.query("PICK Foo f IF f.name=?", "this is foo");
		Assert.assertTrue(fooWithReferenceToBar.hasNext());
		Foo fooWithReferenceToBarFoo = sbf.getSchemaBean(Foo.class, fooWithReferenceToBar.next());
		Assert.assertEquals(fooWithReferenceToBarFoo.getName(), "this is foo");
		Assert.assertNotNull(fooWithReferenceToBarFoo.getMyBar());
		Assert.assertEquals(fooWithReferenceToBarFoo.getMyBar().getName(), "this is bar");
		
		Iterator<Record> anotherFooWithReferenceToBar = accessor.query("PICK Foo f IF f.name=?", "another foo");
		Assert.assertTrue(anotherFooWithReferenceToBar.hasNext());
		Foo anotherFooWithReferenceToBarFoo = sbf.getSchemaBean(Foo.class, anotherFooWithReferenceToBar.next());
		Assert.assertEquals(anotherFooWithReferenceToBarFoo.getName(), "another foo");
		Assert.assertNotNull(anotherFooWithReferenceToBarFoo.getMyBar());
		Assert.assertEquals(anotherFooWithReferenceToBarFoo.getMyBar().getName(), "another bar");
	}
}
