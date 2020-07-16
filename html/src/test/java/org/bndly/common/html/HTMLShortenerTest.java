package org.bndly.common.html;

/*-
 * #%L
 * HTML
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
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class HTMLShortenerTest {

	String _1 = "Important";
	String _2 = "Important do";
	String _3 = "Important do somethin";
	String _4 = "Important do something awesome";

	@Test
	public void testShortener() throws HTMLParsingException, IOException {

		final String input = "<p><h1>Important&nbsp;</h1>do<span> something</span> awesome.<br>this can be our best code ever.\n"
				+ "</p>";
		String shortened = HTMLShortener.shorten(input, _1.length());
		Assert.assertEquals("Important", shortened);
		shortened = HTMLShortener.shorten(input, _2.length());
		Assert.assertEquals("Important&nbsp;do", shortened);

		String shortenedWithMarkup = HTMLShortener.shortenAndKeepMarkup(input, _1.length(), "...");
		Assert.assertEquals("<p><h1>Important...</h1></p>", shortenedWithMarkup);
		HTML html = new Parser(input).parse().getHTML();
		HTMLShortener.shortenAndKeepMarkupOnHTML(html, _1.length(), "...");
		String plain = html.toPlainString();
		Assert.assertEquals("<p><h1>Important...</h1></p>", plain);

		shortenedWithMarkup = HTMLShortener.shortenAndKeepMarkup(input, _2.length());
		Assert.assertEquals("<p><h1>Important&nbsp;</h1>do</p>", shortenedWithMarkup);
		html = new Parser(input).parse().getHTML();
		HTMLShortener.shortenAndKeepMarkupOnHTML(html, _2.length(), "");
		plain = html.toPlainString();
		Assert.assertEquals("<p><h1>Important&nbsp;</h1>do</p>", plain);

		shortenedWithMarkup = HTMLShortener.shortenAndKeepMarkup(input, _3.length());
		Assert.assertEquals("<p><h1>Important&nbsp;</h1>do<span> somethin</span></p>", shortenedWithMarkup);
		html = new Parser(input).parse().getHTML();
		HTMLShortener.shortenAndKeepMarkupOnHTML(html, _3.length(), "");
		plain = html.toPlainString();
		Assert.assertEquals("<p><h1>Important&nbsp;</h1>do<span> somethin</span></p>", plain);

		shortenedWithMarkup = HTMLShortener.shortenAndKeepMarkup(input, _4.length());
		Assert.assertEquals("<p><h1>Important&nbsp;</h1>do<span> something</span> awesome</p>", shortenedWithMarkup);
		html = new Parser(input).parse().getHTML();
		HTMLShortener.shortenAndKeepMarkupOnHTML(html, _4.length(), "");
		plain = html.toPlainString();
		Assert.assertEquals("<p><h1>Important&nbsp;</h1>do<span> something</span> awesome</p>", plain);

	}

	@Test
	public void testShortenerWithContentAtStart() throws HTMLParsingException {
		String input = "Imp<p><h1>ortant</h1>do<span> something</span> awesome.<br>this can be our best code ever.\n</p>";
		String shortened = HTMLShortener.shorten(input, _1.length());
		Assert.assertEquals("Important", shortened);
	}

	@Test
	public void testShortenerWithNewLineAtShortenPosition() throws HTMLParsingException, IOException {
		String input = "<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n"
				+ "<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n"
				+ "<p>aaaaaaaaaaaaaaaaaaaa</p>";
		HTML html = new Parser(input).parse().getHTML();
		HTMLShortener.shortenAndKeepMarkupOnHTML(html, 150, "...");
		String shortened = html.toPlainString();
		String expected = "<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n"
				+ "<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa...</p>";
		Assert.assertEquals(expected, shortened);

		input = "<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n" +
"<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n" +
"<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n" +
"<p>aaaaa</p>\n" +
"<p> aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n" +
"<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n" +
"<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa&quot;aaaaaaaaaaaaaa&quot;aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa&quot;aaaaaaaaaaaa&quot;aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n" +
"<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n" +
"<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>\n" +
"<p>aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa</p>";
		
		html = new Parser(input).parse().getHTML();
		html = html.copy();
		HTMLShortener.shortenAndKeepMarkupOnHTML(html, 300, "...");
		shortened = html.toPlainString();
		shortened=shortened;
	}
	
	@Test
	public void testShorteningWithALinkInHTML() throws HTMLParsingException, IOException {
		String input = "<p>aaaaaaaaaaaaaaaaaaaaaaaaaa&amp;aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa<a target=\"_blank\" href=\"http://www.foobar.com/demo/blablablaaaaaaaa/mam/de/xmlsd?l=de&amp;cid=47112\" data-xyz123=\"false\">aaaaaaaaaaaaaaaaa</a>a<br>\n" +
"<br>\n" +
"<b>aaaaaaaaaaaaaaaaaaaaaaaaaaaaa</b><br>\n" +
"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa&amp;aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa<br>\n" +
"<br>\n" +
"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa<a target=\"_blank\" href=\"http://www.foobarbazbingo.com/78378774747387378378378738/asdaaaaaasdaa?x=de&amp;cid=12312\" adhocenable=\"false\">aaaaaaaaaaaaaaaaa</a></p>";
		
		HTML html = new Parser(input).parse().getHTML();
		String replacement = "...";
		for (int i = input.length(); i > 0; i--) {
			HTML copy = html.copy();
			HTMLShortener.shortenAndKeepMarkupOnHTML(copy, i, "...");
			String s = copy.toPlainString();
			Assert.assertTrue(!s.contains(replacement+replacement), "shortened string should contain replacement only once: i="+i);
		}
	}
}
