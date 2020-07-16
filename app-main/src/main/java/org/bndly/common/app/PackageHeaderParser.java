package org.bndly.common.app;

/*-
 * #%L
 * App Main
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
import java.util.function.Consumer;
import java.util.function.IntConsumer;

class PackageHeaderParser {

	interface PackageDescription {

		String getName();
	}

	STATE state;

	enum STATE {
		NEW_PACKAGE,
		MD_NAME,
		MD_VALUE_SWITCH,
		MD_VALUE_QUOTED,
		MD_VALUE_QUOTED_END,
		MD_VALUE_UNQUOTED,
	}

	int TOKEN_PACKAGE_SEP = ',';
	int TOKEN_META_DATA_SEP = ';';
	int TOKEN_VALUE_ASSIGN = '=';
	int TOKEN_VALUE_QUOTE = '"';

	void parse(String header, Consumer<PackageDescription> consumer) {
		if (header == null) {
			return;
		}
		state = STATE.NEW_PACKAGE;
		PackageParserImpl packageParserImpl = new PackageParserImpl(consumer);
		header.codePoints().forEach(packageParserImpl);
		packageParserImpl.complete();
	}

	private class PackageParserImpl implements IntConsumer {

		private final Consumer<PackageDescription> consumer;

		public PackageParserImpl(Consumer<PackageDescription> consumer) {
			this.consumer = consumer;
		}
		StringBuilder packageNameBuilder = new StringBuilder();
		StringBuilder metaDataBuilder = new StringBuilder();
		List<String> metaData = new ArrayList<>();

		private void notifyConsumer() {
			String name = packageNameBuilder.toString().trim();
			consumer.accept(new PackageDescription() {
				@Override
				public String getName() {
					return name;
				}
			});
			reset();
		}
		
		private void reset() {
			packageNameBuilder = new StringBuilder();
			metaData = new ArrayList<>();
		}

		private void flushMetaData() {
			metaData.add(metaDataBuilder.toString());
			metaDataBuilder = new StringBuilder();
		}

		@Override
		public void accept(int cp) {
			switch (state) {
				case NEW_PACKAGE:
					if (cp == TOKEN_PACKAGE_SEP) {
						// package is complete
						notifyConsumer();
					} else if (cp == TOKEN_META_DATA_SEP) {
						// package is complete, but meta data follows
						state = STATE.MD_NAME;
					} else {
						// character of package name
						packageNameBuilder.appendCodePoint(cp);
					}
					break;
				case MD_NAME:
					if (cp == TOKEN_META_DATA_SEP) {
						// empty meta data can be ignored...
					} else if (cp == TOKEN_VALUE_ASSIGN) {
						// name of meta data is complete
						flushMetaData();
						state = STATE.MD_VALUE_SWITCH;
					} else {
						metaDataBuilder.appendCodePoint(cp);
					}
					break;
				case MD_VALUE_SWITCH:
					if (cp == TOKEN_VALUE_QUOTE) {
						// our value is supposed to be closed by a quote
						state = STATE.MD_VALUE_QUOTED;
					} else {
						// our value is not wrapped in quotes
						metaDataBuilder.appendCodePoint(cp);
						state = STATE.MD_VALUE_UNQUOTED;
					}
					break;
				case MD_VALUE_QUOTED:
					if (cp == TOKEN_VALUE_QUOTE) {
						// our value is complete
						flushMetaData();
						state = STATE.MD_VALUE_QUOTED_END;
					} else {
						// our value is not wrapped in quotes
						metaDataBuilder.appendCodePoint(cp);
					}
					break;
				case MD_VALUE_QUOTED_END:
					if (cp == TOKEN_META_DATA_SEP) {
						// our value is complete
						state = STATE.MD_NAME;
					} else if (cp == TOKEN_PACKAGE_SEP) {
						notifyConsumer();
						state = STATE.NEW_PACKAGE;
					} else {
						// any other character will be ignored
					}
					break;
				case MD_VALUE_UNQUOTED:
					if (cp == TOKEN_META_DATA_SEP) {
						// our value is complete
						flushMetaData();
						state = STATE.MD_NAME;
					} else if (cp == TOKEN_PACKAGE_SEP) {
						flushMetaData();
						notifyConsumer();
						state = STATE.NEW_PACKAGE;
					} else {
						// any other character will be appended as part of the value
						metaDataBuilder.appendCodePoint(cp);
					}
					break;
				default:
					throw new IllegalStateException(state.toString());
			}
		}

		private void complete() {
			switch (state) {
				case NEW_PACKAGE:
					if (packageNameBuilder.length() > 0) {
						// package is complete
						notifyConsumer();
					}
					break;
				case MD_NAME:
					if (metaDataBuilder.length() > 0) {
						// metadata is complete
						flushMetaData();
					}
					notifyConsumer();
					break;
				case MD_VALUE_SWITCH:
					if (metaDataBuilder.length() > 0) {
						// metadata is complete
						flushMetaData();
					}
					notifyConsumer();
					break;
				case MD_VALUE_QUOTED:
					if (metaDataBuilder.length() > 0) {
						// metadata is complete
						flushMetaData();
					}
					notifyConsumer();
					break;
				case MD_VALUE_QUOTED_END:
					if (metaDataBuilder.length() > 0) {
						// metadata is complete
						flushMetaData();
					}
					notifyConsumer();
					break;
				case MD_VALUE_UNQUOTED:
					if (metaDataBuilder.length() > 0) {
						// metadata is complete
						flushMetaData();
					}
					notifyConsumer();
					break;
				default:
					throw new IllegalStateException(state.toString());
			}
		}
	}

}
