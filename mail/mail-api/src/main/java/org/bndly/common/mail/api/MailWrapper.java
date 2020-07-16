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

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MailWrapper implements Mail {

	private final Mail wrapped;

	public MailWrapper(Mail wrapped) {
		if (wrapped == null) {
			throw new IllegalArgumentException("wrapped mail is not allowed to be null");
		}
		this.wrapped = wrapped;
	}

	@Override
	public List<MailContent> getContent() throws MailException {
		return wrapped.getContent();
	}

	@Override
	public List<EmailAddress> getTo() throws MailException {
		return wrapped.getTo();
	}

	@Override
	public List<EmailAddress> getBcc() throws MailException {
		return wrapped.getBcc();
	}

	@Override
	public EmailAddress getSender() throws MailException {
		return wrapped.getSender();
	}

	@Override
	public String getSubject() throws MailException {
		return wrapped.getSubject();
	}

	@Override
	public List<Attachment> getAttachments() throws MailException {
		return wrapped.getAttachments();
	}

}
