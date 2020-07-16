package org.bndly.de.rest.websocket.server.impl;

/*-
 * #%L
 * REST Websocket Server
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
import org.bndly.de.rest.websocket.api.WebsocketEventDispatcher;
import java.io.IOException;
import java.util.concurrent.Future;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class JettyWebsocketEventDispatcher implements WebsocketEventDispatcher, WebSocketListener, Socket {

	private Session session;

	public abstract WebSocketEventListener getWebSocketEventListener();

	@Override
	public void onWebSocketBinary(byte[] bytes, int i, int i1) {
		getWebSocketEventListener().onWebSocketBinary(this, bytes, i, i1);
	}

	@Override
	public void onWebSocketClose(int i, String string) {
		getWebSocketEventListener().onWebSocketClose(this, i, string);
	}

	@Override
	public void onWebSocketConnect(Session sn) {
		this.session = sn;
		getWebSocketEventListener().onWebSocketConnect(this);
	}

	@Override
	public void onWebSocketError(Throwable thrwbl) {
		getWebSocketEventListener().onWebSocketError(this, thrwbl);
	}

	@Override
	public void onWebSocketText(String string) {
		getWebSocketEventListener().onWebSocketText(this, string);
	}

	// convenience around the session
	@Override
	public void close() {
		if (session.isOpen()) {
			session.close();
		}
	}

	@Override
	public boolean isOpened() {
		return session.isOpen();
	}

	/**
	 * this method sends a string synchronously. it has to be synchronized,
	 * otherwise the jetty websocket implementation will raise exceptions.
	 * @param message the string message to send
	 * @throws IOException 
	 */
	@Override
	public synchronized void sendMessage(String message) throws IOException {
		if (session.isOpen()) {
			try {
				RemoteEndpoint r = session.getRemote();
				r.sendString(message);
			} catch (org.eclipse.jetty.websocket.api.WebSocketException e) {
				throw new IOException("could not send message, because websocket implementation raised an exception: " + e.getMessage(), e);
			}
		}
	}

	@Override
	public Future sendMessageAsync(String message) throws IOException {
		if (session.isOpen()) {
			return session.getRemote().sendStringByFuture(message);
		}
		return null;
	}
	
	

}
