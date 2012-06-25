package org.socool.zlibrary.view;

import org.socool.zlibrary.text.ZLTextView;

import android.graphics.*;

class SlideAnimationProvider extends AnimationProvider {
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
		canvas.drawBitmap(getBitmapTo(), 0, 0, myPaint);
		myPaint.setColor(Color.rgb(127, 127, 127));
		final int dX = myEndX - myStartX;
		canvas.drawBitmap(getBitmapFrom(), dX, 0, myPaint);
		if (dX > 0 && dX < myWidth) {
			canvas.drawLine(dX, 0, dX, myHeight + 1, myPaint);
		} else if (dX < 0 && dX > -myWidth) {
			canvas.drawLine(dX + myWidth, 0, dX + myWidth, myHeight + 1, myPaint);
		}
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
