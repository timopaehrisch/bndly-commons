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

public abstract class CollectionCopyMapperImpl implements Mapper {

    protected abstract CollectionTypeAdapter assertCollectionTypeAdapterExists(Object object);
    protected abstract CollectionTypeAdapter assertCollectionTypeAdapterExists(Class<?> type);

    @Override
    public void map(Object source, final Object target, final MappingContext context, MappingState state) {
        if (source != null && target != null) {
            // map the source collection to the target collection
            CollectionTypeAdapter sourceAdapter = assertCollectionTypeAdapterExists(source);
            final CollectionTypeAdapter targetAdapter = assertCollectionTypeAdapterExists(target);
            sourceAdapter.iterate(source, new CollectionTypeAdapter.IterationHandler() {
                @Override
                public void handle(Object entry, MappingState state) {
                    Object r = context.map(entry, state);
                    targetAdapter.addObjectToCollection(r, target, state);
                }
            }, state);
        }
    }

}
