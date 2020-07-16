package org.bndly.common.mail.api;

/*-
 * #%L
 * Mail API
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

public class ConfigurationImpl implements Configuration {

	private EmailAddress senderAdress;
	private Boolean debug;
	private String host;
	private String user;
	private String password;
	private Integer port;
	private Integer connectionTimeout;
	private Integer readTimeout;
	private Integer writeTimeout;
	private EmailAddress from;
	private String localhost;
	private String localAddress;
	private Integer localPort;
	private Boolean ehlo;
	private Boolean auth;
	private String dsnNotify;
	private String dsnRet;
	private Boolean allow8BitMime;
	private Boolean sendPartial;
	private Boolean saslEnable;
	private String saslMechanisms;
	private String saslAuthorizationId;
	private String saslRealm;
	private Boolean saslUseCanonicalHostName;
	private Boolean quitWait;
	private Boolean reportSuccess;
	private String socketFactoryClass;
	private Boolean socketFactoryFallback;
	private Integer socketFactoryPort;
	private Boolean sslCheckServerIdentity;
	private String sslTrust;
	private String sslSocketFactoryClass;
	private Boolean sslSocketFactoryFallback;
	private Integer sslSocketFactoryPort;
	private String sslProtocols;
	private String sslCipherSuites;
	private Boolean startTlsEnable;
	private Boolean startTlsRequired;
	private String socksHost;
	private String socksPort;
	private String mailExtension;
	private Boolean userSet;
	private Boolean noOpStrict;
	private Boolean sslEnable;
	private String submitter;

	@Override
	public EmailAddress getSenderAdress() {
		return senderAdress;
	}

	public void setSenderAdress(EmailAddress senderAdress) {
		this.senderAdress = senderAdress;
	}

	@Override
	public Boolean getDebug() {
		return debug;
	}

	public void setDebug(Boolean debug) {
		this.debug = debug;
	}

	@Override
	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}
	

	@Override
	public Integer getConnectionTimeout() {
		return connectionTimeout;
	}

	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	@Override
	public Integer getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
	}

	@Override
	public Integer getWriteTimeout() {
		return writeTimeout;
	}

	public void setWriteTimeout(Integer writeTimeout) {
		this.writeTimeout = writeTimeout;
	}

	@Override
	public EmailAddress getFrom() {
		return from;
	}

	public void setFrom(EmailAddress from) {
		this.from = from;
	}

	@Override
	public String getLocalhost() {
		return localhost;
	}

	public void setLocalhost(String localhost) {
		this.localhost = localhost;
	}

	@Override
	public String getLocalAddress() {
		return localAddress;
	}

	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

	@Override
	public Integer getLocalPort() {
		return localPort;
	}

	public void setLocalPort(Integer localPort) {
		this.localPort = localPort;
	}

	@Override
	public Boolean getEhlo() {
		return ehlo;
	}

	public void setEhlo(Boolean ehlo) {
		this.ehlo = ehlo;
	}

	@Override
	public Boolean getAuth() {
		return auth;
	}

	public void setAuth(Boolean auth) {
		this.auth = auth;
	}

	@Override
	public String getSubmitter() {
		return submitter;
	}

	public void setSubmitter(String submitter) {
		this.submitter = submitter;
	}
	
	@Override
	public String getDSNNotify() {
		return dsnNotify;
	}

	public void setDSNNotify(String dsnNotify) {
		this.dsnNotify = dsnNotify;
	}

	@Override
	public String getDSNRet() {
		return dsnRet;
	}

	public void setDSNRet(String dsnRet) {
		this.dsnRet = dsnRet;
	}

	@Override
	public Boolean getAllow8BitMime() {
		return allow8BitMime;
	}

	public void setAllow8BitMime(Boolean allow8BitMime) {
		this.allow8BitMime = allow8BitMime;
	}

	@Override
	public Boolean getSendPartial() {
		return sendPartial;
	}

	public void setSendPartial(Boolean sendPartial) {
		this.sendPartial = sendPartial;
	}

	@Override
	public Boolean getSASLEnable() {
		return saslEnable;
	}

	public void setSASLEnable(Boolean saslEnable) {
		this.saslEnable = saslEnable;
	}

	@Override
	public String getSASLMechanisms() {
		return saslMechanisms;
	}

	public void setSASLMechanisms(String saslMechanisms) {
		this.saslMechanisms = saslMechanisms;
	}

	@Override
	public String getSASLAuthorizationId() {
		return saslAuthorizationId;
	}

	public void setSASLAuthorizationId(String saslAuthorizationId) {
		this.saslAuthorizationId = saslAuthorizationId;
	}

	@Override
	public String getSASLRealm() {
		return saslRealm;
	}

	public void setSASLRealm(String saslRealm) {
		this.saslRealm = saslRealm;
	}

	@Override
	public Boolean getSASLUseCanonicalHostName() {
		return saslUseCanonicalHostName;
	}

	public void setSASLUseCanonicalHostName(Boolean saslUseCanonicalHostName) {
		this.saslUseCanonicalHostName = saslUseCanonicalHostName;
	}

	@Override
	public Boolean getQuitWait() {
		return quitWait;
	}

	public void setQuitWait(Boolean quitWait) {
		this.quitWait = quitWait;
	}

	@Override
	public Boolean getReportSuccess() {
		return reportSuccess;
	}

	public void setReportSuccess(Boolean reportSuccess) {
		this.reportSuccess = reportSuccess;
	}

	@Override
	public String getSocketFactoryClass() {
		return socketFactoryClass;
	}

	public void setSocketFactoryClass(String socketFactoryClass) {
		this.socketFactoryClass = socketFactoryClass;
	}

	@Override
	public Boolean getSocketFactoryFallback() {
		return socketFactoryFallback;
	}

	public void setSocketFactoryFallback(Boolean socketFactoryFallback) {
		this.socketFactoryFallback = socketFactoryFallback;
	}

	@Override
	public Integer getSocketFactoryPort() {
		return socketFactoryPort;
	}

	public void setSocketFactoryPort(Integer socketFactoryPort) {
		this.socketFactoryPort = socketFactoryPort;
	}

	@Override
	public Boolean getSSLEnable() {
		return sslEnable;
	}

	public void setSSLEnable(Boolean sslEnable) {
		this.sslEnable = sslEnable;
	}

	@Override
	public Boolean getSSLCheckServerIdentity() {
		return sslCheckServerIdentity;
	}

	public void setSSLCheckServerIdentity(Boolean sslCheckServerIdentity) {
		this.sslCheckServerIdentity = sslCheckServerIdentity;
	}

	@Override
	public String getSSLTrust() {
		return sslTrust;
	}

	public void setSSLTrust(String sslTrust) {
		this.sslTrust = sslTrust;
	}

	@Override
	public String getSSLSocketFactoryClass() {
		return sslSocketFactoryClass;
	}

	public void setSSLSocketFactoryClass(String sslSocketFactoryClass) {
		this.sslSocketFactoryClass = sslSocketFactoryClass;
	}

	@Override
	public Integer getSSLSocketFactoryPort() {
		return sslSocketFactoryPort;
	}

	public void setSSLSocketFactoryPort(Integer sslSocketFactoryPort) {
		this.sslSocketFactoryPort = sslSocketFactoryPort;
	}

	@Override
	public String getSSLProtocols() {
		return sslProtocols;
	}

	public void setSSLProtocols(String sslProtocols) {
		this.sslProtocols = sslProtocols;
	}

	@Override
	public String getSSLCipherSuites() {
		return sslCipherSuites;
	}

	public void setSSLCipherSuites(String sslCipherSuites) {
		this.sslCipherSuites = sslCipherSuites;
	}

	@Override
	public Boolean getStartTLSEnable() {
		return startTlsEnable;
	}

	public void setStartTLSEnable(Boolean startTlsEnable) {
		this.startTlsEnable = startTlsEnable;
	}

	@Override
	public Boolean getStartTLSRequired() {
		return startTlsRequired;
	}

	public void setStartTLSRequired(Boolean startTlsRequired) {
		this.startTlsRequired = startTlsRequired;
	}

	@Override
	public String getSocksHost() {
		return socksHost;
	}

	public void setSocksHost(String socksHost) {
		this.socksHost = socksHost;
	}

	@Override
	public String getSocksPort() {
		return socksPort;
	}

	public void setSocksPort(String socksPort) {
		this.socksPort = socksPort;
	}

	@Override
	public String getMailExtension() {
		return mailExtension;
	}

	public void setMailExtension(String mailExtension) {
		this.mailExtension = mailExtension;
	}

	@Override
	public Boolean getUserSet() {
		return userSet;
	}

	public void setUserSet(Boolean userSet) {
		this.userSet = userSet;
	}

	@Override
	public Boolean getNoOpStrict() {
		return noOpStrict;
	}

	public void setNoOpStrict(Boolean noOpStrict) {
		this.noOpStrict = noOpStrict;
	}

}
