package org.bndly.rest.swagger.model;

/*-
 * #%L
 * REST Swagger Integration
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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class Path {
	private Operation get;
	private Operation put;
	private Operation post;
	private Operation delete;
	private Operation options;
	private Operation head;
	private Operation patch;
	
	public Operation getGet() {
		return get;
	}

	public void setGet(Operation get) {
		this.get = get;
	}

	public Operation getPut() {
		return put;
	}

	public void setPut(Operation put) {
		this.put = put;
	}

	public Operation getPost() {
		return post;
	}

	public void setPost(Operation post) {
		this.post = post;
	}

	public Operation getDelete() {
		return delete;
	}

	public void setDelete(Operation delete) {
		this.delete = delete;
	}

	public Operation getOptions() {
		return options;
	}

	public void setOptions(Operation options) {
		this.options = options;
	}

	public Operation getHead() {
		return head;
	}

	public void setHead(Operation head) {
		this.head = head;
	}

	public Operation getPatch() {
		return patch;
	}

	public void setPatch(Operation patch) {
		this.patch = patch;
	}
	
}
