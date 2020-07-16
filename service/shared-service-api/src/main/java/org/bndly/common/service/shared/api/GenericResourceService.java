/*
 * Copyright (c) 2013, cyber:con GmbH, Bonn.
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

import org.bndly.common.graph.BeanGraphIteratorListener;
import org.bndly.common.mapper.MapperFactory;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.exception.ConstraintViolationClientException;
import org.bndly.rest.common.beans.ListRestBean;
import org.bndly.rest.common.beans.RestBean;
import org.bndly.common.service.model.api.IndexerFunction;
import org.bndly.common.service.model.api.Linkable;
import org.bndly.common.service.model.api.ReferableResource;
import org.bndly.common.service.model.api.ReferenceAttribute;
import org.bndly.common.service.model.api.ReferenceBuildingException;
import org.bndly.rest.client.exception.ClientException;
import org.bndly.rest.client.exception.UnknownResourceClientException;

import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The GenericResourceService is a generic interface to describe the CRUD interaction with a resource.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 * @param <M> The Java type of the resources.
 */
public interface GenericResourceService<M> extends RegistrableService, AssertExistenceSupport<M> {

	/**
	 * Create an instance of the provided model as a resource. Never returns null.
	 * @param model The model to create
	 * @return A fresh instance of the created resource.
	 * @throws ClientException 
	 * @throws org.bndly.rest.client.exception.ConstraintViolationClientException 
	 */
	public M create(M model) throws ClientException, ConstraintViolationClientException;

	/**
	 * Create an instance of the provided model as a resource, but the created resource will not be loaded. 
	 * Instead the location of the resource will be set as a 'self' link in the <code>model</code> parameter.
	 * @param model The model to create
	 * @return The provided model instance with a 'self' link
	 * @throws ClientException 
	 * @throws org.bndly.rest.client.exception.ConstraintViolationClientException 
	 */
	public M createAsReference(M model) throws ClientException, ConstraintViolationClientException;

	public M read(M model) throws ClientException, UnknownResourceClientException;

	public M update(M model) throws ClientException;

	public M updateAsReference(M model) throws ClientException;

	public boolean delete(M model) throws ClientException;

	public M findAsReference(M model) throws ClientException, UnknownResourceClientException;

	public M find(M model) throws ClientException, UnknownResourceClientException;

	/**
	 * Cast the provided model to a ReferableResource interface. If the provided model is null, null will be returned. 
	 * If the provided model is not implementing {@link ReferableResource}, an {@link IllegalArgumentException} will be thrown.
	 * @param <M> A Java interface. Typically this will be a generated schema bean interface.
	 * @param someModel A model instance to cast to a {@link ReferableResource}.
	 * @return null, if the provided model was null - otherwise a {@link ReferableResource}
	 */
	public <M> ReferableResource<M> modelAsReferableResource(M someModel);

	/**
	 * Cast the provided model to a Linkable interface. If the provided model is null, null will be returned. 
	 * If the provided model is not implementing {@link Linkable}, an {@link IllegalArgumentException} will be thrown.
	 * @param model A model instance to cast to a {@link Linkable}.
	 * @return null, if the provided model was null - otherwise a {@link Linkable}
	 */
	public Linkable modelAsLinkable(M model);

	/**
	 * Looks up a full instance of the provided model by either 
	 * <ol>
	 * <li>marking the provided model as a full model or </li>
	 * <li>by creating a new model instance and copying all {@link ReferenceAttribute} marked fields to this instance</li>
	 * </ol>
	 * and finally passing the model to the {@link #find(java.lang.Object) } method. 
	 * If the full model can not be found, an {@link UnknownResourceClientException} may be thrown.
	 * @param model a model instance that holds information, that identifies the instance
	 * @return the found full model instance
	 * @throws ClientException if there is a general communication problem
	 * @throws ReferenceBuildingException if no reference can be created from the provided model instance
	 * @throws UnknownResourceClientException if the full model can not be looked up
	 */
	public M lookupByReference(M model) throws ClientException, UnknownResourceClientException, ReferenceBuildingException;

	public Collection<M> listAll() throws ClientException;

	public long getTotalCount() throws ClientException;

	public <E extends Collection<M>> E listAll(Class<E> collectionType) throws ClientException;

	public <I> Map<I, M> listIndexed(IndexerFunction<I, M> indexer) throws ClientException;

	public Collection<M> findAllLike(M model) throws ClientException;

	public <E extends Collection<M>> E findAllLike(M model, Class<E> collectionType) throws ClientException;

	/**
	 * Get the Java interface class object of the model, that is managed by this service instance.
	 * @return A Java interface class object
	 */
	public Class<? extends M> getModelClass();

	/**
	 * Gets the {@link HATEOASClient} instance of the primary resource for the model, that is managed by this service instance.
	 * @return a {@link HATEOASClient} instance to the primary resource of the service instance model
	 * @throws ClientException if there is a general communication problem
	 */
	public HATEOASClient<? extends ListRestBean<? extends RestBean>> getPrimaryResourceClient() throws ClientException;

	public void print(M m, OutputStream os) throws ClientException;

	public BeanGraphIteratorListener getUpdateResourceServiceGraphListener();

	public <E extends Collection<M>> E findAllLikeEagerly(M model, Class<E> collectionType) throws ClientException;

	public void traverse(Object m, BeanGraphIteratorListener listener);

	/**
	 * Creates a {@link HATEOASClient} around the provided bean. The bean is typically extending {@link RestBean}, but this is not a hard requirement.
	 * @param <E> any Java type
	 * @param bean the bean, that holds the links, which will be followed by the client
	 * @return a new client instance, that wraps the provided <code>bean</code>
	 */
	public <E> HATEOASClient<E> createClient(E bean);

	public SearchSupportingService.SearchResult<M> search(String q, long start, long hitsPerPage) throws ClientException;

	public List toDomainModelList(ListRestBean bean);

	public Object toDomainModel(RestBean bean);

	public Object toDomainReferenceModel(RestBean bean);

	public RestBean toRestModel(Object model);

	public RestBean toRestReferenceModel(Object model);

	/**
	 * Get the mapper factory, that is used by the service instance to transform models to RestBeans.
	 * @return a mapper factory instance
	 */
	public MapperFactory getMapperFactory();
	
	/**
	 * Gets a utility, that helps to break or rebuild object graphs.
	 * @return 
	 */
	public GraphCycleUtil getGraphCycleUtil();
}
