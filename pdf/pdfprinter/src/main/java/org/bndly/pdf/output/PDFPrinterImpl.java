package org.bndly.pdf.output;

/*-
 * #%L
 * PDF Document Printer
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
import org.bndly.css.CSSItem;
import org.bndly.css.CSSReader;
import org.bndly.css.CSSStyle;
import org.bndly.pdf.Point2D;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.PrintingObject;
import org.bndly.pdf.PrintingObjectImpl;
import org.bndly.pdf.style.FontStyles;
import org.bndly.pdf.style.FontWeights;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.Document;
import org.bndly.pdf.visualobject.Image;
import org.bndly.pdf.visualobject.OverflowPage;
import org.bndly.pdf.visualobject.Page;
import org.bndly.pdf.visualobject.SystemText;
import org.bndly.pdf.visualobject.Table;
import org.bndly.pdf.visualobject.Text;
import org.bndly.pdf.visualobject.VisualObject;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFPrinterImpl extends PrintingObjectImpl implements PDFPrinter {

	private static final Logger LOG = LoggerFactory.getLogger(PDFPrinter.class);

	private Document document;
	private PDDocument pddoc;
	private final List<FontBinding> fontBindings = new ArrayList<>();
	private Container currentPageContainer;
	private final List<ImageBinding> imageBindings = new ArrayList<>();
	private PDPageContentStream contentStream;
	private final float debugFontSize = 6f;
	private final boolean isDebugEnabled = false;
	private int currentPageIndex;
	private int totalPages;
	private OutputStream outputStream;

	public PDFPrinterImpl(PrintingContext printingContext, PrintingObject owner) {
		super(printingContext, owner);
		// at this position the init method is allowed to be called from within the constructor, because the context is provided through the constructor
		PDFontTextSizeStrategy s = new PDFontTextSizeStrategy();
		s.setPdfPrinter(this);
		getContext().setTextSizeStrategy(s);
	}

	@Override
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	@Override
	public void setOutputFileName(String outputFileName) {
		try {
			outputStream = new FileOutputStream(outputFileName);
		} catch (FileNotFoundException ex) {
			throw new IllegalStateException("could not set file as output target.");
		}
	}

//	public void setInputStreamResolver(InputStreamResolver resolver) {
//		getContext().setInputStreamResolver(resolver);
//	}

	@Override
	public void print(Document d) {
		print(d, outputStream);
	}

	@Override
	public void print(Document d, OutputStream outputStream) {
		if (outputStream == null) {
			throw new IllegalStateException("no outputStream specified.");
		}

		currentPageIndex = 0;
		document = d;
		try {
			pddoc = new PDDocument();
			loadResources();
			fillSystemText(document, false); // handle preLayout system texts (like !today!)
			document.doLayout();
			totalPages = countPagesOf(document);
			fillSystemText(document, true); // handle postLayout system texts (like !totalPages!)
			generatePages();
			pddoc.save(outputStream);
			pddoc.close();
		} catch (IOException e) {
			LOG.error("failed to print document as pdf: " + e.getMessage(), e);
		}
	}
	

	private int countPagesOf(Document document) {
		int i = 0;
		List<VisualObject> items = document.getItems();
		if (items != null) {
			for (VisualObject visualObject : items) {
				if (visualObject.is(Page.class) && !visualObject.is(OverflowPage.class)) {
					i++;
				}
			}
		}
		return i;
	}

	private void fillSystemText(Container container, boolean postLayout) {
		List<VisualObject> items = container.getItems();
		if (items != null) {
			for (VisualObject visualObject : items) {
				if (visualObject.is(Page.class)) {
					if (postLayout) {
						currentPageIndex++;
					}
				}
				if (visualObject.is(Container.class)) {
					fillSystemText(visualObject.as(Container.class), postLayout);
				} else if (visualObject.is(SystemText.class)) {
					if (postLayout) {
						handlePostLayoutSystemText(visualObject.as(SystemText.class));
					} else {
						handlePreLayoutSystemText(visualObject.as(SystemText.class));
					}
				}
			}
		}
	}

	private void handlePostLayoutSystemText(SystemText systemText) {
		String v = systemText.getValue();
		v = v.replaceAll("!page!", "" + currentPageIndex);
		v = v.replaceAll("!totalPages!", "" + totalPages);
		systemText.setValue(v);
	}

	private void handlePreLayoutSystemText(SystemText systemText) {
		String v = systemText.getValue();
		v = v.replaceAll("!today!", "" + new Date());
		systemText.setValue(v);
	}

	private void generatePages() {
		List<Page> pages = document.getItems(Page.class);
		for (Page container : pages) {
			if (!container.is(OverflowPage.class)) {
				float pWidth = container.getWidth().floatValue();
				float pHeight = container.getHeight().floatValue();
				PDRectangle size = new PDRectangle(pWidth, pHeight);
				PDPage pdpage = new PDPage(size);
				printPageContainerToPDPage(container, pdpage);
				pddoc.addPage(pdpage);
			}
		}

	}

	private void printPageContainerToPDPage(Container container, PDPage pdpage) {
		try {
			boolean didOpenContentStream = false;
			if (contentStream == null) {
				didOpenContentStream = true;
				contentStream = new PDPageContentStream(pddoc, pdpage);
				currentPageContainer = container;
			}
			debugPrint(container, contentStream);

			List<VisualObject> items = container.getItems();
			if (items != null) {
				for (VisualObject visualObject : items) {
					if (visualObject.is(Text.class)) {
						Text t = (Text) visualObject;
						printText(t, contentStream);
					} else if (visualObject.is(Image.class)) {
						Image i = (Image) visualObject;
						printImage(i, contentStream);
					} else if (visualObject.is(Table.class)) {
						Table t = (Table) visualObject;
						printTable(t, contentStream);
					} else if (visualObject.is(Container.class)) {
						Container c = (Container) visualObject;
						printPageContainerToPDPage(c, pdpage);
					}
				}
			}
			if (didOpenContentStream) {
				contentStream.close();
				contentStream = null;
			}
		} catch (IOException e) {
			LOG.error("failed to print document container to pdf page: " + e.getMessage(), e);
		}
	}

	private void printTable(Table t, PDPageContentStream contentStream) throws IOException {
		debugPrint(t, contentStream);
		contentStream.saveGraphicsState();
		Style tableStyle = t.getCalculatedStyle();
		Double borderWidth = tableStyle.get(StyleAttributes.BORDER_WIDTH);
		if (borderWidth != null && borderWidth > 0) {
			printTableGrid(t, contentStream, borderWidth);
		}
		printTableContent(t, contentStream);

		contentStream.restoreGraphicsState();
	}

	private void printTableContent(Table t, PDPageContentStream contentStream) throws IOException {
		contentStream.saveGraphicsState();
		List<Container> columns = t.getItems(Container.class);
		if (columns != null) {
			for (Container column : columns) {
				debugPrint(column, contentStream);
				List<Container> cellItems = column.getItems(Container.class);
				if (cellItems != null) {
					for (Container cell : cellItems) {
						debugPrint(cell, contentStream);
						List<VisualObject> items = cell.getItems();
						if (items != null) {
							for (VisualObject vo : items) {
								if (Text.class.isAssignableFrom(vo.getClass())) {
									printText((Text) vo, contentStream);
								} else if (Image.class.isAssignableFrom(vo.getClass())) {
									printImage((Image) vo, contentStream);
								} else if (Table.class.isAssignableFrom(vo.getClass())) {
									printTable((Table) vo, contentStream);
								}
							}
						}
					}
				}
			}
		}
		contentStream.restoreGraphicsState();
	}

	private void printTableGrid(Table t, PDPageContentStream contentStream, Double borderWidth) throws IOException {
		contentStream.saveGraphicsState();
		contentStream.setLineWidth(borderWidth.floatValue());
		Point2D position = t.getAbsolutePosition();
		position = position.flipY(currentPageContainer.getHeight());

		double currentY = position.getY();
		int rowCount = t.getRowCount();
		float xStart = (float) (position.getX());//+ml.floatValue();
		for (int i = 0; i < rowCount - 1; i++) {
			double height = t.getRowHeightForRow(i);
			currentY -= height;
			contentStream.addLine(xStart, (float) currentY, xStart + t.getWidth().floatValue(), (float) currentY);
		}
		contentStream.addRect(xStart, (float) (position.getY() - t.getHeight()), t.getWidth().floatValue(), t.getHeight().floatValue());
		List<Container> columns = t.getItems(Container.class);
		if (columns != null) {
			for (int i = 0; i < columns.size() - 1; i++) {
				Container column = columns.get(i);
				xStart += column.getWidth().floatValue();
				float xEnd = xStart;
				contentStream.addLine(xStart, (float) (position.getY() - t.getHeight()), xEnd, (float) (position.getY()));
			}
			contentStream.stroke();
		}
		contentStream.restoreGraphicsState();
	}

	private void printText(Text t, PDPageContentStream contentStream) throws IOException {
		debugPrint(t, contentStream);
		contentStream.saveGraphicsState();
		contentStream.beginText();
		Style style = t.getCalculatedStyle();
		String font = style.get(StyleAttributes.FONT);
		Double fontSize = style.get(StyleAttributes.FONT_SIZE);
		FontBinding fontBinding = getFontBinding(font, getFontWeight(style), getFontStyle(style));
		PDFont pdFont = fontBinding.getPdFont();
		contentStream.setFont(pdFont, fontSize.floatValue());
		Point2D position = t.getAbsolutePosition();
		position = position.flipY(currentPageContainer.getHeight());
		position = position.moveY(-fontSize);
		contentStream.newLineAtOffset((float) position.getX(), (float) position.getY());
		contentStream.showText(t.getValue());
		contentStream.endText();
		contentStream.restoreGraphicsState();
	}

	private void debugPrint(VisualObject t, PDPageContentStream contentStream) throws IOException {
		if (isDebugEnabled) {
			contentStream.saveGraphicsState();
			contentStream.setStrokingColor(Color.red);
			String itemId = t.getItemId();
			Point2D position = t.getAbsolutePosition();
			position = position.flipY(currentPageContainer.getHeight());
			float height = t.getHeight().floatValue();
			float width = t.getWidth().floatValue();
			float y = (float) position.getY() - height;
			float x = (float) position.getX();
			contentStream.addRect(x, y, width, height);
			contentStream.stroke();
			if (itemId != null) {
				contentStream.beginText();
				PDFont pdFont = PDType1Font.COURIER;
				contentStream.setFont(pdFont, debugFontSize);
				contentStream.moveTextPositionByAmount((float) position.getX() + 2, (float) position.getY() - height + 2);
				contentStream.drawString(itemId);
				contentStream.endText();
			}
			contentStream.restoreGraphicsState();
		}
	}

	private void printImage(Image i, PDPageContentStream contentStream) throws IOException {
		debugPrint(i, contentStream);
		contentStream.saveGraphicsState();
		ImageBinding binding = getImageBinding(i.getSource());
		PDImageXObject ximage = binding.getPdImage();
		ximage.setWidth(i.getWidth().intValue());
		ximage.setHeight(i.getHeight().intValue());
		Point2D position = i.getAbsolutePosition();
		position = position.flipY(currentPageContainer.getHeight());
		contentStream.drawImage(ximage, (int) position.getX(), (int) position.getY() - ximage.getHeight());
		contentStream.restoreGraphicsState();
	}

	private void loadResources() throws IOException {
		loadResources(document);
	}

	private void loadResources(VisualObject vo) throws IOException {
		Style style = vo.getCalculatedStyle();
		if (style != null) {
			String font = style.get(StyleAttributes.FONT);
			FontWeights fontWeight = getFontWeight(style);
			FontStyles fontStyle = getFontStyle(style);
			if (font != null && !isFontLoaded(font, fontWeight, fontStyle)) {
				try {
					PDFont pdFont = fontNameToPDFont(font, fontWeight, fontStyle);
					FontBinding binding = new FontBinding();
					binding.setPDFont(pdFont);
					binding.setName(font);
					binding.setWeight(fontWeight);
					binding.setStyle(fontStyle);
					fontBindings.add(binding);
				} catch (IOException e) {
					LOG.error("failed to load a font resource: " + e.getMessage(), e);
				}
			}
		}
		if (vo.is(Image.class)) {
			Image i = vo.as(Image.class);
			String src = i.getSource();
			if (src.toLowerCase().endsWith(".jpg")) {
				InputStreamResolver resolver = getContext().getInputStreamResolver();
				ReplayableInputStream is;
				if (resolver != null) {
					is = ReplayableInputStream.newInstance(resolver.resolve(src));
				} else {
					is = ReplayableInputStream.newInstance(new FileInputStream(src));
				}

				PDImageXObject ximage = JPEGFactory.createFromStream(pddoc, is);
//				PDJpeg ximage = new PDJpeg(pddoc, is);
				ximage.setColorSpace(PDDeviceRGB.INSTANCE);
				ImageBinding binding = new ImageBinding();
				binding.setName(src);
				binding.setPdImage(ximage);
				imageBindings.add(binding);
			}
		}
		if (Container.class.isAssignableFrom(vo.getClass())) {
			Container c = (Container) vo;
			List<VisualObject> items = c.getItems();
			if (items != null) {
				for (VisualObject visualObject : items) {
					loadResources(visualObject);
				}
			}
		}
	}

	@Override
	public FontWeights getFontWeight(Style style) {
		String fontWeightString = style.get(StyleAttributes.FONT_WEIGHT);
		FontWeights fontWeight = null;
		if (fontWeightString != null) {
			fontWeight = FontWeights.valueOf(fontWeightString);
		}
		return fontWeight;
	}

	@Override
	public FontStyles getFontStyle(Style style) {
		String fontStyleString = style.get(StyleAttributes.FONT_STYLE);
		FontStyles fontStyle = null;
		if (fontStyleString != null) {
			fontStyle = FontStyles.valueOf(fontStyleString);
		}
		return fontStyle;
	}

	private PDFont fontNameToPDFont(String font, FontWeights fontWeight, FontStyles fontStyle) throws IOException {
		PDFont pdFont = null;
		if ("times".equalsIgnoreCase(font)) {
			if (fontWeight == null && fontStyle == null) {
				pdFont = PDType1Font.TIMES_ROMAN;
			} else if (fontWeight == FontWeights.bold && fontStyle == null) {
				pdFont = PDType1Font.TIMES_BOLD;
			} else if (fontWeight == FontWeights.bold && fontStyle == FontStyles.italic) {
				pdFont = PDType1Font.TIMES_BOLD_ITALIC;
			} else if (fontWeight == null && fontStyle == FontStyles.italic) {
				pdFont = PDType1Font.TIMES_ITALIC;
			}
		} else if ("helvetica".equalsIgnoreCase(font)) {
			if (fontWeight == null && fontStyle == null) {
				pdFont = PDType1Font.HELVETICA;
			} else if (fontWeight == FontWeights.bold && fontStyle == null) {
				pdFont = PDType1Font.HELVETICA_BOLD;
			} else if (fontWeight == FontWeights.bold && fontStyle == FontStyles.italic) {
				pdFont = PDType1Font.HELVETICA_BOLD_OBLIQUE;
			} else if (fontWeight == null && fontStyle == FontStyles.italic) {
				pdFont = PDType1Font.HELVETICA_OBLIQUE;
			}
		} else if ("courier".equalsIgnoreCase(font)) {
			if (fontWeight == null && fontStyle == null) {
				pdFont = PDType1Font.COURIER;
			} else if (fontWeight == FontWeights.bold && fontStyle == null) {
				pdFont = PDType1Font.COURIER_BOLD;
			} else if (fontWeight == FontWeights.bold && fontStyle == FontStyles.italic) {
				pdFont = PDType1Font.COURIER_BOLD_OBLIQUE;
			} else if (fontWeight == null && fontStyle == FontStyles.italic) {
				pdFont = PDType1Font.COURIER_OBLIQUE;
			}

		} else {
			InputStreamResolver isr = getContext().getInputStreamResolver();
			InputStream input = isr.resolve(font);
			if (input != null) {
				try (InputStream tmp = input) {
					pdFont = PDType0Font.load(pddoc, tmp);
				}
			}
//			throw new IllegalStateException(
//					"Seems like you want to use a TTF font '" + font + "'. I discourage you form using these fonts, since they often are poorly "
//					+ "implemented and lead to irregular characterspacing. if you have questions, ask me! bndly@cybercon.de"
//			);
		}
		return pdFont;
	}

	private boolean isFontLoaded(String name, FontWeights fontWeight, FontStyles fontStyle) {
		if (name != null) {
			for (FontBinding b : fontBindings) {
				if (name.equals(b.getName()) && fontStyle == b.getStyle() && fontWeight == b.getWeight()) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public FontBinding getFontBinding(String name, FontWeights fontWeight, FontStyles fontStyle) {
		for (FontBinding b : fontBindings) {
			if (name.equals(b.getName()) && fontStyle == b.getStyle() && fontWeight == b.getWeight()) {
				return b;
			}
		}
		return null;
	}

	public ImageBinding getImageBinding(String name) {
		for (ImageBinding b : imageBindings) {
			if (name.equals(b.getName())) {
				return b;
			}
		}
		return null;
	}

	public void loadStyleSheet(String css) {
		CSSReader cssReader = new CSSReader();
		try {
			List<CSSItem> items = cssReader.read(css);
			List<CSSStyle> styles = getContext().getStyles();
			for (CSSItem item : items) {
				if (CSSStyle.class.isInstance(item)) {
					styles.add((CSSStyle)item);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("failed reading the CSS from " + css, e);
		}
	}
}
