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
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "rules")
@XmlAccessorType(XmlAccessType.NONE)
public class RulesRestBean extends ListRestBean<RuleFunctionReferenceRestBean> {

	@XmlElement
	private String className;

	@XmlElements({
		@XmlElement(name = "rule", type = RuleFunctionRestBean.class),
		@XmlElement(name = "ruleRef", type = RuleFunctionReferenceRestBean.class)
	})
	private List<RuleFunctionReferenceRestBean> items;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	@Override
	public List<RuleFunctionReferenceRestBean> getItems() {
		return items;
	}

	@Override
	public void setItems(List<RuleFunctionReferenceRestBean> items) {
		this.items = items;
	}

}
