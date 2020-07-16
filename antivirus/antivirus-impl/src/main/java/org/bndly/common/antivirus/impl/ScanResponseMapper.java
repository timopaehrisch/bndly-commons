package org.bndly.common.antivirus.impl;

/*-
 * #%L
 * Antivirus Impl
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

import org.bndly.common.antivirus.api.AVResponseParsingException;
import org.bndly.common.antivirus.api.AVScanException;
import org.bndly.common.antivirus.api.AVSizeLimitExceededException;
import org.bndly.common.antivirus.api.ScanResult;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class ScanResponseMapper implements ScanResult {

	private static final Logger LOG = LoggerFactory.getLogger(ScanResponseMapper.class);

	private final Command command;
	private final String response;
	private final Stack<ParsingState> state;
	private final StringBuffer whatHasBeenRead = new StringBuffer();
	private Long requestNumber;
	private Boolean ok;
	private String foundSignature;

	private static final String TOKEN_OK = "OK";
	private static final String TOKEN_FOUND = "FOUND";
	private static final String TOKEN_ERROR = "ERROR";

	public ScanResponseMapper(String response, Command command) {
		LOG.debug("parsing scan response string: {}", response);
		this.response = response;
		this.command = command;
		state = new Stack<>();
		parse();
	}

	private static interface ParsingState {

		void handleChar(char character) throws AVResponseParsingException;

		void eof() throws AVResponseParsingException;
	}

	private abstract class BufferUntilStopCharacter implements ParsingState {

		private final StringBuffer sb = new StringBuffer();
		private final char stopCharacter;

		public BufferUntilStopCharacter(char stopCharacter) {
			this.stopCharacter = stopCharacter;
		}

		@Override
		public void handleChar(char character) throws AVResponseParsingException {
			if (character == stopCharacter) {
				onComplete(sb.toString());
			} else {
				sb.append(character);
			}
		}

		@Override
		public void eof() throws AVResponseParsingException {
			onComplete(sb.toString());
		}

		protected abstract void onComplete(String bufferContent);

	}

	private abstract class AcceptWhiteSpaces implements ParsingState {

		@Override
		public void handleChar(char character) throws AVResponseParsingException {
			if (Character.isWhitespace(character)) {
				// no-op
			} else {
				pop();
				ParsingState newState = createNewStateForCharacter(character);
				if (newState != null) {
					newState.handleChar(character);
					push(newState);
				}
			}
		}

		@Override
		public void eof() throws AVResponseParsingException {
			// no-op
		}

		protected abstract ParsingState createNewStateForCharacter(char character);

	}

	private class RequestNumberParsingState implements ParsingState {

		private final StringBuffer sb = new StringBuffer();

		@Override
		public void handleChar(char character) throws AVResponseParsingException {
			if (':' == character) {
				getNumberFromBuffer();
				pop();
				push(new AcceptWhiteSpaces() {

					@Override
					protected ParsingState createNewStateForCharacter(char character) {
						return new BufferUntilStopCharacter(':') {

							@Override
							protected void onComplete(String bufferContent) {
								if ("stream".equals(bufferContent)) {
									push(new AcceptWhiteSpaces() {

										@Override
										protected ParsingState createNewStateForCharacter(char character) {
											return new BufferUntilStopCharacter('\0') {

												@Override
												protected void onComplete(String bufferContent) {
													if (TOKEN_OK.equals(bufferContent)) {
														ok = Boolean.TRUE;
													} else if (bufferContent.endsWith(TOKEN_FOUND)) {
														ok = Boolean.FALSE;
														String sig = bufferContent.substring(0, bufferContent.length() - TOKEN_FOUND.length());
														foundSignature = sig.trim();
													} else {
														throw new AVResponseParsingException(
																"unsupported content in scan response (read data: " 
																+ whatHasBeenRead.toString() + "): " + bufferContent
														);
													}
												}
											};
										}
									});
								} else if (bufferContent.endsWith(TOKEN_ERROR)) {
									String errorMsg = bufferContent.substring(0, bufferContent.length() - TOKEN_ERROR.length());
									if (errorMsg.startsWith(command.getName())) {
										errorMsg = errorMsg.substring(command.getName().length());
									}
									errorMsg = errorMsg.trim();
									throw createAVExceptionFromErrorMsg(errorMsg);
								} else {
									throw new AVResponseParsingException(
											"unsupported content in scan response (read data: " + whatHasBeenRead.toString() + "): " + bufferContent
									);
								}
							}
						};
					}
				});
			} else if ("0123456789".indexOf(character) > -1) {
				sb.append(character);
			} else {
				throw new AVResponseParsingException("illegal character '" + character + "' in response (read data: " + whatHasBeenRead.toString() + ")");

			}
		}

		@Override
		public void eof() throws AVResponseParsingException {
			getNumberFromBuffer();
		}

		private void getNumberFromBuffer() {
			String numberString = sb.toString();
			if (!"".equals(numberString)) {
				try {
					requestNumber = Long.valueOf(numberString);
				} catch (NumberFormatException e) {
					// no request number
				}
			}
		}

	}

	private AVScanException createAVExceptionFromErrorMsg(String errorMsg) {
		if (errorMsg.contains("size limit exceeded")) {
			// when this happens, clamav will close the socket. the java socket 
			// may still not respond with false when calling isClosed()
			return new AVSizeLimitExceededException("size limit was exceeded on remote");
		}
		return new AVScanException(response);
	}

	private ParsingState peek() {
		if (state.isEmpty()) {
			return null;
		}
		return state.peek();
	}

	private ParsingState pop() {
		if (state.isEmpty()) {
			return null;
		}
		return state.pop();
	}

	private ParsingState push(ParsingState parsingState) {
		return state.push(parsingState);
	}

	private void parse() {
			push(new RequestNumberParsingState());
			for (int i = 0; i < response.length(); i++) {
				char character = response.charAt(i);
				whatHasBeenRead.append(character);
				ParsingState parsingState = peek();
				if (parsingState != null) {
					parsingState.handleChar(character);
				}
			}
			ParsingState s = peek();
			if (s != null) {
				s.eof();
			}
	}

	@Override
	public Long getRequestNumber() {
		return requestNumber;
	}

	@Override
	public boolean isOK() {
		if (ok == null) {
			throw new IllegalStateException("response parsing did not set the 'ok' flag.");
		}
		return ok;
	}

	@Override
	public String getFoundSignature() {
		return foundSignature;
	}

}
