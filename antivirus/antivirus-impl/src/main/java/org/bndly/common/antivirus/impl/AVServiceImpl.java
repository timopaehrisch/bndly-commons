package org.bndly.common.antivirus.impl;

/*-
 * #%L
 * Antivirus Impl
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

import org.bndly.common.antivirus.api.AVException;
import org.bndly.common.antivirus.api.AVService;
import org.bndly.common.antivirus.api.Session;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 * @see
 * <a href="http://manpages.ubuntu.com/manpages/lucid/man8/clamd.8.html">clamd
 * man page</a>
 */
public final class AVServiceImpl implements AVService, CommandFactory {

	private static final Logger LOG = LoggerFactory.getLogger(AVServiceImpl.class);
	private final SocketManager socketManager;
	private final String charset;
	private final int defaultChunkSize;
	private final long timeoutMillis;

	public AVServiceImpl(SocketManager socketManager, String charset, int defaultChunkSize, long timeoutMillis) {
		if (socketManager == null) {
			throw new IllegalArgumentException("socketManager is not allowed to be null");
		}
		if (charset == null) {
			throw new IllegalArgumentException("charset is not allowed to be null");
		}
		this.socketManager = socketManager;
		this.charset = charset;
		if (defaultChunkSize < 1) {
			throw new IllegalArgumentException("default chunk size has to be positive int");
		}
		this.defaultChunkSize = defaultChunkSize;
		this.timeoutMillis = timeoutMillis;
	}

	public long getTimeoutMillis() {
		return timeoutMillis;
	}
	
	@Override
	public CommandBuilder getCommandBuilderPrototype() {
		return new CommandBuilder().charset(charset);
	}

	@Override
	public boolean ping() throws AVException {
		LOG.info("sending ping to clam av");
		Command command = getCommandBuilderPrototype()
				.terminateWithNullCharacter()
				.ping()
				.build();
		try {
			String result = executeCommandSingleUse(command);
			return "PONG".equals(result);
		} catch (IOException e) {
			throw new AVException("failed to retrieve status: " + e.getMessage(), e);
		}
	}
	
	public void endSession(SessionImpl session) throws AVException {
		LOG.info("ending session on clam av");
		Command command = getCommandBuilderPrototype()
				.terminateWithNullCharacter()
				.end()
				.build();
		try {
			if (session.isAlive()) {
				session.executeCommand(command);
			}
		} catch (IOException e) {
			throw new AVException("failed to end id session: " + e.getMessage(), e);
		} finally {
			LazySocket socket = session.getLazySocket();
			socketManager.release(socket);
		}
	}

	@Override
	public Session startSession() throws AVException {
		LOG.info("starting new session on socket to clam av");
		LazySocket socket = socketManager.take(timeoutMillis);
		SessionImpl session = new SessionImpl(socketManager, socket, this, this);
		session.init();
		return session;
	}

	private String executeCommandSingleUse(final Command command) throws IOException {
		return socketManager.runOnSingleUseSocket(command);
	}

	public int getDefaultChunkSize() {
		return defaultChunkSize;
	}

}
