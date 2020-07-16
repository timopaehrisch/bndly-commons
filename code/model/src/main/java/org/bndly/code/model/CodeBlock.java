package org.bndly.code.model;

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

import java.util.ArrayList;
import java.util.List;

public class CodeBlock extends CodeElement {
	private List<CodeElement> elements;

	public List<CodeElement> getElements() {
		return elements;
	}

	public void setElements(List<CodeElement> elements) {
		this.elements = elements;
	}

	public CodeElement add(CodeElement element) {
		if (elements == null) {
			elements = new ArrayList<>();
		}
		elements.add(element);
		return element;
	}

	public <T extends CodeElement> T createContained(Class<T> type) {
		T instance = create(type);
		add(instance);
		return instance;
	}

	public <T extends CodeElement> T createContained(Class<T> type, Object... constructorParameters) {
		T instance = create(type, constructorParameters);
		add(instance);
		return instance;
	}

	public CodeLine line(String value) {
		return createContained(CodeLine.class, value);
	}

	public CodeBlock ifblock(String condition) {
		return createContained(CodeIfBlock.class, condition);
	}
}
