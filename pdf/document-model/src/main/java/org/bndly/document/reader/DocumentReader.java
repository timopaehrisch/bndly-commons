package org.bndly.document.reader;

/*-
 * #%L
 * PDF XML Document Model
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.bndly.document.xml.XContainer;
import org.bndly.document.xml.XDocument;
import org.bndly.document.xml.XVisualObject;
import java.util.logging.Level;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class DocumentReader {

	private static final Logger LOG = LoggerFactory.getLogger(DocumentReader.class);

	public XDocument read(String file) {
		File f = new File(file);
		if (!f.exists()) {
			throw new RuntimeException("could not read " + file + " because it does not exist.");
		}
		FileInputStream is;
		try {
			is = new FileInputStream(f);
			return readXML(XDocument.class, is);
		} catch (FileNotFoundException e) {
			LOG.error("could not read file from provided path: " + file, e);
		}
		throw new IllegalStateException("could not open inputstream from file " + file);
	}

	public XDocument read(InputStream is) {
		XDocument xdoc = readXML(XDocument.class, is);
		setParentContainerOnChildsOf(xdoc);
		return xdoc;
	}

	private void setParentContainerOnChildsOf(XContainer c) {
		List<XVisualObject> items = c.getItems();
		if (items != null) {
			for (XVisualObject xVisualObject : items) {
				xVisualObject.setOwnerContainer(c);
				if (XContainer.class.isAssignableFrom(xVisualObject.getClass())) {
					setParentContainerOnChildsOf((XContainer) xVisualObject);
				}
			}
		}
	}

	private <T> T readXML(Class<T> type, InputStream is) {
		if (is != null) {
			try {
				JAXBContext context = null;
				context = JAXBContext.newInstance(type);
				SAXParserFactory spf = SAXParserFactory.newInstance();
				spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				XMLReader xmlReader = spf.newSAXParser().getXMLReader();
				InputSource inputSource = new InputSource(is);
				SAXSource source = new SAXSource(xmlReader, inputSource);
				
				Unmarshaller unmarshaller = context.createUnmarshaller();
				Object configurations = unmarshaller.unmarshal(source);
				if (configurations == null) {
					return null;
				}
				return type.cast(configurations);
			} catch (SAXException | ParserConfigurationException | JAXBException e) {
				throw new RuntimeException("could not set up JAXB context", e);
			}
		} else {
			throw new IllegalStateException("no inputstream provided to read XML data");
		}
	}

}
