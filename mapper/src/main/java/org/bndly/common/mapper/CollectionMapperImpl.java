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

public abstract class CollectionMapperImpl implements CollectionMapper {
    private final Class<?> collectionType;
    private final Class<?> collectionElementType;

    public CollectionMapperImpl(Class<?> collectionType, Class<?> collectionElementType) {
        this.collectionType = collectionType;
        this.collectionElementType = collectionElementType;
    }

    protected abstract CollectionTypeAdapter assertCollectionTypeAdapterExists(Object object);
    protected abstract CollectionTypeAdapter assertCollectionTypeAdapterExists(Class<?> type);
    
    @Override
    public Class<?> getCollectionType() {
        return collectionType;
    }

    @Override
    public Class<?> getElementType() {
        return collectionElementType;
    }

    @Override
    public void map(Object source, final Object target, MappingContext context, MappingState state) {
        if (source != null && target != null) {
            // map the source collection to the target collection
            CollectionTypeAdapter sourceAdapter = assertCollectionTypeAdapterExists(source);
            final CollectionTypeAdapter targetAdapter = assertCollectionTypeAdapterExists(target);
            sourceAdapter.iterate(source, new CollectionTypeAdapter.IterationHandler() {
                @Override
                public void handle(Object entry, MappingState state) {
                    targetAdapter.addObjectToCollection(entry, target, state);
                }
            }, state);
        }
    }

}
