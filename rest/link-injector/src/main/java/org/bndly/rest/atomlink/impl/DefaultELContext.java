package org.bndly.rest.atomlink.impl;

/*-
 * #%L
 * REST Link Injector
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

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;

public class DefaultELContext extends ELContext {

	private ELResolver elResolver;
	private FunctionMapper functionMapper;
	private VariableMapper variableMapper;

	public DefaultELContext(ELResolver elResolver, FunctionMapper functionMapper, VariableMapper variableMapper) {
		this.elResolver = elResolver;
		this.functionMapper = functionMapper;
		this.variableMapper = variableMapper;
	}

	public DefaultELContext() {
	}

	@Override
	public ELResolver getELResolver() {
		return elResolver;
	}

	@Override
	public FunctionMapper getFunctionMapper() {
		return functionMapper;
	}

	@Override
	public VariableMapper getVariableMapper() {
		return variableMapper;
	}

	public void setElResolver(ELResolver elResolver) {
		this.elResolver = elResolver;
	}

	public void setFunctionMapper(FunctionMapper functionMapper) {
		this.functionMapper = functionMapper;
	}

	public void setVariableMapper(VariableMapper variableMapper) {
		this.variableMapper = variableMapper;
	}

}
