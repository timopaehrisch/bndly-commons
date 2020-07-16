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

import org.bndly.de.rest.websocket.api.Socket;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ClientSocket implements Socket {

	private static final Logger LOG = LoggerFactory.getLogger(ClientSocket.class);

	private Future<Session> session;

	public ClientSocket() {
	}

	public ClientSocket(Future<Session> session) {
		this.session = session;
	}

	private static interface SessionCallback<E> {

		E doWith(Session session) throws IOException;

		E onFailure(Exception ex);
	}

	private abstract static class DefaultSessionCallback<E> implements SessionCallback<E> {

		@Override
		public E onFailure(Exception ex) {
			return null;
		}

	}

	private <E> E doWithSession(SessionCallback<E> callback) {
		if (session == null) {
			return null;
		}
		try {
			Session sess = session.get();
			return callback.doWith(sess);
		} catch (IOException | InterruptedException | ExecutionException ex) {
			LOG.error("could not work with socket session");
			return callback.onFailure(ex);
		}
	}

	@Override
	public void close() {
		doWithSession(new DefaultSessionCallback() {

			@Override
			public Object doWith(Session session) throws IOException {
				session.close();
				return null;
			}
		});
	}

	@Override
	public boolean isOpened() {
		return doWithSession(new SessionCallback<Boolean>() {

			@Override
			public Boolean doWith(Session session) throws IOException {
				return session.isOpen();
			}

			@Override
			public Boolean onFailure(Exception ex) {
				return false;
			}
		});
	}

	@Override
	public void sendMessage(final String message) throws IOException {
		doWithSession(new SessionCallback<Object>() {

			@Override
			public Object doWith(Session session) throws IOException {
				session.getRemote().sendString(message);
				return null;
			}

			@Override
			public Object onFailure(Exception ex) {
				LOG.error("could not send message");
				return null;
			}
		});
	}

	@Override
	public Future sendMessageAsync(final String message) throws IOException {
		return doWithSession(new SessionCallback<Future>() {

			@Override
			public Future doWith(Session session) throws IOException {
				return session.getRemote().sendStringByFuture(message);
			}

			@Override
			public Future onFailure(Exception ex) {
				LOG.error("could not send message");
				return null;
			}
		});
	}

	public void setSession(Future<Session> session) {
		this.session = session;
	}

}
