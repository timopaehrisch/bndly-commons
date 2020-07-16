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

import org.bndly.common.lang.StopWatch;
import org.bndly.shop.common.csv.parsing.CSVDataHandler;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class EntryCreatingCSVDataHandler implements CSVDataHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(EntryCreatingCSVDataHandler.class);

	private static final long IP_START = 0;
	private static final long IP_END = 1;
	private static final long COUNTRY_CODE = 2;
	private static final long COUNTRY_NAME = 3;
	private static final long STATE = 4;
	private static final long CITY = 5;
	private static final long LAT = 6;
	private static final long LNG = 7;
	
	private final Path get;
	private final IPBasedGeoLocatorImpl.Entry[] entries;
	String ipStart;
	String ipEnd;
	String country;
	String countryCode;
	String state;
	String city;
	String lat;
	String lng;
	private long count;
	private StopWatch stopWatch;
	private StopWatch intermediateStopWatch;

	public EntryCreatingCSVDataHandler(Path get, IPBasedGeoLocatorImpl.Entry[] entries) {
		this.get = get;
		this.entries = entries;
	}

	@Override
	public void documentOpened() {
		stopWatch = new StopWatch().start();
		intermediateStopWatch = new StopWatch().start();
		LOG.info("started to parse the CSV at '{}'", get);
	}

	@Override
	public void rowOpened(long index) {
		ipStart = null;
		ipEnd = null;
		country = null;
		city = null;
		lat = null;
		lng = null;
	}

	@Override
	public void value(long index, String value, boolean quoted) {
		if (IP_START == index) {
			ipStart = value;
		} else if (IP_END == index) {
			ipEnd = value;
		} else if (COUNTRY_CODE == index) {
			countryCode = value;
		} else if (COUNTRY_NAME == index) {
			country = value;
		} else if (STATE == index) {
			state = value;
		} else if (CITY == index) {
			city = value;
		} else if (LAT == index) {
			lat = value;
		} else if (LNG == index) {
			lng = value;
		}
	}

	@Override
	public void rowClosed(long index) {
		if (count == Long.MAX_VALUE) {
			throw new IllegalStateException("too many rows in CSV");
		}
		IPBasedGeoLocatorImpl.Entry entry = new IPBasedGeoLocatorImpl.Entry(new BigInteger(ipStart), new BigInteger(ipEnd), Double.valueOf(lat), Double.valueOf(lng), city, country, countryCode, state);
		entries[(int) count] = entry;
		// OR the range elements with s and e
		count++;
		if (count % 10000 == 0) {
			intermediateStopWatch.stop();
			LOG.info("parsed 10000 entries in {}ms", intermediateStopWatch.getMillis());
			intermediateStopWatch.reset();
			intermediateStopWatch.start();
		}
	}

	@Override
	public void documentClosed() {
		stopWatch.stop();
		LOG.info("finished parsing the CSV at '{}' in {}ms", get, stopWatch.getMillis());
		LOG.info("sorting the parsed data now");
		Arrays.sort(entries, IPBasedGeoLocatorImpl.Entry.IP_RANGE_COMPARATOR);
		LOG.info("sorted the parsed data");
	}
	
}
