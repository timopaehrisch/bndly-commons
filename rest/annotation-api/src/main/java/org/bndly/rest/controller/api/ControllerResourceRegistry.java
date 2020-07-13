package org.bndly.rest.controller.api;

/*-
 * #%L
 * REST Annotation API
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
import java.util.Iterator;

public interface ControllerResourceRegistry {

	public void addControllerResourceRegistryListener(ControllerResourceRegistryListener listener);
	public void removeControllerResourceRegistryListener(ControllerResourceRegistryListener listener);
	public void deploy(Object controller);
	public void deploy(Object controller, String baseURI);
	public void undeploy(Object controller);
	public Iterator<ControllerBinding> listDeployedControllerBindings();
	ControllerBinding resolveBindingForResourceURI(ResourceURI uri, HTTPMethod httpMethod);
	public boolean isVariableElement(String element);
	public boolean isVariableElementOfName(String element, String name);
}
