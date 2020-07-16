package org.bndly.rest.schema.beans;

/*-
 * #%L
 * REST Schema Resource Beans
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

import org.bndly.rest.common.beans.RestBean;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "schema")
@XmlAccessorType(XmlAccessType.NONE)
public class SchemaBean extends RestBean {

	@XmlElement(name = "name")
	private String name;
	@XmlElement(name = "namespace")
	private String namespace;
	@XmlElement(name = "type")
	private List<TypeBean> types;
	@XmlElement(name = "mixin")
	private List<MixinBean> mixins;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public List<TypeBean> getTypes() {
		return types;
	}

	public void setTypes(List<TypeBean> types) {
		this.types = types;
	}

	public List<MixinBean> getMixins() {
		return mixins;
	}

	public void setMixins(List<MixinBean> mixins) {
		this.mixins = mixins;
	}

}
