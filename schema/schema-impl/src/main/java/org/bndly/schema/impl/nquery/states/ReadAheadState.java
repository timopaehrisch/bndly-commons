package org.bndly.schema.impl.nquery.states;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.QueryParsingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class ReadAheadState extends AbstractParsingState {
	private boolean didFire;

	private class Candidate {
		private final String stopWord;
		private final StringBuffer bufferBeforeStopWord;
		private final StringBuffer bufferAfterStopWord;
		private boolean matched;
		private int index;

		public Candidate(String stopWord) {
			this.stopWord = stopWord;
			bufferAfterStopWord = new StringBuffer();
			bufferBeforeStopWord = new StringBuffer();
			index = 0;
		}

		public void match(char c) {
			if (matched) {
				bufferAfterStopWord.append(c);
				return;
			} else {
				bufferBeforeStopWord.append(c);
			}
			if (stopWord.charAt(index) == c) {
				index++;
				if (index == stopWord.length()) {
					matched = true;
				}
			} else {
				index = 0;
			}
		}
		
		public boolean didMatch() {
			return matched;
		}
		
		public boolean isPartialMatch() {
			return index > 0 && !matched;
		}
		
		public String getStopWord() {
			return stopWord;
		}

		public StringBuffer getBufferBeforeStopWord() {
			return bufferBeforeStopWord;
		}

		public StringBuffer getBufferAfterStopWord() {
			return bufferAfterStopWord;
		}
		
	}
	
	private final StringBuffer buffer = new StringBuffer();
	protected final String[] stopWords;
	private final List<Candidate> remainingStopWords;

	public ReadAheadState(String... stopWords) {
		this.stopWords = stopWords;
		remainingStopWords = new ArrayList<>(stopWords.length);
		for (String stopWord : stopWords) {
			remainingStopWords.add(new Candidate(stopWord));
		}
	}
	
	protected boolean isAppendableCharacter(char character) throws QueryParsingException {
		return true;
	}
	
	@Override
	public void handleChar(char character, Parser parser) throws QueryParsingException {
		if (!isAppendableCharacter(character)) {
			return;
		} else {
			buffer.append(character);
		}
		Iterator<Candidate> iterator = remainingStopWords.iterator();
		while (iterator.hasNext()) {
			Candidate candidate = iterator.next();
			if (candidate.didMatch()) {
				candidate.getBufferAfterStopWord().append(character);
			} else {
				candidate.match(character);
			}
		}
		fireOnStopWord(character, parser);
	}

	@Override
	public void onEnd(Parser parser) throws QueryParsingException {
		fireOnStopWord(null, parser);
	}
	
	private void fireOnStopWord(Character character, Parser parser) throws QueryParsingException {
		if (didFire) {
			return;
		}
		boolean containsPartialMatch = false;
		Candidate bestMatched = null;
		for (Candidate remainingStopWord : remainingStopWords) {
			if (remainingStopWord.isPartialMatch()) {
				containsPartialMatch = true;
			}
			if (remainingStopWord.didMatch()) {
				if (bestMatched == null) {
					bestMatched = remainingStopWord;
				} else if (bestMatched.getBufferAfterStopWord().length() > remainingStopWord.getBufferAfterStopWord().length()) {
					bestMatched = remainingStopWord;
				}
			}
		}
		if (!containsPartialMatch && bestMatched != null) {
			int offsetInStringBuffer = bestMatched.getBufferBeforeStopWord().length() - bestMatched.getStopWord().length();
			String buffered = bestMatched.getBufferBeforeStopWord().substring(0, offsetInStringBuffer);
			onStopWord(buffered, bestMatched.getStopWord(), character, parser);
			didFire = true;
			parser.reparse(bestMatched.getBufferAfterStopWord().toString());
		}
	}

	protected final String getBuffered() {
		return buffer.toString();
	}
	
	protected abstract void onStopWord(String buffered, String stopWord, char character, Parser parser) throws QueryParsingException;
	
}
