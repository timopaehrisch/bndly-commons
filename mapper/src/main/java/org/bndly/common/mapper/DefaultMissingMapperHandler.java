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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultMissingMapperHandler implements MissingMapperHandler {

	@Override
	public Object handleMissingMapper(Object source, MappingState mappingState) {
		throw new IllegalStateException("no mapper for input type " + source.getClass() + " was found.");
	}

	@Override
	public Object handleMissingMapperOutputType(Object source, MappingState mappingState, Mapper mapper) {
		throw new IllegalStateException("found a mapper for input type " + source.getClass() + ", but couldn't figure out its output type.");
	}

	@Override
	public Object handleMissingOutputMapper(Object source, Object target, Class<?> outputType, MappingState mappingState) {
		throw new IllegalStateException("no mapper for input type " + source.getClass() + " and target type " + outputType + " was found.");
	}
	
}
