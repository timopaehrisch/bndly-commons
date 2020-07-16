package org.bndly.common.mapper;

/*-
 * #%L
 * Mapper
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

import org.bndly.common.reflection.InstantiationUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DomainCollectionAdapter implements CollectionTypeAdapter {

    @Override
    public void addObjectToCollection(Object entry, Object collection, MappingState state) {
        if (collection != null) {
            if (Collection.class.isInstance(collection)) {
                Collection.class.cast(collection).add(entry);
            }

        }
    }

    @Override
    public void iterate(Object collection, CollectionTypeAdapter.IterationHandler handler, MappingState state) {
        if (collection != null) {
            if (Collection.class.isInstance(collection)) {
                for (Object entry : Collection.class.cast(collection)) {
                    handler.handle(entry, state);
                }
            }
        }
    }

    @Override
    public Class<?> getSupportedCollectionType() {
        return Collection.class;
    }

    @Override
    public Object newCollectionInstance(Class<?> type) {
        if (List.class.isAssignableFrom(type) && type.isAssignableFrom(ArrayList.class)) {
            return new ArrayList();
        } else if (Set.class.isAssignableFrom(type) && type.isAssignableFrom(HashSet.class)) {
            return new HashSet();
        }
        Object collection = InstantiationUtil.instantiateType(type);
        if (collection != null) {
            return collection;
        }
        throw new IllegalArgumentException("should instantiate unsupported collection type: " + type.getName());
    }
}
