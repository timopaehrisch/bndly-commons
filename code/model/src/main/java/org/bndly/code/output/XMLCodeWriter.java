package org.bndly.code.output;

/*-
 * #%L
 * Code Model
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

import org.bndly.code.common.CodeGenerationObject;
import org.bndly.code.model.CodeBlock;
import org.bndly.code.model.CodeBlockIndented;
import org.bndly.code.model.CodeLine;
import org.bndly.code.model.XMLAttribute;
import org.bndly.code.model.XMLElement;
import java.util.List;

public class XMLCodeWriter extends CodeGenerationObject {

    public CodeBlock toCode(XMLElement el) {
        CodeBlock block = create(CodeBlock.class);
        block.createContained(CodeLine.class, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        handleElementInBlock(el, block);
        return block;
    }

    private void handleElementInBlock(XMLElement el, CodeBlock block) {
        List<XMLElement> subElements = el.getElements();
        StringBuffer sb = new StringBuffer();
        sb.append("<");
        String elementName = el.getName();

        if (el.getNamespacePrefix() != null) {
            elementName = el.getNamespacePrefix() + ":" + elementName;
        }

        sb.append(elementName);

        List<XMLAttribute> attributes = el.getAttributes();
        if (attributes != null) {
            for (XMLAttribute xmlAttribute : attributes) {
                sb.append(" ");
                if (xmlAttribute.getNamespacePrefix() != null) {
                    sb.append(xmlAttribute.getNamespacePrefix());
                    sb.append(":");
                }
                sb.append(xmlAttribute.getName());
                sb.append("=\"");
                if (xmlAttribute.getValue() != null) {
                    sb.append(xmlAttribute.getValue());
                }
                sb.append("\"");
            }
        }

        if (subElements != null) {
            sb.append(">");
        } else {
            sb.append("/>");
        }

        block.createContained(CodeLine.class, sb.toString());

        if (subElements != null) {
            CodeBlockIndented subElementBlock = block.createContained(CodeBlockIndented.class);
            for (XMLElement subEl : subElements) {
                handleElementInBlock(subEl, subElementBlock);
            }
            block.createContained(CodeLine.class, "</" + elementName + ">");
        }

    }
}
