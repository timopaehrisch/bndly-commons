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

import org.bndly.common.velocity.api.PartialRenderer;
import org.bndly.common.velocity.api.VelocityTemplate;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.velocity.context.Context;
import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.directive.Directive;
import org.apache.velocity.runtime.parser.node.Node;
import org.apache.velocity.runtime.parser.node.SimpleNode;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class Partial extends Directive {

	public static final String NAME = "partial";
	
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
		PartialRenderer partialRenderer = (PartialRenderer) internalUserContext.get("partialRenderer");
		if (partialRenderer == null) {
			return false;
		}
		SimpleNode simpleNode = (SimpleNode) node.jjtGetChild(0);
		Object model = simpleNode.value(ica);
		String templateName = null;
		String varName = null;
		if (!String.class.isInstance(model)) {
			if (model == null) {
				model = ica.get("model");
			}

			if (node.jjtGetNumChildren() > 1) {
				simpleNode = (SimpleNode) node.jjtGetChild(1);
				Object templateNameTmp = simpleNode.value(ica);
				if (String.class.isInstance(templateNameTmp)) {
					templateName = String.class.cast(templateNameTmp);
				}
			}
			if (node.jjtGetNumChildren() > 2) {
				simpleNode = (SimpleNode) node.jjtGetChild(2);
				Object templateNameTmp = simpleNode.value(ica);
				if (String.class.isInstance(templateNameTmp)) {
					varName = String.class.cast(templateNameTmp);
				}
			}
		} else if (String.class.isInstance(model)) {
			templateName = (String) model;
			model = ica.get("model");
			if (node.jjtGetNumChildren() > 1) {
				simpleNode = (SimpleNode) node.jjtGetChild(1);
				Object templateNameTmp = simpleNode.value(ica);
				if (String.class.isInstance(templateNameTmp)) {
					varName = String.class.cast(templateNameTmp);
				}
			}
		}
		if (templateName == null) {
			VelocityTemplate template = (VelocityTemplate) ica.get("template");
			if (template != null) {
				templateName = template.getTemplateName();
			}
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
