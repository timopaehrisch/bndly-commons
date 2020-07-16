package org.bndly.rest.query.beans;

/*-
 * #%L
 * REST Query Resource Beans
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

import org.bndly.rest.common.beans.AnyBean;
import org.bndly.rest.common.beans.ListRestBean;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@XmlRootElement(name = "schemaQueryResult")
@XmlAccessorType(XmlAccessType.NONE)
public class SchemaQueryResultRestBean extends ListRestBean<AnyBean> {

	@XmlElements({
		@XmlElement(name = "item", type = AnyBean.class)
	})
	private List<AnyBean> items;

	@Override
	public void setItems(List<AnyBean> list) {
		this.items = list;
	}

	@Override
	public List<AnyBean> getItems() {
		return items;
	}
	
}
