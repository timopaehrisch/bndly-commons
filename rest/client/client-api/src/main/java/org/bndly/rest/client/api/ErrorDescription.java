package org.bndly.rest.client.api;

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


public class ErrorDescription {
	private String code;
	private String message;
	private String details;
	private String beanClazzName;
	private String beanFieldName;
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getDetails() {
		return details;
	}
	public void setDetails(String details) {
		this.details = details;
	}
	public String getBeanClazzName() {
		return beanClazzName;
	}
	public void setBeanClazzName(String beanClazzName) {
		this.beanClazzName = beanClazzName;
	}
	public String getBeanFieldName() {
		return beanFieldName;
	}
	public void setBeanFieldName(String beanFieldName) {
		this.beanFieldName = beanFieldName;
	}
}
