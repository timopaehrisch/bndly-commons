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

import org.bndly.common.antivirus.api.AVCommandReadTimeoutException;
import org.bndly.common.antivirus.api.AVException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class Command implements SocketManager.Callback<String> {

	private static final Logger LOG = LoggerFactory.getLogger(Command.class);

	private final InputStream dataToSend;
	private final String name;
	private final boolean expectingResponse;
	private final boolean usingNullTermination;
	private final boolean terminatingDataWithZeroLengthChunk;
	private final int chunkSize;
	private final String charset;
	private int retriesRemaining;
	private static final byte[] NOTHING = new byte[0];

	public Command(String name, boolean usingNullTermination, boolean expectingResponse, String charset, int retriesRemaining) {
		this(null, -1, false, name, usingNullTermination, expectingResponse, charset, retriesRemaining);
	}

	public Command(
			InputStream dataToSend, 
			int chunkSize, 
			boolean terminatingDataWithZeroLengthChunk, 
			String name, 
			boolean usingNullTermination, 
			boolean expectingResponse, 
			String charset, 
			int retriesRemaining
	) {
		this.dataToSend = dataToSend;
		this.name = name;
		this.usingNullTermination = usingNullTermination;
		this.chunkSize = chunkSize;
		this.terminatingDataWithZeroLengthChunk = terminatingDataWithZeroLengthChunk;
		this.charset = charset;
		this.expectingResponse = expectingResponse;
		this.retriesRemaining = retriesRemaining;
	}

	public InputStream getDataToSend() {
		return dataToSend;
	}

	public String getName() {
		return name;
	}

	public boolean isUsingNullTermination() {
		return usingNullTermination;
	}

	public int getChunkSize() {
		return chunkSize;
	}

	public boolean isTerminatingDataWithZeroLengthChunk() {
		return terminatingDataWithZeroLengthChunk;
	}

	@Override
	public String runOnSocket(Socket socket, SocketExceptionHandler<String> socketExceptionHandler) throws IOException {
		if (socketExceptionHandler == null) {
			throw new IllegalArgumentException("socketExceptionHandler is not allowed to be null. think about what to do, when the connection breaks!");
		}
		try {
			return runOnSocketInternal(socket, socketExceptionHandler);
		} catch (AVException e) {
			// catch and rethrow
			throw e;
		} catch (Throwable e) {
			throw new AVException("failed to send command: " + e.getMessage(), e);
		} finally {
			if (socket.isClosed()) {
				LOG.warn("socket has been closed after execution of command {}", getName());
			}
		}
	}

	private String runOnSocketInternal(final Socket socket, final SocketExceptionHandler<String> socketExceptionHandler) {
		LOG.info("communicating with clam av daemon now. sending command: {}", getName());
		DataOutputStream sendCommandToMe = createDataOutputStreamFromSocket(socket);
		String head = createHeadOfCommand();
		// send the command. data will be sent later.
		SocketIOExceptionHandler<String> writeExceptionHandler = new SocketIOExceptionHandler<>(socketExceptionHandler, socket, new ResponseReader() {

			@Override
			public String readResponseToMemory() throws ResponseReadingException {
				return internalReadResponseToString(socket);
			}
		});
		writeHeadToStream(head, sendCommandToMe, writeExceptionHandler);
		if (writeExceptionHandler.didFail()) {
			if (writeExceptionHandler.didHandle()) {
				// now i would have to delegate to an external strategy
				return writeExceptionHandler.getValueOfHandledException();
			} else {
				throw new AVException("could not write command head");
			}
		} else {
			sendAdditionalCommandData(sendCommandToMe, writeExceptionHandler);
			if (!writeExceptionHandler.didFail()) {
				// if there is data or not, we always have to flush before reading anything back from the socket.
				flush(sendCommandToMe, writeExceptionHandler);
			}

			if (!writeExceptionHandler.didFail() && !expectingResponse) {
				String responseWeDidNotExpect;
				try {
					// return here rather than reading from a stream that is blocked.
					responseWeDidNotExpect = internalReadResponseToString(socket);
				} catch (ResponseReadingException ex) {
					try {
						responseWeDidNotExpect = new String(ex.getWhatHasBeenReadSoFar(), charset);
					} catch (UnsupportedEncodingException ex2) {
						throw new AVException("could not create string from response with charset " + charset + ": " + ex2.getMessage(), ex);
					}
				}
				if (responseWeDidNotExpect.startsWith(CommandBuilder.RESP_CMD_READ_TIMEOUT)) {
					try {
						socket.close();
					} catch (IOException ex) {
						// silently close the socket. clamd has already close the connection anyways.
					}
					throw new AVCommandReadTimeoutException(
							"clam av daemon reported that reading the command on the socket has timed out while executing command "
							+ getName() + ": " + responseWeDidNotExpect
					);
				}
				return responseWeDidNotExpect;
			}

			if (writeExceptionHandler.didFail() && writeExceptionHandler.didHandle()) {
				return writeExceptionHandler.getValueOfHandledException();
			}
			try {
				return internalReadResponseToString(socket);
			} catch (ResponseReadingException ex) {
				throw new AVException("failed to read response", ex);
			}
		}
	}

	private DataOutputStream createDataOutputStreamFromSocket(Socket socket) {
		try {
			DataOutputStream sendCommandToMe = new DataOutputStream(socket.getOutputStream());
			return sendCommandToMe;
		} catch (IOException ex) {
			throw new AVException("failed to create data output stream on socket: " + ex.getMessage(), ex);
		}
	}

	private String createHeadOfCommand() {
		StringBuffer sb = new StringBuffer();
		if (isUsingNullTermination()) {
			sb.append("z");
		} else {
			sb.append("n");
		}
		sb.append(getName());
		if (isUsingNullTermination()) {
			sb.append("\0");
		} else {
			sb.append("\n");
		}
		String head = sb.toString();
		return head;
	}

	private void writeHeadToStream(String head, DataOutputStream sendCommandToMe, IOExceptionHandler writeExceptionHandler) {
		try {
			sendCommandToMe.write(head.getBytes(charset));
			sendCommandToMe.flush();
		} catch (IOException ex) {
			if (writeExceptionHandler.canHandle(ex)) {
				writeExceptionHandler.handle(ex);
				if (writeExceptionHandler.shouldReturnImmediatly(ex)) {
					return;
				}
			} else {
				throw new AVException("failed to write head to socket: " + ex.getMessage(), ex);
			}
		}
	}

	private void sendAdditionalCommandData(DataOutputStream sendCommandToMe, IOExceptionHandler writeExceptionHandler) {
		InputStream dataToSend = getDataToSend();
		if (dataToSend != null) {
			byte[] buffer = new byte[getChunkSize()];
			int readBytes;
			try {
				while ((readBytes = dataToSend.read(buffer)) > -1) {
					try {
						// write a chunk. [<4byte unsigned int length><data>]
						sendCommandToMe.writeInt(readBytes);
					} catch (IOException ex) {
						if (writeExceptionHandler.canHandle(ex)) {
							writeExceptionHandler.handle(ex);
							if (writeExceptionHandler.shouldReturnImmediatly(ex)) {
								return;
							}
						} else {
							throw new AVException("failed to write size of chunk: " + ex.getMessage(), ex);
						}
					}
					try {
						sendCommandToMe.write(buffer, 0, readBytes);
					} catch (IOException ex) {
						if (writeExceptionHandler.canHandle(ex)) {
							writeExceptionHandler.handle(ex);
							if (writeExceptionHandler.shouldReturnImmediatly(ex)) {
								return;
							}
						} else {
							throw new AVException("failed to write chunk: " + ex.getMessage(), ex);
						}
					}
				}
			} catch (IOException ex) {
				throw new AVException("failed to read data to send: " + ex.getMessage(), ex);
			}

			if (isTerminatingDataWithZeroLengthChunk()) {
				try {
					sendCommandToMe.writeInt(0);
				} catch (IOException ex) {
					if (writeExceptionHandler.canHandle(ex)) {
						writeExceptionHandler.handle(ex);
						if (writeExceptionHandler.shouldReturnImmediatly(ex)) {
							return;
						}
					} else {
						throw new AVException("failed to send zero length chunk: " + ex.getMessage(), ex);
					}
				}
			}
		}
	}

	private void flush(DataOutputStream sendCommandToMe, IOExceptionHandler writeExceptionHandler) {
		try {
			sendCommandToMe.flush();
		} catch (IOException ex) {
			if (writeExceptionHandler.canHandle(ex)) {
				writeExceptionHandler.handle(ex);
				if (writeExceptionHandler.shouldReturnImmediatly(ex)) {
					return;
				}
			} else {
				throw new AVException("failed to flush the sent data: " + ex.getMessage(), ex);
			}
		}
	}
	
	private String internalReadResponseToString(Socket socket) throws ResponseReadingException {
		ByteArrayOutputStream bos = internalReadResponseToMemory(socket);
		try {
			String result = new String(bos.toByteArray(), charset);
			return result;
		} catch (UnsupportedEncodingException ex) {
			throw new AVException("could not create string from response with charset " + charset + ": " + ex.getMessage(), ex);
		}
	}
	
	private ByteArrayOutputStream internalReadResponseToMemory(Socket socket) throws ResponseReadingException {
		LOG.info("reading response of command {} to buffer", getName());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		InputStream readResultFromMe;
		try {
			readResultFromMe = socket.getInputStream();
		} catch (IOException ex) {
			throw new ResponseReadingException("failed to get input stream from socket: " + ex.getMessage(), NOTHING, ex);
		}

		int byteToWrite;
		boolean done = false;
		try {
			int estimateOfAvailableData = readResultFromMe.available();
			if (estimateOfAvailableData == 0 && !expectingResponse) {
				// if we did not expect anything and there is nothing available, we can make things short and return an empty buffer.
				return bos;
			}
			while (!done && (byteToWrite = readResultFromMe.read()) > -1) {
				if (isUsingNullTermination()) {
					if (byteToWrite == '\0') {
						// we are done
						done = true;
					}
				} else {
					if (byteToWrite == '\n') {
						// we are done
						done = true;
					}
				}
				if (!done) {
					bos.write(byteToWrite);
				}
			}
		} catch (IOException ex) {
			throw new ResponseReadingException("failed to read from input stream: " + ex.getMessage(), bos.toByteArray(), ex);
		}
		try {
			bos.flush();
		} catch (IOException ex) {
			throw new ResponseReadingException("failed to flush to local byte array buffer: " + ex.getMessage(), bos.toByteArray(), ex);
		}
		return bos;
	}
	
	public final int decrementRetries() {
		retriesRemaining--;
		return retriesRemaining;
	}
}
