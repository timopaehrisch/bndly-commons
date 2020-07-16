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
package org.bndly.common.service.shared.api;

/*-
 * #%L
 * Service Shared API
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

import org.bndly.common.service.model.api.ReferableResource;

public interface ServicePool {

	public static final String NAME = "cyServicePool";

	/**
	 * adds a service to registry. when the service is added
	 *
	 * @param registrableServiceName the service name
	 * @param service the service to add to the registry
	 */
	public void register(String registrableServiceName, RegistrableService service);

	public void register(RegistrableService service);

	/**
	 * adds a service to registry. for domain model or domain model reference
	 *
	 * @param model domain model interface
	 * @param service (service for communication with shop backend)
	 */
	<D extends ReferableResource> void register(Class<D> model, GenericResourceService service);

	/**
	 * returns a service
	 *
	 * @param registratedServiceClassName the service name to get service
	 * @return object that implements IShopRegistrableService
	 */
	<T extends RegistrableService> T getService(String registratedServiceClassName);

	<T extends RegistrableService> T getServiceByType(Class<T> serviceType);

	/**
	 * returns a resource service for domain model or domain model reference
	 *
	 * @param model domain model interface
	 * @return resourceService
	 * @see GenericResourceService
	 */
	<S extends GenericResourceService, D> S getService(Class<D> model);

	/**
	 * returns a resource service for domain model or domain model reference if exists and if found class are assignable from 'isAssignableFrom' (interface or class )
	 *
	 * @param model domain model interface
	 * @param isAssignableFrom domain model interface
	 * @return resourceService
	 * @see GenericResourceService
	 */
	<S extends GenericResourceService, D extends ReferableResource> S getService(Class<D> model, Class isAssignableFrom);

}
