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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class XVisualObject {
	
	private XContainer ownerContainer;
	
	@XmlAttribute
	private String itemId;

	@XmlAttribute(name = "class")
	private String styleClasses;

	public String getItemId() {
		return itemId;
	}

	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	public String getStyleClasses() {
		return styleClasses;
	}

	public void setStyleClasses(String styleClasses) {
		this.styleClasses = styleClasses;
	}

	public XContainer getOwnerContainer() {
		return ownerContainer;
	}

	public void setOwnerContainer(XContainer ownerContainer) {
		this.ownerContainer = ownerContainer;
	}

}
