package org.bndly.rest.repository.resources.html;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.common.velocity.api.PartialRenderer;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import static org.apache.velocity.runtime.directive.DirectiveConstants.LINE;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NestDirective extends Directive {
	public static final String NAME = "nest";
	
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
		Context internalUserContext = ica.getInternalUserContext();
		ViewResolver viewResolver = (ViewResolver) internalUserContext.get("viewResolver");
		if (viewResolver == null) {
			return false;
		}
		PartialRenderer partialRenderer = (PartialRenderer) internalUserContext.get("partialRenderer");
		if (partialRenderer == null) {
			return false;
		}
		SimpleNode simpleNode = (SimpleNode) node.jjtGetChild(0);
		Object model = simpleNode.value(ica);
		String viewName = null;
		String varName = null;
		if (model == null) {
			model = ica.get("model");
		} else if (String.class.isInstance(model)) {
			viewName = (String) model;
			model = ica.get("model");
			String templateName = viewResolver.resolveTemplateNameForView(model, viewName);
			if (templateName == null) {
				return false;
			}
			partialRenderer.include(model, templateName, writer);
			return true;
		}
		if (node.jjtGetNumChildren() > 1) {
			simpleNode = (SimpleNode) node.jjtGetChild(1);
			Object viewNameTmp = simpleNode.value(ica);
			if (String.class.isInstance(viewNameTmp)) {
				viewName = String.class.cast(viewNameTmp);
			}
		}
		if (node.jjtGetNumChildren() > 2) {
			simpleNode = (SimpleNode) node.jjtGetChild(2);
			Object varNameTmp = simpleNode.value(ica);
			if (String.class.isInstance(varNameTmp)) {
				varName = String.class.cast(varNameTmp);
			}
		}
		String templateName = viewResolver.resolveTemplateNameForView(model, viewName);
		if (templateName == null) {
			return false;
		}
		if (varName == null) {
			partialRenderer.include(model, templateName, writer);
		} else {
			StringWriter sw = new StringWriter();
			partialRenderer.include(model, templateName, sw);
			sw.flush();
			ica.put(varName, sw.toString());
		}
		return true;
	}
}
