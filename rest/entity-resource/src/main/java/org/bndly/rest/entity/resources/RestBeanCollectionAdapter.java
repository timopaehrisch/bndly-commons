package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.common.mapper.CollectionTypeAdapter;
import org.bndly.common.mapper.MappingState;
import org.bndly.common.reflection.InstantiationUtil;
import org.bndly.rest.common.beans.ListRestBean;
import java.util.List;

public class RestBeanCollectionAdapter implements CollectionTypeAdapter {

    @Override
    public Object newCollectionInstance(Class<?> type) {
        return InstantiationUtil.instantiateType(type);
    }

    @Override
    public void addObjectToCollection(Object entry, Object collection, MappingState state) {
        if (collection != null) {
            if (ListRestBean.class.isInstance(collection)) {
                ListRestBean restCollection = ListRestBean.class.cast(collection);
                restCollection.add(entry);
            }
        }
    }

    @Override
    public void iterate(Object collection, CollectionTypeAdapter.IterationHandler handler, MappingState state) {
        if (collection != null) {
            if (ListRestBean.class.isInstance(collection)) {
                ListRestBean restCollection = ListRestBean.class.cast(collection);
                List items = restCollection.getItems();
                if (items != null) {
                    for (Object entry : items) {
                        handler.handle(entry,state);
                    }
                }
            }
        }
    }

    @Override
    public Class<?> getSupportedCollectionType() {
        return ListRestBean.class;
    }
}
