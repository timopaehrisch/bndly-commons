package org.bndly.common.ip2location.impl;

/*-
 * #%L
 * IP2Location
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

import org.bndly.common.ip2location.IPBasedGeoLocator;
import org.bndly.common.ip2location.IPGeoLocation;
import org.bndly.common.lang.StopWatch;
import org.bndly.shop.common.csv.parsing.CSVDataHandler;
import org.bndly.shop.common.csv.parsing.CSVParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = IPBasedGeoLocator.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = IPBasedGeoLocatorImpl.Configuration.class)
public class IPBasedGeoLocatorImpl implements IPBasedGeoLocator {

	private static final Logger LOG = LoggerFactory.getLogger(IPBasedGeoLocatorImpl.class);

	@ObjectClassDefinition
	public @interface Configuration {

		@AttributeDefinition(
				name = "Data location",
				description = "Path to the IP2Location CSV with the address mappings"
		)
		String dataLocation();

		@AttributeDefinition(
				name = "Column index IP start",
				description = "Index of the column in the CSV, that contains the start IP of the IP range"
		)
		long columnIndexIPStart() default 0;

		@AttributeDefinition(
				name = "Column index IP end",
				description = "Index of the column in the CSV, that contains the end IP of the IP range"
		)
		long columnIndexIPEnd() default 1;

		@AttributeDefinition(
				name = "Column index latitude",
				description = "Index of the column in the CSV, that contains the latitude of the IP range location"
		)
		long columnIndexLatitude() default 2;

		@AttributeDefinition(
				name = "Column index longitude",
				description = "Index of the column in the CSV, that contains the longitude of the IP range location"
		)
		long columnIndexLongitude() default 3;

	}
	
	private String dataLocation;
	private ExecutorService executorService;
	private Future<byte[]> init;
	private long columnIndexIPStart;
	private long columnIndexIPEnd;
	private long columnIndexLatitude;
	private long columnIndexLongitude;

	static class Entry implements IPGeoLocation {

		static Comparator<Entry> IP_RANGE_COMPARATOR = new Comparator<Entry>() {

			@Override
			public int compare(Entry o1, Entry o2) {
				return o1.ipStart.subtract(o2.ipStart).signum();
			}

		};

		private final BigInteger ipStart;
		private final BigInteger ipEnd;
		private final double lat;
		private final double lng;
		private final String city;
		private final String country;
		private final String countryCode;
		private final String state;

		public Entry(BigInteger ipStart, BigInteger ipEnd, double lat, double lng, String city, String country, String countryCode, String state) {
			this.ipStart = ipStart;
			this.ipEnd = ipEnd;
			this.lat = lat;
			this.lng = lng;
			this.city = city;
			this.country = country;
			this.countryCode = countryCode;
			this.state = state;
		}

		@Override
		public double getLatitude() {
			return lat;
		}

		@Override
		public double getLongitude() {
			return lng;
		}

		@Override
		public String toString() {
			return lat + "," + lng;
		}
	}

	@Activate
	public void activate(Configuration configuration) {
		dataLocation = configuration.dataLocation();
		columnIndexIPStart = configuration.columnIndexIPStart();
		columnIndexIPEnd = configuration.columnIndexIPEnd();
		columnIndexLatitude = configuration.columnIndexLatitude();
		columnIndexLongitude = configuration.columnIndexLongitude();
		initData();
	}

	private static class RowCountingCSVDataHandler implements CSVDataHandler {
		private long row = 0;
		@Override
		public void documentOpened() {
		}

		@Override
		public void rowOpened(long l) {
		}

		@Override
		public void value(long l, String string, boolean bln) {
		}

		@Override
		public void rowClosed(long l) {
			row++;
		}

		@Override
		public void documentClosed() {
		}

		public long getRowCount() {
			return row;
		}
		
	}
	
	public void initData() {
		if (dataLocation == null) {
			return;
		}
		final Path get = Paths.get(dataLocation);
		if (!Files.isRegularFile(get)) {
			LOG.error("could not find ip2location data at {}", dataLocation);
			return;
		}
		if (executorService == null) {
			executorService = Executors.newSingleThreadExecutor();
		}
		init = executorService.submit(new Callable<byte[]>() {
			@Override
			public byte[] call() throws IOException {
				// first we count the entries, so we don't waste time when writing the parsed results to an in memory array
				RowCountingCSVDataHandler rowCountingCSVDataHandler = new RowCountingCSVDataHandler();
				try (InputStream is = Files.newInputStream(get, StandardOpenOption.READ)) {
					new CSVParser().parse(new BufferedInputStream(is), rowCountingCSVDataHandler);
				}
				LOG.info("the CSV at '{}' contains {} entries", get, rowCountingCSVDataHandler.getRowCount());
				long byteArraySize = rowCountingCSVDataHandler.getRowCount() * 24; // 24 byte per entry
				byte[] bytes = new byte[(int)byteArraySize];
				try (InputStream is = Files.newInputStream(get, StandardOpenOption.READ)) {
					StopWatch sw = new StopWatch().start();
					System.out.println("parsing to bytes...");
					new CSVParser().parse(new BufferedInputStream(is), new ByteBasedCSVDataHandler(bytes, columnIndexIPStart, columnIndexIPEnd, columnIndexLatitude, columnIndexLongitude));
					System.out.println("parsing to bytes done in " + sw.stop().getMillis() + "ms.");
				}
				return bytes;
			}

		});
	}

	@Deactivate
	public void deactivate() {
		if (init != null) {
			init.cancel(true);
			init = null;
		}
		if (executorService != null) {
			executorService.shutdown();
			executorService = null;
		}
	}

	@Override
	public IPGeoLocation getGeoLocationByIPAddress(BigInteger ip) {
		if (init == null) {
			LOG.warn("ip2location could not be loaded during activation. therefore {} can not be resolved to a location", ip);
			return null;
		}
		try {
			byte[] entries = init.get();
			long longValue = ip.longValue();
			int ipInt = (int) ((longValue & 0xff000000) >> 24);
			ipInt = (ipInt << 8) | (int) ((longValue & 0x00ff0000) >> 16);
			ipInt = (ipInt << 8) | (int) ((longValue & 0x0000ff00) >> 8);
			ipInt = (ipInt << 8) | (int) (longValue & 0x000000ff);
			return search(entries, 0, entries.length/24, ipInt);
		} catch (InterruptedException ex) {
			return null;
		} catch (ExecutionException ex) {
			// log the execution exception.
			LOG.error("could not get geo location, because initialization failed", ex);
			return null;
		}
	}
	
	private double toDouble(byte[] bytes, int pos) {
		return ByteBuffer.wrap(bytes, pos, 8).getDouble();
	}
	
	private int toInt(byte[] bytes, int pos) {
		return ((bytes[pos + 0] & 0xff) << 24)
				| ((bytes[pos + 1] & 0xff) << 16)
				| ((bytes[pos + 2] & 0xff) << 8)
				| (bytes[pos + 3] & 0xff);
	}

	private static int compareUnsigned(int x, int y) {
		// by adding MIN_VALUE we perform a "rolling-shift" of the in value.
		// when dealing with an unsigned int, it would be negativ as a signed int. therefore by adding the min value, we bring the int in a natural order.
		x = x + Integer.MIN_VALUE;
		y = y + Integer.MIN_VALUE;
		return (x < y) ? -1 : ((x == y) ? 0 : 1);
	}

	IPGeoLocation search(byte[] bytes, int left, int right, int ip) {
		if (left >= right) {
			return null;
		}
		int pos = left + (right - left) / 2;
		int shift = pos * 24;
		int ipStart = toInt(bytes, shift);
		if (compareUnsigned(ipStart, ip) > 0) {
			// target is left
			return search(bytes, left, pos, ip);
		}
		int ipEnd = toInt(bytes, shift + 4);
		if (compareUnsigned(ipEnd, ip) < 0) {
			// target is right
			return search(bytes, pos + 1, right, ip);
		} else {
			// target is in the range
			final double lat = toDouble(bytes, shift + 8);
			final double lng = toDouble(bytes, shift + 16);
			return new IPGeoLocation() {
				@Override
				public double getLatitude() {
					return lat;
				}

				@Override
				public double getLongitude() {
					return lng;
				}
			};
		}
	}
	
	Entry search(Entry[] entries, int left, int right, BigInteger ip) {
		if (left >= right) {
			return null;
		}
		int pos = left + (right - left) / 2;
		Entry e = entries[pos];
		if (e.ipStart.subtract(ip).signum() == 1) {
			// target is left
			return search(entries, left, pos, ip);
		} else if (e.ipEnd.subtract(ip).signum() == -1) {
			// target is right
			return search(entries, pos + 1, right, ip);
		} else {
			// target is in the range
			return e;
		}
	}

	@Override
	public IPGeoLocation getGeoLocationByIPAddress(String ipAddressString) {
		try {
			InetAddress address = InetAddress.getByName(ipAddressString);
			if (address.isAnyLocalAddress()
					|| address.isLoopbackAddress()
					|| address.isSiteLocalAddress()) {
				return null;
			}

			if (Inet4Address.class.isInstance(address)) {
				return getGeoLocationByIPAddress(new BigInteger(((Inet4Address) address).getAddress()));
			} else if (Inet6Address.class.isInstance(address)) {
				return getGeoLocationByIPAddress(new BigInteger(((Inet6Address) address).getAddress()));
			} else {
				return null;
			}
		} catch (UnknownHostException ex) {
			return null;
		}
	}

	/**
	 * This setter exists only for testing purpose.
	 * @param dataLocation the location of the CSV with the ip data
	 */
	void setDataLocation(String dataLocation) {
		this.dataLocation = dataLocation;
	}

	/**
	 * This setter exists only for testing purpose.
	 * @param columnIndexIPStart index of the ip start column
	 */
	public void setColumnIndexIPStart(long columnIndexIPStart) {
		this.columnIndexIPStart = columnIndexIPStart;
	}

	/**
	 * This setter exists only for testing purpose.
	 * @param columnIndexIPEnd index of the ip end column
	 */
	public void setColumnIndexIPEnd(long columnIndexIPEnd) {
		this.columnIndexIPEnd = columnIndexIPEnd;
	}

	/**
	 * This setter exists only for testing purpose.
	 * @param columnIndexLatitude index of latitude column
	 */
	public void setColumnIndexLatitude(long columnIndexLatitude) {
		this.columnIndexLatitude = columnIndexLatitude;
	}

	/**
	 * This setter exists only for testing purpose.
	 * @param columnIndexLongitude index of longitude column
	 */
	public void setColumnIndexLongitude(long columnIndexLongitude) {
		this.columnIndexLongitude = columnIndexLongitude;
	}
	
}
