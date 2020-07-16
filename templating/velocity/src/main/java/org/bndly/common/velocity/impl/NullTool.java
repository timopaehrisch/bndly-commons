package org.bndly.common.velocity.impl;

/*-
 * #%L
 * Velocity
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

import org.apache.velocity.context.Context;

/**
 * The code of this class is taken from http://wiki.apache.org/velocity/NullTool because it 
 * is not part of the velocity core release. It is a useful utility to work with 
 * <code>null</code> in templates.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class NullTool {

	public static final NullTool INSTANCE = new NullTool();

	private NullTool() {
	}

	/**
	 * Sets the given VTL reference back to <code>null</code>.
	 *
	 * @param context the current Context
	 * @param key the VTL reference to set back to <code>null</code>.
	 */
	public void setNull(Context context, String key) {
		if (this.isNull(context)) {
			return;
		}
		context.remove(key);
	}

	/**
	 * Sets the given VTL reference to the given value. If the value is <code>null</code>, the VTL reference is set to <code>null</code>.
	 *
	 * @param context the current Context
	 * @param key the VTL reference to set.
	 * @param value the value to set the VTL reference to.
	 */
	public void set(Context context, String key, Object value) {
		if (this.isNull(context)) {
			return;
		}
		if (this.isNull(value)) {
			this.setNull(context, key);
			return;
		}
		context.put(key, value);
	}

	/**
	 * Checks if a VTL reference is <code>null</code>.
	 *
	 * @param object the VTL reference to check.
	 * @return <code>true</code> if the VTL reference is <code>null</code>, <code>false</code> if otherwise.
	 */
	public boolean isNull(Object object) {
		return object == null;
	}

	/**
	 * Checks if a VTL reference is not <code>null</code>.
	 *
	 * @param object the VTL reference to check.
	 * @return <code>true</code> if the VTL reference is not <code>null</code>, <code>false</code> if otherwise.
	 */
	public boolean isNotNull(Object object) {
		return !this.isNull(object);
	}

	/**
	 * A convinient method which returns <code>null</code>. Actually, this tool will work the same without this method, because Velocity treats non-existing methods as null. :)
	 *
	 * @return <code>null</code>
	 */
	public Object getNull() {
		return null;
	}
}
