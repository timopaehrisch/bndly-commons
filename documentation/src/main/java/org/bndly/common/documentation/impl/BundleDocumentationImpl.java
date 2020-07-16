package org.bndly.common.documentation.impl;

/*-
 * #%L
 * Documentation
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

import org.bndly.common.documentation.BundleDocumentation;
import static org.bndly.common.documentation.impl.DocumentedBundleTracker.DOCUMENTATION_HEADER_NAME;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class BundleDocumentationImpl implements BundleDocumentation {

	private static final Logger LOG = LoggerFactory.getLogger(BundleDocumentationImpl.class);
	private final ImageResource imageResource;
	private final Bundle bundle;
	private final List<MarkdownEntry> entries = new ArrayList<>();
	private final Map<String, Entry> imageEntries = new HashMap<>();
	private final List<Extension> commonmarkExtensions = Arrays.asList(TablesExtension.create());

	public BundleDocumentationImpl(Bundle bundle, ImageResource imageResource) {
		this.bundle = bundle;
		this.imageResource = imageResource;
		init();
	}

	private void init() {
		String docHeader = bundle.getHeaders().get(DOCUMENTATION_HEADER_NAME);
		if (docHeader == null) {
			return;
		}
		String[] docFiles = docHeader.split(";");
		for (final String docFile : docFiles) {
			final URL docFileUrl = bundle.getEntry(docFile);
			if (docFileUrl == null) {
				return;
			}

			entries.add(createEntry(docFile, docFileUrl, MarkdownEntry.class));
		}
	}

	private <E extends Entry> E createEntry(final String docFile, final URL docFileUrl, Class<E> entryType) {
		if (MarkdownEntry.class.equals(entryType)) {
			return (E) new MarkdownEntry() {
				@Override
				public String getRenderedMarkdown() {
					return renderMarkdown(this);
				}

				@Override
				public String getName() {
					return docFile;
				}

				@Override
				public InputStream getContent() throws IOException {
					return docFileUrl.openStream();
				}

			};
		}
		return (E) new Entry() {
			@Override
			public String getName() {
				return docFile;
			}

			@Override
			public InputStream getContent() throws IOException {
				return docFileUrl.openStream();
			}
		};
	}

	private String renderMarkdown(Entry entry) {
		for (Map.Entry<String, Entry> entry1 : imageEntries.entrySet()) {
			LOG.info("indexed image {} {}", entry1.getKey(), entry1.getValue().getName());
		}
		final String pathOfDocumentationFile = getPathOfFileInBundle(entry.getName());
		Parser parser = Parser.builder().extensions(commonmarkExtensions).build();
		try (Reader reader = new InputStreamReader(entry.getContent(), "UTF-8")) {
			Node document = parser.parseReader(reader);
			document.accept(new AbstractVisitor() {

				@Override
				public void visit(Image image) {
					image.setDestination(imageResource.getUrlOfImage(pathOfDocumentationFile + image.getDestination(), bundle));
					LOG.info("image {}", image.getDestination());
				}

			});
			HtmlRenderer renderer = HtmlRenderer.builder().extensions(commonmarkExtensions).build();
			String rendered = renderer.render(document);
			return rendered;
		} catch (IOException e) {
			LOG.warn("could not render markdown: " + e.getMessage(), e);
		}
		return null;
		}
	
	

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	@Override
	public Bundle getBundle() {
		return bundle;
	}

	@Override
	public Iterable<MarkdownEntry> getMarkdownEntries() {
		return Collections.unmodifiableList(entries);
	}

	public void indexContainedImages(Parser parser) {
		imageEntries.clear();
		for (BundleDocumentation.Entry entry : entries) {
			final String name = entry.getName();
			LOG.info("indexing images of {}", name);
			final String pathOfDocumentationFile = getPathOfFileInBundle(name);
			try (Reader reader = new InputStreamReader(entry.getContent(), "UTF-8")) {
				Node document = parser.parseReader(reader);
				document.accept(new AbstractVisitor() {

					@Override
					public void visit(Image image) {
						String dest = image.getDestination();
						if (dest == null) {
							return;
						}
						String imagePathInBundle = pathOfDocumentationFile + dest;
						if (imageEntries.containsKey(imagePathInBundle)) {
							return;
						}

						// check, if the dest is an URI
						if (isExternalUri(dest)) {
							// if it is an URI, then do nothing
							return;
						}
						// if it is an URI, check if the image is stored within the bundle
						final URL imageFileUrl = bundle.getEntry(imagePathInBundle);
						if (imageFileUrl == null) {
							return;
						}
						// if the image is stored in the bundle, then track it
						Entry imageEntry = createEntry(imagePathInBundle, imageFileUrl, Entry.class);
						LOG.info("found image {} in {}", imagePathInBundle, name);
						imageEntries.put(imagePathInBundle, imageEntry);
					}

				});
			} catch (IOException ex) {
				LOG.warn("Failed indexing images of " + entry.getName() + " from bundle " + bundle, ex);
			}
		}
	}

	public static String getPathOfFileInBundle(final String name) {
		int i = name.lastIndexOf("/");
		final String pathOfDocumentationFile;
		if (i > 0) {
			pathOfDocumentationFile = name.substring(0, i) + "/";
		} else {
			pathOfDocumentationFile = "";
		}
		return pathOfDocumentationFile;
	}

	@Override
	public Entry getImageEntry(String path) {
		return imageEntries.get(path);
	}

	public static boolean isExternalUri(String string) {
		try {
			URI uri = new URI(string);
			if (uri.getScheme() == null) {
				String path = uri.getPath();
				return path != null && path.startsWith("/");
			}
			return true;
		} catch (URISyntaxException ex) {
			return false;
		}
	}
}
