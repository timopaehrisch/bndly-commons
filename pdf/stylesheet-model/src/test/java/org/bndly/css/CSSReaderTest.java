package org.bndly.css;

/*-
 * #%L
 * PDF CSS Model
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

import org.bndly.css.selector.CSSSelector;
import java.io.IOException;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CSSReaderTest {

	public CSSReaderTest() {
		System.setProperty("bndly.css.parser.debug.enabled", "true");
	}
	

	@Test
	public void readFile() throws IOException, CSSParsingException {
		CSSReader reader = new CSSReader();
		List<CSSItem> items = reader.read(getClass().getClassLoader().getResourceAsStream("order.css"));
		
		Assert.assertTrue(items.size() == 11, "css file should contain 11 styles; did contains "+items.size()+" styles");
		CSSItem item = items.get(0);
		Assert.assertEquals(item.getClass(), CSSStyle.class);
		CSSStyle style = (CSSStyle) item;
		String selector = style.getSelector();
		Assert.assertEquals(selector, "document");
		List<CSSAttribute> attributes = style.getAttributes();
		Assert.assertNotNull(attributes);
		Assert.assertEquals(attributes.size(), 3);
		CSSAttribute first = attributes.get(0);
		Assert.assertEquals(first.getName(), "font-family");
		Object value = first.getValue();
		Assert.assertEquals(value, "\"times\"");
	}
	
	@Test
	public void readMediaQueryFile() throws IOException, CSSParsingException {
		CSSReader reader = new CSSReader();
		List<CSSItem> items = reader.read(getClass().getClassLoader().getResourceAsStream("mediaquery.css"));
		
		Assert.assertTrue(items.size() == 5, "css file should contain 5 items; did contains "+items.size()+" items");
		CSSItem item0 = items.get(0);
		Assert.assertEquals(item0.getClass(), CSSStyle.class);
		CSSStyle bodyStyle = (CSSStyle) item0;
		Assert.assertEquals(bodyStyle.getSelector(), "body");
		Assert.assertNotNull(bodyStyle.getAttributes());
		Assert.assertEquals(bodyStyle.getAttributes().size(), 1);
		Assert.assertEquals(bodyStyle.getAttributes().get(0).getName(), "color");
		Assert.assertEquals(bodyStyle.getAttributes().get(0).getValue(), "black");
		
		CSSItem item1 = items.get(1);
		Assert.assertEquals(item1.getClass(), CSSMediaQueryList.class);
		CSSMediaQueryList mql1 = (CSSMediaQueryList) item1;
		Assert.assertNotNull(mql1.getQueries());
		Assert.assertEquals(mql1.getQueries().size(), 1);
		Assert.assertNull(mql1.getQueries().get(0).getModifier());
		Assert.assertEquals(mql1.getQueries().get(0).getType(), CSSMediaQuery.Type.SCREEN);
		Assert.assertNull(mql1.getQueries().get(0).getModifier());
		Assert.assertNotNull(mql1.getQueries().get(0).getFeatures());
		Assert.assertEquals(mql1.getQueries().get(0).getFeatures().size(), 1);
		Assert.assertEquals(mql1.getQueries().get(0).getFeatures().get(0).getName(), "max-width");
		Assert.assertEquals(mql1.getQueries().get(0).getFeatures().get(0).getValue(), "300px");
		Assert.assertNotNull(mql1.getStyles());
		Assert.assertEquals(mql1.getStyles().size(), 1);
		assertStyle(mql1.getStyles().get(0), "body", "background-color", "lightblue");
		
		CSSItem item2 = items.get(2);
		Assert.assertEquals(item2.getClass(), CSSMediaQueryList.class);
		CSSMediaQueryList mql2 = (CSSMediaQueryList) item2;
		Assert.assertNotNull(mql2.getQueries());
		Assert.assertEquals(mql2.getQueries().size(), 1);
		Assert.assertNull(mql2.getQueries().get(0).getModifier());
		Assert.assertEquals(mql2.getQueries().get(0).getType(), CSSMediaQuery.Type.SCREEN);
		Assert.assertNull(mql2.getQueries().get(0).getModifier());
		Assert.assertNotNull(mql2.getQueries().get(0).getFeatures());
		Assert.assertEquals(mql2.getQueries().get(0).getFeatures().size(), 2);
		Assert.assertEquals(mql2.getQueries().get(0).getFeatures().get(0).getName(), "max-width");
		Assert.assertEquals(mql2.getQueries().get(0).getFeatures().get(0).getValue(), "300px");
		Assert.assertEquals(mql2.getQueries().get(0).getFeatures().get(1).getName(), "min-width");
		Assert.assertEquals(mql2.getQueries().get(0).getFeatures().get(1).getValue(), "150px");
		Assert.assertNotNull(mql2.getStyles());
		Assert.assertEquals(mql2.getStyles().size(), 2);
		assertStyle(mql2.getStyles().get(0), "body", "background-color", "brown");
		assertStyle(mql2.getStyles().get(1), "a", "color", "black");
		
		CSSItem item3 = items.get(3);
		Assert.assertEquals(item3.getClass(), CSSMediaQueryList.class);
		CSSMediaQueryList mql3 = (CSSMediaQueryList) item3;
		Assert.assertNotNull(mql3.getQueries());
		Assert.assertEquals(mql3.getQueries().size(), 2);
		Assert.assertNull(mql3.getQueries().get(0).getModifier());
		Assert.assertNull(mql3.getQueries().get(0).getType());
		Assert.assertNotNull(mql3.getQueries().get(0).getFeatures());
		Assert.assertEquals(mql3.getQueries().get(0).getFeatures().size(), 1);
		Assert.assertEquals(mql3.getQueries().get(0).getFeatures().get(0).getName(), "color");
		Assert.assertNull(mql3.getQueries().get(0).getFeatures().get(0).getValue());
		
		Assert.assertNull(mql3.getQueries().get(1).getModifier());
		Assert.assertEquals(mql3.getQueries().get(1).getType(), CSSMediaQuery.Type.HANDHELD);
		Assert.assertNotNull(mql3.getQueries().get(1).getFeatures());
		Assert.assertEquals(mql3.getQueries().get(1).getFeatures().size(), 1);
		Assert.assertEquals(mql3.getQueries().get(1).getFeatures().get(0).getName(), "orientation");
		Assert.assertEquals(mql3.getQueries().get(1).getFeatures().get(0).getValue(), "landscape");
		
		CSSItem item4 = items.get(4);
		Assert.assertEquals(item4.getClass(), CSSMediaQueryList.class);
		CSSMediaQueryList mql4 = (CSSMediaQueryList) item4;
		Assert.assertNotNull(mql4.getQueries());
		Assert.assertEquals(mql4.getQueries().size(), 1);
		Assert.assertEquals(mql4.getQueries().get(0).getModifier(), CSSMediaQuery.Modifier.NOT);
		Assert.assertEquals(mql4.getQueries().get(0).getType(), CSSMediaQuery.Type.ALL);
	}
	
	private void assertStyle(CSSStyle style, String selector, String attributeName, String attributeValue) {
		Assert.assertNotNull(style);
		Assert.assertEquals(style.getSelector(), selector);
		Assert.assertNotNull(style.getAttributes());
		Assert.assertEquals(style.getAttributes().size(), 1);
		Assert.assertEquals(style.getAttributes().get(0).getName(), attributeName);
		Assert.assertEquals(style.getAttributes().get(0).getValue(), attributeValue);
	}
	
	@Test
	public void readCommentFile() throws IOException, CSSParsingException {
		CSSReader reader = new CSSReader();
		List<CSSItem> items = reader.read(getClass().getClassLoader().getResourceAsStream("comment.css"));
		
		Assert.assertTrue(items.size() == 1, "css file should contain 1 comment; did contains "+items.size()+" items");
		CSSItem comment = items.get(0);
		Assert.assertEquals(comment.getClass(), CSSComment.class);
		Assert.assertEquals(((CSSComment)comment).getText(), " this is a comment ");
	}
	
	@Test
	public void readCommentFile2() throws IOException, CSSParsingException {
		CSSReader reader = new CSSReader();
		List<CSSItem> items = reader.read(getClass().getClassLoader().getResourceAsStream("commentInClass.css"));
		
		Assert.assertTrue(items.size() == 1, "css file should contain 1 class; did contains "+items.size()+" items");
		CSSItem cls = items.get(0);
		Assert.assertEquals(cls.getClass(), CSSStyle.class);
		CSSStyle style = (CSSStyle) cls;
		Assert.assertEquals(style.getSelector(), ".aclass");
		List<CSSAttribute> attributes = style.getAttributes();
		Assert.assertNotNull(attributes);
		Assert.assertEquals(attributes.size(), 2);
		Assert.assertEquals(attributes.get(0).getName(), "color");
		Assert.assertEquals(attributes.get(0).getValue(), "red");
		Assert.assertEquals(attributes.get(1).getName(), "foo");
		Assert.assertEquals(attributes.get(1).getValue(), "\"/path/to/bar\"");
	}
}
