package org.bndly.common.app.provisioning;

/*-
 * #%L
 * App Provisioning
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

import org.bndly.common.app.provisioning.model.ProvisioningModel;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ProvisioningModelParser {
	
	public ProvisioningModel parse(InputStream inputStream, StringSearchInterpolator stringSearchInterpolator) throws IOException, InterpolationException {
		return parse(new InputStreamReader(inputStream, "UTF-8"), stringSearchInterpolator);
	}
	
	public ProvisioningModel parse(Reader reader, StringSearchInterpolator stringSearchInterpolator) throws IOException, InterpolationException {
		JSValue ret = new JSONParser().parse(reader);
		if (stringSearchInterpolator != null) {
			replaceStrings(ret, stringSearchInterpolator);
		}
		return new ProvisioningModel((JSObject) ret);
	}

	private void replaceStrings(JSValue value, StringSearchInterpolator stringSearchInterpolator) throws InterpolationException {
		if (value instanceof JSObject) {
			Set<JSMember> members = ((JSObject) value).getMembers();
			if (members == null || members.isEmpty()) {
				return;
			}
			for (JSMember member : members) {
				replaceStrings(member.getValue(), stringSearchInterpolator);
			}
		} else if (value instanceof JSString) {
			String rawValue = ((JSString) value).getValue();
			String interpolatedValue = stringSearchInterpolator.interpolate(rawValue);
			((JSString) value).setValue(interpolatedValue);
		} else if (value instanceof JSArray) {
			for (JSValue jSValue : ((JSArray) value)) {
				replaceStrings(jSValue, stringSearchInterpolator);
			}
		}
	}
}
