package org.bndly.rest.resources;

/*-
 * #%L
 * REST Controller Impl
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

import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.controller.impl.ControllerResourceRegistryImpl;
import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.controller.api.GET;
import static org.bndly.rest.api.HTTPMethod.GET;
import static org.bndly.rest.api.HTTPMethod.POST;
import org.bndly.rest.api.PathCoder;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilderImpl;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.controller.impl.DoubleBoundControllerException;
import java.util.List;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ControllerResourceRegistryImplTest {
	private static final DefaultCharacterEncodingProvider DEFAULT_CHAR_ENCODING_PROVIDER = new DefaultCharacterEncodingProvider() {

		@Override
		public String getCharacterEncoding() {
			return "UTF-8";
		}

		@Override
		public PathCoder createPathCoder() {
			return new PathCoder.UTF8();
		}
	};

	@Path("test")
	public static class TestController {

		@Path("{id}/do")
		@GET
		public Response doSomething() {
			return null;
		}

		@Path("{id}/do")
		@POST
		public Response doSomethingWithOtherHttpMethod() {
			return null;
		}

		@Path("{id}.pdf")
		@GET
		public Response doSomethingWithExtension() {
			return null;
		}

		@Path("{id}")
		@GET
		public Response doSomethingAgain() {
			return null;
		}
	}

	public static class AnotherTestController {

		@POST
		@Path("static")
		public Response doPOST() {
			return null;
		}

		@POST
		@Path("otherstatic")
		public Response doPOSTDifferently() {
			return null;
		}

		@GET
		public Response doGetWithoutPath() {
			return null;
		}
		
		@GET
		@Path("{id}")
		public Response doGet() {
			return null;
		}
	}

	@Path("")
	public static class SingleMethodController {
		@GET
		public Response doStuff() {
			return null;
		}
	}
	
	@Path("Similar")
	public static class SimilarA {
		@GET
		@Path("{paramA}")
		public Response doStuff(@PathParam("paramA") Long id) {
			return null;
		}
	}
	@Path("Similar")
	public static class SimilarB {
		@GET
		@Path("{paramB}.properties")
		public Response doStuff(@PathParam("paramB") String string) {
			return null;
		}
	}
	@Path("Similar")
	public static class SimilarC {
		@GET
		@Path("static/{paramC}.properties")
		public Response doStuff(@PathParam("paramC") String string) {
			return null;
		}
	}
	
	@Test
	public void testRegister() {
		ControllerResourceRegistryImpl registry = new ControllerResourceRegistryImpl();
		registry.setDefaultCharacterEncodingProvider(DEFAULT_CHAR_ENCODING_PROVIDER);
		TestController controller = new TestController();
		registry.deploy(controller, "appcontext");
		List<ControllerBinding> bindings = registry.getBindings();
		assertNotNull(bindings);
		assertEquals(bindings.size(), 4);
	}
	
	@Test
	public void testRegisterSingleMethodController() {
		ControllerResourceRegistryImpl registry = new ControllerResourceRegistryImpl();
		registry.setDefaultCharacterEncodingProvider(DEFAULT_CHAR_ENCODING_PROVIDER);
		SingleMethodController controller = new SingleMethodController();
		registry.deploy(controller, "appcontext");
		List<ControllerBinding> bindings = registry.getBindings();
		assertNotNull(bindings);
		assertEquals(bindings.size(), 1);
		ResourceURI uri = new ResourceURIBuilderImpl(new PathCoder.UTF8()).pathElement("appcontext").build();
		ControllerBinding binding = registry.resolveBindingForResourceURI(uri, GET);
		assertNotNull(binding);
	}
	
	@Test
	public void testRegisterSingleMethodControllerWithoutSegment() {
		ControllerResourceRegistryImpl registry = new ControllerResourceRegistryImpl();
		registry.setDefaultCharacterEncodingProvider(DEFAULT_CHAR_ENCODING_PROVIDER);
		SingleMethodController controller = new SingleMethodController();
		registry.deploy(controller);
		List<ControllerBinding> bindings = registry.getBindings();
		assertNotNull(bindings);
		assertEquals(bindings.size(), 1);
		ResourceURI uri = new ResourceURIBuilderImpl(new PathCoder.UTF8()).build();
		ControllerBinding binding = registry.resolveBindingForResourceURI(uri, GET);
		assertNotNull(binding);
	}

	@Test
	public void testRegisterConflict() {
		ControllerResourceRegistryImpl registry = new ControllerResourceRegistryImpl();
		registry.setDefaultCharacterEncodingProvider(DEFAULT_CHAR_ENCODING_PROVIDER);
		TestController controller = new TestController();
		TestController controller2 = new TestController();
		registry.deploy(controller, "appcontext");
		try {
			registry.deploy(controller2, "appcontext");
			fail("expected second controller to not be deployed.");
		} catch (DoubleBoundControllerException e) {
		}
	}

	@Test
	public void testBindingResolution() throws NoSuchMethodException {
		ControllerResourceRegistryImpl registry = new ControllerResourceRegistryImpl();
		registry.setDefaultCharacterEncodingProvider(DEFAULT_CHAR_ENCODING_PROVIDER);
		TestController controller = new TestController();
		registry.deploy(controller, "appcontext");
		ResourceURI uri = new ResourceURIBuilderImpl(new PathCoder.UTF8()).pathElement("appcontext").pathElement("test").pathElement("avariablevalue").pathElement("do").build();
		ControllerBinding binding = registry.resolveBindingForResourceURI(uri, GET);
		assertNotNull(binding, "could not find binding");
		assertEquals(binding.getHTTPMethod(), GET);
		assertEquals(binding.getController(), controller);
		assertEquals(binding.getMethod(), TestController.class.getMethod("doSomething"));

		binding = registry.resolveBindingForResourceURI(uri, POST);
		assertNotNull(binding, "could not find binding");
		assertEquals(binding.getHTTPMethod(), POST);
		assertEquals(binding.getController(), controller);
		assertEquals(binding.getMethod(), TestController.class.getMethod("doSomethingWithOtherHttpMethod"));
	}

	@Test
	public void testBindingResolutionWithVariableElementAsRoot() throws NoSuchMethodException {
		ControllerResourceRegistryImpl registry = new ControllerResourceRegistryImpl();
		registry.setDefaultCharacterEncodingProvider(DEFAULT_CHAR_ENCODING_PROVIDER);
		AnotherTestController controller = new AnotherTestController();
		registry.deploy(controller, "something");

		ResourceURI uri = new ResourceURIBuilderImpl(new PathCoder.UTF8()).pathElement("something").pathElement("1").build();
		ControllerBinding binding = registry.resolveBindingForResourceURI(uri, GET);
		assertNotNull(binding, "could not find binding");
		
		registry.undeploy(controller);
		binding = registry.resolveBindingForResourceURI(uri, GET);
		assertNull(binding, "could find binding for not existing controller");
		registry.deploy(controller, "something");
	}

	
	@Test
	public void testSimilarControllerWithExtensionDifference() throws NoSuchMethodException {
		ControllerResourceRegistryImpl registry = new ControllerResourceRegistryImpl();
		registry.setDefaultCharacterEncodingProvider(DEFAULT_CHAR_ENCODING_PROVIDER);
		SimilarA a = new SimilarA();
		SimilarB b = new SimilarB();
		SimilarC c = new SimilarC();
		registry.deploy(a);
		registry.deploy(b);
		registry.deploy(c);
		
		testSimilarBindings(registry);
		
		registry = new ControllerResourceRegistryImpl();
		registry.setDefaultCharacterEncodingProvider(DEFAULT_CHAR_ENCODING_PROVIDER);
		registry.deploy(c);
		registry.deploy(b);
		registry.deploy(a);

		testSimilarBindings(registry);
	}

	private void testSimilarBindings(ControllerResourceRegistryImpl registry) throws NoSuchMethodException {
		ResourceURI uriForA = new ResourceURIBuilderImpl(new PathCoder.UTF8()).pathElement("Similar").pathElement("1").build();
		ResourceURI uriForAWithOtherExtension = new ResourceURIBuilderImpl(new PathCoder.UTF8()).pathElement("Similar").pathElement("1").extension("xml").build();
		ResourceURI uriForAWithBExtension = new ResourceURIBuilderImpl(new PathCoder.UTF8()).pathElement("Similar").pathElement("1").extension("properties").build();
		ResourceURI uriForC = new ResourceURIBuilderImpl(new PathCoder.UTF8()).pathElement("Similar").pathElement("static").pathElement("1").extension("properties").build();
		
		ControllerBinding binding;
		binding = registry.resolveBindingForResourceURI(uriForA, GET);
		assertNotNull(binding);
		assertEquals(binding.getMethod(), SimilarA.class.getMethod("doStuff", Long.class));
		binding = registry.resolveBindingForResourceURI(uriForAWithOtherExtension, GET);
		assertNotNull(binding);
		assertEquals(binding.getMethod(), SimilarA.class.getMethod("doStuff", Long.class));
		binding = registry.resolveBindingForResourceURI(uriForAWithBExtension, GET);
		assertNotNull(binding);
		assertEquals(binding.getMethod(), SimilarB.class.getMethod("doStuff", String.class));
		binding = registry.resolveBindingForResourceURI(uriForC, GET);
		assertNotNull(binding);
		assertEquals(binding.getMethod(), SimilarC.class.getMethod("doStuff", String.class));
	}
}
