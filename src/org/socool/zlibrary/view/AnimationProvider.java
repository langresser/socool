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

import java.util.*;

import android.graphics.*;
import android.util.FloatMath;

import org.socool.screader.screader.FBReaderApp;
import org.socool.zlibrary.text.ZLTextView;

abstract class AnimationProvider {
	static enum Mode {
		NoScrolling(false),
		ManualScrolling(false),
		AnimatedScrollingForward(true),
		AnimatedScrollingBackward(true);

		final boolean Auto;

		Mode(boolean auto) {
			Auto = auto;
		}
	}
	private Mode myMode = Mode.NoScrolling;

	protected int myStartX;
	protected int myStartY;
	protected int myEndX;
	protected int myEndY;
	protected float mySpeed;

	protected int myWidth;
	protected int myHeight;

	Mode getMode() {
		return myMode;
	}

	final void terminate() {
		myMode = Mode.NoScrolling;
		mySpeed = 0;
		myDrawInfos.clear();
	}

	final void startManualScrolling(int x, int y) {
		if (!myMode.Auto) {
			myMode = Mode.ManualScrolling;
			myEndX = myStartX = x;
			myEndY = myStartY = y;
		}
	}

	void scrollTo(int x, int y) {
		if (myMode == Mode.ManualScrolling) {
			myEndX = x;
			myEndY = y;
		}
	}
	
	abstract int getDiff(int x, int y);
	abstract int getMinDiff();

	void startAnimatedScrolling(int x, int y, int speed) {
		if (myMode != Mode.ManualScrolling) {
			return;
		}

		if (getPageToScrollTo(x, y) == ZLTextView.PageIndex.current) {
			return;
		}

		final int diff = getDiff(x, y);
		final int dpi = FBReaderApp.Instance().getDisplayDPI();
		final int minDiff = getMinDiff();
		boolean forward = Math.abs(diff) > Math.min(minDiff, dpi / 2);

		myMode = forward ? Mode.AnimatedScrollingForward : Mode.AnimatedScrollingBackward;

		float velocity = 15;
		if (myDrawInfos.size() > 1) {
			int duration = 0;
			for (DrawInfo info : myDrawInfos) {
				duration += info.Duration;
			}
			duration /= myDrawInfos.size();
			final long time = System.currentTimeMillis();
			myDrawInfos.add(new DrawInfo(x, y, time, time + duration));
			velocity = 0;
			for (int i = 1; i < myDrawInfos.size(); ++i) {
				final DrawInfo info0 = myDrawInfos.get(i - 1);
				final DrawInfo info1 = myDrawInfos.get(i);
				final float dX = info0.X - info1.X;
				final float dY = info0.Y - info1.Y;
				velocity += FloatMath.sqrt(dX * dX + dY * dY) / Math.max(1, info1.Start - info0.Start);
			}
			velocity /= myDrawInfos.size() - 1;
			velocity *= duration;
			velocity = Math.min(100, Math.max(15, velocity));
		}
		myDrawInfos.clear();

		if (getPageToScrollTo() == ZLTextView.PageIndex.previous) {
			forward = !forward;
		}

		mySpeed = forward ? -velocity : velocity;

		startAnimatedScrollingInternal(speed, false);
	}

	public void startAnimatedScrolling(ZLTextView.PageIndex pageIndex, Integer x, Integer y, int speed) {
		if (myMode.Auto) {
			return;
		}

		terminate();
		myMode = Mode.AnimatedScrollingForward;
		mySpeed = pageIndex == ZLTextView.PageIndex.next ? -15 : 15;

		setupAnimatedScrollingStart(x, y);
		startAnimatedScrollingInternal(speed, true);
	}

	protected abstract void startAnimatedScrollingInternal(int speed, boolean animationByClick);
	protected abstract void setupAnimatedScrollingStart(Integer x, Integer y);

	boolean inProgress() {
		return myMode != Mode.NoScrolling;
	}

	protected abstract int getScrollingShift();

	final void setup(int width, int height) {
		myWidth = width;
		myHeight = height;
	}

	abstract void doStep();
	abstract int getScrolledPercent();

	static class DrawInfo {
		final int X, Y;
		final long Start;
		final int Duration;

		DrawInfo(int x, int y, long start, long finish) {
			X = x;
			Y = y;
			Start = start;
			Duration = (int)(finish - start);
		}
	}
	final private List<DrawInfo> myDrawInfos = new LinkedList<DrawInfo>();

	final void draw(Canvas canvas) {
		final ZLViewWidget widget = FBReaderApp.Instance().getWidget();
		widget.setBitmapSize(myWidth, myHeight);
		final long start = System.currentTimeMillis();
		drawInternal(canvas);
		myDrawInfos.add(new DrawInfo(myEndX, myEndY, start, System.currentTimeMillis()));
		if (myDrawInfos.size() > 3) {
			myDrawInfos.remove(0);
		}
	}

	protected abstract void drawInternal(Canvas canvas);

	abstract ZLTextView.PageIndex getPageToScrollTo(int x, int y);

	final ZLTextView.PageIndex getPageToScrollTo() {
		return getPageToScrollTo(myEndX, myEndY);
	}

	protected Bitmap getBitmapFrom() {
		final ZLViewWidget widget = FBReaderApp.Instance().getWidget();
		return widget.getBitmap(ZLTextView.PageIndex.current);
	}

	protected Bitmap getBitmapTo() {
		final ZLViewWidget widget = FBReaderApp.Instance().getWidget();
		return widget.getBitmap(getPageToScrollTo());
	}
}
