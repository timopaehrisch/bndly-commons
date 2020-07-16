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

public interface MappingContext {

	public static interface Listener {
		void beforeMapping(Object source, Object target, Class<?> outputType, MappingState mappingState);
		void afterMapping(Object source, Object target, Class<?> outputType, MappingState mappingState);
	}
	
	/**
     * Maps the given source object to a target type, that is inferred by the defined available mappers in the mapping context
     * @param source the object that is mapped to a different type in the given context
     * @return mapping result
     */

    Object map(Object source, MappingState mappingState);
    
    <T> T map(Object source, Class<T> outputType);
    
    <T> T map(Object source, Class<T> outputType, MappingState mappingState);

    void map(Object source, Object target, Class<?> outputType);

    void map(Object source, Object target, Class<?> outputType, MappingState mappingState);
	
	MappingContext addListener(Listener listener);
	
	MappingContext removeListener(Listener listener);
}
