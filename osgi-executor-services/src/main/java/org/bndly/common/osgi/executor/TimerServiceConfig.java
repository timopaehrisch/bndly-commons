package org.bndly.common.osgi.executor;

/*-
 * #%L
 * OSGI Executor Services
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


import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bndly.common.osgi.util.DictionaryAdapter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = TimerServiceConfig.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(factory = true, ocd = TimerServiceConfig.Configuration.class)
public class TimerServiceConfig {

	private final Logger LOG = LoggerFactory.getLogger(TimerServiceConfig.class);

	@ObjectClassDefinition
	public @interface Configuration {

		@AttributeDefinition(
				name = "Name",
				description = "The name of this timer."
		)
		String timername();

		@AttributeDefinition(
				name = "Runnable",
				description = "An OSGI filter expression to access the runnable item, that shall be executed."
		)
		String runnable_target();

		@AttributeDefinition(
				name = "Initial delay",
				description = "The initial delay in milliseconds"
		)
		long delay() default 0;

		@AttributeDefinition(
				name = "Period",
				description = "The period in which the task is to be executed or 0 if the execution should be once."
		)
		long period() default 0;

		@AttributeDefinition(
				name = "Interrupt",
				description = "If this value is true, then a submitted scheduled future will be interrupted during the deactivation of this config component."
		)
		boolean interruptAtDeactivation() default false;
	}

	@Reference(name = "runnable")
	private Runnable timerTask;
	
	private String timerName;
	private Long delay;
	private Long period;
	private ScheduledExecutorService executor;
	private ScheduledFuture<?> scheduledFuture;
	private boolean interruptAtDeactivation;
	
	@Activate
	public void activate(ComponentContext componentContext) {
		DictionaryAdapter dictionaryAdapter = new DictionaryAdapter(componentContext.getProperties());

		interruptAtDeactivation = dictionaryAdapter.getBoolean("interruptAtDeactivation", Boolean.FALSE);
		timerName = dictionaryAdapter.getString("timername");
		if (timerName == null || timerName.isEmpty()) {
			timerName = "Timer-" + dictionaryAdapter.getString(Constants.SERVICE_PID);
		}
		delay = dictionaryAdapter.getLong("delay");
		period = dictionaryAdapter.getLong("period");
		executor = Executors.newSingleThreadScheduledExecutor(new BasicThreadFactory.Builder().namingPattern("ts-" + timerName + "-%d").build());
		scheduledFuture = executor.scheduleAtFixedRate(timerTask, delay, period, TimeUnit.MILLISECONDS);
	}
	
	@Deactivate
	public void deactivate() {
		if (scheduledFuture != null) {
			scheduledFuture.cancel(interruptAtDeactivation);
			scheduledFuture = null;
		}
		if (executor != null) {
			executor.shutdown();
			executor = null;
		}
	}
}
