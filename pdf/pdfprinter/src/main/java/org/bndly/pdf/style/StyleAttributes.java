package org.bndly.pdf.style;

/*-
 * #%L
 * PDF Document Printer
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

public interface StyleAttributes {
	public static final String FONT = "font-family";
	public static final String FONT_WEIGHT = "font-weight";
	public static final String FONT_STYLE = "font-style";
	public static final String FONT_SIZE = "font-size";
	public static final String FONT_BASELINE_SHIFT = "font-baseline-shift";
	public static final String LINE_HEIGHT = "line-height";
	public static final String MARGIN = "margin";
	public static final String PADDING = "padding";
	public static final String BORDER_WIDTH = "border";
	public static final String MARGIN_TOP = "margin-top";
	public static final String MARGIN_RIGHT = "margin-right";
	public static final String MARGIN_BOTTOM = "margin-bottom";
	public static final String MARGIN_LEFT = "margin-left";
	public static final String HORIZONTAL_ALIGN = "text-align";
	public static final String TOP = "top";
	public static final String LEFT = "left";
	public static final String OVERFLOW = "overflow";
	public static final String WIDTH = "width";
	public static final String HEIGHT = "height";
	/**
	 *  1 for 100%
	 */
	public static final String RELATIVE_WIDTH = "relative-width";
	/**
	 * controls a width of a columnLayout item in relation to the total width.
	 * all flex values will be added and the relative width will be: relativeWidth = flex/flexTotal
	 */
	public static final String FLEX = "flex";
}
