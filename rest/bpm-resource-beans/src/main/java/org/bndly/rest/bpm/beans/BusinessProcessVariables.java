package org.bndly.rest.bpm.beans;

/*-
 * #%L
 * REST BPM Resource Beans
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
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "businessProcessVariables")
@XmlAccessorType(XmlAccessType.NONE)
public class BusinessProcessVariables extends ListRestBean<BusinessProcessVariable> {

	@XmlElement
	private List<BusinessProcessVariable> items;

	@Override
	public void setItems(List<BusinessProcessVariable> items) {
		this.items = items;
	}

	@Override
	public List<BusinessProcessVariable> getItems() {
		return items;
	}

}
