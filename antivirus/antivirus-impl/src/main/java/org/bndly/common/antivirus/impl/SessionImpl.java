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
import org.bndly.common.antivirus.api.AVScanException;
import org.bndly.common.antivirus.api.AVSizeLimitExceededException;
import org.bndly.common.antivirus.api.ScanResult;
import org.bndly.common.antivirus.api.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class SessionImpl implements Session, CommandExecutor {
	private static final Logger LOG = LoggerFactory.getLogger(SessionImpl.class);
	
	private Boolean alive;
	private LazySocket socket;
	private final SocketManager socketManager;
	private final CommandFactory commandFactory;
	private final AVServiceImpl service;

	public SessionImpl(SocketManager socketManager, LazySocket socket, CommandFactory commandFactory, AVServiceImpl service) {
		this.socketManager = socketManager;
		this.socket = socket;
		this.commandFactory = commandFactory;
		this.service = service;
	}

	public final LazySocket getLazySocket() {
		return socket;
	}

	public final Socket getSocket() {
		return socket.getRealSocket();
	}

	public void init() {
		Command command = service.getCommandBuilderPrototype()
				.terminateWithNullCharacter()
				.idSession()
				.build();
		
		try {
			executeCommand(command);
			if (getSocket().isClosed()) {
				throw new AVException("failed to start a new session");
			}
		} catch (IOException e) {
			throw new AVException("failed to start id session: " + e.getMessage(), e);
		}
	}
	
	@Override
	public boolean isAlive() {
		if (alive != null) {
			return alive;
		}
		return !getSocket().isClosed();
	}

	@Override
	public ScanResult scan(final InputStream is) throws AVScanException {
		LOG.info("scaning data with clam av");
		Command command = commandFactory.getCommandBuilderPrototype()
				.terminateWithNullCharacter()
				.chunkSize(service.getDefaultChunkSize())
				.instream(is)
				.build();
		try {
			String response = executeCommand(command);
			return new ScanResponseMapper(response, command);
		} catch (AVSizeLimitExceededException e) {
			// rethrow, but close the socket. it is dead anyways, because
			// clam av closes it upon this exceptional state.
			try {
				getSocket().close();
			} catch (IOException ex) {
				LOG.error("could not close socket, after remote had already closed it due to an exceeded size limit");
			}
			alive = false;
			throw e;
		} catch (IOException e) {
			LOG.error("communication to clam av daemon failed: {}", e.getMessage(), e);
			throw new AVScanException("failed to scan input: " + e.getMessage(), e);
		}
	}
	
	@Override
	public String version() throws AVException {
		LOG.info("sending ping to clam av");
		Command command = commandFactory.getCommandBuilderPrototype()
				.terminateWithNullCharacter()
				.version()
				.build();
		try {
			String result = executeCommand(command);
			return result;
		} catch (IOException e) {
			throw new AVException("failed to retrieve status: " + e.getMessage(), e);
		}
	}

	@Override
	public String status() throws AVException {
		LOG.info("requesting status from clam av");
		Command command = commandFactory.getCommandBuilderPrototype()
				.terminateWithNullCharacter()
				.stats()
				.build();
		try {
			return executeCommand(command);
		} catch (IOException e) {
			throw new AVException("failed to retrieve status: " + e.getMessage(), e);
		}
	}

	@Override
	public ScanResult scan(byte[] bytes) throws AVScanException {
		return scan(new ByteArrayInputStream(bytes));
	}
	
	@Override
	public synchronized String executeCommand(final Command command) throws IOException {
		if (!isAlive()) {
			// session has been terminated.
			if (CommandBuilder.CMD_IDSESSION.equals(command.getName())) {
				throw new AVException("session could not be started");
			} else {
				throw new AVException("session has been terminated");
			}
		}
		return command.runOnSocket(getSocket(), new SocketExceptionHandler<String>() {

			@Override
			public String handleConnectionReset(SocketException exception, Socket socket, ResponseReader responseReader) {
				return handleBrokenPipe(exception, socket, responseReader);
			}

			@Override
			public String handleBrokenPipe(SocketException exception, Socket socket, ResponseReader responseReader) {
				if (CommandBuilder.CMD_INSTREAM.equals(command.getName())) {
					// the connection might break if the input stream is too 
					// long, or it might break if it is physically broken
					try {
						// might have been reset because there was too much input
						return responseReader.readResponseToMemory();
					} catch (ResponseReadingException e) {
						// might be a really broken connection
						return tryAgain();
					}
				} else if (CommandBuilder.CMD_END.equals(command.getName())) {
					// we don't care.
					return null;
				}
				if (command.decrementRetries() > -1) {
					return tryAgain();
				} else {
					throw new AVException("could not perform any more retries to execute command " + command.getName() + " because all retries have failed.");
				}
			}
			
			private String tryAgain() {
				// return the old socket. its broken anyways
				try {
					getSocket().close();
				} catch (IOException ex) {
					// we don't care
				}
				socketManager.release(getLazySocket());
				
				// get a fresh socket
				socket = socketManager.take(service.getTimeoutMillis());
				
				// reinitialize a session
				init();
				InputStream dataToSend = command.getDataToSend();
				if (dataToSend != null) {
					if (!dataToSend.markSupported()) {
						throw new AVException(
								"can not reset input data stream because "
								+ dataToSend.getClass().getName()
								+ " does not support reset(). This is required because the command has to be resent "
								+ "and the input data might already have been partially read."
						);
					} else {
						try {
							dataToSend.reset();
						} catch (IOException ex) {
							throw new AVException("could not reset input data stream while retrying to send a command: " + ex.getMessage(), ex);
						}
					}
				}
				try {
					// execute the command again
					return executeCommand(command);
				} catch (IOException ex) {
					throw new AVException("failed to recover while running command " + command.getName(), ex);
				}
			}

		});
	}

	@Override
	public void close() throws AVException {
		try {
			service.endSession(this);
		} finally {
			alive = Boolean.FALSE;
		}
	}
}
