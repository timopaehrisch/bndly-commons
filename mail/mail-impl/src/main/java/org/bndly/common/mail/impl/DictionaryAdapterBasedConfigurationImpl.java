package org.bndly.common.mail.impl;

/*-
 * #%L
 * Mail Impl
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

import org.bndly.common.mail.api.Configuration;
import org.bndly.common.mail.api.EmailAddress;
import org.bndly.common.osgi.util.DictionaryAdapter;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DictionaryAdapterBasedConfigurationImpl implements Configuration {

	protected static final String SENDER_ADDRESS = "senderAddress";
	protected static final String DEBUG = "debug";
	protected static final String HOST = "host";
	protected static final String PASSWORD = "password";
	protected static final String USER = "user";
	protected static final String PORT = "port";
	protected static final String CONNECTION_TIMEOUT = "connectionTimeout";
	protected static final String READ_TIMEOUT = "readTimeout";
	protected static final String WRITE_TIMEOUT = "writeTimeout";
	protected static final String FROM = "from";
	protected static final String LOCALHOST = "localhost";
	protected static final String LOCAL_ADDRESS = "localAddress";
	protected static final String LOCAL_PORT = "localPort";
	protected static final String EHLO = "ehlo";
	protected static final String AUTH = "auth";
	protected static final String SUBMITTER = "submitter";
	protected static final String DSN_NOTIFY = "dsnNotify";
	protected static final String DSN_RET = "dsnRet";
	protected static final String ALLOW8_BIT_MIME = "allow8BitMime";
	protected static final String SEND_PARTIAL = "sendPartial";
	protected static final String SASL_ENABLE = "saslEnable";
	protected static final String SASL_MECHANISMS = "saslMechanisms";
	protected static final String SASL_AUTHORIZATION_ID = "saslAuthorizationId";
	protected static final String SASL_REALM = "saslRealm";
	protected static final String SASL_USE_CANONICAL_HOST_NAME = "saslUseCanonicalHostName";
	protected static final String QUIT_WAIT = "quitWait";
	protected static final String REPORT_SUCCESS = "reportSuccess";
	protected static final String SOCKET_FACTORY_CLASS = "socketFactoryClass";
	protected static final String SOCKET_FACTORY_FALLBACK = "socketFactoryFallback";
	protected static final String SOCKET_FACTORY_PORT = "socketFactoryPort";
	protected static final String SSL_ENABLE = "sslEnable";
	protected static final String SSL_CHECK_SERVER_IDENTITY = "sslCheckServerIdentity";
	protected static final String SSL_TRUST = "sslTrust";
	protected static final String SSL_SOCKET_FACTORY_CLASS = "sslSocketFactoryClass";
	protected static final String SSL_SOCKET_FACTORY_PORT = "sslSocketFactoryPort";
	protected static final String SSL_PROTOCOLS = "sslProtocols";
	protected static final String SSL_CIPHER_SUITES = "sslCipherSuites";
	protected static final String START_TLS_ENABLE = "startTlsEnable";
	protected static final String START_TLS_REQUIRED = "startTlsRequired";
	protected static final String SOCKS_HOST = "socksHost";
	protected static final String SOCKS_PORT = "socksPort";
	protected static final String MAIL_EXTENSION = "mailExtension";
	protected static final String USER_SET = "userSet";
	protected static final String NO_OP_STRICT = "noOpStrict";
	
	private final DictionaryAdapter dictionaryAdapter;

	public DictionaryAdapterBasedConfigurationImpl(DictionaryAdapter dictionaryAdapter) {
		this.dictionaryAdapter = dictionaryAdapter.emptyStringAsNull();
	}

	@Override
	public EmailAddress getSenderAdress() {
		String sa = dictionaryAdapter.getString(SENDER_ADDRESS);
		return sa != null ? new EmailAddress(sa) : null;
	}

	@Override
	public Boolean getDebug() {
		return dictionaryAdapter.getBoolean(DEBUG);
	}

	@Override
	public String getHost() {
		return dictionaryAdapter.getString(HOST);
	}

	@Override
	public String getPassword() {
		return dictionaryAdapter.getString(PASSWORD);
	}

	@Override
	public String getUser() {
		return dictionaryAdapter.getString(USER);
	}

	@Override
	public Integer getPort() {
		return dictionaryAdapter.getInteger(PORT, 25);
	}

	@Override
	public Integer getConnectionTimeout() {
		return dictionaryAdapter.getInteger(CONNECTION_TIMEOUT);
	}

	@Override
	public Integer getReadTimeout() {
		return dictionaryAdapter.getInteger(READ_TIMEOUT);
	}

	@Override
	public Integer getWriteTimeout() {
		return dictionaryAdapter.getInteger(WRITE_TIMEOUT);
	}

	@Override
	public EmailAddress getFrom() {
		String from = dictionaryAdapter.getString(FROM);
		return from != null ? new EmailAddress(from) : null;
	}

	@Override
	public String getLocalhost() {
		return dictionaryAdapter.getString(LOCALHOST);
	}

	@Override
	public String getLocalAddress() {
		return dictionaryAdapter.getString(LOCAL_ADDRESS);
	}

	@Override
	public Integer getLocalPort() {
		return dictionaryAdapter.getInteger(LOCAL_PORT);
	}

	@Override
	public Boolean getEhlo() {
		return dictionaryAdapter.getBoolean(EHLO);
	}

	@Override
	public Boolean getAuth() {
		return dictionaryAdapter.getBoolean(AUTH);
	}

	@Override
	public String getSubmitter() {
		return dictionaryAdapter.getString(SUBMITTER);
	}

	@Override
	public String getDSNNotify() {
		return dictionaryAdapter.getString(DSN_NOTIFY);
	}

	@Override
	public String getDSNRet() {
		return dictionaryAdapter.getString(DSN_RET);
	}

	@Override
	public Boolean getAllow8BitMime() {
		return dictionaryAdapter.getBoolean(ALLOW8_BIT_MIME);
	}

	@Override
	public Boolean getSendPartial() {
		return dictionaryAdapter.getBoolean(SEND_PARTIAL);
	}

	@Override
	public Boolean getSASLEnable() {
		return dictionaryAdapter.getBoolean(SASL_ENABLE);
	}

	@Override
	public String getSASLMechanisms() {
		return dictionaryAdapter.getString(SASL_MECHANISMS);
	}

	@Override
	public String getSASLAuthorizationId() {
		return dictionaryAdapter.getString(SASL_AUTHORIZATION_ID);
	}

	@Override
	public String getSASLRealm() {
		return dictionaryAdapter.getString(SASL_REALM);
	}

	@Override
	public Boolean getSASLUseCanonicalHostName() {
		return dictionaryAdapter.getBoolean(SASL_USE_CANONICAL_HOST_NAME);
	}

	@Override
	public Boolean getQuitWait() {
		return dictionaryAdapter.getBoolean(QUIT_WAIT);
	}

	@Override
	public Boolean getReportSuccess() {
		return dictionaryAdapter.getBoolean(REPORT_SUCCESS);
	}

	@Override
	public String getSocketFactoryClass() {
		return dictionaryAdapter.getString(SOCKET_FACTORY_CLASS);
	}

	@Override
	public Boolean getSocketFactoryFallback() {
		return dictionaryAdapter.getBoolean(SOCKET_FACTORY_FALLBACK);
	}

	@Override
	public Integer getSocketFactoryPort() {
		return dictionaryAdapter.getInteger(SOCKET_FACTORY_PORT);
	}

	@Override
	public Boolean getSSLEnable() {
		return dictionaryAdapter.getBoolean(SSL_ENABLE, false);
	}

	@Override
	public Boolean getSSLCheckServerIdentity() {
		return dictionaryAdapter.getBoolean(SSL_CHECK_SERVER_IDENTITY);
	}

	@Override
	public String getSSLTrust() {
		return dictionaryAdapter.getString(SSL_TRUST);
	}

	@Override
	public String getSSLSocketFactoryClass() {
		return dictionaryAdapter.getString(SSL_SOCKET_FACTORY_CLASS);
	}

	@Override
	public Integer getSSLSocketFactoryPort() {
		return dictionaryAdapter.getInteger(SSL_SOCKET_FACTORY_PORT);
	}

	@Override
	public String getSSLProtocols() {
		return dictionaryAdapter.getString(SSL_PROTOCOLS);
	}

	@Override
	public String getSSLCipherSuites() {
		return dictionaryAdapter.getString(SSL_CIPHER_SUITES);
	}

	@Override
	public Boolean getStartTLSEnable() {
		return dictionaryAdapter.getBoolean(START_TLS_ENABLE);
	}

	@Override
	public Boolean getStartTLSRequired() {
		return dictionaryAdapter.getBoolean(START_TLS_REQUIRED);
	}

	@Override
	public String getSocksHost() {
		return dictionaryAdapter.getString(SOCKS_HOST);
	}

	@Override
	public String getSocksPort() {
		return dictionaryAdapter.getString(SOCKS_PORT);
	}

	@Override
	public String getMailExtension() {
		return dictionaryAdapter.getString(MAIL_EXTENSION);
	}

	@Override
	public Boolean getUserSet() {
		return dictionaryAdapter.getBoolean(USER_SET);
	}

	@Override
	public Boolean getNoOpStrict() {
		return dictionaryAdapter.getBoolean(NO_OP_STRICT);
	}
}
