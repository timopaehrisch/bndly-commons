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

import java.util.List;

public abstract class ManagedClientException extends ClientException {

	private static final long serialVersionUID = -7155439020412019442L;

	private final RemoteCause remoteCause;

	public ManagedClientException(String message, RemoteCause remoteCause) {
		super(message);
		this.remoteCause = remoteCause;
	}
	
	public ManagedClientException(String message) {
		this(message, null);
	}

	public RemoteCause getRemoteCause() {
		return remoteCause;
	}

	protected String printRemoteStackTrace() {
		StringBuffer sb = new StringBuffer();
		RemoteCause c = remoteCause;
		while (c != null) {
			sb.append("\ncaused by:");
			List<StackTraceElement> items = c.getStackTraceElements();
			if (items != null) {
				for (StackTraceElement element : items) {
					sb.append("\n")
					.append("  ")
					.append(element.getClassName())
					.append(".")
					.append(element.getMethodName())
					.append("(")
					.append(element.getFileName())
					.append(" #")
					.append(element.getLineNumber())
					.append(")");
				}
			}
			c = c.getParentCause();
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + getMessage() + printRemoteStackTrace();
	}
}
