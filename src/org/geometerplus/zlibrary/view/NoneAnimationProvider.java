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

	NoneAnimationProvider(BitmapManager bitmapManager) {
		super(bitmapManager);
	}

	@Override
	protected void drawInternal(Canvas canvas) {
		canvas.drawBitmap(getBitmapFrom(), 0, 0, myPaint);
	}

	@Override
	void doStep() {
		if (getMode().Auto) {
			terminate();
		}
	}

	@Override
	protected void setupAnimatedScrollingStart(Integer x, Integer y) {
		if (myDirection.IsHorizontal) {
			myStartX = mySpeed < 0 ? myWidth : 0;
			myEndX = myWidth - myStartX;
			myEndY = myStartY = 0;
		} else {
			myEndX = myStartX = 0;
			myStartY = mySpeed < 0 ? myHeight : 0;
			myEndY = myHeight - myStartY;
		}
	}

	@Override
	protected void startAnimatedScrollingInternal(int speed) {
	}

	@Override
	ZLTextView.PageIndex getPageToScrollTo(int x, int y) {
		if (myDirection == null) {
			return ZLTextView.PageIndex.current;
		}

		switch (myDirection) {
			case rightToLeft:
				return myStartX < x ? ZLTextView.PageIndex.previous : ZLTextView.PageIndex.next;
			case leftToRight:
				return myStartX < x ? ZLTextView.PageIndex.next : ZLTextView.PageIndex.previous;
			case up:
				return myStartY < y ? ZLTextView.PageIndex.previous : ZLTextView.PageIndex.next;
			case down:
				return myStartY < y ? ZLTextView.PageIndex.next : ZLTextView.PageIndex.previous;
		}
		return ZLTextView.PageIndex.current;
	}
}
