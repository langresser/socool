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

import org.geometerplus.zlibrary.text.view.ZLTextView;

import android.graphics.*;


class NoneAnimationProvider extends AnimationProvider {
	private final Paint myPaint = new Paint();
	private float mySpeedFactor;

	@Override
	protected void drawInternal(Canvas canvas) {
		myPaint.setColor(Color.rgb(127, 127, 127));
		final int dY = myEndY - myStartY;
		canvas.drawBitmap(getBitmapTo(), 0, dY > 0 ? dY - myHeight : dY + myHeight, myPaint);
		canvas.drawBitmap(getBitmapFrom(), 0, dY, myPaint);
		if (dY > 0 && dY < myHeight) {
			canvas.drawLine(0, dY, myWidth + 1, dY, myPaint);
		} else if (dY < 0 && dY > -myHeight) {
			canvas.drawLine(0, dY + myHeight, myWidth + 1, dY + myHeight, myPaint);
		}
	}

	@Override
	protected void setupAnimatedScrollingStart(Integer x, Integer y) {
		myEndX = myStartX = 0;
		myStartY = mySpeed < 0 ? myHeight : 0;
		myEndY = myHeight - myStartY;
	}

	@Override
	protected void startAnimatedScrollingInternal(int speed, boolean animationByClick) {
		// 点击进行翻页不要动画，手指拖动可以产生动画
		if (!animationByClick) {
			mySpeedFactor = (float)Math.pow(1.5, 0.25 * speed);
			doStep();
		}
	}

	@Override
	ZLTextView.PageIndex getPageToScrollTo(int x, int y) {
		return myStartY < y ? ZLTextView.PageIndex.previous : ZLTextView.PageIndex.next;
	}

	@Override
	void doStep() {
		// 无动画效果
//		if (getMode().Auto) {
//			terminate();
//			return;
//		}

		// 滚动动画
		if (!getMode().Auto) {
			return;
		}

		myEndY += (int)mySpeed;

		final int bound;
		if (getMode() == Mode.AnimatedScrollingForward) {
			bound = myHeight;
		} else {
			bound = 0;
		}
		if (mySpeed > 0) {
			if (getScrollingShift() >= bound) {
				myEndY = myStartY + bound;
				terminate();
				return;
			}
		} else {
			if (getScrollingShift() <= -bound) {
				myEndY = myStartY - bound;
				terminate();
				return;
			}
		}
		mySpeed *= mySpeedFactor;
	}
	
	protected int getScrollingShift() {
		return myEndY - myStartY;
	}
	
	int getScrolledPercent() {
		final int full = myHeight;
		final int shift = Math.abs(getScrollingShift());
		return 100 * shift / full;
	}
	
	int getDiff(int x, int y)
	{
		return y - myStartY;
	}
	
	int getMinDiff()
	{
		return (myHeight > myWidth ? myHeight / 4 : myHeight / 3);
	}
}
