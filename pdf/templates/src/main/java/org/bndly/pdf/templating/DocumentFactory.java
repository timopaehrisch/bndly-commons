package org.bndly.pdf.templating;

/*-
 * #%L
 * PDF templating
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

import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.data.io.SmartBufferOutputStream;
import org.bndly.common.velocity.api.Renderer;
import org.bndly.common.velocity.api.VelocityDataProvider;
import org.bndly.css.CSSItem;
import org.bndly.css.CSSParsingException;
import org.bndly.css.CSSReader;
import org.bndly.css.CSSStyle;
import org.bndly.document.reader.DocumentReader;
import org.bndly.document.xml.XDocument;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.mapper.DocumentMapper;
import org.bndly.pdf.output.InputStreamResolver;
import org.bndly.pdf.output.PDFPrinter;
import org.bndly.pdf.visualobject.Document;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DocumentFactory.class)
public class DocumentFactory {
	private static final Logger LOG = LoggerFactory.getLogger(DocumentFactory.class);

	@Reference
	private Renderer renderer;
	private final List<VelocityDataProvider> velocityDataProviders = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	@Reference(
			bind = "addVelocityDataProvider",
			unbind = "removeVelocityDataProvider",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = VelocityDataProvider.class
	)
	public void addVelocityDataProvider(VelocityDataProvider velocityDataProvider) {
		if (velocityDataProvider != null) {
			lock.writeLock().lock();
			try {
				velocityDataProviders.add(velocityDataProvider);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void removeVelocityDataProvider(VelocityDataProvider velocityDataProvider) {
		if (velocityDataProvider != null) {
			lock.writeLock().lock();
			try {
				Iterator<VelocityDataProvider> iterator = velocityDataProviders.iterator();
				while (iterator.hasNext()) {
					VelocityDataProvider next = iterator.next();
					if (next == velocityDataProvider) {
						iterator.remove();
					}
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}
	
	public boolean isTemplateAvailable(String templateName) {
		lock.readLock().lock();
		try {
			for (VelocityDataProvider velocityDataProvider : velocityDataProviders) {
				if (velocityDataProvider.exists(templateName)) {
					return true;
				}
			}
			return false;
		} finally {
			lock.readLock().unlock();
		}
	}
	
	private XDocument buildXDocument(final PDFTemplate template) {
		// i'd prefer the other solution, but it does not work for some reason.
		SmartBufferOutputStream smartOutputBuffer = SmartBufferOutputStream.newInstance();
		OutputStreamWriter writer;
		try {
			writer = new OutputStreamWriter(smartOutputBuffer, "UTF-8");
			renderer.render(template, writer);
			writer.flush();
			writer.close();
			smartOutputBuffer.flush();
			ReplayableInputStream is = smartOutputBuffer.getBufferedDataAsReplayableStream().doReplay();
			XDocument document = new DocumentReader().read(is.noOp());
			return document;
		} catch (IOException ex) {
			throw new IllegalStateException("failed to render document", ex);
		}
	}

	public void renderTemplateToDocument(PDFTemplate template, String outFileName) {
		try {
			File f = new File(outFileName);
			if (!f.exists()) {
				f.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(f);
			renderTemplateToDocument(template, fos, true);
		} catch (FileNotFoundException ex) {
			throw new IllegalStateException("could not render template to file " + outFileName + ": " + ex.getMessage(), ex);
		} catch (IOException ex) {
			throw new IllegalStateException("could not render template to file " + outFileName + ": " + ex.getMessage(), ex);
		}
	}

	public void renderTemplateToDocument(PDFTemplate template, OutputStream outputStream) {
		renderTemplateToDocument(template, outputStream, false);
	}

	public void renderTemplateToDocument(PDFTemplate template, OutputStream outputStream, boolean flushAndClose) {
		XDocument xDocument = buildXDocument(template);
		renderDocument(xDocument, template.getCssNames(), outputStream, flushAndClose);
	}

	public void renderDocument(XDocument xDocument, String[] cssNames, OutputStream outputStream, boolean flushAndClose) {
		try {
			final List<CSSStyle> styles = new ArrayList<>();
			for (String cssName : cssNames) {
				try {
					InputStream is = createInputStreamResolver().resolve(cssName);
					List<CSSItem> items = new CSSReader().read(is);
					if (items != null) {
						for (CSSItem item : items) {
							if (CSSStyle.class.isInstance(item)) {
								styles.add((CSSStyle) item);
							}
						}
					}
				} catch (IllegalStateException e) {
					LOG.error("failed to load CSS " + cssName, e);
				}
			}
			renderDocument(xDocument, styles, outputStream, flushAndClose);
		} catch (IOException ex) {
			throw new IllegalStateException("failed to read CSS from inputstream: " + ex.getMessage(), ex);
		} catch (CSSParsingException ex) {
			throw new IllegalStateException("failed to parse CSS: " + ex.getMessage(), ex);
		}
	}

	public void renderDocument(XDocument xDocument, List<CSSStyle> styles, OutputStream outputStream, boolean flushAndClose) {
		PrintingContext ctx = new PrintingContext();
		ctx.setInputStreamResolver(createInputStreamResolver());
		ctx.getStyles().addAll(styles);
		PDFPrinter printer = ctx.createPDFPrinter();

		Document pdfdocument = new DocumentMapper().toDocument(xDocument, printer);
		printer.print(pdfdocument, outputStream);
		if (flushAndClose) {
			try {
				outputStream.flush();
			} catch (IOException ex) {
				// ignore
			} finally {
				try {
					outputStream.close();
				} catch (IOException ex) {
					// silently close
				}
			}
		}
	}

	private InputStreamResolver createInputStreamResolver() {
		InputStreamResolver isr = new InputStreamResolver() {
			@Override
			public InputStream resolve(String fileName) throws IOException {
				lock.readLock().lock();
				try {
					Iterator<VelocityDataProvider> iter = velocityDataProviders.iterator();
					while (iter.hasNext()) {
						VelocityDataProvider next = iter.next();
						InputStream stream = next.getStream(fileName);
						if (stream != null) {
							return stream;
						}
					}
					return null;
				} finally {
					lock.readLock().unlock();
				}
			}
		};
		return isr;
	}

	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

	/**
	 * use addVelocityDataProvider instead
	 * @param velocityDataProvider
	 * @deprecated
	 */
	@Deprecated
	public void setVelocityDataProvider(VelocityDataProvider velocityDataProvider) {
		addVelocityDataProvider(velocityDataProvider);
	}
}
