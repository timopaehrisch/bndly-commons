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

import java.util.List;

public class MailImpl implements Mail {

	private EmailAddress sender;
	private List<EmailAddress> to;
	private List<EmailAddress> bcc;
	private String subject;
	private List<MailContent> content;
	private List<Attachment> attachments;

	@Override
	public List<MailContent> getContent() {
		return content;
	}

	public void setContent(List<MailContent> content) {
		this.content = content;
	}

	@Override
	public EmailAddress getSender() {
		return sender;
	}

	public void setSender(EmailAddress sender) {
		this.sender = sender;
	}

	@Override
	public List<EmailAddress> getTo() {
		return to;
	}

	public void setTo(List<EmailAddress> to) {
		this.to = to;
	}

	@Override
	public List<EmailAddress> getBcc() {
		return bcc;
	}

	public void setBcc(List<EmailAddress> bcc) {
		this.bcc = bcc;
	}

	@Override
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	@Override
	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

}
