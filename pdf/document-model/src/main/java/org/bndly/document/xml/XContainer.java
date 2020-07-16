package org.bndly.document.xml;

/*-
 * #%L
 * PDF XML Document Model
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

import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "container")
@XmlAccessorType(XmlAccessType.NONE)
public class XContainer extends XVisualObject {

	@XmlElements({
		@XmlElement(name = "container", type = XContainer.class),
		@XmlElement(name = "paragraph", type = XParagraph.class),
		@XmlElement(name = "image", type = XImage.class),
		@XmlElement(name = "overflowPage", type = XOverflowPage.class),
		@XmlElement(name = "page", type = XPage.class),
		@XmlElement(name = "pageTemplate", type = XPageTemplate.class),
		@XmlElement(name = "table", type = XTable.class),
		@XmlElement(name = "tableCell", type = XTableCell.class),
		@XmlElement(name = "tableColumn", type = XTableColumn.class),
		@XmlElement(name = "systemText", type = XSystemText.class),
		@XmlElement(name = "text", type = XText.class)
	})
	private List<XVisualObject> items;

	public List<XVisualObject> getItems() {
		return items;
	}

	public void setItems(List<XVisualObject> items) {
		this.items = items;
	}

}
