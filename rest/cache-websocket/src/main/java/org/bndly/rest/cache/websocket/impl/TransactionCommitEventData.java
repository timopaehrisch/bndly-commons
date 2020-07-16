package org.bndly.rest.cache.websocket.impl;

/*-
 * #%L
 * REST Cache Websocket
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class TransactionCommitEventData {

	private final String transaction;
	private boolean complete;
	private List<String> paths;
	private List<String> recursivePaths;

	public TransactionCommitEventData(String transaction) {
		this.transaction = transaction;
	}

	public TransactionCommitEventData() {
		this.transaction = UUID.randomUUID().toString();
	}

	public List<String> getPaths() {
		return paths;
	}

	public List<String> getRecursivePaths() {
		return recursivePaths;
	}

	public void addPath(String path) {
		if (complete) {
			return;
		}
		if (paths == null) {
			paths = new ArrayList<>();
		}
		paths.add(path);
	}

	public void addRecursivePath(String path) {
		if (complete) {
			return;
		}
		if (recursivePaths == null) {
			recursivePaths = new ArrayList<>();
		}
		recursivePaths.add(path);
	}

	public String getTransaction() {
		return transaction;
	}

	public void setComplete(boolean complete) {
		if (complete) {
			paths = null;
			recursivePaths = null;
		}
		this.complete = complete;
	}

	public boolean isComplete() {
		return complete;
	}

}
