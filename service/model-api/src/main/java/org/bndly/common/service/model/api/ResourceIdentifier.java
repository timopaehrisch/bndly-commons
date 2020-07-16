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
package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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


/**
 * A Resource Identifier allows to refer to a Resource by its Type and its unique ID.
 */
public abstract class ResourceIdentifier {
	private Object id;
	private Class<?> resourceType;
	
	/**
	 * establishes an informal contract on how to get instances of resourceIdentifiers when the Key (ID) is only present
	 * as a String. MUST be implemented by Sub Classes or else an UnsupportedOperationException will be thrown upon a call
	 * to it (you must have a Sub Type Reference though).
	 * 
	 * @param path the path of the resource
	 * @param resourceType the type of the resource
	 * @return the resource identifier
	 * 
	 * @throws UnsupportedOperationException
	 */
	public static ResourceIdentifier createResourceIdentifier(String path, Class<?> resourceType) {
		throw new UnsupportedOperationException("This static method is to be supplied by Sub Classes");
	}

	/**
	 * Default No Argument Constructor 
	 */
	protected ResourceIdentifier() {
		super();
	}
	
	/**
	 * Constructor taking in a Key and a Resource Type Class.
	 * 
	 * @param id
	 * @param resourceType
	 */
	public ResourceIdentifier(Object id, Class<?> resourceType) {
		this();
		this.id = id;
		this.resourceType = resourceType;
	}
	
	
	////////////////////////////////
	// Utilities and Auxiliaries
	/////////////////////////////////

	/**
	 * Recreates the resource from a string
	 * @param key resource identifier as a string
	 * @return recreated resource
	 */
	public abstract Object fromString(String key);
	
	/**
	 * The toString() should return null if the Key (ID) is null.
	 * 
	 * @return null if the key is null. otherwise the key.
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();
	
	//////////////////////
	// Getter and Setter 
	//////////////////////

	/**
	 * Returns the Class the ResourceIdentifier refers to.
	 * 
	 * @return The java type of the resource
	 */
	public Class<?> getResourceType() {
		return resourceType;
	}
	
	protected void setResourceType(Class<?> resourceType) {
		this.resourceType = resourceType;
	}
	
	/**
	 * Returns the Key to identify and refer to a Resource in question.
	 *  
	 * @return id
	 * 			  K
	 */
	public Object getId() {
		return id;
	}
	
	protected void setId(Object id) {
		this.id = id;
	}

	////////////////////////////////
	// hashCode + equals + toString
	/////////////////////////////////	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof ResourceIdentifier)) {
			return false;
		}
		ResourceIdentifier other = (ResourceIdentifier) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (resourceType == null) {
			if (other.resourceType != null) {
				return false;
			}
		} else if (!resourceType.equals(other.resourceType)) {
			return false;
		}
		return true;
	}
	
}

