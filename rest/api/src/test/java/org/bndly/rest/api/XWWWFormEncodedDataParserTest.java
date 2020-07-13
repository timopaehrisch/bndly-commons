package org.bndly.rest.api;

/*-
 * #%L
 * REST API
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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class XWWWFormEncodedDataParserTest {
	
	private static class MapParsingListener implements XWWWFormEncodedDataParser.Listener {
		private final Map<String, String> map = new LinkedHashMap<>();
		private String currentVariable;
		@Override
		public XWWWFormEncodedDataParser.IterationResult onVariable(String variable) {
			if (currentVariable != null) {
				map.put(currentVariable, null);
			}
			currentVariable = variable;
			return XWWWFormEncodedDataParser.IterationResult.CONTINUE;
		}

		@Override
		public XWWWFormEncodedDataParser.IterationResult onVariableValue(String value) {
			map.put(currentVariable, value);
			currentVariable = null;
			return XWWWFormEncodedDataParser.IterationResult.CONTINUE;
		}

		@Override
		public void onEnd() {
			if (currentVariable != null) {
				map.put(currentVariable, null);
			}
		}

		public Map<String, String> getMap() {
			return map;
		}
		
	}
	
	@Test
	public void testSingleVariableNoValue() throws IOException {
		XWWWFormEncodedDataParser parser = new XWWWFormEncodedDataParser(new PathCoder.UTF8());
		MapParsingListener listener = new MapParsingListener();
		parser.parse("variable", listener);
		Assert.assertTrue(listener.getMap().containsKey("variable"));
		
		listener = new MapParsingListener();
		parser.parse("variable&v2", listener);
		Assert.assertTrue(listener.getMap().containsKey("variable"));
		Assert.assertTrue(listener.getMap().containsKey("v2"));
	}
	
	@Test
	public void testSingleVariableWithValue() throws IOException {
		XWWWFormEncodedDataParser parser = new XWWWFormEncodedDataParser(new PathCoder.UTF8());
		MapParsingListener listener = new MapParsingListener();
		parser.parse("variable=value", listener);
		Assert.assertEquals(listener.getMap().get("variable"), "value");

		listener = new MapParsingListener();
		parser.parse("variable=value&v2=val2", listener);
		Assert.assertEquals(listener.getMap().get("variable"), "value");
		Assert.assertEquals(listener.getMap().get("v2"), "val2");

		listener = new MapParsingListener();
		parser.parse("variable=value&v3", listener);
		Assert.assertEquals(listener.getMap().get("variable"), "value");
		Assert.assertTrue(listener.getMap().containsKey("v3"));

		listener = new MapParsingListener();
		parser.parse("variable=value&v3=", listener);
		Assert.assertEquals(listener.getMap().get("variable"), "value");
		Assert.assertTrue(listener.getMap().containsKey("v3"));
	}
	
	@Test
	public void testTerminate() throws IOException {
		XWWWFormEncodedDataParser parser = new XWWWFormEncodedDataParser(new PathCoder.UTF8());
		MapParsingListener listener = new MapParsingListener() {

			@Override
			public XWWWFormEncodedDataParser.IterationResult onVariable(String variable) {
				super.onVariable(variable);
				return XWWWFormEncodedDataParser.IterationResult.TERMINATE;
			}

		};
		parser.parse("variable=value", listener);
		Assert.assertEquals(listener.getMap().get("variable"), null);
		Assert.assertTrue(listener.getMap().containsKey("variable"), null);

		listener.getMap().clear();
		parser.parse("variable=value&v2=val2", listener);
		Assert.assertEquals(listener.getMap().get("variable"), null);
		Assert.assertTrue(listener.getMap().containsKey("variable"), null);
		Assert.assertEquals(listener.getMap().get("v2"), null);
		Assert.assertTrue(!listener.getMap().containsKey("v2"), null);
	}
	
	@Test
	public void testDecoding() throws IOException {
		XWWWFormEncodedDataParser parser = new XWWWFormEncodedDataParser(new PathCoder.UTF8());
		MapParsingListener listener = new MapParsingListener();
		parser.parse("variable=a%20b", listener);
		Assert.assertEquals(listener.getMap().get("variable"), "a b");
		parser.parse("variable=a+b", listener);
		Assert.assertEquals(listener.getMap().get("variable"), "a b");
		parser.parse("variable=%c3%a4", listener);
		Assert.assertEquals(listener.getMap().get("variable"), "ä");
	}
}
