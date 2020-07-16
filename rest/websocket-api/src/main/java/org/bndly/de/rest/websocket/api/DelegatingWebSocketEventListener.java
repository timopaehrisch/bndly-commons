package org.bndly.de.rest.websocket.api;

/*-
 * #%L
 * REST Websocket API
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DelegatingWebSocketEventListener implements WebSocketEventListener {

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final List<WebSocketEventListener> listeners = new ArrayList<>();

	public void addListener(WebSocketEventListener listener) {
		if (listener != null) {
			lock.writeLock().lock();
			try {
				listeners.add(listener);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeListener(WebSocketEventListener listener) {
		if (listener != null) {
			lock.writeLock().lock();
			try {
				listeners.remove(listener);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	@Override
	public void onWebSocketBinary(Socket socket, byte[] bytes, int i, int i1) {
		lock.readLock().lock();
		try {
			for (WebSocketEventListener listener : listeners) {
				listener.onWebSocketBinary(socket, bytes, i, i1);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void onWebSocketClose(Socket socket, int i, String string) {
		lock.readLock().lock();
		try {
			for (WebSocketEventListener listener : listeners) {
				listener.onWebSocketClose(socket, i, string);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void onWebSocketConnect(Socket socket) {
		lock.readLock().lock();
		try {
			for (WebSocketEventListener listener : listeners) {
				listener.onWebSocketConnect(socket);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void onWebSocketError(Socket socket, Throwable throwable) {
		lock.readLock().lock();
		try {
			for (WebSocketEventListener listener : listeners) {
				listener.onWebSocketError(socket, throwable);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void onWebSocketText(Socket socket, String string) {
		lock.readLock().lock();
		try {
			for (WebSocketEventListener listener : listeners) {
				listener.onWebSocketText(socket, string);
			}
		} finally {
			lock.readLock().unlock();
		}
		
	}
}
