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

import org.bndly.rest.common.beans.ListRestBean;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

@XmlAccessorType(XmlAccessType.NONE)
public class FunctionsRestBean extends ListRestBean<FunctionReferenceRestBean> {

	@XmlElements({
		@XmlElement(name = "and", type = ANDFunctionRestBean.class),
		@XmlElement(name = "or", type = ORFunctionRestBean.class),
		@XmlElement(name = "empty", type = EmptyFunctionRestBean.class),
		@XmlElement(name = "intervall", type = IntervallFunctionRestBean.class),
		@XmlElement(name = "precision", type = PrecisionFunctionRestBean.class),
		@XmlElement(name = "value", type = ValueFunctionRestBean.class),
		@XmlElement(name = "mul", type = MultiplyFunctionRestBean.class),
		@XmlElement(name = "not", type = NotFunctionRestBean.class),
		@XmlElement(name = "maxSize", type = MaxSizeFunctionRestBean.class),
		@XmlElement(name = "charset", type = CharsetFunctionRestBean.class),
		@XmlElement(name = "regex", type = RegExFunctionRestBean.class),
		@XmlElement(name = "equals", type = EqualsFunctionRestBean.class),
		@XmlElement(name = "rule", type = RuleFunctionRestBean.class)
	})
	private List<FunctionReferenceRestBean> parameters;

	@Override
	public List<FunctionReferenceRestBean> getItems() {
		return parameters;
	}

	@Override
	public void setItems(List<FunctionReferenceRestBean> items) {
		this.parameters = items;
	}

	public void removeParameter(FunctionReferenceRestBean b) {
		if (parameters != null) {
			parameters.remove(b);
		}
	}

	public <F extends FunctionReferenceRestBean> F createParameter(Class<F> type) {
		try {
			F fn = type.newInstance();
			addParameter(fn);
			return fn;
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}

	public void addParameter(FunctionReferenceRestBean b) {
		if (parameters == null) {
			parameters = new ArrayList<>();
		}
		parameters.add(b);
	}

	public void addAllParameters(List<FunctionReferenceRestBean> functions) {
		if (functions != null) {
			for (FunctionReferenceRestBean functionRestBean : functions) {
				addParameter(functionRestBean);
			}
		}
	}

}
