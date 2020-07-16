package org.bndly.common.html;

/*-
 * #%L
 * HTML
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
 * The default handler is an empty no-op HTML parsing callback handler. It can
 * be used as a base when creating a custom handler.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultHandler implements Handler {

	public static final DefaultHandler NO_OP = new DefaultHandler();
	
	@Override
	public void onEntity(Entity entity) {
	}

	@Override
	public void onText(Text text) {
	}

	@Override
	public void onSelfClosingTag(SelfClosingTag tag) {
	}

	@Override
	public void openedTag(Tag tag) {
	}

	@Override
	public void closedTag(Tag tag) {
	}

}
