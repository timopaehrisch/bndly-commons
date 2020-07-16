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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportPackageUtil {
	
	private static int S_PACKAGE = 0;
	private static int S_META_DATA_CONTENT = 2;
	private static int S_META_DATA_CONTENT_QUOTED = 3;
	private static int S_NEXT = 4;
	
	public static interface PackageConsumer {
		void consumePackage(String packageName, List<String> metaData);
	}
	private static final Pattern VERSION_PATTERN = Pattern.compile("^version=(?:([^\\r\\n\\t\\f\\v \"]+)|(?:\"([^\\r\\n\\t\\f\\v \"]+)\"))$");
	
	public static String getVersionFromMetaData(List<String> metaData) {
		if (metaData == null || metaData.isEmpty()) {
			return null;
		}
		for (String md : metaData) {
			Matcher matcher = VERSION_PATTERN.matcher(md);
			if (matcher.matches()) {
				String version = matcher.group(1);
				if (version == null) {
					version = matcher.group(2);
				}
				return version;
			}
		}
		return null;
	}
	
	public static void parse(String exportPackageHeader, PackageConsumer packageConsumer) {
		int state = S_PACKAGE;
		int packageStart = 0;
		int metaDataStart = -1;
		String pckName = null;
		List<String> metaData = new ArrayList<>();
		for (int i = 0; i < exportPackageHeader.length(); i++) {
			char c = exportPackageHeader.charAt(i);
			if (state == S_PACKAGE) {
				if (c == ';') {
					pckName = exportPackageHeader.substring(packageStart, i).trim();
					metaDataStart = i + 1;
					state = S_META_DATA_CONTENT;
				} else if (c == ',') {
					pckName = exportPackageHeader.substring(packageStart, i).trim();
					packageConsumer.consumePackage(pckName, metaData);
					packageStart = i + 1;
				} else {
					// consume
				}
			} else if (state == S_META_DATA_CONTENT) {
				if (c == '"') {
					state = S_META_DATA_CONTENT_QUOTED;
				} else if (c == ';') {
					metaData.add(exportPackageHeader.substring(metaDataStart, i));
					metaDataStart = i + 1;
					state = S_META_DATA_CONTENT;
				} else if (c == ',') {
					metaData.add(exportPackageHeader.substring(metaDataStart, i));
					packageConsumer.consumePackage(pckName, metaData);
					packageStart = i + 1;
					metaDataStart = -1;
					metaData.clear();
					state = S_PACKAGE;
				} else {
					// consume
				}
			} else if (state == S_META_DATA_CONTENT_QUOTED) {
				if (c == '"') {
					state = S_META_DATA_CONTENT;
				} else {
					// consume
				}
			} else {
				throw new IllegalStateException("can not parse");
			}
		}
		if (metaDataStart != -1) {
			metaData.add(exportPackageHeader.substring(metaDataStart));
		}
		if (state == S_PACKAGE) {
			pckName = exportPackageHeader.substring(packageStart).trim();
			if (pckName.isEmpty()) {
				pckName = null;
			}
		}
		if (pckName != null) {
			packageConsumer.consumePackage(pckName, metaData);
		}
	}
}
