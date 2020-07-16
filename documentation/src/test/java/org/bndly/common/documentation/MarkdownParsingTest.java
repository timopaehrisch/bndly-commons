package org.bndly.common.documentation;

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

import org.bndly.common.documentation.impl.BundleDocumentationImpl;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Heading;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MarkdownParsingTest {

	@Test
	public void testParsing() throws IOException {
		Parser parser = Parser.builder().build();
		try (Reader reader = Files.newBufferedReader(Paths.get("src", "test", "resources", "test.md"), Charset.forName("UTF-8"))) {
			Node document = parser.parseReader(reader);
			document.accept(new AbstractVisitor() {

				@Override
				public void visit(Image image) {
					String dest = image.getDestination();
					String title = image.getTitle();
				}

				@Override
				public void visit(Heading heading) {
					heading = heading;
				}

			});
			HtmlRenderer renderer = HtmlRenderer.builder().build();
			String rendered = renderer.render(document);
			rendered = rendered;
		}
	}
	
	@Test
	public void testURIDetection() {
		Assert.assertTrue(BundleDocumentationImpl.isExternalUri("http://www.example.com/image.jpg"));
		Assert.assertTrue(BundleDocumentationImpl.isExternalUri("/foo/bar.jpg"));
		Assert.assertFalse(BundleDocumentationImpl.isExternalUri("foo/bar.jpg"));
	}
}
