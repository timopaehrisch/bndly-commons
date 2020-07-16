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

package org.bndly.common.service.shared;

/*-
 * #%L
 * Service Shared Impl
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

import org.bndly.common.graph.BeanGraphIterator;
import org.bndly.common.graph.BeanGraphIteratorListener;
import org.bndly.common.graph.CompiledBeanIterator;
import org.bndly.common.graph.CompiledBeanIterator.CompiledBeanIteratorProvider;
import org.bndly.common.graph.EntityCollectionDetector;
import org.bndly.common.graph.ReferenceDetector;
import org.bndly.common.mapper.MapperFactory;
import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.common.service.model.api.AbstractEntity;
import org.bndly.rest.client.api.CreateSupport;
import org.bndly.rest.client.api.DeleteSupport;
import org.bndly.rest.client.api.FindSupport;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.HATEOASClientFactory;
import org.bndly.rest.client.api.ListSupport;
import org.bndly.rest.client.api.PrimaryResourceSupport;
import org.bndly.rest.client.api.PrintSupport;
import org.bndly.rest.client.api.ReadSupport;
import org.bndly.rest.client.api.SearchSupport;
import org.bndly.rest.client.api.UpdateSupport;
import org.bndly.rest.client.exception.ConstraintViolationClientException;
import org.bndly.rest.client.exception.MissingLinkClientException;
import org.bndly.rest.client.exception.UnknownResourceClientException;
import org.bndly.rest.common.beans.AtomLink;
import org.bndly.rest.common.beans.ListRestBean;
import org.bndly.rest.common.beans.PaginationRestBean;
import org.bndly.rest.common.beans.RestBean;
import org.bndly.rest.search.beans.SearchParameters;
import org.bndly.common.service.model.api.CollectionIndexer;
import org.bndly.common.service.model.api.IndexerFunction;
import org.bndly.common.service.model.api.ReferenceAttribute;
import org.bndly.common.service.model.api.Linkable;
import org.bndly.common.service.model.api.ReferableResource;
import org.bndly.common.service.model.api.ReferenceBuildingException;
import org.bndly.common.service.shared.api.AssertExistenceSupport;
import org.bndly.common.service.shared.api.GenericResourceService;
import org.bndly.common.service.shared.api.GraphCycleUtil;
import org.bndly.common.service.shared.api.SearchSupportingService.Pagination;
import org.bndly.common.service.shared.api.SearchSupportingService.SearchResult;
import org.bndly.rest.client.exception.ClientException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class GenericResourceServiceImpl
	<
	ModelT,
	RestListModelT extends ListRestBean<ReferenceModelT>,
	ReferenceModelT extends RestBean,
	RestModelT extends ReferenceModelT
	>
	implements GenericResourceService<ModelT>, AssertExistenceSupport<ModelT> {

	private MapperFactory mapperFactory;
	
	protected CreateSupport<RestModelT> createSupport;
	protected ReadSupport<RestModelT> readSupport;
	protected UpdateSupport<RestModelT> updateSupport;
	protected DeleteSupport<RestModelT> deleteSupport;
	protected FindSupport<RestModelT, RestListModelT> findSupport;
	protected PrintSupport<RestModelT> printSupport;
	protected SearchSupport searchSupport;
	protected ListSupport<RestListModelT, RestModelT> listSupport;
	protected PrimaryResourceSupport<RestListModelT> primaryResourceSupport;

	private HATEOASClientFactory clientFactory;

	protected BeanGraphIteratorListener createResourceServiceGraphListener;
	protected BeanGraphIteratorListener updateResourceServiceGraphListener;
	protected BeanGraphIteratorListener readResourceServiceGraphListener;

	private ReferenceDetector referenceDetector = new ReferableResourceDetector();
	private EntityCollectionDetector entityCollectionDetector = new ReferableResourceCollectionDetector("org.bndly.ebx.model"); // this is here just for legacy support

	protected Class<ModelT> modelType;
	private Class<RestModelT> restBeanType;
	private Class<ReferenceModelT> restReferenceBeanType;
	private Class<RestListModelT> listRestBeanType;
	private CompiledBeanIteratorProvider compiledBeanIteratorProvider;
	private GraphCycleUtil graphCycleUtil;

	@Override
	public BeanGraphIteratorListener getUpdateResourceServiceGraphListener() {
		return this.updateResourceServiceGraphListener;
	}

	public GenericResourceServiceImpl() {
		List<Class<?>> typeParameters = ReflectionUtil.collectGenericTypeParametersFromType(getClass());
		if (typeParameters != null && typeParameters.size() > 0) {
			modelType = (Class<ModelT>) typeParameters.get(0);
			listRestBeanType = (Class<RestListModelT>) typeParameters.get(1);
			restReferenceBeanType = (Class<ReferenceModelT>) typeParameters.get(2);
			restBeanType = (Class<RestModelT>) typeParameters.get(3);
		}
	}

	public Class<RestListModelT> getListRestBeanType() {
		return listRestBeanType;
	}

	public Class<ReferenceModelT> getRestBeanReferenceType() {
		return restReferenceBeanType;
	}

	public Class<RestModelT> getRestBeanType() {
		return restBeanType;
	}

	@Override
	public Class<? extends ModelT> getModelClass() {
		return modelType;
	}

	@Override
	public final <M> ReferableResource<M> modelAsReferableResource(M someModel) {
		if (someModel == null) {
			return null;
		}
		if (ReferableResource.class.isInstance(someModel)) {
			return (ReferableResource<M>) someModel;
		} else {
			throw new IllegalArgumentException("provided model is not a referable resource");
		}
	}

	@Override
	public final Linkable modelAsLinkable(ModelT model) {
		if (model == null) {
			return null;
		}
		if (Linkable.class.isInstance(model)) {
			return (Linkable) model;
		} else {
			throw new IllegalArgumentException("provided model is not Linkable");
		}
	}

	/**
	 * traverses a complete object graph in order to apply cross cutting model manipulation. right now, the traverse method will activate two listeners, 
	 * that will automatically set an update date and a create date, if they are missing.
	 *
	 * @param m the entity root node to traverse
	 */
	@Override
	public void traverse(Object m, BeanGraphIteratorListener listener) {
		if (listener == null) {
			return;
		}
		if (compiledBeanIteratorProvider != null) {
			if (m != null) {
				CompiledBeanIterator iterator = compiledBeanIteratorProvider.getCompiledBeanIteratorForType(m.getClass());
				if (iterator != null) {
					Class ctxType = listener.getIterationContextType();
					iterator.traverse(m, listener, ctxType == null ? null : InstantiationUtil.instantiateType(ctxType));
				}
			}
		} else {
			new BeanGraphIterator(referenceDetector, entityCollectionDetector, listener).traverse(m);
		}
	}

	@Override
	public ModelT create(ModelT model) throws ClientException, ConstraintViolationClientException {
		return _create(model, false);
	}

	@Override
	public ModelT createAsReference(ModelT model) throws ClientException, ConstraintViolationClientException {
		ModelT created = _create(model, true);
		if (created == null) {
			return null;
		}
		// this should be a wrapped cast, because the second 'true' parameter says that we care only about the location of the created resource
		// hence the returned object is a domainModelReference with a self link
		ReferableResource<ModelT> rr = (ReferableResource<ModelT>) created;
		if (rr.isResourceReference()) {
			return created;
		} else {
			try {
				return rr.buildReference();
			} catch (ReferenceBuildingException ex) {
				return created;
			}
		}
	}

	private ModelT _create(ModelT model, boolean onlyLocation) throws ClientException, ConstraintViolationClientException {
		traverse(model, createResourceServiceGraphListener);
		RestModelT restBean = (RestModelT) toRestModel(model);
		RestModelT restBeanFromService;

		if (onlyLocation) {
			restBeanFromService = createSupport.createLocation(restBean);
			AtomLink lnk = restBeanFromService.follow("self");
			if (lnk == null) {
				return null;
			}
			modelAsLinkable(model).addLink("self", lnk.getHref(), lnk.getMethod());
			ModelT ref = (ModelT) toDomainReferenceModel(restBeanFromService);
			modelAsLinkable(ref).addLink("self", lnk.getHref(), lnk.getMethod());
			return ref;
		} else {
			restBeanFromService = createSupport.create(restBean);
		}

		ModelT modelFromService = (ModelT) toDomainModel(restBeanFromService);

		traverse(modelFromService, readResourceServiceGraphListener);
		return modelFromService;
	}

	@Override
	public ModelT read(ModelT model) throws ClientException, UnknownResourceClientException {
		String selfLink = null;
		if (Linkable.class.isInstance(model)) {
			selfLink = ((Linkable) model).follow("self");
		}
		RestModelT restBeanFromService;

		if (selfLink != null) {
			restBeanFromService = readSupport.readFromUrl(selfLink);
		} else {
			RestModelT restBean = (RestModelT) toRestModel(model);
			restBeanFromService = readSupport.read(restBean);
		}

		ModelT modelFromService = (ModelT) toDomainModel(restBeanFromService);
		traverse(modelFromService, readResourceServiceGraphListener);
		return modelFromService;
	}

	@Override
	public ModelT update(ModelT model) throws ClientException {
		return _update(model, false);
	}

	@Override
	public ModelT updateAsReference(ModelT model) throws ClientException {
		ModelT updated = _update(model, true);
		if (updated == null) {
			return null;
		}

		return updated;
	}

	private ModelT _update(ModelT model, boolean onlyLocation) throws ClientException {
		try {
 			assertLinkExistsInModel(model, "update");
		} catch (ReferenceBuildingException | UnknownResourceClientException | MissingLinkClientException e) {
			throw new ClientException("could not assert, that the link 'update' exists in model", e);
		}
		traverse(model, updateResourceServiceGraphListener);

		RestModelT restBean = (RestModelT) toRestModel(model);

		if (onlyLocation) {
			return (ModelT) toDomainReferenceModel(updateSupport.updateLocation(restBean));
		}

		RestModelT updatedRestBeanFromService = updateSupport.update(restBean);

		ModelT modelFromService = (ModelT) toDomainModel(updatedRestBeanFromService);

		traverse(modelFromService, readResourceServiceGraphListener);
		return modelFromService;
	}

	@Override
	public boolean delete(ModelT model) throws ClientException {
		try {
			assertLinkExistsInModel(model, "remove");
			if (modelAsLinkable(model).follow("remove") == null) {
				return false;
			}
			RestModelT restBean = (RestModelT) toRestModel(model);
			return deleteSupport.delete(restBean);
		} catch (ReferenceBuildingException | UnknownResourceClientException | MissingLinkClientException e) {
			return false;
		}
	}

	@Override
	public ModelT findAsReference(ModelT model) throws ClientException, UnknownResourceClientException {
		model = breakCycles(model);
		RestModelT restBean = (RestModelT) toRestModel(model);
		RestModelT restBeanFromService = findSupport.findLocation(restBean);

		if (restBeanFromService.follow("self") == null) {
			return null;
		}
		ModelT ref = (ModelT) toDomainReferenceModel(restBeanFromService);
		return ref;
	}

	@Override
	public ModelT find(ModelT model) throws ClientException, UnknownResourceClientException {
		return _find(model, false);
	}

	private ModelT _find(ModelT model, boolean onlyLocation) throws ClientException, UnknownResourceClientException {
		model = breakCycles(model);
		RestModelT restBean = (RestModelT) toRestModel(model);
		RestModelT restBeanFromService;

		if (onlyLocation) {
			restBeanFromService = findSupport.findLocation(restBean);
		} else {
			restBeanFromService = findSupport.find(restBean);
		}

		ModelT modelFromService = (ModelT) toDomainModel(restBeanFromService);
		traverse(modelFromService, readResourceServiceGraphListener);
		return modelFromService;
	}

	@Override
	public long getTotalCount() throws ClientException {
		return listSupport.getTotalCount();
	}

	@Override
	public Collection<ModelT> listAll() throws ClientException {
		return listAll(ArrayList.class);
	}

	@Override
	public <E extends Collection<ModelT>> E listAll(Class<E> collectionType) throws ClientException {
		Collection<RestModelT> listFromService = listSupport.listAllAsCollection();

		E domainList;
		try {
			domainList = collectionType.newInstance();
		} catch (Exception ex) {
			throw new IllegalStateException("could not instantiate collection type. " + collectionType.getSimpleName());
		}
		if (listFromService != null) {
			for (RestModelT restBean : listFromService) {
				ModelT modelFromService = (ModelT) toDomainModel(restBean);
				traverse(modelFromService, readResourceServiceGraphListener);
				domainList.add(modelFromService);
			}
		}
		return domainList;
	}

	@Override
	public <I> Map<I, ModelT> listIndexed(IndexerFunction<I, ModelT> indexer) throws ClientException {
		Collection<ModelT> col = listAll();
		return new CollectionIndexer<I, ModelT>().index(col, indexer);
	}

	@Override
	public Collection<ModelT> findAllLike(ModelT model) throws ClientException {
		return findAllLikeEagerly(model, ArrayList.class);
	}

	/**
	 * reads only the first page of the service for the entries that match the provided model prototype
	 */
	@Override
	public <E extends Collection<ModelT>> E findAllLike(ModelT model, Class<E> collectionType) throws ClientException {
		return _findAllLike(model, collectionType, false); // do not eagerly load. this means the service pagination will be ignored
	}

	/**
	 * reads all pages of the service for the entries that match the provided model prototype
	 */
	@Override
	public <E extends Collection<ModelT>> E findAllLikeEagerly(ModelT model, Class<E> collectionType) throws ClientException {
		return _findAllLike(model, collectionType, true); // do not eagerly load. this means the service pagination will be ignored
	}

	private <E extends Collection<ModelT>> E _findAllLike(ModelT model, Class<E> collectionType, boolean eager) throws ClientException {
		model = breakCycles(model);
		RestModelT restBean = (RestModelT) toRestModel(model);
		RestListModelT fromService = findSupport.findAllLike(restBean);
		List<ReferenceModelT> allLikeFromService = fromService.getItems();
		while (eager && fromService.follow("next") != null) {
			fromService = listSupport.readNextPage(fromService);
			allLikeFromService.addAll(fromService.getItems());
		}
		E domainList;
		try {
			domainList = collectionType.newInstance();
		} catch (Exception ex) {
			throw new IllegalArgumentException("could not instantiate " + collectionType.getSimpleName());
		}
		if (allLikeFromService != null) {
			for (ReferenceModelT restBeanFromService : allLikeFromService) {
				ModelT modelFromService = (ModelT) toDomainModel(restBeanFromService);
				traverse(modelFromService, readResourceServiceGraphListener);
				domainList.add(modelFromService);
			}
		}
		return domainList;
	}

	@Override
	public void print(ModelT m, OutputStream os) throws ClientException {
		if (printSupport == null) {
			throw new IllegalStateException("this service does not support printing. " + getClass().getSimpleName());
		}
		try {
			assertLinkExistsInModel(m, "print");
		} catch (ReferenceBuildingException | UnknownResourceClientException | MissingLinkClientException e) {
			throw new ClientException("could not assert that the link 'print' exists in model", e);
		}
		RestModelT restModel = (RestModelT) toRestModel(m);
		printSupport.print(restModel, os);
	}

	@Override
	public ModelT lookupByReference(ModelT m) throws ClientException, UnknownResourceClientException, ReferenceBuildingException {
		return internalLookupByReference(m);
	}

	public abstract ModelT instantiateModel();

	private ModelT internalLookupByReference(ModelT model) throws ClientException, UnknownResourceClientException, ReferenceBuildingException {
		ModelT ref = modelAsReferableResource(model).buildReference();
		ModelT proto;
		if (AbstractEntity.class.isInstance(ref)) {
			((AbstractEntity) ref).markAsFullModel();
			proto = ref;
		} else {
			proto = instantiateModel();
			List<Field> identifyingFields = ReflectionUtil.getFieldsWithAnnotation(ReferenceAttribute.class, model);
			for (Field field : identifyingFields) {
				Object value = ReflectionUtil.getFieldValue(field, ref);
				ReflectionUtil.setFieldValue(field, value, proto);
			}
		}
		return find((ModelT) proto);
	}

	@Override
	public SearchResult<ModelT> search(String q, long start, long hitsPerPage) throws ClientException {
		if (searchSupport != null) {
			Object parameters = searchSupport.getSearchParameters();
			if (parameters == null) {
				parameters = new SearchParameters();
			}
			if (SearchParameters.class.isInstance(parameters)) {
				SearchParameters p = (SearchParameters) parameters;
				p.setSearchTerm(q);
				PaginationRestBean pagination = new PaginationRestBean();
				pagination.setStart(start);
				pagination.setSize(hitsPerPage);
				p.setPage(pagination);
				RestListModelT result = (RestListModelT) searchSupport.search(p);
				SearchResult<ModelT> r = new SearchResult();
				Pagination pg = new Pagination();
				pg.setPageSize(hitsPerPage);
				PaginationRestBean resultPage = result.getPage();
				if (resultPage != null) {
					Long pageSize = resultPage.getSize();
					Long tr = resultPage.getTotalRecords();
					Long totalPages = tr / pageSize;
					if (tr % pageSize != 0) {
						totalPages++;
					}
					Long curPageIndex = resultPage.getStart() / pageSize;
					pg.setCurrentPageIndex(curPageIndex);
					pg.setTotalPages(totalPages);
				}
				r.setPagination(pg);
				r.setQuery(q);
				r.setItems(toDomainModelList(result));
				r.setTotalHits(result.getPage().getTotalRecords());
				traverse(r, readResourceServiceGraphListener);
				return r;
			} else {
				throw new IllegalStateException("unknown searchParameterObject: " + parameters.getClass().getSimpleName());
			}
		} else {
			throw new IllegalStateException("this service does not support searching");
		}
	}

	public void setResourceDAO(Object dao) {
		if (CreateSupport.class.isAssignableFrom(dao.getClass())) {
			createSupport = (CreateSupport<RestModelT>) dao;
		}
		if (ReadSupport.class.isAssignableFrom(dao.getClass())) {
			readSupport = (ReadSupport<RestModelT>) dao;
		}
		if (UpdateSupport.class.isAssignableFrom(dao.getClass())) {
			updateSupport = (UpdateSupport<RestModelT>) dao;
		}
		if (DeleteSupport.class.isAssignableFrom(dao.getClass())) {
			deleteSupport = (DeleteSupport<RestModelT>) dao;
		}
		if (FindSupport.class.isAssignableFrom(dao.getClass())) {
			findSupport = (FindSupport<RestModelT, RestListModelT>) dao;
		}
		if (PrintSupport.class.isAssignableFrom(dao.getClass())) {
			printSupport = (PrintSupport<RestModelT>) dao;
		}
		if (SearchSupport.class.isAssignableFrom(dao.getClass())) {
			searchSupport = (SearchSupport) dao;
		}
		if (ListSupport.class.isAssignableFrom(dao.getClass())) {
			listSupport = (ListSupport<RestListModelT, RestModelT>) dao;
		}
		if (PrimaryResourceSupport.class.isAssignableFrom(dao.getClass())) {
			primaryResourceSupport = (PrimaryResourceSupport<RestListModelT>) dao;
		}
	}

	@Override
	public HATEOASClient<RestListModelT> getPrimaryResourceClient() throws ClientException {
		return primaryResourceSupport.getPrimaryResourceClient();
	}

	private void assertLinkExistsInModel(ModelT model, String rel) throws ClientException, UnknownResourceClientException, ReferenceBuildingException, MissingLinkClientException {
		Linkable ml = modelAsLinkable(model);
		if (ml.follow(rel) == null) {
			ModelT lookedUp = lookupByReference(model);
			Linkable ll = modelAsLinkable(lookedUp);

			if (ll == null) {
				throw new IllegalArgumentException("linkable is unexpectedly null");
			}

			String link = ll.follow(rel);

			if (link == null) {
				throw new MissingLinkClientException("could not find link " + rel);
			}

			String method = ll.followForMethod(rel);
			ml.addLink(rel, link, method);

			// also set the identity link of the resource. the client might use it to reload a resource
			if (!"self".equals(rel)) {
				link = ll.follow("self");
				if (link != null) {
					method = ll.followForMethod("self");
					ml.addLink("self", link, method);
				}
			}
		}
	}

	@Override
	public ModelT assertExists(ModelT model) throws ClientException, ReferenceBuildingException {
		try {
			return lookupByReference(model);
		} catch (UnknownResourceClientException e) {
			return create(model);
		}
	}

	@Override
	public ModelT assertExistsAsReference(ModelT m) throws ClientException {
		try {
			return findAsReference(m);
		} catch (UnknownResourceClientException e) {
			return createAsReference(m);
		}
	}

	@Override
	public <E> HATEOASClient<E> createClient(E bean) {
		return clientFactory.build(bean);
	}

	public void setClientFactory(HATEOASClientFactory clientFactory) {
		this.clientFactory = clientFactory;
	}

	public void setMapperFactory(MapperFactory mapperFactory) {
		this.mapperFactory = mapperFactory;
	}

	@Override
	public ModelT toDomainModel(RestBean bean) {
		return rebuildCycles(mapperFactory.buildContext().map(bean, getModelClass()));
	}

	@Override
	public ModelT toDomainReferenceModel(RestBean bean) {
		ModelT model = mapperFactory.buildContext().map(bean, getModelClass());
		if (ReferableResource.class.isInstance(model)) {
			try {
				return ((ReferableResource<ModelT>) model).buildReference();
			} catch (ReferenceBuildingException ex) {
				return null;
			}
		}
		return null;
	}

	@Override
	public RestBean toRestModel(Object model) {
 		return toRestModel(model, true);
	}
	
	private RestBean toRestModel(Object model, boolean breakCycles) {
		if (breakCycles) {
			model = breakCycles(model);
		}
 		return mapperFactory.buildContext().map(model, getRestBeanType());
	}

	@Override
	public RestBean toRestReferenceModel(Object model) {
		return (RestBean) mapperFactory.buildContext().map(model, getRestBeanType().getSuperclass());
	}

	@Override
	public List<ModelT> toDomainModelList(ListRestBean bean) {
		return (List<ModelT>) mapperFactory.buildContext().map(bean, List.class);
	}

	private <E> E breakCycles(E model) {
		if (graphCycleUtil != null) {
			return graphCycleUtil.breakCycles(model);
		}
		return model;
	}
	
	private <E> E rebuildCycles(E model) {
		if (graphCycleUtil != null) {
			return graphCycleUtil.rebuildCycles(model);
		}
		return model;
	}

	@Override
	public MapperFactory getMapperFactory() {
		return mapperFactory;
	}

	@Override
	public GraphCycleUtil getGraphCycleUtil() {
		return graphCycleUtil;
	}

	public void setEntityCollectionDetector(EntityCollectionDetector entityCollectionDetector) {
		this.entityCollectionDetector = entityCollectionDetector;
	}

	public void setReferenceDetector(ReferenceDetector referenceDetector) {
		this.referenceDetector = referenceDetector;
	}

	public void setReadResourceServiceGraphListener(BeanGraphIteratorListener readResourceServiceGraphListener) {
		this.readResourceServiceGraphListener = readResourceServiceGraphListener;
	}

	public void setCreateResourceServiceGraphListener(BeanGraphIteratorListener createResourceServiceGraphListener) {
		this.createResourceServiceGraphListener = createResourceServiceGraphListener;
	}

	public void setUpdateResourceServiceGraphListener(BeanGraphIteratorListener updateResourceServiceGraphListener) {
		this.updateResourceServiceGraphListener = updateResourceServiceGraphListener;
	}

	public void setCompiledBeanIteratorProvider(CompiledBeanIteratorProvider compiledBeanIteratorProvider) {
		this.compiledBeanIteratorProvider = compiledBeanIteratorProvider;
	}

	public void setGraphCycleUtil(GraphCycleUtil graphCycleUtil) {
		this.graphCycleUtil = graphCycleUtil;
	}
	
}
