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

public class ConstraintViolationClientException extends ManagedClientException {

	private static final long serialVersionUID = -3423255169574003434L;

	private final String beanClazzName;
	private final String beanFieldName;
	private final String constraintType;

	public ConstraintViolationClientException(String beanClazzName, String beanFieldName, String constraintType, String message, RemoteCause remoteCause) {
		super(message, remoteCause);
		this.beanClazzName = beanClazzName;
		this.beanFieldName = beanFieldName;
		this.constraintType = constraintType;
	}

	public ConstraintViolationClientException(String beanClazzName, String beanFieldName, String constraintType, String message) {
		super(message);
		this.beanClazzName = beanClazzName;
		this.beanFieldName = beanFieldName;
		this.constraintType = constraintType;
	}

	public String getBeanClazzName() {
		return beanClazzName;
	}

	public String getBeanFieldName() {
		return beanFieldName;
	}

	public String getConstraintType() {
		return constraintType;
	}

}
