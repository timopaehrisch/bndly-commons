package org.bndly.rest.entity.resources.descriptor;

/*-
 * #%L
 * REST Entity Resource
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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TypeNameReplacingDocumentationDecoratorTest {

	private final TypeNameReplacingDocumentationDecorator.VariableResolver voidResolver = new TypeNameReplacingDocumentationDecorator.VariableResolver() {
		@Override
		public String resolve(String variableName) {
			return null;
		}
	};

	@Test
	public void testParsing() {
		String input = "asd";
		String output = TypeNameReplacingDocumentationDecorator.filterStringForVariables(input, voidResolver);
		Assert.assertEquals(output, input);
	}

	@Test
	public void testSingleVariable() {
		String input = "{{VAR}}";
		String output = TypeNameReplacingDocumentationDecorator.filterStringForVariables(input, voidResolver);
		Assert.assertEquals(output, "");
	}
	
	@Test
	public void testMultipleVariable() {
		String input = "{{VAR}} asd {{VAR2}}";
		String output = TypeNameReplacingDocumentationDecorator.filterStringForVariables(input, voidResolver);
		Assert.assertEquals(output, " asd ");
	}

	@Test
	public void testEmptyVariable() {
		String input = "{{}}";
		String output = TypeNameReplacingDocumentationDecorator.filterStringForVariables(input, voidResolver);
		Assert.assertEquals(output, "");
	}

	@Test
	public void testBrokenVariable() {
		String input = "{{}";
		String output = TypeNameReplacingDocumentationDecorator.filterStringForVariables(input, voidResolver);
		Assert.assertEquals(output, "{{}");
	}

	@Test
	public void testBrokenVariable2() {
		String input = "{}}";
		String output = TypeNameReplacingDocumentationDecorator.filterStringForVariables(input, voidResolver);
		Assert.assertEquals(output, "{}}");
	}

	@Test
	public void testBrokenVariable3() {
		String input = "{{";
		String output = TypeNameReplacingDocumentationDecorator.filterStringForVariables(input, voidResolver);
		Assert.assertEquals(output, "{{");
	}
	@Test
	public void testBrokenVariable4() {
		String input = "{";
		String output = TypeNameReplacingDocumentationDecorator.filterStringForVariables(input, voidResolver);
		Assert.assertEquals(output, "{");
	}
}
