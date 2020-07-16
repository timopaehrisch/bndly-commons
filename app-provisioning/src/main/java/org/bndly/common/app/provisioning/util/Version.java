package org.bndly.common.app.provisioning.util;

/*-
 * #%L
 * App Provisioning
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

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Version implements Comparable<Version> {

	private final int[] numerics;
	private final String suffix;

	private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)*)?(\\S*)");
	private static final int[] EMPTY_NUMBERS = new int[]{};
	private static final String EMPTY_SUFFIX = "";
	
	public static Version from(String versionAsString) {
		Matcher matcher = NUMBER_PATTERN.matcher(versionAsString);
		if (matcher.matches()) {
			String numberPart = matcher.group(1);
			int[] n;
			if (numberPart != null && !numberPart.isEmpty()) {
				String[] numbers = numberPart.split("\\.");
				n = new int[numbers.length];
				for (int i = 0; i < numbers.length; i++) {
					n[i] = Integer.valueOf(numbers[i]);
				}
			} else {
				n = EMPTY_NUMBERS;
			}
			String suffix = matcher.group(2);
			if (suffix != null && suffix.isEmpty()) {
				suffix = EMPTY_SUFFIX;
			}
			return new Version(n, suffix);
		} else {
			throw new IllegalArgumentException("can not process version: " + versionAsString);
		}
	}
	
	private Version(int[] numerics, String suffix) {
		this.numerics = numerics;
		this.suffix = suffix;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < numerics.length; i++) {
			int numeric = numerics[i];
			if (i > 0) {
				sb.append(".");
			}
			sb.append(numeric);
		}
		if (suffix != null) {
			sb.append(suffix);
		}
		return sb.toString();
	}

	public String toOsgiString() {
		if (numerics.length == 0) {
			return "";
		} else {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < numerics.length; i++) {
				int numeric = numerics[i];
				if (i > 0) {
					sb.append(".");
				}
				sb.append(numeric);
			}
			return sb.toString();
		}
	}
	
	@Override
	public int hashCode() {
		int hash = 5;
		hash = 23 * hash + Arrays.hashCode(this.numerics);
		hash = 23 * hash + Objects.hashCode(this.suffix);
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Version other = (Version) obj;
		if (!Objects.equals(this.suffix, other.suffix)) {
			return false;
		}
		if (!Arrays.equals(this.numerics, other.numerics)) {
			return false;
		}
		return true;
	}

	@Override
	public int compareTo(Version o) {
		// comare suffixes
		int ls = o.numerics.length < numerics.length ? o.numerics.length : numerics.length;
		int ll = o.numerics.length > numerics.length ? o.numerics.length : numerics.length;
		for (int i = 0; i < ls; i++) {
			if (o.numerics[i] != numerics[i]) {
				return numerics[i] - o.numerics[i];
			}
		}
		for (int i = ls; i < ll; i++) {
			if (o.numerics.length > i) {
				if (o.numerics[i] != 0) {
					return -1;
				}
			} else if (numerics.length > i) {
				if (numerics[i] != 0) {
					return 1;
				}
			}
		}
		// so far we are equal
		if (suffix == o.suffix) {
			return 0;
		} else {
			return suffix.compareTo(o.suffix);
		}
	}
	
}
