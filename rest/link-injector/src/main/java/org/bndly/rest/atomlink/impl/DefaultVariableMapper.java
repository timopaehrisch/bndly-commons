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

import java.util.HashMap;
import java.util.Map;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

public class DefaultVariableMapper extends VariableMapper {

	private final Map<String, ValueExpression> variables = new HashMap<>();

	public Map<String, ValueExpression> getVariables() {
		return variables;
	}

	@Override
	public ValueExpression resolveVariable(String string) {
		ValueExpression ex = variables.get(string);
		return ex;
	}

	@Override
	public ValueExpression setVariable(String string, ValueExpression ve) {
		ValueExpression prev = variables.get(string);
		variables.put(string, ve);
		return prev;
	}
}
