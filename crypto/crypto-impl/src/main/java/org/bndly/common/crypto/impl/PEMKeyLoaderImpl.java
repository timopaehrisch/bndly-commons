package org.bndly.common.crypto.impl;

/*-
 * #%L
 * Crypto Impl
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

import org.bndly.common.crypto.impl.shared.Base64Util;
import org.bndly.common.crypto.api.CryptoException;
import org.bndly.common.crypto.api.PEMKeyLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Stack;
import org.osgi.service.component.annotations.Component;

/**
 * The PEMKeyLoaderImpl is an implementation of {@link PEMKeyLoader}. It will load PEM key data from Java NIO Paths, input streams or byte arrays.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = PEMKeyLoader.class)
public class PEMKeyLoaderImpl implements PEMKeyLoader {
	
	private static final String BEGIN = "BEGIN PUBLIC KEY";
	private static final String END = "END PUBLIC KEY";
	
	private interface ParsingState {
		void handleChar(char character);
		void onEnd();
	}
	
	@Override
	public PublicKey loadRSAPublicKeyFromFile(Path filePath) throws CryptoException {
		if (Files.notExists(filePath)) {
			throw new CryptoException("could not find file at " + filePath);
		}
		byte[] keyBytes;
		try (final InputStream is = Files.newInputStream(filePath, StandardOpenOption.READ)) {
			StringBuffer data = readBase64StringFromPEMFileData(is);
			ByteArrayInputStream dataIS = new ByteArrayInputStream(data.toString().getBytes("ASCII"));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Base64Util.decode(dataIS, bos);
			bos.flush();
			keyBytes = bos.toByteArray();
		} catch (IOException e) {
			throw new CryptoException("could not read file data", e);
		}
		return loadRSAPublicKeyFromBytes(keyBytes);
	}

	@Override
	public PublicKey loadRSAPublicKeyFromStream(InputStream is) throws CryptoException {
		byte[] keyBytes;
		try {
			StringBuffer data = readBase64StringFromPEMFileData(is);
			ByteArrayInputStream dataIS = new ByteArrayInputStream(data.toString().getBytes("ASCII"));
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			Base64Util.decode(dataIS, bos);
			bos.flush();
			keyBytes = bos.toByteArray();
			return loadRSAPublicKeyFromBytes(keyBytes);
		} catch (IOException e) {
			throw new CryptoException("could not read stream data", e);
		}
	}
	
	@Override
	public PublicKey loadRSAPublicKeyFromBytes(byte[] keyBytes) throws CryptoException {
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicKey = keyFactory.generatePublic(spec);
			return publicKey;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
			throw new CryptoException("failed create public key from read bytes: " + ex.getMessage(), ex);
		}
	}
	
	/**
	 * Extracts the Base64 content from a PEM file data.
	 * @param is the stream to the PEM file data
	 * @return a Base64 encoded string
	 * @throws IOException if the stream could not be read
	 * @throws CryptoException if the PEM file data seems to be corrupted
	 */
	public StringBuffer readBase64StringFromPEMFileData(InputStream is) throws IOException, CryptoException {
		final StringBuffer sb = new StringBuffer();
		final Stack<ParsingState> stateStack = new Stack<>();
		stateStack.push(new AcceptOneSymbolOnlyState('-') {

			@Override
			protected void onOtherChar(char character, final int minusCharCount) {
				stateStack.pop();
				stateStack.push(new AcceptStaticStringState(BEGIN) {

					@Override
					protected void onStringAccepted() {
						stateStack.pop();
						stateStack.push(new AcceptOneSymbolOnlyState('-', minusCharCount) {

							@Override
							protected void onOtherChar(char character, int charCount) {
								if (character == '\n') {
									stateStack.pop();
									stateStack.push(new AcceptAnythingUntilStopCharState('-') {

										@Override
										protected void onChar(char character) {
											if (character != '\n') {
												sb.append(character);
											}
										}

										@Override
										protected void onStop(char character) {
											stateStack.pop();
											stateStack.push(new AcceptOneSymbolOnlyState('-', minusCharCount) {

												@Override
												protected void onOtherChar(char character, int charCount) {
													stateStack.pop();
													stateStack.push(new AcceptStaticStringState(END) {

														@Override
														protected void onStringAccepted() {
															stateStack.pop();
															stateStack.push(new AcceptOneSymbolOnlyState('-', minusCharCount) {

																@Override
																protected void onOtherChar(char character, int charCount) {
																	if (character == '\n') {
																		// we are fine
																	} else {
																		throw new CryptoException(
																			"found illegal character after end sequence"
																		);
																	}
																}
															});
														}

														@Override
														protected void onStringNotAccepted() {
															throw new CryptoException("failed to read end sequence");
														}

													});
													stateStack.peek().handleChar(character);
												}
											});
											stateStack.peek().handleChar(character);
										}

									});
									stateStack.peek().handleChar(character);
								} else {
									throw new CryptoException("exptected a new line after begin sequence");
								}
							}

							@Override
							protected void onTooManyChars() {
								throw new CryptoException("exptected a new line but there was another - symbol");
							}

						});
					}

					@Override
					protected void onStringNotAccepted() {
						throw new CryptoException("failed to read begin sequence");
					}

				});
				stateStack.peek().handleChar(character);
			}
		});
		int data;
		while ((data = is.read()) > -1) {
			stateStack.peek().handleChar((char) data);
		}
		stateStack.peek().onEnd();
		return sb;
	}
	
	private static class AcceptStaticStringState implements ParsingState {
		private final String toAccept;
		private int pos;

		public AcceptStaticStringState(String toAccept) {
			this.toAccept = toAccept;
			pos = 0;
		}
		
		@Override
		public void handleChar(char character) {
			if (pos == toAccept.length()) {
				onStringAccepted();
			} else {
				if (toAccept.charAt(pos) != character) {
					onStringNotAccepted();
				}
				pos++;
			}
		}

		@Override
		public void onEnd() {
		}

		protected void onStringAccepted() {
		}
		protected void onStringNotAccepted() {
		}
		
	}
	
	private abstract static class AcceptOneSymbolOnlyState implements ParsingState {

		private final char acceptedChar;
		private int charCount;
		private final Integer maxCharCount;

		public AcceptOneSymbolOnlyState(char acceptedChar) {
			this.acceptedChar = acceptedChar;
			charCount = 0;
			maxCharCount = null;
		}

		public AcceptOneSymbolOnlyState(char acceptedChar, int maxCharCount) {
			this.acceptedChar = acceptedChar;
			charCount = 0;
			this.maxCharCount = maxCharCount;
		}

		@Override
		public void handleChar(char character) {
			if (character == acceptedChar) {
				if (maxCharCount == null || charCount < maxCharCount) {
					// ok
					charCount++;
				} else {
					onTooManyChars();
				}
			} else {
				onOtherChar(character, charCount);
			}
		}

		@Override
		public void onEnd() {
		}

		protected abstract void onOtherChar(char character, int charCount);

		protected void onTooManyChars() {
		}

	}

	private static class AcceptAnythingUntilStopCharState implements ParsingState {

		private final char stopChar;

		public AcceptAnythingUntilStopCharState(char stopChar) {
			this.stopChar = stopChar;
		}

		@Override
		public void handleChar(char character) {
			if (character == stopChar) {
				onStop(character);
			} else {
				onChar(character);
			}
		}

		@Override
		public void onEnd() {
		}

		protected void onStop(char character) {
		}

		protected void onChar(char character) {
		}
	}
}
