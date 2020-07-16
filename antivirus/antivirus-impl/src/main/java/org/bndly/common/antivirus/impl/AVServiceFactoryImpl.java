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
import org.bndly.common.antivirus.api.AVService;
import org.bndly.common.antivirus.api.AVServiceFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class AVServiceFactoryImpl implements AVServiceFactory {
	private static final Logger LOG = LoggerFactory.getLogger(AVServiceFactoryImpl.class);
	private int connectTimeout = 3000;
	private int socketTimeout = 3000;
	private int maxConnectionsPerInstance = 10;
	private String charset = "UTF-8";
	private int defaultChunkSize = 2048;
	private long defaultTimeoutMillis = -1;
	private int maxCommandRetries = 0;

	private final List<CreatedAVService> createdServices = new ArrayList<>();
	private final Map<SocketKey, Pool<LazySocket>> socketPools = new ConcurrentHashMap<>();
	
	private static final SocketExceptionHandler DEFAULT_BROKEN_PIPE_HANDLER = new SocketExceptionHandler() {

		@Override
		public Object handleConnectionReset(SocketException exception, Socket socket, ResponseReader responseReader) {
			throw new AVException("connection was reset by remote");
		}

		@Override
		public Object handleBrokenPipe(SocketException exception, Socket socket, ResponseReader responseReader) {
			throw new AVException("pipe did break, because remote did close the connection");
		}
	};

	private static interface PoolAccess<E> {
		E access(Map<SocketKey, Pool<LazySocket>> pools);
	}
	
	private static final class SocketKey {
		private final InetAddress address;
		private final int port;

		public SocketKey(InetAddress address, int port) {
			if (address == null) {
				throw new IllegalArgumentException("address is not allowed to be null");
			}
			this.address = address;
			this.port = port;
		}

		public int getPort() {
			return port;
		}

		public InetAddress getAddress() {
			return address;
		}

		@Override
		public int hashCode() {
			int hash = 5;
			hash = 79 * hash + Objects.hashCode(this.address);
			hash = 79 * hash + this.port;
			return hash;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final SocketKey other = (SocketKey) obj;
			if (!Objects.equals(this.address, other.address)) {
				return false;
			}
			if (this.port != other.port) {
				return false;
			}
			return true;
		}
		
	}
	
	private static final class CreatedAVService {

		private final AVService service;
		private final SocketKey socketKey;

		public CreatedAVService(AVService service, SocketKey socketKey) {
			this.service = service;
			this.socketKey = socketKey;
		}

		public AVService getService() {
			return service;
		}

		public InetAddress getInetAddress() {
			return socketKey.getAddress();
		}

		public int getPort() {
			return socketKey.getPort();
		}

		public SocketKey getSocketKey() {
			return socketKey;
		}

	}

	public void activate() {

	}

	public void deactivate() {
		accessPools(new PoolAccess<Object>() {

			@Override
			public Object access(Map<SocketKey, Pool<LazySocket>> pools) {
				Iterator<Pool<LazySocket>> iter = pools.values().iterator();
				while (iter.hasNext()) {
					Pool<LazySocket> next = iter.next();
					next.destruct();
					iter.remove();
				}
				return null;
			}
		});
		synchronized (createdServices) {
			createdServices.clear();
		}
	}

	private synchronized <E> E accessPools(PoolAccess<E> access) {
		return access.access(socketPools);
	}
	
	@Override
	public AVService createAVService(InetAddress address, int port) {
		SocketKey key = new SocketKey(address, port);
		SocketManager sr = createSocketRunner(key);
		AVService service = new AVServiceImpl(sr, charset, defaultChunkSize, defaultTimeoutMillis);
		CreatedAVService cs = new CreatedAVService(service, key);
		synchronized (createdServices) {
			createdServices.add(cs);
		}
		return service;
	}

	private SocketManager createSocketRunner(final SocketKey key) {
		return new SocketManager() {
			
			@Override
			public <E> E runOnSingleUseSocket(SocketManager.Callback<E> callback) throws AVScanException, IOException {
				Socket socket = null;
				try {
					socket = createFreshSocket(key);
					LOG.info("invoking socket callback on a single-use socket");
					return (E) callback.runOnSocket(socket, DEFAULT_BROKEN_PIPE_HANDLER);
				} finally {
					if (socket != null) {
						try {
							socket.close();
						} catch (IOException e) {
							LOG.error("failed to close socket: " + e.getMessage(), e);
						}
					}
				}
			}

			@Override
			public LazySocket take() {
				try {
					LazySocket socket = waitForSocket(key);
					if (socket == null) {
						LOG.error("could not retrieve a socket. hence the socket callback will not be invoked.");
						throw new AVScanException("could not obtain a socket");
					}
					return socket;
				} catch (PoolExhaustedException ex) {
					throw new AVScanException("could not obtain a socket", ex);
				}
			}

			@Override
			public LazySocket take(long timeoutMillis) {
				try {
					LazySocket socket = waitForSocket(key, timeoutMillis);
					if (socket == null) {
						LOG.error("could not retrieve a socket. hence the socket callback will not be invoked.");
						throw new AVScanException("could not obtain a socket");
					}
					return socket;
				} catch (PoolExhaustedException ex) {
					throw new AVScanException("could not obtain a socket", ex);
				}
			}

			@Override
			public void release(LazySocket socket) {
				if (socket.isRealSocketClosed()) {
					LOG.warn("a closed socket was released. the socket will be dropped as soon as it will be validated.");
				} else {
					socket.destruct();
				}
				returnSocket(socket, key);
			}

		};
	}

	private LazySocket createLazySocket(final SocketKey key) {
		return new LazySocket() {
			Socket sock = null;
			
			@Override
			public Socket getRealSocket() {
				if (sock == null) {
					sock = createFreshSocket(key);
				}
				return sock;
			}

			@Override
			public boolean isRealSocketClosed() {
				if (sock == null) {
					return false;
				}
				return sock.isClosed();
			}

			@Override
			public void destruct() {
				if (sock != null) {
					try {
						if (!sock.isClosed()) {
							if (LOG.isInfoEnabled()) {
								LOG.info("closing socket to {}:{}", key.getAddress().toString(), key.getPort());
							}
							sock.close();
						}
					} catch (IOException ex) {
						LOG.error("failed to close socket to {}:{}: " + ex.getMessage(), new Object[]{key.getAddress().toString(), key.getPort()}, ex);
						throw new IllegalStateException("could not close socket: " + ex.getMessage(), ex);
					} finally {
						sock = null;
					}
				}
			}
			
			
		};
	}
	
	private Socket createFreshSocket(SocketKey key) {
		// open the socket.
		if (LOG.isInfoEnabled()) {
			LOG.info("creating socket to {}:{}", key.getAddress().toString(), key.getPort());
		}
		Socket socket;
		socket = new Socket();
		try {
			socket.connect(new InetSocketAddress(key.getAddress(), key.getPort()), connectTimeout);
		} catch (IOException ex) {
			LOG.error("could not connect to {}:{}: " + ex.getMessage(), key.getAddress().toString(), key.getPort(), ex);
			throw new AVException("could not connect socket: " + ex.getMessage(), ex);
		}
		try {
			socket.setSoTimeout(socketTimeout);
		} catch (SocketException ex) {
			LOG.error("could not set timeout {} on socket to {}:{}: " + ex.getMessage(), new Object[]{socketTimeout, key.getAddress().toString(), key.getPort()}, ex);
			throw new AVException("could not set timeout on socket: " + ex.getMessage(), ex);
		}
		return socket;
	}
	
	private LazySocket waitForSocket(final SocketKey key) throws PoolExhaustedException {
		Pool<LazySocket> pool = getPoolForKey(key);
		return pool.get();
	}
	
	private LazySocket waitForSocket(final SocketKey key, long timeoutMillis) throws PoolExhaustedException {
		Pool<LazySocket> pool = getPoolForKey(key);
		return pool.get(timeoutMillis);
	}

	private Pool<LazySocket> getPoolForKey(final SocketKey key) {
		if (LOG.isInfoEnabled()) {
			LOG.info("requesting pool with sockets to {}:{}", key.getAddress().toString(), key.getPort());
		}
		Pool<LazySocket> pool = accessPools(new PoolAccess<Pool<LazySocket>>() {
			
			@Override
			public Pool<LazySocket> access(Map<SocketKey, Pool<LazySocket>> pools) {
				Pool<LazySocket> pool = pools.get(key);
				if (pool == null) {
					LOG.info("pool did not exist yet. creating a new one.");
					// create a pool
					pool = new Pool<LazySocket>() {

						@Override
						protected LazySocket createItem() {
							return createLazySocket(key);
						}

						@Override
						protected void destroyItem(LazySocket item) {
							item.destruct();
						}
					};
					pool.setMaxSize(maxConnectionsPerInstance);
					pool.init();
					pools.put(key, pool);
					LOG.info("pool created and initialized.");
				}
				return pool;
			}
		});
		return pool;
	}
	
	private void returnSocket(LazySocket socket, SocketKey key) {
		if (LOG.isInfoEnabled()) {
			LOG.info("returning socket {}:{} to pool", key.getAddress().toString(), key.getPort());
		}
		Pool<LazySocket> pool = getPoolForKey(key);
		pool.put(socket);
	}

	@Override
	public void destroyAVService(AVService service) {
		synchronized (createdServices) {
			Iterator<CreatedAVService> iter = createdServices.iterator();
			while (iter.hasNext()) {
				CreatedAVService next = iter.next();
				if (next.getService() == service) {
					iter.remove();
				}
			}
		}
	}

	@Override
	public void setSocketTimeout(int socketTimeout) {
		this.socketTimeout = socketTimeout;
	}

	@Override
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setMaxConnectionsPerInstance(int maxConnectionsPerInstance) {
		this.maxConnectionsPerInstance = maxConnectionsPerInstance;
	}

	public void setCharset(String charset) {
		if (charset == null) {
			throw new IllegalArgumentException("charset is not allowed to be null");
		}
		this.charset = charset;
	}

	public void setDefaultChunkSize(int defaultChunkSize) {
		this.defaultChunkSize = defaultChunkSize;
	}

	public void setMaxCommandRetries(int maxCommandRetries) {
		this.maxCommandRetries = maxCommandRetries;
	}

	public void setDefaultTimeoutMillis(long defaultTimeoutMillis) {
		this.defaultTimeoutMillis = defaultTimeoutMillis;
	}
}
