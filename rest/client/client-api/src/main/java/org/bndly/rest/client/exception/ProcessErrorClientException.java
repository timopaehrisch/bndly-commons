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

public class ProcessErrorClientException extends ManagedClientException {

	private static final long serialVersionUID = 3130278336425090488L;

	private String errorCode;
	private String processName;
	private String processId;

	public ProcessErrorClientException(String message, RemoteCause remoteCause) {
		super(message, remoteCause);
	}

	public ProcessErrorClientException(String message, RemoteCause remoteCause, String errorCode, String processName, String processId) {
		super(message, remoteCause);
		this.processId = processId;
		this.processName = processName;
		this.errorCode = errorCode;
	}
	
	public ProcessErrorClientException(String message, String errorCode, String processName, String processId) {
		super(message);
		this.processId = processId;
		this.processName = processName;
		this.errorCode = errorCode;
	}

	public String getProcessId() {
		return processId;
	}

	public String getProcessName() {
		return processName;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
