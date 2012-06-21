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

package org.geometerplus.zlibrary.view;

import java.io.File;
import java.util.*;

import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLImageData;
import org.geometerplus.zlibrary.util.ZLColor;
import org.geometerplus.zlibrary.options.ZLBooleanOption;
import org.geometerplus.zlibrary.util.ZLAndroidColorUtil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.EmbossMaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.Log;

public class ZLPaintContext {
	private final ArrayList<String> myFamilies = new ArrayList<String>();
	public static ZLBooleanOption AntiAliasOption = new ZLBooleanOption("Fonts", "AntiAlias", true);
	public static ZLBooleanOption DeviceKerningOption = new ZLBooleanOption("Fonts", "DeviceKerning", false);
	public static ZLBooleanOption DitheringOption = new ZLBooleanOption("Fonts", "Dithering", false);
	public static ZLBooleanOption SubpixelOption = new ZLBooleanOption("Fonts", "Subpixel", false);

	public final Canvas myCanvas;
	private final Paint myTextPaint = new Paint();
	private final Paint myLinePaint = new Paint();
	private final Paint myFillPaint = new Paint();
	private final Paint myOutlinePaint = new Paint();

	private final int myWidth;
	private final int myHeight;

	private ZLColor myBackgroundColor = new ZLColor(0, 0, 0);

	private HashMap<String,Typeface[]> myTypefaces = new HashMap<String,Typeface[]>();
	private static ZLFile ourWallpaperFile;
	private static Bitmap ourWallpaper;
	private static int m_widthWallpaper;
	private static int m_heightWallpaper;
	
	private boolean myResetFont = true;
	private String myFontFamily = "";
	private int myFontSize;
	private boolean myFontIsBold;
	private boolean myFontIsItalic;
	private boolean myFontIsUnderlined;
	private boolean myFontIsStrikedThrough;

	public interface LineStyle {
		int SOLID_LINE = 0;
		int DASH_LINE = 1;
	};

	public interface FillStyle {
		int SOLID_FILL = 0;
		int HALF_FILL = 1;
	};
	
	protected ZLPaintContext(Canvas canvas, int width, int height) {
		myCanvas = canvas;
		myWidth = width;
		myHeight = height;
		
		if (myWidth != m_widthWallpaper || myHeight != m_heightWallpaper) {
			ourWallpaperFile = null;
		}

		myTextPaint.setLinearText(false);
		myTextPaint.setAntiAlias(AntiAliasOption.getValue());
		if (DeviceKerningOption.getValue()) {
			myTextPaint.setFlags(myTextPaint.getFlags() | Paint.DEV_KERN_TEXT_FLAG);
		} else {
			myTextPaint.setFlags(myTextPaint.getFlags() & ~Paint.DEV_KERN_TEXT_FLAG);
		}
		myTextPaint.setDither(DitheringOption.getValue());
		myTextPaint.setSubpixelText(SubpixelOption.getValue());

		myLinePaint.setStyle(Paint.Style.STROKE);

		myOutlinePaint.setColor(Color.rgb(255, 127, 0));
		myOutlinePaint.setAntiAlias(true);
		myOutlinePaint.setDither(true);
		myOutlinePaint.setStrokeWidth(4);
		myOutlinePaint.setStyle(Paint.Style.STROKE);
		myOutlinePaint.setPathEffect(new CornerPathEffect(5));
		myOutlinePaint.setMaskFilter(new EmbossMaskFilter(new float[] {1, 1, 1}, .4f, 6f, 3.5f));
	}

	public final void setFont(String family, int size, boolean bold, boolean italic, boolean underline, boolean strikeThrough) {
		if ((family != null) && !myFontFamily.equals(family)) {
			myFontFamily = family;
			myResetFont = true;
		}
		if (myFontSize != size) {
			myFontSize = size;
			myResetFont = true;
		}
		if (myFontIsBold != bold) {
			myFontIsBold = bold;
			myResetFont = true;
		}
		if (myFontIsItalic != italic) {
			myFontIsItalic = italic;
			myResetFont = true;
		}
		if (myFontIsUnderlined != underline) {
			myFontIsUnderlined = underline;
			myResetFont = true;
		}
		if (myFontIsStrikedThrough != strikeThrough) {
			myFontIsStrikedThrough = strikeThrough;
			myResetFont = true;
		}
		if (myResetFont) {
			myResetFont = false;
			setFontInternal(myFontFamily, size, bold, italic, underline, strikeThrough);
			mySpaceWidth = -1;
			myStringHeight = -1;
			myDescent = -1;
		}
	}

	final public void setLineColor(ZLColor color) {
		setLineColor(color, LineStyle.SOLID_LINE);
	}

	final public void setFillColor(ZLColor color, int alpha) {
		setFillColor(color, alpha, FillStyle.SOLID_FILL);
	}
	final public void setFillColor(ZLColor color) {
		setFillColor(color, 0xFF, FillStyle.SOLID_FILL);
	}
	
	public final int getStringWidth(String string) {
		return getStringWidth(string.toCharArray(), 0, string.length());
	}

	private int mySpaceWidth = -1;
	public final int getSpaceWidth() {
		int spaceWidth = mySpaceWidth;
		if (spaceWidth == -1) {
			spaceWidth = getSpaceWidthInternal();
			mySpaceWidth = spaceWidth;
		}
		return spaceWidth;
	}

	private int myStringHeight = -1;
	public final int getStringHeight() {
		int stringHeight = myStringHeight;
		if (stringHeight == -1) {
			stringHeight = getStringHeightInternal();
			myStringHeight = stringHeight;
		}
		return stringHeight;
	}

	private int myDescent = -1;
	public final int getDescent() {
		int descent = myDescent;
		if (descent == -1) {
			descent = getDescentInternal();
			myDescent = descent;
		}
		return descent;
	}

	public final void drawString(int x, int y, String string) {
		drawString(x, y, string.toCharArray(), 0, string.length());
	}
	
	public static final class Size {
		public final int Width;
		public final int Height;

		public Size(int w, int h) {
			Width = w;
			Height = h;
		}

		@Override
		public boolean equals(Object other) {
			if (other == this) {
				return true;
			}
			if (!(other instanceof Size)) {
				return false;
			}
			final Size s = (Size)other;
			return Width == s.Width && Height == s.Height;
		}
	}
	public static enum ScalingType {
		OriginalSize,
		IntegerCoefficient,
		FitMaximum
	}

	public ArrayList<String> fontFamilies() {
		if (myFamilies.isEmpty()) {
			fillFamiliesList(myFamilies);
		}
		return myFamilies;
	}	

	public void clear(ZLFile wallpaperFile) {
		if (ourWallpaperFile == null || !wallpaperFile.equals(ourWallpaperFile)) {
			ourWallpaperFile = wallpaperFile;
			ourWallpaper = null;
			try {
				final Bitmap fileBitmap = BitmapFactory.decodeStream(wallpaperFile.getInputStream());
				Matrix m = new Matrix();
				final int width = fileBitmap.getWidth();
		        final int height = fileBitmap.getHeight();
		        final float sx = myWidth  / (float) width;
		        final float sy = myHeight / (float) height;
		        m.setScale(sx, sy);

		        Bitmap wallpaper = Bitmap.createBitmap(fileBitmap, 0, 0, width, height, m, true);
				ourWallpaper = wallpaper;
				
				m_widthWallpaper = myWidth;
				m_heightWallpaper = myHeight;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (ourWallpaper != null) {
			myBackgroundColor = ZLAndroidColorUtil.getAverageColor(ourWallpaper);
			myCanvas.drawBitmap(ourWallpaper, 0, 0, myFillPaint);
		} else {
			clear(new ZLColor(128, 128, 128));
		}
	}

	public void clear(ZLColor color) {
		myBackgroundColor = color;
		myFillPaint.setColor(ZLAndroidColorUtil.rgb(color));
		myCanvas.drawRect(0, 0, myWidth, myHeight, myFillPaint);
	}

	public ZLColor getBackgroundColor() {
		return myBackgroundColor;
	}

	public void fillPolygon(int[] xs, int ys[]) {
		final Path path = new Path();
		final int last = xs.length - 1;
		path.moveTo(xs[last], ys[last]);
		for (int i = 0; i <= last; ++i) {
			path.lineTo(xs[i], ys[i]);
		}
		myCanvas.drawPath(path, myFillPaint);
	}

	public void drawPolygonalLine(int[] xs, int ys[]) {
		final Path path = new Path();
		final int last = xs.length - 1;
		path.moveTo(xs[last], ys[last]);
		for (int i = 0; i <= last; ++i) {
			path.lineTo(xs[i], ys[i]);
		}
		myCanvas.drawPath(path, myLinePaint);
	}

	public void drawOutline(int[] xs, int ys[]) {
		final int last = xs.length - 1;
		int xStart = (xs[0] + xs[last]) / 2;
		int yStart = (ys[0] + ys[last]) / 2;
		int xEnd = xStart;
		int yEnd = yStart;
		if (xs[0] != xs[last]) {
			if (xs[0] > xs[last]) {
				xStart -= 5;
				xEnd += 5;
			} else {
				xStart += 5;
				xEnd -= 5;
			}
		} else {
			if (ys[0] > ys[last]) {
				yStart -= 5;
				yEnd += 5;
			} else {
				yStart += 5;
				yEnd -= 5;
			}
		}

		final Path path = new Path();
		path.moveTo(xStart, yStart);
		for (int i = 0; i <= last; ++i) {
			path.lineTo(xs[i], ys[i]);
		}
		path.lineTo(xEnd, yEnd);
		myCanvas.drawPath(path, myOutlinePaint);
	}

	protected void setFontInternal(String family, int size, boolean bold, boolean italic, boolean underline, boolean strikeThrought) {
		family = realFontFamilyName(family);
		final int style = (bold ? Typeface.BOLD : 0) | (italic ? Typeface.ITALIC : 0);
		Typeface[] typefaces = myTypefaces.get(family);
		if (typefaces == null) {
			typefaces = new Typeface[4];
			myTypefaces.put(family, typefaces);
		}
		Typeface tf = typefaces[style];
		if (tf == null) {
			File[] files = AndroidFontUtil.getFontMap(false).get(family);
			if (files != null) {
				try {
					if (files[style] != null) {
						tf = AndroidFontUtil.createFontFromFile(files[style]);
					} else {
						for (int i = 0; i < 4; ++i) {
							if (files[i] != null) {
								tf = (typefaces[i] != null) ?
									typefaces[i] : AndroidFontUtil.createFontFromFile(files[i]);
								typefaces[i] = tf;
								break;
							}
						}
					}
				} catch (Throwable e) {
				}
			}
			if (tf == null) {
				tf = Typeface.create(family, style);
			}
			typefaces[style] = tf;
		}
		myTextPaint.setTypeface(tf);
		myTextPaint.setTextSize(size);
		myTextPaint.setUnderlineText(underline);
		myTextPaint.setStrikeThruText(strikeThrought);
	}

	public void setTextColor(ZLColor color) {
		myTextPaint.setColor(ZLAndroidColorUtil.rgb(color));
	}

	public void setLineColor(ZLColor color, int style) {
		// TODO: use style
		myLinePaint.setColor(ZLAndroidColorUtil.rgb(color));
	}
	
	public void setLineWidth(int width) {
		myLinePaint.setStrokeWidth(width);
	}

	public void setFillColor(ZLColor color, int alpha, int style) {
		// TODO: use style
		myFillPaint.setColor(ZLAndroidColorUtil.rgba(color, alpha));
	}

	public int getWidth() {
		return myWidth;
	}
	public int getHeight() {
		return myHeight;
	}
	
	public int getStringWidth(char[] string, int offset, int length) {
		boolean containsSoftHyphen = false;
		for (int i = offset; i < offset + length; ++i) {
			if (string[i] == (char)0xAD) {
				containsSoftHyphen = true;
				break;
			}
		}
		if (!containsSoftHyphen) {
			return (int)(myTextPaint.measureText(new String(string, offset, length)) + 0.5f);
		} else {
			final char[] corrected = new char[length];
			int len = 0;
			for (int o = offset; o < offset + length; ++o) {
				final char chr = string[o];
				if (chr != (char)0xAD) {
					corrected[len++] = chr;
				}
			}
			return (int)(myTextPaint.measureText(corrected, 0, len) + 0.5f);
		}
	}

	protected int getSpaceWidthInternal() {
		return (int)(myTextPaint.measureText(" ", 0, 1) + 0.5f);
	}

	protected int getStringHeightInternal() {
		return (int)(myTextPaint.getTextSize() + 0.5f);
	}

	protected int getDescentInternal() {
		return (int)(myTextPaint.descent() + 0.5f);
	}

	public void drawString(int x, int y, char[] string, int offset, int length) {
		boolean containsSoftHyphen = false;
		for (int i = offset; i < offset + length; ++i) {
			if (string[i] == (char)0xAD) {
				containsSoftHyphen = true;
				break;
			}
		}
		if (!containsSoftHyphen) {
//			// TODO delete it  wangjia
//			String word = new String(string, offset, length);
//			Log.e("drawString", String.format("word: %1s   x: %1d  y:%1d", word, x, y));
			myCanvas.drawText(string, offset, length, x, y, myTextPaint);
		} else {
			final char[] corrected = new char[length];
			int len = 0;
			for (int o = offset; o < offset + length; ++o) {
				final char chr = string[o];
				if (chr != (char)0xAD) {
					corrected[len++] = chr;
				}
			}
			myCanvas.drawText(corrected, 0, len, x, y, myTextPaint);
		}
	}

	public Size imageSize(ZLImageData imageData, Size maxSize, ScalingType scaling) {
		final Bitmap bitmap = imageData.getBitmap(maxSize, scaling);
		return (bitmap != null && !bitmap.isRecycled())
			? new Size(bitmap.getWidth(), bitmap.getHeight()) : null;
	}

	public void drawImage(int x, int y, ZLImageData imageData, Size maxSize, ScalingType scaling) {
		final Bitmap bitmap = imageData.getBitmap(maxSize, scaling);
		if (bitmap != null && !bitmap.isRecycled()) {
			myCanvas.drawBitmap(bitmap, x, y - bitmap.getHeight(), myFillPaint);
		}
	}

	public void drawLine(int x0, int y0, int x1, int y1) {
		final Canvas canvas = myCanvas;
		final Paint paint = myLinePaint;
		paint.setAntiAlias(false);
		canvas.drawLine(x0, y0, x1, y1, paint);
		canvas.drawPoint(x0, y0, paint);
		canvas.drawPoint(x1, y1, paint);
		paint.setAntiAlias(true);
	}

	public void fillRectangle(int x0, int y0, int x1, int y1) {
		if (x1 < x0) {
			int swap = x1;
			x1 = x0;
			x0 = swap;
		}
		if (y1 < y0) {
			int swap = y1;
			y1 = y0;
			y0 = swap;
		}
		myCanvas.drawRect(x0, y0, x1 + 1, y1 + 1, myFillPaint);
	}

	public void drawFilledCircle(int x, int y, int r) {
		// TODO: implement
	}

	public String realFontFamilyName(String fontFamily) {
		return AndroidFontUtil.realFontFamilyName(fontFamily);
	}

	protected void fillFamiliesList(ArrayList<String> families) {
		AndroidFontUtil.fillFamiliesList(families, false);
	}
}
