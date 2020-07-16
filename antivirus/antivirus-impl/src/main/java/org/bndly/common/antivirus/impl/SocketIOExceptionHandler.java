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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SocketIOExceptionHandler<E> implements IOExceptionHandler {
	private boolean isSocketException;
	private final SocketExceptionHandler<E> socketExceptionHandler;
	private final ResponseReader responseReader;
	private boolean pipeIsBroken;
	private boolean connectionIsReset;
	private boolean failed;
	private boolean handled;
	private E valueOfHandledException;
	private final Socket socket;

	public SocketIOExceptionHandler(SocketExceptionHandler<E> socketExceptionHandler, Socket socket, ResponseReader responseReader) {
		this.socketExceptionHandler = socketExceptionHandler;
		this.socket = socket;
		this.responseReader = responseReader;
	}

	public E getValueOfHandledException() {
		return valueOfHandledException;
	}

	@Override
	public boolean canHandle(IOException exception) {
		failed = true;
		this.isSocketException = SocketException.class.isInstance(exception);
		String msg = exception.getMessage();
		String msgLc = msg.toLowerCase();
		if ("Broken pipe".equals(exception.getMessage()) || msgLc.contains("broken pipe")) {
			// there is no BrokenPipeException, hence I do this little hack.
			// connection has been closed by the remote. there is no way 
			// to recover it. hence we will close the current socket and 
			// get a new one.
			pipeIsBroken = true;
		} else if ("Connection reset".equals(exception.getMessage()) || msgLc.contains("connection reset")) {
			// there is no BrokenPipeException, hence I do this little hack.
			// connection has been closed by the remote. there is no way 
			// to recover it. hence we will close the current socket and 
			// get a new one.
			connectionIsReset = true;
		}
		return isSocketException;
	}

	@Override
	public void handle(IOException exception) {
		if (socketExceptionHandler != null) {
			if (pipeIsBroken) {
				handled = true;
				valueOfHandledException = socketExceptionHandler.handleBrokenPipe((SocketException) exception, socket, responseReader);
			} else if (connectionIsReset) {
				handled = true;
				valueOfHandledException = socketExceptionHandler.handleConnectionReset((SocketException) exception, socket, responseReader);
			}
		}
	}

	@Override
	public boolean shouldReturnImmediatly(IOException exception) {
		return isSocketException;
	}

	@Override
	public boolean didFail() {
		return failed;
	}

	@Override
	public boolean didHandle() {
		return handled;
	}
	
	
	
}
