package org.bndly.de.rest.websocket.client.impl;

/*-
 * #%L
 * REST Websocket Client
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

import java.util.UUID;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FlushEventData {
	
	public final String transaction;
	public final String path;
	public final boolean recursive;
	public final boolean complete;

	public static FlushEventData complete() {
		return new FlushEventData(UUID.randomUUID().toString(), null, false, true);
	}
	
	public FlushEventData(String path, boolean recursive) {
		this(UUID.randomUUID().toString(), path, recursive, false);
	}

	public FlushEventData(String transaction, String path, boolean recursive, boolean complete) {
		this.transaction = transaction;
		this.path = path;
		this.recursive = recursive;
		this.complete = complete;
	}

	public String getTransaction() {
		return transaction;
	}

	public String getPath() {
		return path;
	}

	public boolean isRecursive() {
		return recursive;
	}
	
}
