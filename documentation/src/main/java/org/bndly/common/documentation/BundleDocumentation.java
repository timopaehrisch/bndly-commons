package org.bndly.common.documentation;

/*-
 * #%L
 * Documentation
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

import java.io.IOException;
import java.io.InputStream;
import org.osgi.framework.Bundle;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface BundleDocumentation {

	public interface Entry {

		String getName();

		InputStream getContent() throws IOException;

	}

	public interface MarkdownEntry extends Entry {

		String getRenderedMarkdown();
	}

	Bundle getBundle();

	Iterable<MarkdownEntry> getMarkdownEntries();

	Entry getImageEntry(String path);
}
