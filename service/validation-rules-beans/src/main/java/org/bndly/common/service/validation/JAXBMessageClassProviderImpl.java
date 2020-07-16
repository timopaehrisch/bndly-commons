package org.bndly.common.service.validation;

/*-
 * #%L
 * Validation Rules Beans
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

import org.bndly.rest.atomlink.api.JAXBMessageClassProvider;
import java.util.Collection;
import java.util.HashSet;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = JAXBMessageClassProvider.class, immediate = true)
public class JAXBMessageClassProviderImpl implements JAXBMessageClassProvider {

	private static final Collection<Class<?>> classes;

	static {
		classes = new HashSet<>();
		classes.add(ANDFunctionReferenceRestBean.class);
		classes.add(ANDFunctionRestBean.class);
		classes.add(BooleanFunctionReferenceRestBean.class);
		classes.add(CharsetFunctionReferenceRestBean.class);
		classes.add(CharsetFunctionRestBean.class);
		classes.add(EmptyFunctionReferenceRestBean.class);
		classes.add(EmptyFunctionRestBean.class);
		classes.add(EqualsFunctionReferenceRestBean.class);
		classes.add(EqualsFunctionRestBean.class);
		classes.add(FunctionReferenceRestBean.class);
		classes.add(FunctionsRestBean.class);
		classes.add(IntervallFunctionReferenceRestBean.class);
		classes.add(IntervallFunctionRestBean.class);
		classes.add(MaxSizeFunctionReferenceRestBean.class);
		classes.add(MaxSizeFunctionRestBean.class);
		classes.add(MultiplyFunctionReferenceRestBean.class);
		classes.add(MultiplyFunctionRestBean.class);
		classes.add(NotFunctionReferenceRestBean.class);
		classes.add(NotFunctionRestBean.class);
		classes.add(NumericFunctionReferenceRestBean.class);
		classes.add(ORFunctionReferenceRestBean.class);
		classes.add(ORFunctionRestBean.class);
		classes.add(PrecisionFunctionReferenceRestBean.class);
		classes.add(PrecisionFunctionRestBean.class);
		classes.add(RegExFunctionReferenceRestBean.class);
		classes.add(RegExFunctionRestBean.class);
		classes.add(RuleFunctionReferenceRestBean.class);
		classes.add(RuleFunctionRestBean.class);
		classes.add(RuleSetReferenceRestBean.class);
		classes.add(RuleSetRestBean.class);
		classes.add(RuleSetsRestBean.class);
		classes.add(RulesRestBean.class);
		classes.add(ValueFunctionReferenceRestBean.class);
		classes.add(ValueFunctionRestBean.class);
	}

	@Override
	public Collection<Class<?>> getJAXBMessageClasses() {
		return classes;
	}
}
