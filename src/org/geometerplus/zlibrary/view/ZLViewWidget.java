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

import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.util.AttributeSet;

import org.geometerplus.zlibrary.application.ZLApplication;
import org.geometerplus.zlibrary.text.view.ZLTextView;

import org.geometerplus.android.fbreader.SCReaderActivity;

public class ZLViewWidget extends View implements View.OnLongClickListener {
	private final Paint myPaint = new Paint();
	private final BitmapManager myBitmapManager = new BitmapManager();

	public ZLViewWidget(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public ZLViewWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ZLViewWidget(Context context) {
		super(context);
		init();
	}

	private void init() {
		// next line prevent ignoring first onKeyDown DPad event
		// after any dialog was closed
		setFocusableInTouchMode(true);
		setDrawingCacheEnabled(false);
		setOnLongClickListener(this);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		getAnimationProvider().terminate();
		if (myScreenIsTouched) {
			final ZLTextView view = ZLApplication.Instance().getCurrentView();
			myScreenIsTouched = false;
			view.onScrollingFinished(ZLTextView.PageIndex.current);
		}
	}

	@Override
	protected void onDraw(final Canvas canvas) {
		final Context context = getContext();
		if (context instanceof SCReaderActivity) {
			((SCReaderActivity)context).createWakeLock();
		} else {
			System.err.println("A surprise: view's context is not a ZLAndroidActivity");
		}
		super.onDraw(canvas);

		if (getAnimationProvider().inProgress()) {
			onDrawInScrolling(canvas);
		} else {
			onDrawStatic(canvas);
			ZLApplication.Instance().onRepaintFinished();
		}
	}

	private AnimationProvider myAnimationProvider;
	private ZLTextView.Animation myAnimationType;
	private AnimationProvider getAnimationProvider() {
		final ZLTextView.Animation type = ZLApplication.Instance().getCurrentView().getAnimationType();
		if (myAnimationProvider == null || myAnimationType != type) {
			myAnimationType = type;
			switch (type) {
				case none:
					myAnimationProvider = new NoneAnimationProvider(myBitmapManager);
					break;
				case curl:
					myAnimationProvider = new CurlAnimationProvider(myBitmapManager);
					break;
				case shift:
					myAnimationProvider = new ShiftAnimationProvider(myBitmapManager);
					break;
			}
		}
		return myAnimationProvider;
	}

	private void onDrawInScrolling(Canvas canvas) {
		final ZLTextView view = ZLApplication.Instance().getCurrentView();

		final AnimationProvider animator = getAnimationProvider();
		final AnimationProvider.Mode oldMode = animator.getMode();
		animator.doStep();
		if (animator.inProgress()) {
			animator.draw(canvas);
			if (animator.getMode().Auto) {
				postInvalidate();
			}
		} else {
			switch (oldMode) {
				case AnimatedScrollingForward:
				{
					final ZLTextView.PageIndex index = animator.getPageToScrollTo();
					myBitmapManager.shift(index == ZLTextView.PageIndex.next);
					view.onScrollingFinished(index);
					ZLApplication.Instance().onRepaintFinished();
					break;
				}
				case AnimatedScrollingBackward:
					view.onScrollingFinished(ZLTextView.PageIndex.current);
					break;
			}
			onDrawStatic(canvas);
		}
	}

	public void reset() {
		myBitmapManager.reset();
	}

	public void repaint() {
		postInvalidate();
	}

	public void startManualScrolling(int x, int y, ZLTextView.Direction direction) {
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight());
		animator.startManualScrolling(x, y);
	}

	public void scrollManuallyTo(int x, int y) {
		final ZLTextView view = ZLApplication.Instance().getCurrentView();
		final AnimationProvider animator = getAnimationProvider();
		if (view.canScroll(animator.getPageToScrollTo(x, y))) {
			animator.scrollTo(x, y);
			postInvalidate();
		}
	}

	public void startAnimatedScrolling(ZLTextView.PageIndex pageIndex, int x, int y, ZLTextView.Direction direction, int speed) {
		final ZLTextView view = ZLApplication.Instance().getCurrentView();
		if (pageIndex == ZLTextView.PageIndex.current || !view.canScroll(pageIndex)) {
			return;
		}
		final AnimationProvider animator = getAnimationProvider();
		animator.setup(direction, getWidth(), getMainAreaHeight());
		
		if (x == -1 && y == -1) {
			animator.startAnimatedScrolling(pageIndex, null, null, speed);
		} else {
			animator.startAnimatedScrolling(pageIndex, x, y, speed);
		}
		
		if (animator.getMode().Auto) {
			postInvalidate();
		}
	}

	public void startAnimatedScrolling(int x, int y, int speed) {
		final ZLTextView view = ZLApplication.Instance().getCurrentView();
		final AnimationProvider animator = getAnimationProvider();
		if (!view.canScroll(animator.getPageToScrollTo(x, y))) {
			animator.terminate();
			return;
		}
		animator.startAnimatedScrolling(x, y, speed);
		postInvalidate();
	}

	void drawOnBitmap(Bitmap bitmap, ZLTextView.PageIndex index) {
		final ZLTextView view = ZLApplication.Instance().getCurrentView();
		if (view == null) {
			return;
		}

		final ZLPaintContext context = new ZLPaintContext(
			new Canvas(bitmap),
			getWidth(),
			getMainAreaHeight(),
			view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
		view.paint(context, index);
		
		// draw footer
		final ZLTextView.FooterArea footer = view.getFooterArea();

		if (footer == null) {
			return;
		}

		final ZLPaintContext contextFooter = new ZLPaintContext(
			new Canvas(bitmap),
			getWidth(),
			footer.getHeight(),
			view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
		footer.paint(contextFooter, index, false);
	}

	private void onDrawStatic(Canvas canvas) {
		myBitmapManager.setSize(getWidth(), getMainAreaHeight());
		canvas.drawBitmap(myBitmapManager.getBitmap(ZLTextView.PageIndex.current), 0, 0, myPaint);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
		} else {
			ZLApplication.Instance().getCurrentView().onTrackballRotated((int)(10 * event.getX()), (int)(10 * event.getY()));
		}
		return true;
	}


	private class LongClickRunnable implements Runnable {
		public void run() {
			if (performLongClick()) {
				myLongClickPerformed = true;
			}
		}
	}
	private volatile LongClickRunnable myPendingLongClickRunnable;
	private volatile boolean myLongClickPerformed;

	private void postLongClickRunnable() {
        myLongClickPerformed = false;
		myPendingPress = false;
        if (myPendingLongClickRunnable == null) {
            myPendingLongClickRunnable = new LongClickRunnable();
        }
        postDelayed(myPendingLongClickRunnable, 2 * ViewConfiguration.getLongPressTimeout());
    }

	private volatile boolean myPendingPress;
	private int myPressedX, myPressedY;
	private boolean myScreenIsTouched;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int x = (int)event.getX();
		int y = (int)event.getY();

		final ZLTextView view = ZLApplication.Instance().getCurrentView();
		switch (event.getAction()) {
			case MotionEvent.ACTION_UP:
				if (myLongClickPerformed) {
					view.onFingerReleaseAfterLongPress(x, y);
				} else {
					if (myPendingLongClickRunnable != null) {
						removeCallbacks(myPendingLongClickRunnable);
						myPendingLongClickRunnable = null;
					}
					if (myPendingPress) {
						view.onFingerSingleTap(x, y);
					} else {
						view.onFingerRelease(x, y);
					}
				}
				myPendingPress = false;
				myScreenIsTouched = false;
				break;
			case MotionEvent.ACTION_DOWN:
				postLongClickRunnable();
				myPendingPress = true;
				myScreenIsTouched = true;
				myPressedX = x;
				myPressedY = y;
				break;
			case MotionEvent.ACTION_MOVE:
			{
				final int slop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
				final boolean isAMove =
					Math.abs(myPressedX - x) > slop || Math.abs(myPressedY - y) > slop;
				if (myLongClickPerformed) {
					view.onFingerMoveAfterLongPress(x, y);
				} else {
					if (myPendingPress) {
						if (isAMove) {
							if (myPendingLongClickRunnable != null) {
								removeCallbacks(myPendingLongClickRunnable);
							}
							view.onFingerPress(myPressedX, myPressedY);
							myPendingPress = false;
						}
					}
					if (!myPendingPress) {
						view.onFingerMove(x, y);
					}
				}
				break;
			}
		}

		return true;
	}

	public boolean onLongClick(View v) {
		final ZLTextView view = ZLApplication.Instance().getCurrentView();
		return view.onFingerLongPress(myPressedX, myPressedY);
	}

	private int myKeyUnderTracking = -1;
	private long myTrackingStartTime;

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyDown(keyCode, event);
		}

		final ZLApplication application = ZLApplication.Instance();

		if (application.hasActionForKey(keyCode, true) ||
			application.hasActionForKey(keyCode, false)) {
			if (myKeyUnderTracking != -1) {
				if (myKeyUnderTracking == keyCode) {
					return true;
				} else {
					myKeyUnderTracking = -1;
				}
			}
			if (application.hasActionForKey(keyCode, true)) {
				myKeyUnderTracking = keyCode;
				myTrackingStartTime = System.currentTimeMillis();
				return true;
			} else {
				return application.runActionByKey(keyCode, false);
			}
		} else {
			return false;
		}
	}

	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (myKeyUnderTracking != -1) {
			if (myKeyUnderTracking == keyCode) {
				final boolean longPress = System.currentTimeMillis() >
					myTrackingStartTime + ViewConfiguration.getLongPressTimeout();
				ZLApplication.Instance().runActionByKey(keyCode, longPress);
			}
			myKeyUnderTracking = -1;
			return true;
		} else {
			final ZLApplication application = ZLApplication.Instance();
			return
				application.hasActionForKey(keyCode, false) ||
				application.hasActionForKey(keyCode, true);
		}
	}

	private int getMainAreaHeight() {
		final ZLTextView.FooterArea footer = ZLApplication.Instance().getCurrentView().getFooterArea();
		return footer != null ? getHeight() - footer.getHeight() : getHeight();
	}
}
