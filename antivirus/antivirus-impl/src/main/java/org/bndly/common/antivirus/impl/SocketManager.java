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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface SocketManager {
	
	public static interface Callback<E> {

		E runOnSocket(Socket socket, SocketExceptionHandler<E> socketExceptionHandler) throws IOException;
	}

	/**
	 * invokes the callback on a socket that is freshly created and closed for each invocation.
	 * @param <E> whatever the callback wants to return
	 * @param callback the callback that will be able to work with the socket
	 * @return the return value of the callback
	 * @throws IOException underlying IOExceptions caused by socket interaction.
	 */
	<E> E runOnSingleUseSocket(Callback<E> callback) throws IOException;
	
	LazySocket take(long timeoutMillis);
	LazySocket take();
	void release(LazySocket socket);
}
