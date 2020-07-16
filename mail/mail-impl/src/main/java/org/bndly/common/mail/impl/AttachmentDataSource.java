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

import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.mail.api.Attachment;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataSource;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class AttachmentDataSource implements DataSource {
	private final Attachment attachment;
	private ReplayableInputStream ris;

	public AttachmentDataSource(Attachment attachment) {
		this.attachment = attachment;
	}
	
	@Override
	public synchronized InputStream getInputStream() throws IOException {
		if (ris == null) {
			ris = ReplayableInputStream.newInstance(attachment.getData());
			ris.doReplay();
		} else {
			ris.replay();
			ris.getLength();
		}
		return ris;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		throw new IOException("attachments do not have an output stream");
	}

	@Override
	public String getContentType() {
		return attachment.getContentType();
	}

	@Override
	public String getName() {
		return attachment.getFileName();
	}
	
}
