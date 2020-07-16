package org.bndly.rest.client.api;

/*-
 * #%L
 * REST Client API
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


public class ConversationContextModel implements Cloneable {

	private String localRequestID;
	private String remoteRequestID;
	private String remoteAddress;
	private String serviceAddress;
	private String servicePort;
	private String serviceProtocol;
	private String serviceName;
	private String userName;
	private String sessionID;

	/**
	EventType

	Anmerkungen: Mögliche Freitext Inhalte wären z.B.:
		Request
		Response
		Exception
		AssertionViolation
		Monitoring
		Stats
		Custom (default)
	*/
	private String EventType;

	/*
	Anmerkungen: Mögliche Freitext Inhalte wären z.B.:
		CAE
		CAE - Workflow Wunschliste anlegen
		CAE - Workflow User Registrieren
		
		Studio
		Shop Core Solr
		Shop Core Rest
		Shop Core Service
		Shop Core Backend
		Shop Core Mailer
		Shop Core Search
	*/
	private String EventComponent;

	public String getRemoteRequestID() {
		return remoteRequestID;
	}

	public void setRemoteRequestID(String remoteRequestID) {
		this.remoteRequestID = remoteRequestID;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public String getServiceAddress() {
		return serviceAddress;
	}

	public void setServiceAddress(String serviceAddress) {
		this.serviceAddress = serviceAddress;
	}

	public String getServicePort() {
		return servicePort;
	}

	public void setServicePort(String servicePort) {
		this.servicePort = servicePort;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

	public String getEventType() {
		return EventType;
	}

	public void setEventType(String eventType) {
		this.EventType = eventType;
	}

	public String getEventComponent() {
		return EventComponent;
	}

	public void setEventComponent(String eventComponent) {
		this.EventComponent = eventComponent;
	}

	public String getServiceProtocol() {
		return serviceProtocol;
	}

	public void setServiceProtocol(String serviceProtocol) {
		this.serviceProtocol = serviceProtocol;
	}

	public String getLocalRequestID() {
		return localRequestID;
	}

	public void setLocalRequestID(String localRequestID) {
		this.localRequestID = localRequestID;
	}

	@Override
	public ConversationContextModel clone() throws CloneNotSupportedException {
		return (ConversationContextModel) super.clone();
	}

	public String toMultipleParameters() {
		String conversationContextModelAsMultipleParameters = "";

		conversationContextModelAsMultipleParameters += "localRequestID=" + localRequestID + ",";
		conversationContextModelAsMultipleParameters += "remoteRequestID=" + remoteRequestID + ",";
		conversationContextModelAsMultipleParameters += "remoteAddress=" + remoteAddress + ",";
		conversationContextModelAsMultipleParameters += "serviceAddress=" + serviceAddress + ",";
		conversationContextModelAsMultipleParameters += "servicePort=" + servicePort + ",";
		conversationContextModelAsMultipleParameters += "serviceName=" + serviceName + ",";
		conversationContextModelAsMultipleParameters += "userName=" + userName + ",";
		conversationContextModelAsMultipleParameters += "serviceProtocol=" + serviceProtocol + ",";
		conversationContextModelAsMultipleParameters += "sessionID=" + sessionID;

		return conversationContextModelAsMultipleParameters;
	}
}
