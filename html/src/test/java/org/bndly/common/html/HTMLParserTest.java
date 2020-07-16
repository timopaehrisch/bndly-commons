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
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A test that tries to parse various HTML documents.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class HTMLParserTest {

	@Test
	public void testHTMLEntities() throws IOException, HTMLParsingException {
		HTML5EntityMap.INSTANCE.hasEntity('\n');
	}
	
	@Test
	public void testHTMLTagCaseInsensitive() throws IOException, HTMLParsingException {
		List<Content> content = new Parser("<A><br></a>", new DefaultParserConfig(){
			
			@Override
			public boolean isAutomaticLowerCaseEnabled(String tagName) {
				return true;
			}
		}).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		Tag tag = (Tag) content.get(0);
		Assert.assertEquals(tag.getName(), "a");
		
		content = new Parser("<a><br></A>", new DefaultParserConfig(){
			
			@Override
			public boolean isAutomaticLowerCaseEnabled(String tagName) {
				return true;
			}
		}).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (Tag) content.get(0);
		Assert.assertEquals(tag.getName(), "a");
		
		content = new Parser("<a><BR></A>", new DefaultParserConfig(){
			
			@Override
			public boolean isAutomaticLowerCaseEnabled(String tagName) {
				return true;
			}
		}).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (Tag) content.get(0);
		Assert.assertEquals(tag.getName(), "a");
		
		try {
			content = new Parser("<a><br></A>", new DefaultParserConfig(){

				@Override
				public boolean isAutomaticLowerCaseEnabled(String tagName) {
					return false;
				}
			}).parse().getContent();
			Assert.fail("expected exception");
		} catch(HTMLParsingException e) {
		}
	}
	
	@Test
	public void testUnquotedAttributeValue() throws HTMLParsingException, IOException {
		List<Content> content;
		Tag tag;
		Attribute attribute;
		content = new Parser("<a att=value></a>", new DefaultParserConfig(){

			@Override
			public boolean isUnquotedAttributeValueTolerated() {
				return true;
			}
			
		}).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (Tag) content.get(0);
		Assert.assertEquals(tag.getName(), "a");
		attribute = tag.getAttribute("att");
		Assert.assertNotNull(attribute);
		Assert.assertEquals(attribute.getName(), "att");
		Assert.assertEquals(attribute.getValue(), "value");
		
		content = new Parser("<a att=value ></a>", new DefaultParserConfig(){

			@Override
			public boolean isUnquotedAttributeValueTolerated() {
				return true;
			}
			
		}).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (Tag) content.get(0);
		Assert.assertEquals(tag.getName(), "a");
		attribute = tag.getAttribute("att");
		Assert.assertNotNull(attribute);
		Assert.assertEquals(attribute.getName(), "att");
		Assert.assertEquals(attribute.getValue(), "value");
		
		content = new Parser("<a att='value' ></a>", new DefaultParserConfig(){

			@Override
			public boolean isUnquotedAttributeValueTolerated() {
				return true;
			}
			
		}).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (Tag) content.get(0);
		Assert.assertEquals(tag.getName(), "a");
		attribute = tag.getAttribute("att");
		Assert.assertNotNull(attribute);
		Assert.assertEquals(attribute.getName(), "att");
		Assert.assertEquals(attribute.getValue(), "value");
		
		content = new Parser("<a att1=value1 att2=value2></a>", new DefaultParserConfig(){

			@Override
			public boolean isUnquotedAttributeValueTolerated() {
				return true;
			}
			
		}).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (Tag) content.get(0);
		Assert.assertEquals(tag.getName(), "a");
		attribute = tag.getAttribute("att1");
		Assert.assertNotNull(attribute);
		Assert.assertEquals(attribute.getName(), "att1");
		Assert.assertEquals(attribute.getValue(), "value1");
		attribute = tag.getAttribute("att2");
		Assert.assertNotNull(attribute);
		Assert.assertEquals(attribute.getName(), "att2");
		Assert.assertEquals(attribute.getValue(), "value2");
		Assert.assertEquals(tag.getAttributes().size(), 2);
		
		try {
			content = new Parser("<a att=value ></a>", new DefaultParserConfig(){

				@Override
				public boolean isUnquotedAttributeValueTolerated() {
					return false;
				}

			}).parse().getContent();
			Assert.fail("expected exception");
		} catch(HTMLParsingException e) {
		}
	}
	
	
	@Test
	public void testUnbalancedTag() throws HTMLParsingException, IOException {
		String[] examples = new String[]{
			"<a><br>", 
			"<table><tbody><tr><td></td></tbody></table>", 
			"<table><tbody><tr><td></tr></tbody></table>", 
			"<table><tbody><tr><td></td></tr></table>", 
			"</table>", 
			"</table", 
			"</", 
			"<", 
			"<div></span></div>",
			"<div></span></di",
			"<div><span></div>",
			"<div><span></",
			"<div></div",
			"<div/",
			"<div  ",
			"<div  attribute",
			"<div  attribute=",
			"<div  attribute='",
			"<div  attribute='value'",
			"<div  attribute='value"
		};
		for (String example : examples) {
			try {
				new Parser(example, new DefaultParserConfig(){

					@Override
					public boolean isUnbalancedTagTolerated() {
						return false;
					}
					
				}).parse();
				Assert.fail("expected exception: "+example);
			} catch (HTMLParsingException | IOException ex) {
			}
		}
		for (String example : examples) {
			try {
				new Parser(example, new DefaultParserConfig() {

					@Override
					public boolean isUnbalancedTagTolerated() {
						return true;
					}

				}).parse();
			} catch (HTMLParsingException | IOException ex) {
				Assert.fail(ex.getMessage()+": " + example, ex);
			}
		}
	}
	
	@Test
	public void testIncompleteEntity() throws HTMLParsingException, IOException {
		DefaultParserConfig cfg = new DefaultParserConfig() {

			@Override
			public boolean isIncompleteEntityTolerated() {
				return true;
			}
		};
		List<Content> content;
		content = new Parser("&",cfg).parse().getContent();
		assertTotalChildren(content, 1);
		content = new Parser("& ",cfg).parse().getContent();
		assertTotalChildren(content, 2);
	}
	
	@Test
	public void testComments() throws HTMLParsingException, IOException {
		DefaultParserConfig cfg = new DefaultParserConfig() {

			@Override
			public boolean isCommentParsingEnabled() {
				return true;
			}

		};
		List<Content> content;
		content = new Parser("<!--comment-->", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), "comment");
		
		content = new Parser("<!-- comment -->", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), " comment ");
		
		content = new Parser("<!-- -->", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), " ");
		
		content = new Parser("<!---->", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), "");
		
		cfg = new DefaultParserConfig();
		content = new Parser("<!--comment-->", cfg).parse().getContent();
		assertTotalChildren(content, 0);
		
		content = new Parser("<!-- comment -->", cfg).parse().getContent();
		assertTotalChildren(content, 0);
		
		content = new Parser("<!-- -->", cfg).parse().getContent();
		assertTotalChildren(content, 0);
		
		content = new Parser("<!---->", cfg).parse().getContent();
		assertTotalChildren(content, 0);
		
		cfg = new DefaultParserConfig(){

			@Override
			public boolean isCommentParsingEnabled() {
				return true;
			}

			@Override
			public boolean isUnbalancedTagTolerated() {
				return true;
			}
			
		};
		content = new Parser("<!--comment", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), "comment");
		
		content = new Parser("<!-- comment ", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), " comment ");
		
		content = new Parser("<!-- ", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), " ");
		
		content = new Parser("<!--", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), "");
		
		content = new Parser("<!--comment--", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), "comment--");
		
		content = new Parser("<!-- comment --", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), " comment --");
		
		content = new Parser("<!-- --", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), " --");
		
		content = new Parser("<!----", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		assertCommentText(assertComment(content.get(0)), "--");
	}
	
	@Test
	public void testUnescapedGT() throws HTMLParsingException, IOException {
		List<Content> content = new Parser(">", new DefaultParserConfig(){
			
			@Override
			public boolean isIncompleteEntityTolerated() {
				return true;
			}
			
		}).parse().getContent();
		assertTotalChildren(content, 1);
		Entity entity = (Entity) content.get(0);
		Assert.assertEquals(entity.getName(), "gt");
	}
	
	@Test
	public void testUnbalanced() throws HTMLParsingException, IOException{
		DefaultParserConfig cfg = new DefaultParserConfig() {
			
			@Override
			public boolean isUnbalancedTagTolerated() {
				return true;
			}
			
		};
		List<Content> content;
		AbstractTag tag;
		Attribute attribute;
		content = new Parser("<div/", cfg).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 0);
		
		content = new Parser("<div  ", cfg).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 0);
		
		content = new Parser("<div  attribute", cfg).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 1);
		attribute = tag.getAttributes().get(0);
		Assert.assertEquals(attribute.getName(), "attribute");
		Assert.assertNull(attribute.getValue());
		
		content = new Parser("<div  attribute=", cfg).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 1);
		attribute = tag.getAttributes().get(0);
		Assert.assertEquals(attribute.getName(), "attribute");
		Assert.assertNull(attribute.getValue());
		
		content = new Parser("<div  attribute='", cfg).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 1);
		attribute = tag.getAttributes().get(0);
		Assert.assertEquals(attribute.getName(), "attribute");
		Assert.assertTrue(attribute.getValue().isEmpty());
		
		content = new Parser("<div  attribute='value'", cfg).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 1);
		attribute = tag.getAttributes().get(0);
		Assert.assertEquals(attribute.getName(), "attribute");
		Assert.assertEquals(attribute.getValue(), "value");
		
		content = new Parser("<div  attribute='value", cfg).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 1);
		attribute = tag.getAttributes().get(0);
		Assert.assertEquals(attribute.getName(), "attribute");
		Assert.assertEquals(attribute.getValue(), "value");
		
		content = new Parser("<div  attribute='value' /", cfg).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 1);
		attribute = tag.getAttributes().get(0);
		Assert.assertEquals(attribute.getName(), "attribute");
		Assert.assertEquals(attribute.getValue(), "value");
		
		content = new Parser("<div  attribute=value /", new DefaultParserConfig(){

			@Override
			public boolean isUnquotedAttributeValueTolerated() {
				return true;
			}

			@Override
			public boolean isUnbalancedTagTolerated() {
				return true;
			}
			
		}).parse().getContent();
		Assert.assertEquals(content.size(), 1);
		tag = (AbstractTag) content.get(0);
		Assert.assertEquals(tag.getName(), "div");
		Assert.assertEquals(tag.getAttributes().size(), 1);
		attribute = tag.getAttributes().get(0);
		Assert.assertEquals(attribute.getName(), "attribute");
		Assert.assertEquals(attribute.getValue(), "value");
		
		content = new Parser("<table><tbody><tr><td></td></tbody></table>", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		Tag fullTag = assertChildIsTag(content, 0, "table");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "tbody");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "tr");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "td");
		assertTotalChildren(fullTag, 0);
		
		content = new Parser("<table><tbody><tr><td></tr></tbody></table>", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		fullTag = assertChildIsTag(content, 0, "table");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "tbody");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "tr");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "td");
		assertTotalChildren(fullTag, 0);
		
		content = new Parser("<table><tbody><tr><td></td></tr></table>", cfg).parse().getContent();
		assertTotalChildren(content, 1);
		fullTag = assertChildIsTag(content, 0, "table");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "tbody");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "tr");
		assertTotalChildren(fullTag, 1);
		fullTag = assertChildIsTag(fullTag, 0, "td");
		assertTotalChildren(fullTag, 0);
	}
	
	Tag assertChildIsTag(ContentContainer contentContainer, int index, String tagName) {
		return assertChildIsTag(contentContainer.getContent(), index, tagName);
	}
	
	Tag assertChildIsTag(List<Content> content, int index, String tagName) {
		Assert.assertNotNull(content);
		Tag tag = (Tag) content.get(index);
		Assert.assertEquals(tag.getName(), tagName);
		return tag;
	}
	
	Comment assertComment(Content content) {
		Assert.assertTrue(Comment.class.isInstance(content));
		return (Comment) content;
	}
	
	Comment assertCommentText(Comment comment, String commentString) {
		Assert.assertNotNull(comment);
		Assert.assertEquals(comment.getValue(), commentString);
		return comment;
	}
	
	void assertTotalChildren(ContentContainer contentContainer, int size) {
		assertTotalChildren(contentContainer.getContent(), size);
	}
	
	void assertTotalChildren(List<Content> content, int size) {
		Assert.assertNotNull(content);
		Assert.assertEquals(content.size(), size);
	}
	
	@Test
	public void testSamplesFromVespaPico() throws IOException, HTMLParsingException {
		String[] goodCases = new String[]{
			"<test id='test'>test</test>"
			,"<br>"
			,"<br />"
			,"<tag attribut></tag>"
			,"<tag attribut='attribut'></tag>"
		};
		for (String goodCase : goodCases) {
			try {
				new Parser(goodCase).parse();
			} catch (HTMLParsingException e) {
				Assert.fail("could not parse good case: "+goodCase, e);
			}
		}
		String[] badCases = new String[]{
			"<test111 id='test'>test</test222>"
			,"div></div>"
			,"<div</div>"
			,"<div>"
		};
		for (String badCase : badCases) {
			try {
				new Parser(badCase).parse();
				Assert.fail("expected exception for: "+badCase);
			} catch (HTMLParsingException e) {
			}
		}
	}
	
	@Test
	public void testEscapingInAttribute() throws HTMLParsingException, IOException {
		new Parser("<tag att=\"value>\"></tag>").parse();
		try {
			new Parser("<tag att=\"value\"\"></tag>").parse();
			Assert.fail("expected exception");
		} catch(HTMLParsingException e) {
			
		}
	}
	
	@Test
	public void testTextAndEntities() throws IOException, HTMLParsingException {
		String input = "Hallo Welt. &nbsp; Wie geht es dir?";
		String input2 = "&nbsp;&nbsp;Hallo Welt.&nbsp;";
		List<Content> content = new Parser(input).parse().getContent();
		Assert.assertNotNull(content);
		Assert.assertEquals(content.size(), 3);

		content = new Parser(input2).parse().getContent();
		Assert.assertNotNull(content);
		Assert.assertEquals(content.size(), 4);
	}

	@Test
	public void testCleanXHTML() throws IOException, HTMLParsingException {
		String input = "<test foo=\"bar\"></test>";
		String input2 = "<test foo=\"bar\" bar=\"baz\"></test>";
		List<Content> content = new Parser(input).parse().getContent();
		Assert.assertNotNull(content);
		Assert.assertEquals(content.size(), 1);
		content = new Parser(input2).parse().getContent();
		Assert.assertNotNull(content);
		Assert.assertEquals(content.size(), 1);
	}
	
	@Test
	public void testBrokenHTML() throws IOException, HTMLParsingException {
		String input = "<test foo=\"bar\"></test";
		try {
			List<Content> result = new Parser(input).parse().getContent();
			Assert.fail("exception was expected");
		} catch (HTMLParsingException e) {
		}
	}
	
	@Test
	public void testBrokenHTML2() throws IOException, HTMLParsingException {
		String input = "<test foo=\"bar\">";
		try {
			List<Content> result = new Parser(input).parse().getContent();
			Assert.fail("exception was expected");
		} catch (HTMLParsingException e) {
		}
		List<Content> result = new Parser(input, new DefaultParserConfig(){

			@Override
			public boolean isSelfClosingTag(String tagName) {
				return "test".equals(tagName);
			}
			
		}).parse().getContent();
		Assert.assertNotNull(result);
		Assert.assertEquals(result.size(), 1);
		Content tag = unbox(result.get(0));
		Assert.assertEquals(tag.getClass(), SelfClosingTag.class);
	}
	
	private Content unbox(Content content) {
		if(ProxyContent.class.isInstance(content)) {
			return unbox(((ProxyContent)content).getContent());
		}
		return content;
	}
	
	@Test
	public void testBrokenHTML3() throws IOException, HTMLParsingException {
		String input = "<test foo=\"bar\"";
		try {
			List<Content> result = new Parser(input).parse().getContent();
			Assert.fail("exception was expected");
		} catch (HTMLParsingException e) {
		}
	}
	
	@Test
	public void testBrokenHTML4() throws IOException, HTMLParsingException {
		String input = "div>asd</div>";
		try {
			List<Content> result = new Parser(input).parse().getContent();
			Assert.fail("exception was expected");
		} catch (HTMLParsingException e) {
		}
	}
	
	@Test
	public void testBrokenHTML5() throws IOException, HTMLParsingException {
		String input = "divasd</div>";
		try {
			List<Content> result = new Parser(input).parse().getContent();
			Assert.fail("exception was expected");
		} catch (HTMLParsingException e) {
		}
	}
	@Test
	public void testBrokenHTML55() throws IOException, HTMLParsingException {
		String input = "</div>";
		try {
			List<Content> result = new Parser(input).parse().getContent();
			Assert.fail("exception was expected");
		} catch (HTMLParsingException e) {
		}
	}
	
	@Test
	public void testBrokenHTML6() throws IOException, HTMLParsingException {
		String input = "divasd<";
		try {
			List<Content> result = new Parser(input).parse().getContent();
			Assert.fail("exception was expected");
		} catch (HTMLParsingException e) {
		}
	}
	
	@Test
	public void testIncompleteEntityXHTML() throws IOException, HTMLParsingException {
		String input = "<span></span>&incomplete";
		try {
			new Parser(input).parse();
			Assert.fail("expected a HTMLParsingException because entity is incomplete");
		} catch(HTMLParsingException e) {
			// expected
		}
		List<Content> content = new Parser(input+";").parse().getContent();
		Assert.assertNotNull(content);
		Assert.assertEquals(content.size(), 2);
	}

	@Test
	public void testCleanXHTMLWithNestedElements() throws IOException, HTMLParsingException {
		String input = "<test foo=\"bar\"><foo/><br><foo /></test>";
		List<Content> content = new Parser(input).parse().getContent();
		Assert.assertNotNull(content);
		Assert.assertEquals(content.size(), 1);
		Tag t = (Tag) content.get(0);
		Assert.assertNotNull(t.getAttributes());
		Assert.assertEquals(t.getAttributes().size(), 1);
		Assert.assertEquals(t.getName(), "test");
		Assert.assertNotNull(t.getContent());
		Assert.assertEquals(t.getContent().size(), 3);
	}
	
	@Test
	public void testSelfClosingTags() throws IOException, HTMLParsingException {
		String[] tests = new String[]{"<img src=\"asdasd\">", "<img src=\"asdasd\"/>", "<img src=\"asdasd\" />"};
		for (String test : tests) {
			List<Content> content = new Parser(test).parse().getContent();
			Assert.assertNotNull(content);
			Assert.assertEquals(content.size(), 1);
			SelfClosingTag t = (SelfClosingTag) content.get(0);
			Assert.assertEquals(t.getName(), "img");
			Assert.assertNotNull(t.getAttributes());
			Attribute att = t.getAttributes().get(0);
			Assert.assertEquals(att.getName(), "src");
			Assert.assertEquals(att.getValue(), "asdasd");
		}
		ParserConfig cfg = new DefaultParserConfig(){

			@Override
			public boolean isSelfClosingTag(String tagName) {
				return false;
			}
			
		};
		for (int i = 0; i < tests.length; i++) {
			String test = tests[i];
			if(i == 0) {
				try {
					new Parser(test, cfg).parse().getContent();
					Assert.fail("expected an exception");
				} catch(HTMLParsingException e) {
				}
			} else {
				List<Content> content = new Parser(test, cfg).parse().getContent();
				Assert.assertNotNull(content);
				Assert.assertEquals(content.size(), 1);
				SelfClosingTag t = (SelfClosingTag) content.get(0);
				Assert.assertEquals(t.getName(), "img");
				Assert.assertNotNull(t.getAttributes());
				Attribute att = t.getAttributes().get(0);
				Assert.assertEquals(att.getName(), "src");
				Assert.assertEquals(att.getValue(), "asdasd");
			}
		}
	}

	@Test
	public void testMixedXHTML() throws IOException, HTMLParsingException {
		PrettyPrintHandler handler = new PrettyPrintHandler();
		String input = "foo.&nbsp;barrrr<test foo=\"bar\"></test>bla";
		List<Content> content = new Parser(input)
				.handler(handler)
				.parse()
				.getContent();
		Assert.assertNotNull(content);
		Assert.assertEquals(content.size(), 5);
		Text t = (Text) content.get(0);
		Entity e = (Entity) content.get(1);
		Text t2 = (Text) content.get(2);
		Tag p = (Tag) content.get(3);
		Text t3 = (Text) content.get(4);
		Assert.assertEquals(t.getValue(), "foo.");
		Assert.assertEquals(e.getName(), "nbsp");
		Assert.assertEquals(t2.getValue(), "barrrr");
		Assert.assertEquals((p).getName(), "test");
		Assert.assertEquals(t3.getValue(), "bla");
		String pretty = handler.getPrettyString();
		Assert.assertEquals(pretty, "foo.\n&nbsp;\nbarrrr\n<test foo=\"bar\">\n</test>\nbla\n");
	}
	
	public void validateHTMLContent(List<Content> contents) {
		if(contents == null || contents.isEmpty()) {
			return;
		}
		for (Content content : contents) {
			validateHTMLContent(content);
		}
	}
	
	public void validateHTMLContent(Content content) {
		if(Tag.class.isInstance(content)) {
			// validate tags
			List<Content> tagContent = ((Tag)content).getContent();
			validateHTMLContent(tagContent);
		} else if(SelfClosingTag.class.isInstance(content)) {
			// validate self closing tags like br img or stuff like <asdasd />
		} else if(Text.class.isInstance(content)) {
			// validate text contents
		} else if(Entity.class.isInstance(content)) {
			// validate html entities like &amp; (or just ignore those)
		} else if(ProxyContent.class.isInstance(content)) {
			// proxy content is just a proxy for real content. just pipe the proxyed content again into the validation
			validateHTMLContent(((ProxyContent)content).getContent());
		} else {
			throw new IllegalStateException("unsupported content");
		}
	}
}
