/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.util;

/**
 * class Color. Color is presented as the triple of short's (Red, Green, Blue components)
 * Each component should be in the range 0..255
 */
public final class ZLColor {
	public final short Red;
	public final short Green;
	public final short Blue;
	public short alpha = 255;
	
	public ZLColor(int r, int g, int b) {
		Red = (short)(r & 0xFF);
		Green = (short)(g & 0xFF);
		Blue = (short)(b & 0xFF);
	}
	
	public ZLColor(String text)
	{
		String[] infos = text.split(",");

		Red = getColorValue(infos[0]);
		Green = getColorValue(infos[1]);
		Blue = getColorValue(infos[2]);
		
		if (infos.length == 4) {
			alpha = getColorValue(infos[3]);
		}	
	}
	
	private short getColorValue(String value)
	{
		if (value.compareTo("0") == 0) {
			return 0;
		}
		
		if (value.compareTo("1") == 0) {
			return 255;
		}

		double temp = Double.parseDouble(value);
		if (temp > 0) {
			return (short)temp;
		} else {
			return (short)(temp * 255);
		}
	}
	
	public ZLColor(int intValue) {
		Red = (short)((intValue >> 16) & 0xFF);
		Green = (short)((intValue >> 8) & 0xFF);
		Blue = (short)(intValue & 0xFF);
	}
	
	public int getIntValue() {
		return (Red << 16) + (Green << 8) + Blue;
	}

	public boolean equals(Object o) {
		if (o == this) { 
			return true;
		}

		if (!(o instanceof ZLColor)) {
			return false;
		}

		ZLColor color = (ZLColor)o;
		return (color.Red == Red) && (color.Green == Green) && (color.Blue == Blue);
	}

	public int hashCode() {
		return getIntValue();
	}
}
