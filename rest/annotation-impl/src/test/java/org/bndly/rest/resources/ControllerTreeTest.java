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

import org.bndly.rest.api.HTTPMethod;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.impl.ExtensionNode;
import org.bndly.rest.controller.impl.MethodNode;
import org.bndly.rest.controller.impl.PathNode;
import org.bndly.rest.controller.impl.SelectorNode;
import java.lang.reflect.Method;
import java.util.List;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ControllerTreeTest {
	private static final ControllerBinding DUMMYBINDING = new ControllerBinding() {

		@Override
		public HTTPMethod getHTTPMethod() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Method getMethod() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public ResourceURI getResourceURIPattern() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Object getController() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Class<?> getControllerType() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public Documentation getDocumentation() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public String getBaseURI() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}

		@Override
		public List<AtomLinkDescription> getAtomLinkDescriptions() {
			throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
		}
	};
	
	@Test
	public void testRemovalRootBinding() {
		PathNode root = new PathNode(null);
		SelectorNode selectorNode = root.getSelectorNode();
		ExtensionNode extensionNode = selectorNode.getExtensionNode();
		MethodNode methodNode = new MethodNode(extensionNode.getMethodNode(), HTTPMethod.GET, DUMMYBINDING);
		extensionNode.getMethodNode().addChild(methodNode);
		methodNode.removeFromTree();
	}
}
