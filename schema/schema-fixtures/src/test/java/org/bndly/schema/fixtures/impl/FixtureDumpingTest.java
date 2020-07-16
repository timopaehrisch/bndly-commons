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

import org.bndly.common.data.io.IOUtils;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.SchemaBeanProvider;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.beans.ActiveRecord;
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
public class FixtureDumpingTest extends AbstractSchemaTest {

	private Deployer deployer;
	private AccessorImpl accessor;
	private FixtureDeployerImpl fixtureDeployerImpl;
	private SchemaBeanFactory sbf;
	
	@Test
	public void testDumping() throws FixtureDeploymentException, IOException {
		bootstrap();
		RecordContext rc = accessor.buildRecordContext();
		
		NonVirtualType nonVirtualTypeInstance = sbf.getSchemaBean(NonVirtualType.class, rc.create(NonVirtualType.class.getSimpleName()));
		nonVirtualTypeInstance.setName("instance1");
		((ActiveRecord)nonVirtualTypeInstance).persist();
		
		NonVirtualType nonVirtualTypeInstance2 = sbf.getSchemaBean(NonVirtualType.class, rc.create(NonVirtualType.class.getSimpleName()));
		nonVirtualTypeInstance2.setName("instance2");
		((ActiveRecord)nonVirtualTypeInstance2).persist();
		
		NonVirtualType nonVirtualTypeInstance3 = sbf.getSchemaBean(NonVirtualType.class, rc.create(NonVirtualType.class.getSimpleName()));
		nonVirtualTypeInstance3.setName("instance3");
		
		NonVirtualType nonVirtualTypeInstance4 = sbf.getSchemaBean(NonVirtualType.class, rc.create(NonVirtualType.class.getSimpleName()));
		nonVirtualTypeInstance4.setName("instance4");
		((ActiveRecord)nonVirtualTypeInstance4).persist();
		
		OtherNonVirtualType otherNonVirtualTypeInstance = sbf.getSchemaBean(OtherNonVirtualType.class, rc.create(OtherNonVirtualType.class.getSimpleName()));
		otherNonVirtualTypeInstance.setNonVirtualType(nonVirtualTypeInstance);
		((ActiveRecord)otherNonVirtualTypeInstance).persist();
		
		OtherNonVirtualType otherNonVirtualTypeInstance2 = sbf.getSchemaBean(OtherNonVirtualType.class, rc.create(OtherNonVirtualType.class.getSimpleName()));
		otherNonVirtualTypeInstance2.setNonVirtualType(nonVirtualTypeInstance3);
		((ActiveRecord)otherNonVirtualTypeInstance2).persist();
		
		OtherNonVirtualType otherNonVirtualTypeInstance3 = sbf.getSchemaBean(OtherNonVirtualType.class, rc.create(OtherNonVirtualType.class.getSimpleName()));
		otherNonVirtualTypeInstance3.setNonVirtualType(nonVirtualTypeInstance4);
		((ActiveRecord)otherNonVirtualTypeInstance3).persist();
		((ActiveRecord)nonVirtualTypeInstance4).delete();
		
		StringWriter sw = new StringWriter();
		fixtureDeployerImpl.dumpFixture("test", sw);
		sw.flush();
		String toString = sw.getBuffer().toString();
		try (InputStream is = Files.newInputStream(Paths.get("src", "test", "resources", "dumpWithRef.json"), StandardOpenOption.READ)) {
			String expected = IOUtils.readToString(is, "UTF-8");
			Assert.assertEquals(toString, expected);
		}
	}
	
	@Test
	public void testInstallingDumpWithRef() throws IOException, FixtureDeploymentException {
		bootstrap();
		try (InputStream is = Files.newInputStream(Paths.get("src", "test", "resources", "dumpWithRef.json"), StandardOpenOption.READ)) {
			fixtureDeployerImpl.deploy("test", new InputStreamReader(is, "UTF-8"));
			
			// there should be 2 instances of NonVirtualType with name=instance1 and name=instance2
			Assert.assertEquals(accessor.count("COUNT " + NonVirtualType.class.getSimpleName()).longValue(), 2L);
			Iterator<Record> result = accessor.query("PICK " + NonVirtualType.class.getSimpleName());
			Assert.assertEquals(sbf.getSchemaBean(NonVirtualType.class, result.next()).getName(), "instance1");
			Assert.assertEquals(sbf.getSchemaBean(NonVirtualType.class, result.next()).getName(), "instance2");
			
			// there should be 3 instances of OtherNonVirtualType with
			// --- one that has a reference to instance1 with id
			// --- one that has a reference to instance3 without id
			// --- one that has a reference to instance4 without id
			Assert.assertEquals(accessor.count("COUNT " + OtherNonVirtualType.class.getSimpleName()).longValue(), 3L);
			result = accessor.query("PICK " + OtherNonVirtualType.class.getSimpleName());
			OtherNonVirtualType otherNonVirtualType1 = sbf.getSchemaBean(OtherNonVirtualType.class, result.next());
			OtherNonVirtualType otherNonVirtualType3 = sbf.getSchemaBean(OtherNonVirtualType.class, result.next());
			OtherNonVirtualType otherNonVirtualType4 = sbf.getSchemaBean(OtherNonVirtualType.class, result.next());
			Assert.assertEquals(otherNonVirtualType1.getNonVirtualType().getName(), "instance1");
			Assert.assertEquals(otherNonVirtualType3.getNonVirtualType().getName(), "instance3");
			Assert.assertEquals(otherNonVirtualType4.getNonVirtualType().getName(), "instance4");
			
		}
	}
	
	private void bootstrap() {
		this.deployer = engine.getDeployer();
		this.accessor = engine.getAccessor();
		SchemaBuilder sb = new SchemaBuilder("test", "http://test.bndly.org");
		sb
		.type(NonVirtualType.class.getSimpleName())
			.attribute("name", StringAttribute.class)
		
		.type(OtherNonVirtualType.class.getSimpleName())
			.jsonTypeAttribute("nonVirtualType", NonVirtualType.class.getSimpleName())
				
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
		sbf.registerTypeBinding(NonVirtualType.class.getSimpleName(), NonVirtualType.class);
		sbf.registerTypeBinding(OtherNonVirtualType.class.getSimpleName(), OtherNonVirtualType.class);
		sbf.setEngine(engine);
		
		// create a FixtureDeployerImpl
		this.fixtureDeployerImpl = new FixtureDeployerImpl();
		fixtureDeployerImpl.setBase64Service(base64Service);
		fixtureDeployerImpl.setRecordJsonConverter(new RecordJsonConverterImpl());
		
		fixtureDeployerImpl.addSchemaBeanFactory(sbf);
	}
}
