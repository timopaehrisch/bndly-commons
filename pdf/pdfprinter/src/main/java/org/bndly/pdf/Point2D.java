package org.bndly.pdf;

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

public class Point2D {

	private final double x;
	private final double y;

	public static final Point2D ZERO = new Point2D(0,0);
	
	public Point2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public Point2D moveX(double delta) {
		return new Point2D(x + delta, y);
	}

	public Point2D moveY(double delta) {
		return new Point2D(x, y + delta);
	}

	@Override
	public String toString() {
		return "(" + x + " | " + y + ")";
	}

	public Point2D flipY(double height) {
		return new Point2D(x, height - y);
	}

	public Point2D flipX(double width) {
		return new Point2D(width - x, y);
	}
}
