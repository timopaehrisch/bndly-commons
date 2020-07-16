package org.bndly.common.ip2location;

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

import java.math.BigInteger;
import java.net.InetAddress;

/**
 * The IPBasedGeoLocator determines the GeoLocation based on a provided IP address.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface IPBasedGeoLocator {
	
	/**
	 * Takes an IP address as a sequency of bits and resolves it to a geo location. If no geo location can be resolved, null will be returned.
	 * @param ip The IP address to look up in the index of IP to geo location mappings.
	 * @return A geo location or null, if no location can be determined.
	 */
	IPGeoLocation getGeoLocationByIPAddress(BigInteger ip);
	
	/**
	 * Takes an IP address as a String an converts it to a {@link InetAddress}. The address is then passed to {@link #getGeoLocationByIPAddress(java.math.BigInteger) }.
	 * @param ipAddressString The IP address to look up in the index of IP to geo location mappings.
	 * @return A geo location or null, if no location can be determined or if the provided IP address string can not be converted to a valid address.
	 */
	IPGeoLocation getGeoLocationByIPAddress(String ipAddressString);
}
