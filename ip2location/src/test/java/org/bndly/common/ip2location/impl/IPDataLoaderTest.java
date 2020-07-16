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

import org.bndly.common.ip2location.IPGeoLocation;
import java.io.IOException;
import java.nio.file.Paths;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class IPDataLoaderTest {

	@Test
	public void loadData() throws IOException {
		IPBasedGeoLocatorImpl ipBasedGeoLocatorImpl = new IPBasedGeoLocatorImpl();
		ipBasedGeoLocatorImpl.setDataLocation(Paths.get("src", "test", "resources", "IP2LOCATION-LITE-DB5.CSV").toString());
		ipBasedGeoLocatorImpl.setColumnIndexIPStart(0);
		ipBasedGeoLocatorImpl.setColumnIndexIPEnd(1);
		ipBasedGeoLocatorImpl.setColumnIndexLatitude(6);
		ipBasedGeoLocatorImpl.setColumnIndexLongitude(7);
		ipBasedGeoLocatorImpl.initData();
		
		// IP address of cybercon in cologne 5.147.253.139
		// get the entry, where the ip is in the range
		IPGeoLocation location = ipBasedGeoLocatorImpl.getGeoLocationByIPAddress("1.0.15.255");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getLatitude(), 23.116670);
		Assert.assertEquals(location.getLongitude(), 113.250000);

		location = ipBasedGeoLocatorImpl.getGeoLocationByIPAddress("1.0.15.254");
		Assert.assertNotNull(location);
		Assert.assertEquals(location.getLatitude(), 23.116670);
		Assert.assertEquals(location.getLongitude(), 113.250000);

		// Private IPv4 address spaces https://en.wikipedia.org/wiki/Private_network#Private_IPv4_address_spaces
		location = ipBasedGeoLocatorImpl.getGeoLocationByIPAddress("10.1.2.3");
		Assert.assertEquals(location, null);

		location = ipBasedGeoLocatorImpl.getGeoLocationByIPAddress("172.30.225.225");
		Assert.assertEquals(location, null);

		location = ipBasedGeoLocatorImpl.getGeoLocationByIPAddress("192.168.2.0");
		Assert.assertEquals(location, null);
	}
	
}
