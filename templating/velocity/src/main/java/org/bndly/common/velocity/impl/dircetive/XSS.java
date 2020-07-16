package org.bndly.common.velocity.impl.dircetive;

/*-
 * #%L
 * Velocity
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
import java.io.Writer;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.owasp.encoder.Encode;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class XSS extends Directive {

	public static final String NAME = "xss";
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getType() {
		return LINE;
	}

	@Override
	public boolean render(InternalContextAdapter ica, Writer writer, Node node) throws IOException, ResourceNotFoundException, ParseErrorException, MethodInvocationException {
		SimpleNode simpleNode = (SimpleNode) node.jjtGetChild(0);
		Object tmp = simpleNode.value(ica);
		String encoderName = tmp == null ? null : tmp.toString();
		if (encoderName == null) {
			return false;
		}
		simpleNode = (SimpleNode) node.jjtGetChild(1);
		Object toEncode = simpleNode.value(ica);
		if (toEncode == null) {
			return false;
		}
		String stringToEncode = toEncode.toString();
		
		if ("html".equals(encoderName)) {
			Encode.forHtml(writer, stringToEncode);
		} else if ("htmlattribute".equals(encoderName)) {
			Encode.forHtmlAttribute(writer, stringToEncode);
		} else if ("htmlcontent".equals(encoderName)) {
			Encode.forHtmlContent(writer, stringToEncode);
		} else if ("htmlunquotedattribute".equals(encoderName)) {
			Encode.forHtmlUnquotedAttribute(writer, stringToEncode);
		} else if ("javascript".equals(encoderName)) {
			Encode.forJavaScript(writer, stringToEncode);
		} else if ("javascriptattribute".equals(encoderName)) {
			Encode.forJavaScriptAttribute(writer, stringToEncode);
		} else if ("javascriptblock".equals(encoderName)) {
			Encode.forJavaScriptBlock(writer, stringToEncode);
		} else if ("javascriptsource".equals(encoderName)) {
			Encode.forJavaScriptSource(writer, stringToEncode);
		} else if ("xml".equals(encoderName)) {
			Encode.forXml(writer, stringToEncode);
		} else if ("xmlattribute".equals(encoderName)) {
			Encode.forXmlAttribute(writer, stringToEncode);
		} else if ("xmlcomment".equals(encoderName)) {
			Encode.forXmlComment(writer, stringToEncode);
		} else if ("xmlcontent".equals(encoderName)) {
			Encode.forXmlContent(writer, stringToEncode);
		} else {
			return false;
		}
		return true;
	}
	
}
