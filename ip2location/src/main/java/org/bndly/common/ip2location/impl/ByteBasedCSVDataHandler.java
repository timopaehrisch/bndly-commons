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

import org.bndly.shop.common.csv.parsing.CSVDataHandler;
import java.nio.ByteBuffer;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class ByteBasedCSVDataHandler implements CSVDataHandler {

	private final byte[] bytes;
	
	private final byte[] doubleValueBuffer = new byte[8];
	
	private final long columnIndexIPStart;
	private final long columnIndexIPEnd;
	private final long columnIndexLatitude;
	private final long columnIndexLongitude;

	public ByteBasedCSVDataHandler(byte[] bytes, long columnIndexIPStart, long columnIndexIPEnd, long columnIndexLatitude, long columnIndexLongitude) {
		this.bytes = bytes;
		this.columnIndexIPStart = columnIndexIPStart;
		this.columnIndexIPEnd = columnIndexIPEnd;
		this.columnIndexLatitude = columnIndexLatitude;
		this.columnIndexLongitude = columnIndexLongitude;
	}
	
	String ipStart;
	String ipEnd;
	String lat;
	String lng;
	
	private long count;
	
	@Override
	public void documentOpened() {
		count = 0;
	}

	@Override
	public void rowOpened(long l) {
		ipStart = null;
		ipEnd = null;
		lat = null;
		lng = null;
	}

	@Override
	public void value(long index, String value, boolean bln) {
		if (columnIndexIPStart == index) {
			ipStart = value;
		} else if (columnIndexIPEnd == index) {
			ipEnd = value;
		} else if (columnIndexLatitude == index) {
			lat = value;
		} else if (columnIndexLongitude == index) {
			lng = value;
		}
	}

	@Override
	public void rowClosed(long l) {
		if (ipStart != null && ipEnd != null && lat != null && lng != null) {
			int s = parseUnsignedInt(ipStart);
			int e = parseUnsignedInt(ipEnd);
			double latitutde = Double.parseDouble(lat);
			double longitude = Double.parseDouble(lng);
			writeInt(s, 0);
			writeInt(e, 4);
			writeDouble(latitutde, 8);
			writeDouble(longitude, 16);
			count++;
		}
	}

	@Override
	public void documentClosed() {
	}

	private void writeInt(int intValue, int offset) {
		int shift = (int) (count * 24 + offset);
		bytes[shift] = (byte) ((intValue & 0xFF000000) >> 24);
		bytes[shift + 1] = (byte) ((intValue & 0x00FF0000) >> 16);
		bytes[shift + 2] = (byte) ((intValue & 0x0000FF00) >> 8);
		bytes[shift + 3] = (byte) (intValue & 0x000000FF);
	}

	private void writeDouble(double doubleValue, int offset) {
		ByteBuffer.wrap(doubleValueBuffer).putDouble(doubleValue);
		int shift = (int) (count * 24 + offset);
		System.arraycopy(doubleValueBuffer, 0, bytes, shift, doubleValueBuffer.length);
	}

	private int parseUnsignedInt(String numberString) {
		long ell = Long.parseLong(numberString, 10);
		if ((ell & 0xffff_ffff_0000_0000L) != 0) {
			throw new IllegalStateException("value to big for an integer: " + numberString);
		}
		return (int) ell;
	}
	
}
