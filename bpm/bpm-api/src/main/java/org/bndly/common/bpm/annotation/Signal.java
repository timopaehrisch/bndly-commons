package org.bndly.common.bpm.annotation;

/*-
 * #%L
 * BPM API
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation adds the information to a method, that an existing process instance should be informed about an incoming event.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Signal {
	/**
	 * The activity that shall receive the event.
	 * @return the event target activity
	 */
	String activity();
	
	/**
	 * The message that shall be carried in the event.
	 * @return the event message
	 */
	String message();
}
