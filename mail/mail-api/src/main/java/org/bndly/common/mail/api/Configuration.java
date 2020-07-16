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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Configuration {

	EmailAddress getSenderAdress();
	Boolean getDebug();
	String getHost();
	Integer getPort();
	String getPassword();
	String getUser();
	Integer getConnectionTimeout();
	Integer getReadTimeout();
	Integer getWriteTimeout();
	EmailAddress getFrom();
	String getLocalhost();
	String getLocalAddress();
	Integer getLocalPort();
	Boolean getEhlo();
	Boolean getAuth();
	String getSubmitter();
	String getDSNNotify();
	String getDSNRet();
	Boolean getAllow8BitMime();
	Boolean getSendPartial();
	Boolean getSASLEnable();
	String getSASLMechanisms();
	String getSASLAuthorizationId();
	String getSASLRealm();
	Boolean getSASLUseCanonicalHostName();
	Boolean getQuitWait();
	Boolean getReportSuccess();
	String getSocketFactoryClass();
	Boolean getSocketFactoryFallback();
	Integer getSocketFactoryPort();
	Boolean getSSLEnable();
	Boolean getSSLCheckServerIdentity();
	String getSSLTrust();
	String getSSLSocketFactoryClass();
	Integer getSSLSocketFactoryPort();
	String getSSLProtocols();
	String getSSLCipherSuites();
	Boolean getStartTLSEnable();
	Boolean getStartTLSRequired();
	String getSocksHost();
	String getSocksPort();
	String getMailExtension();
	Boolean getUserSet();
	Boolean getNoOpStrict();
}
