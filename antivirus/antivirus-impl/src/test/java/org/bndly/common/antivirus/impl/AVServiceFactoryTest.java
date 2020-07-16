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
import org.bndly.common.antivirus.api.AVSizeLimitExceededException;
import org.bndly.common.antivirus.api.ScanResult;
import org.bndly.common.antivirus.api.Session;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class AVServiceFactoryTest {

	private static final Logger LOG = LoggerFactory.getLogger(AVServiceFactoryTest.class);
	private static final String EICAR_DEMO_VIRUS = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*";
	
	private static final int CLAMAV_CONF_MaxConnectionQueueLength=20;
	private static final int CLAMAV_CONF_MaxThreads=10;
	
	private AVServiceFactoryImpl factory;
	private boolean skipTests;
	private String host;
	private int port;

	@BeforeClass
	public void beforeClass() {
		skipTests = System.getProperties().getProperty("test.clamavskip", "true").equals("true");
		host = System.getProperties().getProperty("test.clamavhost", "127.0.0.1");
		String portString = System.getProperties().getProperty("test.clamavport", "3310");
		try {
			port = Integer.valueOf(portString);
		} catch (NumberFormatException e) {
			port = 3310;
		}
		
	}
	
	@BeforeMethod
	public void before() {
		if(!skipTests) {
			factory = new AVServiceFactoryImpl();
			factory.setConnectTimeout(1000);
			factory.setSocketTimeout(60 * 1000);
			factory.setMaxConnectionsPerInstance(1);
		}
	}
	
	@AfterMethod
	public void after() {
		factory = null;
	}
	
	@Test
	public void testPing() throws UnknownHostException, IOException {
		if(!skipTests) {
			factory.activate();
			AVService service = createAVService();
			boolean alive = service.ping();
			Assert.assertTrue(alive, "service is not alive");
			alive = service.ping();
			Assert.assertTrue(alive, "service is not alive when requesting a second time");
			factory.deactivate();
		}
	}

	private AVService createAVService() throws UnknownHostException {
		AVService service = factory.createAVService(Inet4Address.getByName(host), port);
		return service;
	}

	@Test
	public void testSession() throws UnknownHostException, IOException {
		if(!skipTests) {
			factory.activate();
			AVService service = createAVService();
			try (Session session = service.startSession()) {
				// do nothing. session is auto closed.
			}
			factory.deactivate();
		}
	}

	@Test
	public void testMultipleSessions() throws UnknownHostException, IOException {
		if(!skipTests) {
			factory.setMaxConnectionsPerInstance(2);
			factory.activate();
			AVService service = createAVService();
			try (Session session = service.startSession()) {
				try (Session session2 = service.startSession()) {
					// do nothing. session is auto closed.
				}
			}
			factory.deactivate();
		}
	}

	@Test
	public void testMultipleSessionsWithTimeout() throws UnknownHostException, IOException {
		if(!skipTests) {
			factory.setMaxConnectionsPerInstance(2);
			factory.setDefaultTimeoutMillis(1000);
			factory.activate();
			AVService service = createAVService();
			try (Session session = service.startSession()) {
				try (Session session2 = service.startSession()) {
					try (Session session3 = service.startSession()) {
						// do nothing. session is auto closed.
						Assert.fail("session 3 should not be available, because 1 and 2 are still active");
					} catch (AVException e) {
						// we expected this one
					}
				}
			}
			factory.deactivate();
		}
	}
	
	

	@Test
	public void testWithRoland() throws UnknownHostException, IOException {
		if (!skipTests) {
			factory.setMaxConnectionsPerInstance(1);
			factory.setDefaultTimeoutMillis(1000);
			factory.activate();
			try {
				AVService service = createAVService();
				try (Session session = service.startSession()) {
				}
				
				// socket is returned to pool now.
				// try again
				try (Session session = service.startSession()) {
				}
				
			} finally {
				factory.deactivate();
			}
		}
	}

	@Test
	public void testScan() throws UnknownHostException, IOException, InterruptedException {
		if(!skipTests) {
			factory.activate();
			AVService service = createAVService();
			try (Session session = service.startSession()) {
				ScanResult result = session.scan("aaaaa".getBytes("UTF-8"));
				Assert.assertNotNull(result);
				Assert.assertEquals(result.getRequestNumber(), Long.valueOf(1));
				Assert.assertTrue(result.isOK(), "expected result to be ok.");
			} finally {
				factory.deactivate();
			}
		}
	}
	
	@Test
	public void testScanBrokenInputStream() throws UnknownHostException, IOException, InterruptedException {
		if(!skipTests) {
			factory.activate();
			AVService service = createAVService();
			try (Session session = service.startSession()) {
				byte[] buf = "aaaa".getBytes("UTF-8");
				InputStream is = new FilterInputStream(new ByteArrayInputStream(buf)) {

					@Override
					public int read() throws IOException {
						int r = super.read();
						throwExceptionAtEnd(r);
						return r;
					}

					@Override
					public int read(byte[] b, int off, int len) throws IOException {
						int r = super.read(b, off, len);
						throwExceptionAtEnd(r);
						return r;
					}
					
					private void throwExceptionAtEnd(int r) throws IOException {
						if(r < 0) {
							throw new IOException("this is a test exception");
						}
					}
					
				};
				
				session.scan(is);
				Assert.fail("expected an exception, because the input stream is broken");
			} catch(AVException e) {
				Assert.assertEquals(e.getCause().getClass(), IOException.class);
				// this one is expected
			} finally {
				factory.deactivate();
			}
		}
	}

	@Test
	public void testScanVirus() throws UnknownHostException, IOException {
		if(!skipTests) {
			factory.activate();
			AVService service = createAVService();
			try (Session session = service.startSession()) {
				ScanResult result = session.scan(EICAR_DEMO_VIRUS.getBytes("UTF-8"));
				Assert.assertNotNull(result);
				Assert.assertEquals(result.getRequestNumber(), Long.valueOf(1));
				Assert.assertFalse(result.isOK(), "expected result to be not ok.");
				Assert.assertEquals(result.getFoundSignature(), "Eicar-Test-Signature");
			} finally {
				factory.deactivate();
			}
		}
	}

	@Test
	public void testScanTooMuchData() throws UnknownHostException, IOException {
		if(!skipTests) {
			factory.activate();
			AVService service = createAVService();
			int sizeLimit = 30 * 1024 * 1024; // 30MB
			ByteArrayOutputStream bos = new ByteArrayOutputStream(sizeLimit);
			for (int i = 0; i < sizeLimit; i++) {
				int b = i % 2 + 1;
				bos.write(b);
			}
			try (Session session = service.startSession()) {
				try {
					session.scan(bos.toByteArray());
					Assert.fail("expected a AVSizeLimitExceededException");
				} catch (AVSizeLimitExceededException exception) {
					// expected the unexpected
				}
			} finally {
				factory.deactivate();
			}
		}
	}
	
	@Test
	public void testScanConcurrentlyWithMultipleServicesOnOneConnection() throws UnknownHostException, IOException {
		testScanConcurrentlyWithMultipleServices(20, 1, 10); // 10ms
	}
	
	@Test
	public void testScanConcurrentlyWithMultipleServicesOnEqualManyConnections() throws UnknownHostException, IOException {
		testScanConcurrentlyWithMultipleServices(CLAMAV_CONF_MaxConnectionQueueLength, CLAMAV_CONF_MaxConnectionQueueLength, 10); // 10ms
	}
	
	@Test
	public void testScanConcurrentlyWithMultipleServicesOnMoreConnectionsThanClamAVProvides() throws UnknownHostException, IOException {
		// NOTE: the max connection queue length is a queue length in clamav when connections are made
		// when the queue is full, no new connections can be made.
		// the queue is used, when all threads of clamav are in use.
		// this means, that this test has to use more java threads than threads + queue size in order to fail to connect.
		int number = CLAMAV_CONF_MaxConnectionQueueLength+CLAMAV_CONF_MaxThreads;
		// this number will use all threads and the entire connection queue.
		testScanConcurrentlyWithMultipleServices(number, number, 10); // 10ms
	}
	
	@Test
	public void testScanConcurrentlyWithMultipleServicesOnMoreConnectionsThanClamAVHandle() throws UnknownHostException, IOException {
		// NOTE: the max connection queue length is a queue length in clamav when connections are made
		// when the queue is full, no new connections can be made.
		// the queue is used, when all threads of clamav are in use.
		// this means, that this test has to use more java threads than threads + queue size in order to fail to connect.
		int number = CLAMAV_CONF_MaxConnectionQueueLength+CLAMAV_CONF_MaxThreads;
		// this number will use all threads and more than the entire connection queue.
		number *= 2;
		testScanConcurrentlyWithMultipleServices(number, number, 100);  // 100ms
	}
	
	private void testScanConcurrentlyWithMultipleServices(int numberOfConcurrentThreads, int concurrentConnections, final int sleepTime) throws UnknownHostException, IOException {
		if(!skipTests) {
			try {
				factory.setMaxConnectionsPerInstance(concurrentConnections);
				factory.activate();
				
				final String testData = "abcdefg";
				
				final Exception[] exceptions = new Exception[numberOfConcurrentThreads];
				Thread[] threads = new Thread[numberOfConcurrentThreads];
				
				for (int i = 0; i < threads.length; i++) {
					final int index = i;
					threads[i] = new Thread(new Runnable() {

						@Override
						public void run() {
							try {
								AVService service = createAVService();
								try (Session session = service.startSession()) {
									ScanResult result = session.scan(new SlowStringInputStream(testData, "UTF-8", sleepTime));
									Assert.assertTrue(result.isOK(), "expected scan result to be ok");
								}
							} catch (Exception ex) {
								LOG.error("exception in execution.", ex);
								exceptions[index] = ex;
							}
						}
					});
				}
				for (int i = 0; i < threads.length; i++) {
					Thread thread = threads[i];
					thread.start();
				}
				for (int i = 0; i < threads.length; i++) {
					Thread thread = threads[i];
					thread.join();
				}
				int succeededScans = 0;
				for (Exception exception : exceptions) {
					if(exception != null) {
						Assert.assertEquals(exception.getMessage(), "session could not be started");
					} else {
						succeededScans++;
					}
				}
				Assert.assertTrue(CLAMAV_CONF_MaxConnectionQueueLength <= succeededScans, "expected at least MaxConnectionQueueLength succeeded scans.");
			} catch (InterruptedException ex) {
				Assert.fail("could not join concurrent av scan threads", ex);
			} finally {
				factory.deactivate();
			}
		}
	}

}
