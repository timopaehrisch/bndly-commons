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

import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class AsyncPoolTest {
	
	private final static Logger LOG = LoggerFactory.getLogger(AsyncPoolTest.class);
	private final int maxRetries = 10;
	
	@Test
	public void testAsyncPool() {
		ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
		try {
			FutureTask<Socket> initTask = new FutureTask<>(new Callable<Socket>(){
				
				private int retry = 0;
				
				@Override
				public Socket call() throws Exception {
					try {
						LOG.info("trying to connect with attempt {}", retry);
						return new Socket("127.0.0.1", 3310);
					} catch(Exception e) {
						LOG.info("connection failed.");
						// if we can't connect, wait and try again
						if(retry < maxRetries) {
							retry++;
							LOG.info("retry attempt {}", retry);
							Thread.sleep(1000);
							return call();
						} else {
							LOG.info("connection can not be created. enough is enough.");
							throw e;
						}
						
					}
				}
			});
			executor.execute(initTask);
			Object v;
			try {
				v = initTask.get();
				Assert.assertNotNull(v);
			} catch (InterruptedException ex) {
				// shutting down
			} catch (ExecutionException ex) {
				Class<? extends Throwable> exType = ex.getCause().getClass();
				Assert.assertTrue(SocketException.class.isAssignableFrom(exType), "expected a socket exception or a subtype of socket exception");
			}
		} finally {
			executor.shutdown();
		}
	}
	
	
}
