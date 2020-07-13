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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class PaginationRestBean {

	@XmlElement
	private Long start;
	@XmlElement
	private Long size;
	@XmlElement
	private Long totalRecords;

	public Long getStart() {
		return start;
	}

	public void setStart(Long start) {
		this.start = start;
	}

	public Long getSize() {
		return size;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public Long getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(Long totalRecords) {
		this.totalRecords = totalRecords;
	}

	public boolean hasNextPage() {
		if (totalRecords != null && start != null && size != null) {
			return (start + size) < totalRecords;
		}
		return false;
	}

	public boolean hasPreviousPage() {
		if (totalRecords != null && start != null && size != null) {
			return start > 0;
		}
		return false;
	}

	public Long nextPageStart() {
		if (start != null && size != null) {
			return start + size;
		}
		return null;
	}

	public Long previousPageStart() {
		if (start != null && size != null) {
			long s = start - size;
			if (s < 0) {
				return 0L;
			}
			return s;
		}
		return null;
	}
}
