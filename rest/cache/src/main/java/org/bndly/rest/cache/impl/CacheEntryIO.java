package org.bndly.rest.cache.impl;

/*-
 * #%L
 * REST Cache
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

import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.cache.api.CacheEntry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CacheEntryIO {

	public void serialize(CacheEntry cacheEntry, OutputStream outputStream) {
		TarArchiveOutputStream tos = new TarArchiveOutputStream(outputStream);
		
		// like HTTP: first write the meta data, than comes the payload
		StringBuffer sb = new StringBuffer();
		writeMetaData(sb, "ETag", cacheEntry.getETag());
		writeMetaData(sb, "Last-Modified", cacheEntry.getLastModified());
		String encoding = cacheEntry.getEncoding();
		if (encoding != null) {
			writeMetaData(sb, "Content-Type", cacheEntry.getContentType() + "; charset=" + encoding);
		} else {
			writeMetaData(sb, "Content-Type", cacheEntry.getContentType());
		}
		writeMetaData(sb, "Content-Language", cacheEntry.getContentLanguage());
		Integer maxAge = cacheEntry.getMaxAge();
		if (maxAge != null) {
			writeMetaData(sb, "Cache-Control", "max-age=" + maxAge.toString());
		}
		long size = makeSureThatWeKnowCacheEntrySize(cacheEntry);
		writeMetaData(sb, "Content-Length", Long.toString(size));
		String meta = sb.toString();
		try {
			byte[] metaBytes = meta.getBytes("UTF-8");
			appendToTar(tos, "meta", metaBytes);
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException("utf-8 is a required encoding");
		}
		
		appendToTar(tos, "data.bin", cacheEntry.getData(), size);
	}

	private long makeSureThatWeKnowCacheEntrySize(CacheEntry cacheEntry) {
		long size = makeSureThatWeKnowCacheEntrySize(cacheEntry.getData(), cacheEntry.getContentLength());
		return size;
	}
	
	private long makeSureThatWeKnowCacheEntrySize(ReplayableInputStream is, Long size) {
		if (size == null) {
			// we have to read the stream to get the full size
			if (is.getLength() < 0) {
				try {
					is = is.doReplay(); // this will read the entire stream and buffer it in memory or on disk (for large data)
				} catch (IOException ex) {
					throw new IllegalStateException("failed to perform a doReplay on a replayable input stream in order to get its total length", ex);
				}
			}
			if (is.getLength() < 0) {
				// for unknown reason, the length could not be determined. seems like we are dealing with a no-op instance of replayableinputstream
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try {
					IOUtils.copy(is, bos);
					bos.flush();
					byte[] buf = bos.toByteArray();
					size = (long) buf.length;
				} catch (IOException ex) {
					throw new IllegalStateException("failed to copy inputstream to buffer in order to determine its total length", ex);
				}
			} else {
				size = is.getLength();
			}
		}
		return size;
	}
	
	private void appendToTar(TarArchiveOutputStream tos, String name, byte[] bytes) {
		long size = bytes.length;
		appendToTar(tos, name, ReplayableInputStream.newInstance(bytes), size);
	}
	
	private void appendToTar(TarArchiveOutputStream tos, String name, ReplayableInputStream is, Long size) {
		TarArchiveEntry entry = new TarArchiveEntry(name);
		makeSureThatWeKnowCacheEntrySize(is, size);
		entry.setSize(size);
		try {
			tos.putArchiveEntry(entry);
		} catch (IOException e) {
			throw new IllegalStateException("failed to add entry to tar: " + e.getMessage(), e);
		}
		try {
			IOUtils.copy(is, tos, size);
		} catch (IOException e) {
			throw new IllegalStateException("failed to read cache entry data: " + e.getMessage(), e);
		}

		try {
			tos.closeArchiveEntry();
		} catch (IOException e) {
			throw new IllegalStateException("failed to close tar archive: " + e.getMessage(), e);
		}
	}

	public CacheEntry deserialize(InputStream inputStream) {
		final TarArchiveInputStream tis = new TarArchiveInputStream(inputStream);
		CacheEntryImpl cacheEntry = new CacheEntryImpl() {

			@Override
			public void close() throws Exception {
				tis.close();
			}
			
		};
		try {
			TarArchiveEntry tarEntry = tis.getNextTarEntry();
			boolean expectingFurtherEntries;
			while (tarEntry != null) {
				expectingFurtherEntries = appendTarEntryToCacheEntry(tis, tarEntry, cacheEntry);
				if (expectingFurtherEntries) {
					tarEntry = tis.getNextTarEntry();
				} else {
					break;
				}
			}
		} catch (IOException ex) {
			throw new IllegalStateException("could not switch to next tar entry: " + ex.getMessage(), ex);
		}
		if (cacheEntry.getData() == null) {
			return null;
		}
		return cacheEntry;
	}

	private void writeMetaData(StringBuffer sb, String name, String value) {
		if (value == null) {
			return;
		}
		writeStringToBuffer(sb, name, "\n\r:").append(':');
		writeStringToBuffer(sb, value, "\n\r").append('\n');

	}

	private void writeMetaData(StringBuffer sb, String name, Date value) {
		if (value == null) {
			return;
		}
		String formatted = buildSimpleDateFormat().format(value);
		writeMetaData(sb, name, formatted);
	}

	private SimpleDateFormat buildSimpleDateFormat() {
		return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
	}

	private StringBuffer writeStringToBuffer(StringBuffer sb, String name, String excluded) {
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (excluded.indexOf(c) > -1) {
				continue;
			}
			sb.append(c);
		}
		return sb;
	}

	private boolean appendTarEntryToCacheEntry(TarArchiveInputStream tis, TarArchiveEntry tarEntry, final CacheEntryImpl cacheEntry) {
		String tarEntryName = tarEntry.getName();
		if ("data.bin".equals(tarEntryName)) {
			ReplayableInputStream content = readTarEntryDataToBytes(tis, tarEntry, true); // i assume, that data.bin is always the last tar entry!
			long l = content.getLength();
			if (l > -1) {
				cacheEntry.setContentLength(l);
			}
			cacheEntry.setData(content);
			return false;
		} else if ("meta".equals(tarEntryName)) {
			ReplayableInputStream content = readTarEntryDataToBytes(tis, tarEntry, false); // i assume, that meta is never the last tar entry!
			try {
				String metaData = IOUtils.readToString(content, "UTF-8");
				parseMetaData(metaData, new MetaDataCallback() {

					@Override
					public void handle(String name, String value) {
						if ("ETag".equals(name)) {
							cacheEntry.setEtag(value);
						} else if ("Content-Type".equals(name)) {
							String marker = "; charset=";
							int index = value.indexOf(marker);
							if (index > -1) {
								cacheEntry.setContentType(value.substring(0, index));
								cacheEntry.setEncoding(value.substring(index + marker.length()));
							} else {
								cacheEntry.setContentType(value);
								cacheEntry.setEncoding(null);
							}
						} else if ("Content-Language".equals(name)) {
							cacheEntry.setContentLanguage(value);
						} else if ("Content-Length".equals(name)) {
							cacheEntry.setContentLength(Long.valueOf(value));
						} else if ("Cache-Control".equals(name)) {
							String prefix = "max-age=";
							if (value.startsWith(prefix)) {
								cacheEntry.setMaxAge(Integer.valueOf(value.substring(prefix.length())));
							}
						} else if ("Last-Modified".equals(name)) {
							try {
								Date d = buildSimpleDateFormat().parse(value);
								cacheEntry.setLastModified(d);
							} catch (ParseException ex) {
								throw new IllegalStateException("could not parse date from cache entry meta data");
							}
						} else {
							// unsupported meta data
						}
					}
				});
				return true;
			} catch (IOException ex) {
				throw new IllegalStateException("could not read meta data: " + ex.getMessage(), ex);
			}
		} else {
			// unsupported tar entry
			return true;
		}
	}

	private ReplayableInputStream readTarEntryDataToBytes(TarArchiveInputStream tis, TarArchiveEntry tarEntry, boolean isLast) throws IllegalStateException {
		try {
			ReplayableInputStream rpis = ReplayableInputStream.newInstance(tis, tarEntry.getSize(), false);
			if (!isLast) {
				rpis.doReplay(); // load the stream data to an internal buffer.
			}
			return rpis;
		} catch (IOException ex) {
			throw new IllegalStateException("failed to read from tar", ex);
		}
	}

	private static interface MetaDataCallback {

		void handle(String name, String value);
	}

	private void parseMetaData(String metaData, MetaDataCallback cb) {
		StringBuffer nameSb = null;
		StringBuffer valueSb = null;
		for (int i = 0; i < metaData.length(); i++) {
			char c = metaData.charAt(i);
			if (c == ':' && valueSb == null) {
				valueSb = new StringBuffer();
				continue;
			} else if (c == '\n') {
				if (nameSb != null && valueSb != null) {
					String name = nameSb.toString();
					String value = valueSb.toString();
					cb.handle(name, value);
					nameSb = null;
					valueSb = null;
				}
			} else {
				if (valueSb == null) {
					if (nameSb == null) {
						nameSb = new StringBuffer();
					}
					nameSb.append(c);
				} else {
					valueSb.append(c);
				}
			}
		}

		if (nameSb != null && valueSb != null) {
			String name = nameSb.toString();
			String value = valueSb.toString();
			cb.handle(name, value);
		}
	}
}
