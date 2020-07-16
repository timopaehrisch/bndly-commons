package org.bndly.common.bpm.exception;

/*-
 * #%L
 * BPM API
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

public class ProcessErrorException extends RuntimeException {
	private static final long serialVersionUID = -2923325637461197253L;
	
	private final String errorCode;
	private final String processName;
	private final String processId;
	
	public ProcessErrorException(String message, String errorCode, String processName, String processId) {
		super(message);
		this.processId = processId;
		this.processName = processName;
		this.errorCode = errorCode;
	}
	
	public ProcessErrorException(String errorCode, String processName, String processId) {
		super();
		this.processId = processId;
		this.processName = processName;
		this.errorCode = errorCode;
	}
	
	public String getErrorCode() {
		return errorCode;
	}

	public String getProcessId() {
		return processId;
	}
	
	public String getProcessName() {
		return processName;
	}
}
