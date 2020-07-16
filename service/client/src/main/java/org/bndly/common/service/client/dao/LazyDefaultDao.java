package org.bndly.common.service.client.dao;

/*-
 * #%L
 * Service Client
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

import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.ValidationResult;
import org.bndly.rest.client.exception.ClientException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

public abstract class LazyDefaultDao implements DefaultDao {

    protected abstract DefaultDao waitForRealDao();

    @Override
    public Object getSearchParameters() throws ClientException {
        return waitForRealDao().getSearchParameters();
    }

    @Override
    public Object search(Object searchDescription) throws ClientException {
        return waitForRealDao().search(searchDescription);
    }
    
    @Override
    public Class getListType() {
        return waitForRealDao().getListType();
    }

    @Override
    public Object listAll() throws ClientException {
        return waitForRealDao().listAll();
    }

    @Override
    public Collection listAllAsCollection() throws ClientException {
        return waitForRealDao().listAllAsCollection();
    }

    @Override
    public Object findAllLike(Object prototype) throws ClientException {
        return waitForRealDao().findAllLike(prototype);
    }

    @Override
    public Object readNextPage(Object currentPage) throws ClientException {
        return waitForRealDao().readNextPage(currentPage);
    }

    @Override
    public Object readPreviousPage(Object currentPage) throws ClientException {
        return waitForRealDao().readPreviousPage(currentPage);
    }

    @Override
    public long getTotalCount() throws ClientException {
        return waitForRealDao().getTotalCount();
    }

    @Override
    public Object findLocation(Object entity) throws ClientException {
        return waitForRealDao().findLocation(entity);
    }

    @Override
    public Object find(Object entity) throws ClientException {
        return waitForRealDao().find(entity);
    }

    @Override
    public Object findWithLocale(Object entity, String locale) throws ClientException {
        return waitForRealDao().findWithLocale(entity, locale);
    }

    @Override
    public Collection findAllLikeAsCollection(Object entity) throws ClientException {
        return waitForRealDao().findAllLikeAsCollection(entity);
    }

    @Override
    public void print(Object entity, OutputStream os) throws ClientException {
        waitForRealDao().print(entity, os);
    }

    @Override
    public Object create(Object entity) throws ClientException {
        return waitForRealDao().create(entity);
    }

    @Override
    public Object createLocation(Object entity) throws ClientException {
        return waitForRealDao().createLocation(entity);
    }

    @Override
    public Object readFromUrl(String url) throws ClientException {
        return waitForRealDao().readFromUrl(url);
    }

    @Override
    public Object read(Object entity) throws ClientException {
        return waitForRealDao().read(entity);
    }

    @Override
    public Object update(Object entity) throws ClientException {
        return waitForRealDao().update(entity);
    }

    @Override
    public Object updateLocation(Object entity) throws ClientException {
        return waitForRealDao().updateLocation(entity);
    }

    @Override
    public boolean delete(Object entity) throws ClientException {
        return waitForRealDao().delete(entity);
    }

    @Override
    public ValidationResult validate(Object bean) throws ClientException {
        return waitForRealDao().validate(bean);
    }

    @Override
    public List listMandatoryFields() throws ClientException {
        return waitForRealDao().listMandatoryFields();
    }

    @Override
    public Class getBeanType() {
        return waitForRealDao().getBeanType();
    }

    @Override
    public HATEOASClient getPrimaryResourceClient() throws ClientException {
        return waitForRealDao().getPrimaryResourceClient();
    }
    
}
