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

public interface CollectionTypeAdapter {
    public static interface IterationHandler {
        void handle(Object entry, MappingState state);
    }
    void addObjectToCollection(Object entry, Object collection, MappingState state);
    void iterate(Object collection, IterationHandler handler, MappingState state);
    Class<?> getSupportedCollectionType();
    Object newCollectionInstance(Class<?> type);
}
