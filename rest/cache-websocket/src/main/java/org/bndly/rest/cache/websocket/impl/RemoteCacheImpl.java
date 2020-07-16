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

import org.bndly.de.rest.websocket.api.Socket;
import org.bndly.de.rest.websocket.api.WebSocketEventListener;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RemoteCacheImpl implements RemoteCache, WebSocketEventListener {

	private Socket socket;
	private final RemoteCacheFactory factory;

	public RemoteCacheImpl(RemoteCacheFactory factory) {
		this.factory = factory;
	}
	
	@Override
	public void onWebSocketBinary(Socket socket, byte[] bytes, int i, int i1) {
	}

	@Override
	public void onWebSocketClose(Socket socket, int i, String string) {
		factory.removeRemoteCache(this);
	}

	@Override
	public void onWebSocketConnect(Socket socket) {
	}

	@Override
	public void onWebSocketError(Socket socket, Throwable throwable) {
	}

	@Override
	public void onWebSocketText(Socket socket, String string) {
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void close() {
		if (this.socket != null) {
			socket.close();
			this.socket = null;
		}
	}
}
