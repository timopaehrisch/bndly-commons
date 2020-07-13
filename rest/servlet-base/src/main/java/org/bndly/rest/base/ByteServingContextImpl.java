package org.bndly.rest.base;

/*-
 * #%L
 * REST Servlet Base
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

import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.data.io.WriteIOException;
import org.bndly.rest.api.ByteServingContext;
import org.bndly.rest.api.DataRange;
import org.bndly.rest.api.StatusWriter;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
class ByteServingContextImpl implements ByteServingContext {
	
	private final ContextImpl context;
	private DataRange requestedRange;
	private DataRange servedRange;
	private DataRange.Unit acceptRanges;

	public ByteServingContextImpl(ContextImpl context) {
		this.context = context;
	}

	@Override
	public DataRange getRequestedRange() {
		if (this.requestedRange == null) {
			if (context.isHttpRequest()) {
				String rangeHeader = context.getHeaderReader().read("Range");
				requestedRange = RangeHeadersUtil.parseRangeHeader(rangeHeader);
			}
		}
		return this.requestedRange;
	}

	@Override
	public void setServedRange(DataRange range) {
		servedRange = range;
		if (range != null) {
			Long length = RangeHeadersUtil.getServedContentLength(range);

			String val = RangeHeadersUtil.formatContentRangeHeader(range);
			if (context.isHttpResponse()) {
				if (val != null) {
					context.getHeaderWriter().write("Content-Range", val);
				}
				if (length != null) {
					context.getHeaderWriter().write("Content-Length", length.toString());
				}
			}
		}
	}

	@Override
	public void setAcceptRanges(DataRange.Unit unit) {
		acceptRanges = unit;
		if (unit != null) {
			if (context.isHttpResponse()) {
				context.getHeaderWriter().write("Accept-Ranges", unit.toHeaderString());
			}
		}
	}

	@Override
	public String getIfRange() {
		String ifRange = null;
		if (context.isHttpRequest()) {
			ifRange = context.getHeaderReader().read("If-Range");
		}
		return ifRange;
	}

	@Override
	public boolean isByteServingRequest() {
		return getRequestedRange() != null;
	}

	@Override
	public void serveDataFromStream(ReplayableInputStream rpis, Long rpisLength, String etag) {
		if (rpisLength == null || etag == null) {
			throw new IllegalArgumentException("stream length and etag have to be non-null values");
		}
		if (rpisLength < 0) {
			throw new IllegalArgumentException("stream length has to be 0 or positive");
		}
		String requestedEtag = getIfRange();

		// when there is an If-Range, we have to check that with the etag. those have to be in sync.
		// if they are not, the client is looking for bytes in a resource that has already changed.
		boolean isEtagPresent = requestedEtag != null;
		boolean isEtagInSync = requestedEtag != null && requestedEtag.equals(etag);
		DataRange rr = getRequestedRange();
		if (isEtagInSync || (!isEtagPresent && rr.getStart() != null && rr.getEnd() != null)) {
			// check the requested range with the available data
			Long end = rr.getEnd();
			if (end == null) {
				end = rpisLength - 1;
			} else if (end >= rpisLength) {
				// return a 416 status
				context.getStatusWriter().write(StatusWriter.Code.REQUESTED_RANGE_NOT_SATISFIABLE);
				return;
			}
			Long start = rr.getStart();
			if (start == null) {
				start = 0L;
			} else if (start >= rpisLength || start < 0) {
				// return a 416 status
				context.getStatusWriter().write(StatusWriter.Code.REQUESTED_RANGE_NOT_SATISFIABLE);
				return;
			}
			setServedRange(createDataRange(start, end, rpisLength));
			// define the actually served range
			// serve the range
			try {
				rpis.skipUnreplayable(start);
				// write the remaining data to the output stream
				context.getStatusWriter().write(StatusWriter.Code.PARTIAL_CONTENT);
				OutputStream os = context.getOutputStream();
				IOUtils.copy(rpis, os, RangeHeadersUtil.getServedContentLength(servedRange));
				os.flush();
			} catch (WriteIOException e) {
				// the other end might have closed the connection. hence we silently ignore this.
			} catch (IOException e) {
				throw new IllegalStateException("could not send byte range: " + e.getMessage(), e);
			}
		} else {
			// start new byte serving
			setAcceptRanges(DataRange.Unit.BYTES);
			context.getStatusWriter().write(StatusWriter.Code.OK);
		}
	}

	private DataRange createDataRange(final Long start, final Long end, final Long total) {
		return new DataRange() {

			@Override
			public Long getStart() {
				return start;
			}

			@Override
			public Long getEnd() {
				return end;
			}

			@Override
			public Long getTotal() {
				return total;
			}

			@Override
			public DataRange.Unit getUnit() {
				return Unit.BYTES;
			}
		};
	}
}
