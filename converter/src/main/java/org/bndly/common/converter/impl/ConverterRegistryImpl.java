package org.bndly.common.converter.impl;

/*-
 * #%L
 * Converter
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

import org.bndly.common.converter.api.ConversionException;
import org.bndly.common.converter.api.Converter;
import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.common.converter.api.TwoWayConverter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(service = org.bndly.common.converter.api.ConverterRegistry.class, immediate = true)
public class ConverterRegistryImpl implements ConverterRegistry {

	private final List<Converter> converters = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	@Reference(
			policy = ReferencePolicy.DYNAMIC,
			cardinality = ReferenceCardinality.MULTIPLE,
			bind = "bindConverter", 
			unbind = "unbindConverter", 
			service = Converter.class
	)
	protected void bindConverter(Converter converter) {
		addConverter(converter);
	}

	protected void unbindConverter(Converter converter) {
		removeConverter(converter);
	}

	private class NoOpConverter implements Converter {

		@Override
		public Converter canConvertFromTo(Class sourceType, Class targetType) {
			if (sourceType.equals(targetType)) {
				return this;
			}
			return null;
		}

		@Override
		public Object convert(Object source) throws ConversionException {
			return source;
		}

	}
	
	@Activate
	public void init() {
		clear();
		addConverter(new NoOpConverter());
		addConverter(new BigDecimalStringConverter());
		addConverter(new BooleanStringConverter());
		addConverter(new ByteStringConverter());
		addConverter(new DateStringConverter());
		addConverter(new DoubleStringConverter());
		addConverter(new FloatStringConverter());
		addConverter(new IntegerStringConverter());
		addConverter(new LongStringConverter());
		addConverter(new ShortStringConverter());
	}

	@Override
	public Converter getConverter(Class sourceType, Class targetType) {
		lock.readLock().lock();
		try {
			for (Converter converter : converters) {
				Converter c = converter.canConvertFromTo(sourceType, targetType);
				if (c == null) {
					if (TwoWayConverter.class.isInstance(converter)) {
						c = reverseConverter((TwoWayConverter) converter);
						c = c.canConvertFromTo(sourceType, targetType);
					}
				}
				if (c != null) {
					return c;
				}
			}
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}

	private Converter reverseConverter(final TwoWayConverter converter) {
		return new Converter() {

			@Override
			public Converter canConvertFromTo(Class sourceType, Class targetType) {
				if (converter.canConvertFromTo(targetType, sourceType) != null) {
					return this;
				}
				return null;
			}

			@Override
			public Object convert(Object source) throws ConversionException {
				return converter.reverseConvert(source);
			}
		};
	}

	@Override
	public void clear() {
		lock.writeLock().lock();
		try {
			converters.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	@Override
	public void addConverter(Converter converter) {
		if (converter != null) {
			lock.writeLock().lock();
			try {
				converters.add(0, converter);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	@Override
	public void removeConverter(Converter converter) {
		lock.writeLock().lock();
		try {
			Iterator<Converter> iterator = converters.iterator();
			while (iterator.hasNext()) {
				if (iterator.next() == converter) {
					iterator.remove();
				}
			}
		} finally {
			lock.writeLock().unlock();
		}
	}
}
