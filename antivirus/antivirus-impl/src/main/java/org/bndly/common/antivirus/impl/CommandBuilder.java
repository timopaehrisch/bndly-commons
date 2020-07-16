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

import org.bndly.common.antivirus.api.AVException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class CommandBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(CommandBuilder.class);

	public static final String CMD_VERSION = "VERSION";
	public static final String CMD_PING = "PING";
	public static final String CMD_STATS = "STATS";
	public static final String CMD_INSTREAM = "INSTREAM";
	public static final String CMD_IDSESSION = "IDSESSION";
	public static final String CMD_END = "END";
	
	public static final String RESP_CMD_READ_TIMEOUT = "COMMAND READ TIMED OUT";

	private String command;
	private Boolean terminateWithNull;
	private Boolean terminatingDataWithZeroLengthChunk;
	private InputStream dataToSend;
	private boolean commandContainsData;
	private boolean expectingResponse;
	private Integer chunkSize;
	private String charset;
	private int retries;

	public CommandBuilder charset(String charset) {
		this.charset = charset;
		return this;
	}
	
	public CommandBuilder terminateWithNullCharacter() {
		terminateWithNull = true;
		return this;
	}

	public CommandBuilder terminateWithNewLineCharacter() {
		terminateWithNull = false;
		return this;
	}
	
	public CommandBuilder terminatingDataWithZeroLengthChunk() {
		terminatingDataWithZeroLengthChunk = true;
		return this;
	}

	public CommandBuilder doNotTerminateDataWithZeroLengthChunk() {
		terminatingDataWithZeroLengthChunk = false;
		return this;
	}

	public CommandBuilder chunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
		return this;
	}
	
	public CommandBuilder retries(int retries) {
		if (retries < 0) {
			retries = 0;
		}
		this.retries = retries;
		return this;
	}
	
	////////////////////////////
	// HERE START THE COMMANDS
	////////////////////////////

	public CommandBuilder version() {
		command = CMD_VERSION;
		expectingResponse = true;
		commandContainsData = false;
		return this;
	}

	public CommandBuilder stats() {
		command = CMD_STATS;
		expectingResponse = true;
		commandContainsData = false;
		return this;
	}

	public CommandBuilder ping() {
		command = CMD_PING;
		expectingResponse = true;
		commandContainsData = false;
		return this;
	}
	
	public CommandBuilder idSession() {
		command = CMD_IDSESSION;
		expectingResponse = false;
		commandContainsData = false;
		return this;
	}
	public CommandBuilder end() {
		command = CMD_END;
		expectingResponse = false;
		commandContainsData = false;
		return this;
	}
	
	public CommandBuilder instream(InputStream is) {
		if (is == null) {
			throw new IllegalArgumentException("inputstream is not allowed to be null");
		}
		command = CMD_INSTREAM;
		expectingResponse = true;
		commandContainsData = true;
		terminatingDataWithZeroLengthChunk();
		dataToSend = is;
		return this;
	}
	
	////////////////////////////
	// HERE END THE COMMANDS
	////////////////////////////

	public Command build() throws AVException {
		if (terminateWithNull == null) {
			LOG.warn("termination character not specified while using command builder. falling back to null character");
			terminateWithNullCharacter();
		}
		if (command == null) {
			throw new AVException("no command specied while using the command builder.");
		}
		if (charset == null) {
			LOG.warn("no charset defined. falling back to utf-8.");
			charset = "UTF-8";
		}
		if (commandContainsData) {
			if (chunkSize == null) {
				chunkSize = 2048;
				LOG.warn("sending data but no chunk size defined. falling back to default of 2048 bytes");
			}
			if (terminatingDataWithZeroLengthChunk == null) {
				throw new AVException("when sending data 'terminatingDataWithZeroLengthChunk' should be defined per command.");
			}
			return new Command(dataToSend, chunkSize, terminatingDataWithZeroLengthChunk, command, terminateWithNull, expectingResponse, charset, retries + 1);
		} else {
			return new Command(command, terminateWithNull, expectingResponse, charset, retries + 1);
		}
	}

}
