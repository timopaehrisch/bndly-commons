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

import org.bndly.rest.atomlink.api.annotation.BeanID;
import org.bndly.rest.common.beans.RestBean;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class FunctionReferenceRestBean extends RestBean {

	@BeanID
	@XmlElement
	private Long id;

	@XmlElement(name = "pos")
	private Long position;

	@XmlElement
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getPosition() {
		return position;
	}

	public void setPosition(Long position) {
		this.position = position;
	}

	public abstract void setParameters(FunctionsRestBean parameters);

	public abstract FunctionsRestBean getParameters();

	@Override
	public String toString() {
		XmlRootElement rootElement = getClass().getAnnotation(XmlRootElement.class);
		if (rootElement != null) {
			if (!"##default".equals(rootElement.name())) {
				String parameters = "";
				if (getParameters() != null) {
					parameters = getParameters().toString();
				}
				return rootElement.name() + " " + parameters;
			}
		}
		return super.toString();
	}

}
