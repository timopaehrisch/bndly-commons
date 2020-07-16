package org.bndly.css.selector;

/*-
 * #%L
 * PDF CSS Model
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CSSSelectorParser {
	private static final Logger LOG = LoggerFactory.getLogger(CSSSelectorParser.class);
	
	private enum State {
		Start,
		ReadTypeName,
		CreateTypeSelector,
		SpaceAfterTypeName,
		CreatedChildSelector,
		CreatedDescendantSelector
	}
	
	private List<CSSSelector> selectors;
	private StringBuffer sb;
	private State state;
	
	public List<CSSSelector> parseSelectorString(String selectorString) {
		selectors = new ArrayList<>();
		sb = new StringBuffer();
		state = State.Start;

		for (int i = 0; i < selectorString.length(); i++) {
			char c = selectorString.charAt(i);
			boolean isLastChar = i == selectorString.length() - 1;
			handleChar(c, isLastChar);
		}

		return selectors;
	}

	private void handleChar(char c, boolean isLastChar) {
		if (state == State.Start || state == State.ReadTypeName) {
			if (c != ' ' && c != '>') {
				sb.append(c);
				state = State.ReadTypeName;
				if (isLastChar) {
					createTypeSelector();
				}
			} else if (c == ' ') {
				createTypeSelector();
				state = State.SpaceAfterTypeName;
			} else if (c == '>') {
				createTypeSelector();
				createChildSelector();
				state = State.SpaceAfterTypeName;
			}
		} else if (state == State.SpaceAfterTypeName) {
			if (c == '>') {
				createChildSelector();
			} else if (c != ' ') {
				createDescendantSelector();
				sb = new StringBuffer();
				sb.append(c);
				state = State.ReadTypeName;
			}
		} else if (state == State.CreatedChildSelector) {
			if (c != ' ') {
				sb = new StringBuffer();
				sb.append(c);
				state = State.ReadTypeName;
			}
		}

	}
	
	private <T extends CSSSelector> T createSelector(Class<T> type) {
		try {
			T instance = type.newInstance();
			selectors.add(instance);
			return instance;
		} catch (InstantiationException | IllegalAccessException e) {
			LOG.error("failed to create a selector instance from class " + type, e);
		}
		return null;
	}

	private void createDescendantSelector() {
		createSelector(CSSDescendantSelector.class);
		state = State.CreatedDescendantSelector;
	}

	private void createChildSelector() {
		createSelector(CSSChildSelector.class);
		state = State.CreatedChildSelector;
	}

	private void createTypeSelector() {
		CSSTypeSelector s = createSelector(CSSTypeSelector.class);
		s.setTypeName(sb.toString());
	}
	
}
