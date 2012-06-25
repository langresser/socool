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

package org.socool.zlibrary.view;

import org.socool.zlibrary.text.ZLTextView;

import android.graphics.*;

class ShiftAnimationProvider extends AnimationProvider {
	private final Paint myPaint = new Paint();
	private float mySpeedFactor;

	@Override
	ZLTextView.PageIndex getPageToScrollTo(int x, int y) {
		return myStartX < x ? ZLTextView.PageIndex.previous : ZLTextView.PageIndex.next;
	}

	@Override
	protected void setupAnimatedScrollingStart(Integer x, Integer y) {
		if (x == null || y == null) {
			x = mySpeed < 0 ? myWidth : 0;
			y = 0;
		}
		myEndX = myStartX = x;
		myEndY = myStartY = y;
	}

	@Override
	protected void startAnimatedScrollingInternal(int speed, boolean animationByClick) {
		mySpeedFactor = (float)Math.pow(1.5, 0.25 * speed);
		doStep();
	}

	@Override
	void doStep() {
		if (!getMode().Auto) {
			return;
		}

		myEndX += (int)mySpeed;
		final int bound;
		if (getMode() == Mode.AnimatedScrollingForward) {
			bound = myWidth;
		} else {
			bound = 0;
		}
		if (mySpeed > 0) {
			if (getScrollingShift() >= bound) {
				myEndX = myStartX + bound;
				terminate();
				return;
			}
		} else {
			if (getScrollingShift() <= -bound) {
				myEndX = myStartX - bound;
				terminate();
				return;
			}
		}
		mySpeed *= mySpeedFactor;
	}

	@Override
	protected void drawInternal(Canvas canvas) {
		myPaint.setColor(Color.rgb(127, 127, 127));
		final int dX = myEndX - myStartX;
		canvas.drawBitmap(getBitmapTo(), dX > 0 ? dX - myWidth : dX + myWidth, 0, myPaint);
		canvas.drawBitmap(getBitmapFrom(), dX, 0, myPaint);
	}
	
	protected int getScrollingShift() {
		return myEndX - myStartX;
	}
	
	int getScrolledPercent() {
		final int full = myWidth;
		final int shift = Math.abs(getScrollingShift());
		return 100 * shift / full;
	}
	
	int getDiff(int x, int y)
	{
		return x - myStartX;
	}
	
	int getMinDiff()
	{
		return (myWidth > myHeight ? myWidth / 4 : myWidth / 3);
	}
}
