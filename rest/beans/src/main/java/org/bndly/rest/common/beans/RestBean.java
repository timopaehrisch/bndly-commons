package org.bndly.rest.common.beans;

/*-
 * #%L
 * REST Common Beans
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

import org.bndly.rest.atomlink.api.annotation.AtomLinkHolder;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class RestBean {

	@AtomLinkHolder(AtomLink.class)
	@XmlElement(name = "lnk")
	private List<AtomLink> links;

	@XmlElement
	private PaginationRestBean page;
	@XmlElement
	private SortRestBean sorting;

	public AtomLink follow(String rel) {
		if (rel == null) {
			return null;
		}
		if (links == null) {
			return null;
		}
		for (AtomLink atomLink : links) {
			if (rel.equals(atomLink.getRel())) {
				return atomLink;
			}
		}
		return null;
	}

	public void setLink(String rel, String href, String method) {
		AtomLink l = follow(rel);
		if (l == null) {
			l = new AtomLink();
		}
		l.setRel(rel);
		l.setHref(href);
		l.setMethod(method);
		if (links == null) {
			links = new ArrayList<>();
		}
		links.add(l);
	}

	public PaginationRestBean getPage() {
		return page;
	}

	public void setPage(PaginationRestBean page) {
		this.page = page;
	}

	public SortRestBean getSorting() {
		return sorting;
	}

	public void setSorting(SortRestBean sorting) {
		this.sorting = sorting;
	}

}
