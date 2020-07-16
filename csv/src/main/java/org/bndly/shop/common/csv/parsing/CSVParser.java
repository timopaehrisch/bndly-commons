package org.bndly.shop.common.csv.parsing;

/*-
 * #%L
 * CSV
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

import org.bndly.shop.common.csv.CSVConfig;
import org.bndly.shop.common.csv.model.DocumentImpl;
import org.bndly.shop.common.csv.model.RowImpl;
import org.bndly.shop.common.csv.model.ValueImpl;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Stack;

public class CSVParser {

	private final CSVConfig config;
	private Stack<CSVParsingState> states;

	public CSVParser() {
		this(CSVConfig.DEFAULT);
	}

	public CSVParser(CSVConfig config) {
		this.config = config;
	}

	public DocumentImpl parse(InputStream in) {
		return parse(new InputStreamReader(in));
	}
	
	private static class InternalDataHandler {
		private final CSVDataHandler dataHandler;
		private long currentRowIndex;
		private long columnIndex;
		private boolean inRow;

		public InternalDataHandler(CSVDataHandler dataHandler) {
			this.dataHandler = dataHandler;
		}

		private void documentOpened() {
			dataHandler.documentOpened();
		}

		private void documentClosed() {
			dataHandler.documentClosed();
		}

		private void value(String value, boolean quoted) {
			if (!inRow) {
				inRow = true;
				dataHandler.rowOpened(currentRowIndex);
			}
			dataHandler.value(columnIndex, value, quoted);
			columnIndex++;
		}

		private void rowClosed() {
			dataHandler.rowClosed(currentRowIndex);
			columnIndex = 0;
			inRow = false;
			currentRowIndex++;
		}
		
	}

	public DocumentImpl parse(Reader in) {
		final DocumentImpl documentImpl = new DocumentImpl();
		parse(in, new CSVDataHandler() {
			
			private RowImpl currentRow;
			
			@Override
			public void documentOpened() {
			}

			@Override
			public void rowOpened(long currentRowIndex) {
				currentRow = new RowImpl(documentImpl, currentRowIndex);
				documentImpl.addRow(currentRow);
			}

			@Override
			public void value(long columnIndex, String value, boolean quoted) {
				ValueImpl val = new ValueImpl(value, currentRow, currentRow.getValues().size(), quoted);
				currentRow.addValue(val);
			}

			@Override
			public void rowClosed(long currentRowIndex) {
				currentRow = null;
				currentRowIndex++;
			}

			@Override
			public void documentClosed() {
			}
		});
		return documentImpl;
	}
	
	public void parse(InputStream in, CSVDataHandler dataHandler) {
		parse(new InputStreamReader(in), dataHandler);
	}
	
	public void parse(Reader in, CSVDataHandler dataHandler) {
		parse(in, new InternalDataHandler(dataHandler));
	}
	
	private void parse(Reader in, InternalDataHandler dataHandler) {
		states = new Stack<>();
		states.push(new QuotedValueDetectionParsingState());
		//StringBuffer whatHasBeenRead = new StringBuffer();
		try {
			int i;
			dataHandler.documentOpened();
			while (((i = in.read()) > -1)) {
				char c = (char) i;
				//whatHasBeenRead.append(c);
				handleChar(c, dataHandler);
			}
			dataHandler.documentClosed();
		} catch (IOException ex) {
			throw new ParsingException("could not parse CSV input stream.", ex);
		}
		while (!states.isEmpty()) {
			states.pop().onEnd(dataHandler);
		}
	}

	private void replay(CharSequence charSequence, InternalDataHandler dataHandler) {
		for (int i = 0; i < charSequence.length(); i++) {
			char c = charSequence.charAt(i);
			handleChar(c, dataHandler);
		}
	}
	
	private void handleChar(char c, InternalDataHandler dataHandler) {
		states.peek().handleChar(c, dataHandler);
	}
	
	/*
	private void onValueComplete(String value, boolean quoted) {
		if (currentRow == null) {
			currentRow = new RowImpl(documentImpl, currentRowIndex);
			documentImpl.addRow(currentRow);
		}
		ValueImpl val = new ValueImpl(value, currentRow, currentRow.getValues().size(), quoted);
		currentRow.addValue(val);
	}
	
	private void onRowComplete() {
		currentRow = null;
		currentRowIndex++;
	}
	*/
	
	////////////////////////////////
	// inner parsing state classes
	////////////////////////////////
	private abstract class CSVParsingState {

		protected abstract void handleChar(char c, InternalDataHandler dataHandler);
		protected abstract void onEnd(InternalDataHandler dataHandler);
	}
	
	private class BufferStringParsingState extends CSVParsingState {
		protected final String[] endTokens;
		protected final StringBuffer buffer = new StringBuffer();

		public BufferStringParsingState(String... endTokens) {
			this.endTokens = endTokens;
			if (endTokens.length == 0) {
				throw new IllegalArgumentException("at least one end token is required");
			}
		}

		@Override
		protected final void handleChar(char c, InternalDataHandler dataHandler) {
			buffer.append(c);
			for (String endToken : endTokens) {
				if (buffer.length() >= endToken.length()) {
					int offsetInBuffer = buffer.length() - endToken.length();
					for (int i = 0; i < endToken.length(); i++) {
						if (buffer.charAt(offsetInBuffer + i) != endToken.charAt(i)) {
							break;
						} else {
							if (i == endToken.length() - 1) {
								states.pop();
								onEndTokenFound(buffer.subSequence(0, offsetInBuffer), endToken);
								return;
							}
						}
					}
				}
			}
		}

		@Override
		protected final void onEnd(InternalDataHandler dataHandler) {
			onEndTokenNotFound();
		}
		
		protected void onEndTokenFound(CharSequence bufferWithoutEndToken, String endToken) {
		}
		protected void onEndTokenNotFound() {
		}
		
	}
	
	private class AcceptStringParsingState extends CSVParsingState {
		protected final String[] toAccept;
		protected final boolean[] stillCandidate;
		protected final StringBuffer acceptBuffer = new StringBuffer();
		private int pos;

		public AcceptStringParsingState(String... toAccept) {
			this.toAccept = toAccept;
			if (toAccept.length == 0) {
				throw new IllegalArgumentException("at least one string to accept is required.");
			}
			stillCandidate = new boolean[toAccept.length];
			for (int i = 0; i < stillCandidate.length; i++) {
				stillCandidate[i] = true;
			}
		}

		@Override
		protected final void handleChar(char c, InternalDataHandler dataHandler) {
			acceptBuffer.append(c);
			boolean increment = false;
			boolean hasCandidates = false;
			for (int i = 0; i < stillCandidate.length; i++) {
				boolean isStillCandidate = stillCandidate[i];
				if (isStillCandidate) {
					String candid = toAccept[i];
					if (candid.charAt(pos) == c) {
						increment = true;
					} else {
						isStillCandidate = false;
						stillCandidate[i] = isStillCandidate;
					}
					if (isStillCandidate && pos == (candid.length() - 1)) {
						states.pop();
						accepted(candid, dataHandler);
						return;
					}
				}
				hasCandidates = hasCandidates || isStillCandidate;
			}
			if (increment) {
				pos++;
			}

			if (!hasCandidates) {
				states.pop();
				notAccepted(dataHandler);
			}
		}

		@Override
		protected final void onEnd(InternalDataHandler dataHandler) {
			notAccepted(dataHandler);
		}
		
		protected void notAccepted(InternalDataHandler dataHandler) {
		}
		protected void accepted(String acceptedString, InternalDataHandler dataHandler) {
		}
	}

	private abstract class QuotedValueParsingState extends BufferStringParsingState {
		protected final StringBuffer quotedValue;
		
		public QuotedValueParsingState() {
			this(new StringBuffer());
		}
		
		private QuotedValueParsingState(StringBuffer existingBuffer) {
			super(config.getQuote());
			quotedValue = existingBuffer;
		}

		@Override
		protected final void onEndTokenFound(CharSequence bufferWithoutEndToken, String endToken) {
			quotedValue.append(bufferWithoutEndToken);
			final QuotedValueParsingState prev = this;
			states.push(new AcceptStringParsingState(config.getQuote()) {

				@Override
				protected void accepted(String acceptedString, InternalDataHandler dataHandler) {
					// the quote was escaped.
					quotedValue.append(acceptedString);
					states.push(new QuotedValueParsingState(quotedValue) {

						@Override
						protected void quotedValueClosed() {
							prev.quotedValueClosed();
						}

						@Override
						protected void quotedValueLeftUnclosed() {
							prev.quotedValueLeftUnclosed();
						}
						
					});
				}

				@Override
				protected void notAccepted(InternalDataHandler dataHandler) {
					// the quote actually closed the value
					// we have to replay the handled chars, so that other states might apply
					quotedValueClosed();
					replay(acceptBuffer, dataHandler);
				}
			
			});
		}

		@Override
		protected final void onEndTokenNotFound() {
			quotedValueLeftUnclosed();
		}
		
		protected abstract void quotedValueClosed();
		
		protected abstract void quotedValueLeftUnclosed();
		
	}
	
	private class QuotedValueDetectionParsingState extends AcceptStringParsingState {

		public QuotedValueDetectionParsingState() {
			super(config.getQuote());
		}

		@Override
		protected void accepted(String accepted, final InternalDataHandler dataHandler) {
			// now a quoted value follows. the end of the value will be detected by the quote sequence
			states.push(new QuotedValueParsingState() {

				@Override
				protected void quotedValueClosed() {
					dataHandler.value(quotedValue.toString(), true);

					// now a new quoted value or a new row might be parsed
					// this means we need some kind of switch state
					states.push(new AcceptStringParsingState(config.getSeparator(), config.getNewLine()) {

						@Override
						protected void accepted(String acceptedString, InternalDataHandler dataHandler) {
							if (acceptedString.equals(config.getNewLine())) {
								dataHandler.rowClosed();
							}
							states.push(new QuotedValueDetectionParsingState());
						}

					});
				}

				@Override
				protected void quotedValueLeftUnclosed() {
					throw new ParsingException("quoted value was left unclosed");
				}
				
			});
		}

		@Override
		protected void notAccepted(final InternalDataHandler dataHandler) {
			// the sequence that has been read so far will be part of the value. the value will be ended with the separator sequence
			states.push(new BufferStringParsingState(config.getSeparator(), config.getNewLine()) {

				@Override
				protected void onEndTokenFound(CharSequence bufferWithoutEndToken, String endToken) {
					dataHandler.value(acceptBuffer.append(bufferWithoutEndToken).toString(), false);
					if (config.getNewLine().equals(endToken)) {
						dataHandler.rowClosed();
						states.push(new QuotedValueDetectionParsingState());
					} else if (config.getSeparator().equals(endToken)) {
						states.push(new QuotedValueDetectionParsingState());
					} else {
						throw new ParsingException("unsupported end token: " + endToken);
					}
				}

				@Override
				protected void onEndTokenNotFound() {
					dataHandler.value(acceptBuffer.append(buffer).toString(), false);
				}
				
			});
		}
	}

}
