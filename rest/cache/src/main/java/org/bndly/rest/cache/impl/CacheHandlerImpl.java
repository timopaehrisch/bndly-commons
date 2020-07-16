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

import org.bndly.common.crypto.api.HashService;
import org.bndly.common.crypto.api.SaltedHashResult;
import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.data.io.WriteIOException;
import org.bndly.rest.api.ByteServingContext;
import org.bndly.rest.api.CacheContext;
import org.bndly.rest.api.CacheHandler;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.HTTPMethod;
import org.bndly.rest.api.QuantifiedContentType;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.cache.api.CacheEntry;
import org.bndly.rest.cache.api.CacheTransaction;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class CacheHandlerImpl implements CacheHandler {

	private CacheEntry cacheEntry;

	private final CacheInterceptor cacheInterceptor;
	private final HashService hashService;
	private static final byte[] insecureSalt = new byte[]{0, 1, 2, 3, 4, 5, 6, 7};
	private CacheTransaction cacheTransaction;
	private CacheTransaction cacheTransactionWithSuppressedEventing;

	public CacheHandlerImpl(CacheInterceptor cacheInterceptor, HashService hashService) {
		if (cacheInterceptor == null) {
			throw new IllegalArgumentException("cacheInterceptor is not allowed to be null");
		}
		if (hashService == null) {
			throw new IllegalArgumentException("hashService is not allowed to be null");
		}
		this.cacheInterceptor = cacheInterceptor;
		this.hashService = hashService;
	}

	private boolean isCachingPrevented(Context context) {
		CacheContext cacheContext = context.getCacheContext();
		if (cacheContext != null && cacheContext.isCachingPrevented()) {
			return true;
		}
		return false;
	}

	public final CacheTransaction getCacheTransaction() {
		if (cacheTransaction == null) {
			cacheTransaction = cacheInterceptor.createCacheTransaction();
		}
		return cacheTransaction;
	}

	public final CacheTransaction getCacheTransactionWithSuppressedEventing() {
		if (cacheTransactionWithSuppressedEventing == null) {
			cacheTransactionWithSuppressedEventing = cacheInterceptor.createCacheTransactionWithSuppressedEventing();
		}
		return cacheTransactionWithSuppressedEventing;
	}

	@Override
	public boolean canBeCached(Context context) {
		if (isCachingPrevented(context)) {
			return false;
		}
		if (!context.getMethod().equals(HTTPMethod.GET)) {
			return false;
		}
		StatusWriter.Code statusCode = context.getStatus();
		if (statusCode != null && statusCode.getHttpCode() >= 300) {
			return false;
		}
		ResourceURI uri = context.getURI();
		List<ResourceURI.QueryParameter> params = uri.getParameters();
		if (params != null && !params.isEmpty()) {
			return false;
		}
		return true;
	}

	@Override
	public void returnCachedData(Context context) {
		CacheContext cc = context.getCacheContext();
		String etag = cc.getETag();
		boolean etagAccepted = etag == null || etag.equals(cacheEntry.getETag());
		boolean dateAccepted = true;
		Date modifiedSince = cc.getIfModifiedSince();
		if (modifiedSince != null) {
			Date lm = cacheEntry.getLastModified();
			if (lm != null) {
				if (lm.getTime() >= modifiedSince.getTime()) {
					dateAccepted = true;
				} else {
					dateAccepted = false;
				}
			}
		}

		final String encoding = cacheEntry.getEncoding();
		ContentType ct = getContentTypeOfCacheEntry(cacheEntry);
		if (etagAccepted && dateAccepted) {
			if (etag != null || modifiedSince != null) {
				ByteServingContext bsc = context.getByteServingContext();
				boolean isRangeRequest = bsc.isByteServingRequest();
				if (!isRangeRequest) {
					// return not modified
					if (ct != null) {
						context.setOutputContentType(ct, encoding);
					}
					context.getStatusWriter().write(StatusWriter.Code.NOT_MODIFIED);
					return;
				}
			}
		}

		ReplayableInputStream is = cacheEntry.getData();
		if (ct != null) {
			context.setOutputContentType(ct, encoding);
		}
		if (cacheEntry.getContentLanguage() != null) {
			context.setOutputContentLanguage(cacheEntry.getContentLanguage());
		}
		context.getCacheContext().setETag(cacheEntry.getETag());
		context.getCacheContext().setLastModified(cacheEntry.getLastModified());
		final String contentTypeName = cacheEntry.getContentType();
		if (contentTypeName != null) {
			context.setOutputContentType(new ContentType() {

				@Override
				public String getName() {
					return contentTypeName;
				}

				@Override
				public String getExtension() {
					return null;
				}
				
			}, encoding);
		}
		// make the replayable input stream a no-op. this means, we don't want 
		// to buffer here, because we are already serving data from disk.
		is = is.noOp();
		ByteServingContext bsc = context.getByteServingContext();
		if (bsc.isByteServingRequest()) {
			bsc.serveDataFromStream(is, cacheEntry.getContentLength(), cacheEntry.getETag());
		} else {
			try (OutputStream os = context.getOutputStream()) {
				IOUtils.copy(is, os);
				os.flush();
			} catch (WriteIOException ex) {
				// we ignore this case, because the other end might have closed the connection
			} catch (IOException ex) {
				throw new IllegalStateException("failed to get outputstream: " + ex.getMessage(), ex);
			}
		}
	}

	private ContentType getContentTypeOfCacheEntry(CacheEntry cacheEntry) {
		if (cacheEntry == null) {
			return null;
		}
		final String ctName = cacheEntry.getContentType();
		if (ctName == null) {
			return null;
		}
		return new ContentType() {
			
			@Override
			public String getName() {
				return ctName;
			}

			@Override
			public String getExtension() {
				return null;
			}
			
		};
	}

	@Override
	public boolean canBeServedFromCache(Context context) {
		if (isCachingPrevented(context)) {
			return false;
		}
		if (!canBeCached(context)) {
			// if it can not be cached in the first place, we don't have to look it up
			return false;
		}
		this.cacheEntry = lookupCacheEntry(context);
		if (cacheEntry == null) {
			return false;
		}
		Integer maxAge = cacheEntry.getMaxAge();
		if (maxAge != null) {
			Date lm = cacheEntry.getLastModified();
			if (lm != null) {
				long currentTimeMillis = System.currentTimeMillis();
				long endOfValidity = lm.getTime() + (maxAge * 1000);
				if (endOfValidity < currentTimeMillis) {
					return false;
				}
			}
		}
		return true;
	}

	private String buildCacheEtagFromData(InputStream inputStream) throws IOException {
		// in the case of etag generation we don't have to change the salt. etags are not used to hash secure information.
		SaltedHashResult hash = hashService.hash(inputStream, insecureSalt, 1);
		return hash.getHashBase64();
	}

	private CacheEntryImpl buildCacheEntryFromContext(Context context) {
		CacheEntryImpl localCacheEntry = new CacheEntryImpl() {

			@Override
			public void close() throws Exception {
				// nothing to do, because this entry is not loaded from a closeable resource
			}
			
		};
		localCacheEntry.setUri(context.getURI());
		localCacheEntry.setLastModified(new Date());
		ContentType ct = context.getOutputContentType();
		if (ct != null) {
			localCacheEntry.setContentType(ct.getName());
			localCacheEntry.setEncoding(context.getOutputEncoding());
		}
		Locale locale = context.getLocale();
		if (locale != null) {
			String language = locale.getLanguage();
			localCacheEntry.setContentLanguage(language);
		}
		CacheContext cc = context.getCacheContext();
		Integer maxAge = cc.getServerSideMaxAge();
		if (maxAge != null) {
			localCacheEntry.setMaxAge(maxAge);
		}
		return localCacheEntry;
	}

	private CacheEntry lookupCacheEntry(Context context) {
		if (isCachingPrevented(context)) {
			return null;
		}
		List<QuantifiedContentType> ct = context.getDesiredContentTypes();
		Path filePath = null;
		if (!ct.isEmpty()) {
			for (QuantifiedContentType contentType : ct) {
				Path tmp = buildFilePathToResource(context, contentType);
				if (Files.notExists(tmp)) {
					continue;
				} else {
					filePath = tmp;
					break;
				}
			}
		} else {
			filePath = buildFilePathToResource(context, context.getDesiredContentType());
			if (Files.notExists(filePath)) {
				filePath = null;
			}
		}
		if (filePath == null) {
			return null;
		}
		InputStream is = null;
		try {
			// the CacheEntry is autocloseable. this means that the input stream
			// will be closed, when the cache entry is being closed. this has to 
			// happen outside of this method block
			is = Files.newInputStream(filePath, StandardOpenOption.READ);
		} catch (IOException ex) {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					// silently close
				}
			}
			throw new IllegalStateException("could not deserialize cache file: " + ex.getMessage(), ex);
		}
		CacheEntry localCacheEntry = cacheInterceptor.getCacheEntryIO().deserialize(is);
		return localCacheEntry;
	}

	private Path buildFilePathToResource(Context context, ContentType ct) {
		ResourceURI uri = context.getURI();
		Path cacheFilePath = Paths.get(cacheInterceptor.getCacheRoot());
		boolean hasPath = uri.getPath() != null;

		Locale locale = context.getLocale();
		String lang = null;
		if (locale != null) {
			lang = locale.getLanguage();
		}
		if ("".equals(lang)) {
			lang = null;
		}
		if (hasPath) {
			for (String pathElement : uri.getPath()) {
				cacheFilePath = cacheFilePath.resolve(pathElement);
			}
		}
		
		StringBuffer fileName = null;
		
		if (uri.getSelectors() != null) {
			for (ResourceURI.Selector selector : uri.getSelectors()) {
				if (fileName == null) {
					fileName = new StringBuffer();
				} else {
					fileName.append(".");
				}
				fileName.append(selector.getName());
			}
		}
		if (lang != null) {
			if (fileName == null) {
				fileName = new StringBuffer();
			} else {
				fileName.append(".");
			}
			fileName.append(lang);
		}
		if (uri.getExtension() != null) {
			if (fileName == null) {
				fileName = new StringBuffer();
			} else {
				fileName.append(".");
			}
			fileName.append(uri.getExtension().getName());
		}
		String fileInFolder = fileName == null ? null : fileName.toString();
		
		boolean hasFileName = fileInFolder != null;
		if (hasFileName) {
			cacheFilePath = cacheFilePath.resolve(fileInFolder);
		}

		if (uri.getExtension() == null && ct != null) {
			// map content-type to extension
			if (ContentType.XML.getName().equals(ct.getName())) {
				if (hasPath || hasFileName) {
					if (hasFileName) {
						cacheFilePath = Paths.get(cacheFilePath.toString() + ".xml");
					} else {
						cacheFilePath = cacheFilePath.resolve("xml");
						hasFileName = true;
					}
				} else {
					if (lang != null) {
						cacheFilePath = cacheFilePath.resolve(lang + ".xml");
					} else {
						cacheFilePath = cacheFilePath.resolve("xml");
						hasFileName = true;
					}
				}
			} else if (ContentType.JSON.getName().equals(ct.getName())) {
				if (hasPath || hasFileName) {
					if (hasFileName) {
						cacheFilePath = Paths.get(cacheFilePath.toString() + ".json");
					} else {
						cacheFilePath = cacheFilePath.resolve("json");
						hasFileName = true;
					}
				} else {
					if (lang != null) {
						cacheFilePath = cacheFilePath.resolve(lang + ".json");
					} else {
						cacheFilePath = cacheFilePath.resolve("json");
						hasFileName = true;
					}
				}
			}
		}
		if (hasFileName) {
			cacheFilePath = Paths.get(cacheFilePath.toString() + ".tar");
		} else {
			cacheFilePath = cacheFilePath.resolve("tar");	
		}
		
		return cacheFilePath;
	}

	@Override
	public void saveCacheData(ReplayableInputStream is, Context context) {
		CacheEntryImpl localCacheEntry = buildCacheEntryFromContext(context);
		try {
			// in order to generate an etag, the data has to be hashed and therefore read completely
			try (ReplayableInputStream ris = is) {
				String etag = buildCacheEtagFromData(ris);
				localCacheEntry.setEtag(etag);
			}
			is = is.replay();
		} catch (IOException ex) {
			throw new IllegalStateException("could not create etag for cache entry: " + ex.getMessage(), ex);
		}
		localCacheEntry.setData(is);
		context.getCacheContext().setETag(localCacheEntry.getETag());
		context.getCacheContext().setLastModified(localCacheEntry.getLastModified());
		Path tempPath = buildTemporaryFilePath(localCacheEntry);
		try (OutputStream os = Files.newOutputStream(tempPath, StandardOpenOption.WRITE)) {
			cacheInterceptor.getCacheEntryIO().serialize(localCacheEntry, os);
			os.flush();
		} catch (IOException ex) {
			throw new IllegalStateException("cache file could not be written", ex);
		}
		Path filePath = buildFilePathToResource(context, context.getOutputContentType());
		boolean shouldRemoveTempFile = true;
		try {
			Path parentPath = filePath.getParent();
			if (parentPath != null) {
				if (Files.notExists(parentPath)) {
					try {
						Files.createDirectories(parentPath);
					} catch (IOException ex) {
						throw new IllegalStateException("failed to create parent folders to cache " + filePath.toString(), ex);
					}
				}
			}
			try {
				Files.move(tempPath, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
				shouldRemoveTempFile = false;
			} catch (IOException ex) {
				throw new IllegalStateException("failed to move the temporary cache file", ex);
			}

			try {
				is.replay();
			} catch (IOException ex) {
				throw new IllegalStateException("could not replay input stream to write it to the output stream");
			}
		} finally {
			if (shouldRemoveTempFile) {
				try {
					Files.delete(tempPath);
				} catch (IOException ex) {
					throw new IllegalStateException("failed to remove temporary cache file", ex);
				}
			}
		}
		
	}

	private Path buildTemporaryFilePath(CacheEntryImpl localCacheEntry) throws IllegalStateException {
		return buildTemporaryFilePath(localCacheEntry, 0);
	}

	private Path buildTemporaryFilePath(CacheEntryImpl localCacheEntry, int iteration) throws IllegalStateException {
		Path tempPath = buildTempPath(localCacheEntry, iteration);
		try {
			tempPath = Files.createFile(tempPath);
		} catch (FileAlreadyExistsException ex) {
			return buildTemporaryFilePath(localCacheEntry, iteration + 1);
		} catch (IOException ex) {
			throw new IllegalStateException("could not create temporary file for saving cache data.", ex);
		}
		return tempPath;
	}

	private Path buildTempPath(CacheEntry cacheEntry, int iteration) {
		Path cacheRootPath = Paths.get(cacheInterceptor.getCacheRoot());
		String fileName = cacheEntry.getETag();
		StringBuffer sb = new StringBuffer();
		for (int index = 0; index < fileName.length(); index++) {
			char character = fileName.charAt(index);
			if (character == '/' || character == '=') {
				continue;
			}
			sb.append(character);
		}
		sb.append("_iter").append(iteration);
		fileName = sb.toString();
		cacheRootPath = cacheRootPath.resolve(fileName);
		return cacheRootPath;
	}

	@Override
	public void close() throws Exception {
		try {
			if (cacheTransaction != null) {
				cacheTransaction.close();
			}
		} finally {
			try {
				if (cacheTransactionWithSuppressedEventing != null) {
					cacheTransactionWithSuppressedEventing.close();
				}
			} finally {
				if (cacheEntry != null) {
					cacheEntry.close();
				}
			}
		}
	}
	
}
