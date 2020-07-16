package org.bndly.rest.repository.resources;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.schema.api.repository.Path;
import org.bndly.schema.api.repository.Property;

/**
 * A PropertyDescription is a meta model, that is derived from a form parameter, that is sent to the 
 * {@link RepositoryResource#handleUserData(java.lang.String, org.bndly.rest.api.Context)} method.
 * 
 * Instances of this interface will be created by {@link ParameterPayloadToNodeConverter}.
 * The format is: <code>/path/to/node.propertyName@TYPE[index]#action</code>
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface PropertyDescription {

	/**
	 * Gets the path of the node, to which the parameter is being applied
	 * @return the target node path
	 */
	Path getNodePath();

	/**
	 * Get the name of the property, which is addressed by the parameter.
	 * @return the target property name
	 */
	String getName();

	/**
	 * Get the property type of the property, which is addressed by the parameter.
	 * @return the target property type
	 */
	Property.Type getType();

	/**
	 * Get the flag of the addressed property, that indicates whether the property is multivalued or not.
	 * @return true, if the property is multi valued
	 */
	boolean isMulti();

	/**
	 * If the addressed property is multi valued, this method returns the index of the value in the property
	 * @return null for single valued properties and the index - if it is known - of the property value
	 */
	Integer getIndex();
	
	/**
	 * Get the name of the action that is performed on the property, that is addressed by the parameter
	 * @return null or an action name
	 */
	String getAction();
	
}
