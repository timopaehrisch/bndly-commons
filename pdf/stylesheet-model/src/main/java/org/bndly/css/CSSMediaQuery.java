package org.bndly.css;

/*-
 * #%L
 * PDF CSS Model
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

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CSSMediaQuery {
	public static enum Type {
		ALL,
		AURAL,
		BRAILLE,
		HANDHELD,
		PRINT,
		PROJECTION,
		SCREEN,
		TTY,
		TV,
		EMBOSSED,
		SPEECH
	}
	
	public static enum Modifier {
		NOT,
		ONLY;
	}
	private Modifier modifier;
	private Type type;
	private List<CSSMediaFeature> features;

	public Modifier getModifier() {
		return modifier;
	}

	public void setModifier(Modifier modifier) {
		this.modifier = modifier;
	}

	public List<CSSMediaFeature> getFeatures() {
		return features;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public void addFeature(CSSMediaFeature feature) {
		if (feature != null) {
			if (features == null) {
				features = new ArrayList<>();
			}
			features.add(feature);
		}
	}

}
