package org.bndly.rest.client.exception;

/*-
 * #%L
 * REST Client API
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

public class PageOutOfBoundsClientException extends ManagedClientException {

	private static final long serialVersionUID = 3960748497486823018L;

	private final Long start;
	private final Long size;
	private final Long total;

	public PageOutOfBoundsClientException(Long start, Long size, Long total, String message, RemoteCause remoteCause) {
		super(message, remoteCause);
		this.start = start;
		this.size = size;
		this.total = total;
	}

	public PageOutOfBoundsClientException(Long start, Long size, Long total, String message) {
		super(message);
		this.start = start;
		this.size = size;
		this.total = total;
	}

	public Long getSize() {
		return size;
	}

	public Long getStart() {
		return start;
	}

	public Long getTotal() {
		return total;
	}

}
