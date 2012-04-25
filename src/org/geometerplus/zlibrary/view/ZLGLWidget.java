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
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import org.geometerplus.zlibrary.text.view.ZLTextView;

import org.geometerplus.android.fbreader.SCReaderActivity;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class ZLGLWidget extends GLSurfaceView implements View.OnLongClickListener, CurlRenderer.Observer  {	
	// Page meshes. Left and right meshes are 'static' while curl is used to
	// show page flipping.
	private CurlMesh mPageCurl;
	private CurlMesh mPageLeft;
	private CurlMesh mPageRight;
	private CurlMesh mPageNextCache;
	
	// Curl state. We are flipping none, left or right page.
	private static final int CURL_NONE = 0;
	private static final int CURL_LEFT = 1;
	private static final int CURL_RIGHT = 2;
	private int mCurlState = CURL_NONE;
	
	// Bitmap size. These are updated from renderer once it's initialized.
	private int mPageBitmapWidth = -1;
	private int mPageBitmapHeight = -1;
	
	private final int SIZE = 3;
	private final Bitmap[] myBitmaps = new Bitmap[SIZE];
	private final ZLTextView.PageIndex[] myIndexes = new ZLTextView.PageIndex[SIZE];
	
	// Start position for dragging.
	private PointF mDragStartPos = new PointF();
	private PointF mCurlPos = new PointF();
	private PointF mCurlDir = new PointF();

	private boolean m_needRepaint = false;
	private boolean mAnimate = false;
	private PointF mAnimationSource = new PointF();
	private PointF mAnimationTarget = new PointF();
	private long mAnimationStartTime;
	private long mAnimationDurationTime = 500;
	private int mAnimationTargetEvent;
	
	// Constants for mAnimationTargetEvent.
	private static final int SET_CURL_TO_LEFT = 1;
	private static final int SET_CURL_TO_RIGHT = 2;
	
	private CurlRenderer mRenderer;

	public ZLGLWidget(Context context, AttributeSet attrs, int defStyle) {
		this(context, attrs);
	}

	public ZLGLWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ZLGLWidget(Context context) {
		super(context);
		init(context);
	}

	public void reset() {
		for (int i = 0; i < SIZE; ++i) {
			myIndexes[i] = null;
		}
	}
	
	public void releaseBitmap() {
		for (int i = 0; i < SIZE; ++i) {
			myBitmaps[i] = null;
			myIndexes[i] = null;
		}

		System.gc();
	}

	Bitmap getBitmap(ZLTextView.PageIndex index, boolean forceUpdateStatusBar) {
		for (int i = 0; i < SIZE; ++i) {
			if (index == myIndexes[i]) {
//				if (forceUpdateStatusBar) {
//					// draw footer
//					final ZLTextView view = ZLApplication.Instance().getCurrentView();
//					final ZLTextView.FooterArea footer = view.getFooterArea();
//
//					if (footer != null) {
//						final ZLAndroidPaintContext contextFooter = new ZLAndroidPaintContext(
//								new Canvas(myBitmaps[i]), mPageBitmapWidth, footer.getHeight(),
//								view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0);
//						footer.paint(contextFooter, index, true);
//					}
//				}
				if (forceUpdateStatusBar) {
					drawOnBitmap(myBitmaps[i], index);
				}
				return myBitmaps[i];
			}
		}

		final int iIndex = getInternalIndex(index);
		myIndexes[iIndex] = index;
		if (myBitmaps[iIndex] == null) {
			try {
				myBitmaps[iIndex] = Bitmap.createBitmap(mPageBitmapWidth, mPageBitmapHeight, Bitmap.Config.RGB_565);
			} catch (OutOfMemoryError e) {
				System.gc();
				myBitmaps[iIndex] = Bitmap.createBitmap(mPageBitmapWidth, mPageBitmapHeight, Bitmap.Config.RGB_565);
			}
		}
		
		drawOnBitmap(myBitmaps[iIndex], index);
		return myBitmaps[iIndex];
	}
	
	private int getInternalIndex(ZLTextView.PageIndex index) {
		for (int i = 0; i < SIZE; ++i) {
			if (myIndexes[i] == null) {
				return i;
			}
		}
		for (int i = 0; i < SIZE; ++i) {
			if (myIndexes[i] != ZLTextView.PageIndex.current) {
				return i;
			}
		}
		throw new RuntimeException("That's impossible");
	}
	
	void shift(boolean forward) {
		for (int i = 0; i < SIZE; ++i) {
			if (myIndexes[i] == null) {
				continue;
			}
			myIndexes[i] = forward ? myIndexes[i].getPrevious() : myIndexes[i].getNext();
		}
	}

	public void repaint(boolean force) {
		if (force) {
			updateBitmaps();
			requestRender();
			m_needRepaint = false;
		} else {
			m_needRepaint = true;
		}
	}
	
	public void repaintStatusBar()
	{
		if (mPageBitmapWidth <= 0 || mPageBitmapHeight <= 0) {
			return;
		}

		// Remove meshes from renderer.		
		ZLTextView.PageIndex leftPageIndex = ZLTextView.PageIndex.current;
		ZLTextView.PageIndex rightPageIndex = ZLTextView.PageIndex.current;
		ZLTextView.PageIndex curlPageIndex = ZLTextView.PageIndex.current;
		
		if (mCurlState == CURL_LEFT) {
			curlPageIndex = ZLTextView.PageIndex.previous;
			rightPageIndex = ZLTextView.PageIndex.current;
		} else if (mCurlState == CURL_RIGHT) {
			leftPageIndex = ZLTextView.PageIndex.previous;
			curlPageIndex = ZLTextView.PageIndex.current;
			rightPageIndex = ZLTextView.PageIndex.next;
		} else if (mCurlState == CURL_NONE) {
			leftPageIndex = ZLTextView.PageIndex.previous;
			rightPageIndex = ZLTextView.PageIndex.current;
		}
		
		// 右侧页面放当前页文字
		final ZLTextView view = FBReaderApp.Instance().getCurrentView();
		
		if (mCurlState == CURL_LEFT || mCurlState == CURL_NONE) {
			if (view.canScroll(ZLTextView.PageIndex.next)) {
				mPageNextCache.setBitmap(getBitmap(ZLTextView.PageIndex.next, true));
			}
		}
		
		if (view.canScroll(rightPageIndex)) {			
			mPageRight.setBitmap(getBitmap(rightPageIndex, true));
		}
		

		// 左侧页面放上一页（只有翻页的时候需要用到，这个页面并不参与显示）
		if (view.canScroll(leftPageIndex)) {
			mPageLeft.setBitmap(getBitmap(leftPageIndex, true));
		}
		
		// 如果当前处于翻页动画状态，要更新翻卷页面
		if (mCurlState == CURL_RIGHT) {
			mPageCurl.setBitmap(getBitmap(curlPageIndex, true));
		} else if (mCurlState == CURL_LEFT) {
			mPageCurl.setBitmap(getBitmap(curlPageIndex, true));
		}

		requestRender();
	}

	void drawOnBitmap(Bitmap bitmap, ZLTextView.PageIndex index) {
		final ZLTextView view = FBReaderApp.Instance().getCurrentView();

		if (view == null) {
			return;
		}
		
		if (mPageBitmapWidth == -1 || mPageBitmapHeight == -1) {
			return;
		}

		// draw text
		final ZLPaintContext context = new ZLPaintContext(
			new Canvas(bitmap),
			mPageBitmapWidth,
			getMainAreaHeight(),
			view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
		view.paint(context, index);

		// draw footer
		final ZLTextView.Footer footer = view.getFooterArea();

		if (footer == null) {
			return;
		}

		final ZLPaintContext contextFooter = new ZLPaintContext(
			new Canvas(bitmap),
			mPageBitmapWidth,
			footer.getHeight(),
			view.isScrollbarShown() ? getVerticalScrollbarWidth() : 0
		);
		footer.paint(contextFooter, index, false);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, null);
		} else {
			FBReaderApp.Instance().getCurrentView().onTrackballRotated((int)(10 * event.getX()), (int)(10 * event.getY()));
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
		
		// No dragging during animation at the moment.
		// TODO: Stop animation on touch event and return to drag mode.
		if (mAnimate) {
			return false;
		}

		final ZLTextView view = FBReaderApp.Instance().getCurrentView();
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
				final boolean isAMove = Math.abs(myPressedX - x) > slop || Math.abs(myPressedY - y) > slop;
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
		return FBReaderApp.Instance().getCurrentView().onFingerLongPress(myPressedX, myPressedY);
	}

	private int myKeyUnderTracking = -1;
	private long myTrackingStartTime;

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return super.onKeyDown(keyCode, event);
		}

		if (FBReaderApp.Instance().hasActionForKey(keyCode, true) ||
				FBReaderApp.Instance().hasActionForKey(keyCode, false)) {
			if (myKeyUnderTracking != -1) {
				if (myKeyUnderTracking == keyCode) {
					return true;
				} else {
					myKeyUnderTracking = -1;
				}
			}
			if (FBReaderApp.Instance().hasActionForKey(keyCode, true)) {
				myKeyUnderTracking = keyCode;
				myTrackingStartTime = System.currentTimeMillis();
				return true;
			} else {
				return FBReaderApp.Instance().runActionByKey(keyCode, false);
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
					FBReaderApp.Instance().runActionByKey(keyCode, longPress);
			}
			myKeyUnderTracking = -1;
			return true;
		} else {
			return
				FBReaderApp.Instance().hasActionForKey(keyCode, false) ||
				FBReaderApp.Instance().hasActionForKey(keyCode, true);
		}
	}

	private int getMainAreaHeight() {
		final ZLTextView.Footer footer = FBReaderApp.Instance().getCurrentView().getFooterArea();
		return footer != null ? getHeight() - footer.getHeight() : getHeight();
	}

	@Override
	public void onDrawFrame() {
		// We are not animating.
		if (mAnimate == false) {
			return;
		}
		
		final Context context = getContext();
		if (context instanceof SCReaderActivity) {
			((SCReaderActivity)context).createWakeLock();
		} else {
			System.err.println("A surprise: view's context is not a ZLAndroidActivity");
		}

		long currentTime = System.currentTimeMillis();
		// If animation is done.
		if (currentTime > mAnimationStartTime + mAnimationDurationTime + 25) {
			final ZLTextView view = FBReaderApp.Instance().getCurrentView();

			if (mAnimationTargetEvent == SET_CURL_TO_RIGHT) {
				// Switch curled page to right.
				CurlMesh right = mPageCurl;
				CurlMesh curl = mPageRight;
				right.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
				right.setFlipTexture(false);
				right.reset();
				mRenderer.removeCurlMesh(curl);
				mPageCurl = curl;
				mPageRight = right;
				// If we were curling left page update current index.
				if (mCurlState == CURL_RIGHT) {
					// 还原
					view.onScrollingFinished(ZLTextView.PageIndex.current);
				} else {
					// 上一页
					shift(false);
					view.onScrollingFinished(ZLTextView.PageIndex.previous);
					FBReaderApp.Instance().onRepaintFinished();
				}
			} else if (mAnimationTargetEvent == SET_CURL_TO_LEFT) {
				// Switch curled page to left.
				CurlMesh left = mPageCurl;
				CurlMesh curl = mPageLeft;
				left.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
				left.setFlipTexture(true);
				left.reset();
				mRenderer.removeCurlMesh(curl);
				mRenderer.removeCurlMesh(left);
				mPageCurl = curl;
				mPageLeft = left;
				// If we were curling right page update current index.
				if (mCurlState == CURL_RIGHT) {
					// 下一页
					shift(true);
					view.onScrollingFinished(ZLTextView.PageIndex.next);
					FBReaderApp.Instance().onRepaintFinished();
				} else {
					// 还原
					view.onScrollingFinished(ZLTextView.PageIndex.current);
				}
			}

			mAnimate = false;
			requestRender();
			
			// 动画执行完毕，更新cache页面
			if (view.canScroll(ZLTextView.PageIndex.next)) {
				mPageNextCache.setBitmap(getBitmap(ZLTextView.PageIndex.next, false));
				mPageNextCache.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
				mPageNextCache.setFlipTexture(false);
				mPageNextCache.reset();
			}
			
			mCurlState = CURL_NONE;
		} else {
			float x = mAnimationSource.x;
			float y = mAnimationSource.y;
			float t = (float) Math.sqrt((double) (currentTime - mAnimationStartTime) / mAnimationDurationTime);
			x += (mAnimationTarget.x - mAnimationSource.x) * t;
			y += (mAnimationTarget.y - mAnimationSource.y) * t;
			updateCurlPos(x, y);
		}
	}

	@Override
	public void onPageSizeChanged(int width, int height) {
		mPageBitmapWidth = width;
		mPageBitmapHeight = height;
		
		// TODO 结束动画
//		if (myScreenIsTouched) {
//			final ZLTextView view = ZLApplication.Instance().getCurrentView();
//			myScreenIsTouched = false;
//			view.onScrollingFinished(ZLTextView.PageIndex.current);
//		}
		
		
		if (m_needRepaint) {
			m_needRepaint = false;
			
			// 当大小改变的时候重新加载贴图
			reset();
			updateBitmaps();
			requestRender();
		}
	}

	@Override
	public void onSurfaceCreated() {
		// In case surface is recreated, let page meshes drop allocated texture
		// ids and ask for new ones. There's no need to set textures here as
		// onPageSizeChanged should be called later on.
		mPageLeft.resetTexture();
		mPageRight.resetTexture();
		mPageCurl.resetTexture();
		
		m_needRepaint = true;
	}

	public void startManualScrolling(int x, int y, ZLTextView.Direction direction) {
		// We need page rects quite extensively so get them for later use.
		RectF rightRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT);

		// Once we receive pointer down event its position is mapped to
		// right or left edge of page and that'll be the position from where
		// user is holding the paper to make curl happen.
		mDragStartPos.set(x, y);
		mRenderer.translate(mDragStartPos);
	
		// First we make sure it's not over or below page. Pages are
		// supposed to be same height so it really doesn't matter do we use
		// left or right one.
		if (mDragStartPos.y > rightRect.top) {
			mDragStartPos.y = rightRect.top;
		} else if (mDragStartPos.y < rightRect.bottom) {
			mDragStartPos.y = rightRect.bottom;
		}
	
		// Then we have to make decisions for the user whether curl is going
		// to happen from left or right, and on which page.
		float halfX = (rightRect.right + rightRect.left) / 2;
		if (mDragStartPos.x < halfX) {
			mDragStartPos.x = rightRect.left;
			startCurl(CURL_LEFT);
		} else {
			mDragStartPos.x = rightRect.right;
			startCurl(CURL_RIGHT);
		}
		// If we have are in curl state, let this case clause flow through
		// to next one. We have pointer position and drag position defined
		// and this will create first render request given these points.
		if (mCurlState == CURL_NONE) {
			return;
		}
	}

	public void scrollManuallyTo(int x, int y) {
		if (mCurlState == CURL_LEFT || mCurlState == CURL_RIGHT) {
			PointF point = new PointF(x, y);
			mRenderer.translate(point);
			updateCurlPos(point.x, point.y);
		}
	}
	
	// 点击（或其他按键操作）直接进行翻页
	public void startAnimatedScrolling(ZLTextView.PageIndex pageIndex, ZLTextView.Direction direction, int speed) {		
		// 初始化翻页
		if (pageIndex == ZLTextView.PageIndex.next) {
			mDragStartPos.set(mPageBitmapWidth, mPageBitmapHeight / 2);
			mRenderer.translate(mDragStartPos);
			startCurl(CURL_RIGHT);
		} else if (pageIndex == ZLTextView.PageIndex.previous) {
			mDragStartPos.set(0, mPageBitmapHeight / 2);
			mRenderer.translate(mDragStartPos);
			startCurl(CURL_LEFT);
		} else {
			return;
		}
		
		// We need page rects quite extensively so get them for later use.
		RectF leftRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT);
		if (mCurlState == CURL_RIGHT) {
			// Animation source is the point from where animation starts.
			// Also it's handled in a way we actually simulate touch events
			// meaning the output is exactly the same as if user drags the
			// page to other side. While not producing the best looking
			// result (which is easier done by altering curl position and/or
			// direction directly), this is done in a hope it made code a
			// bit more readable and easier to maintain.
			mAnimationSource.set(mPageBitmapWidth, mPageBitmapHeight / 2);
			mRenderer.translate(mAnimationSource);
			mAnimationStartTime = System.currentTimeMillis();

			// Given the explanation, here we decide whether to simulate
			// drag to left or right end.
			mAnimationTarget.set(mDragStartPos);
			mAnimationTarget.x = leftRect.left;
			mAnimationTargetEvent = SET_CURL_TO_LEFT;
			mAnimate = true;
			requestRender();
		} else if (mCurlState == CURL_LEFT) {
			mAnimationSource.set(0, mPageBitmapHeight / 2);
			mRenderer.translate(mAnimationSource);
			mAnimationStartTime = System.currentTimeMillis();

			// Given the explanation, here we decide whether to simulate
			// drag to left or right end.
			mAnimationTarget.set(mDragStartPos);
			mAnimationTarget.x = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).right;
			mAnimationTargetEvent = SET_CURL_TO_RIGHT;
			mAnimate = true;
			requestRender();
		}
	}

	// 拖动到一定位置进行翻页动画
	public void startAnimatedScrolling(ZLTextView.PageIndex pageIndex, int x, int y, ZLTextView.Direction direction, int speed) {		
		// We need page rects quite extensively so get them for later use.
		RectF rightRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT);
		RectF leftRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT);
		if (mCurlState == CURL_RIGHT) {
			// Animation source is the point from where animation starts.
			// Also it's handled in a way we actually simulate touch events
			// meaning the output is exactly the same as if user drags the
			// page to other side. While not producing the best looking
			// result (which is easier done by altering curl position and/or
			// direction directly), this is done in a hope it made code a
			// bit more readable and easier to maintain.
			mAnimationSource.set(x, y);
			mRenderer.translate(mAnimationSource);
			mAnimationStartTime = System.currentTimeMillis();

			// Given the explanation, here we decide whether to simulate
			// drag to left or right end.
			if ( mAnimationSource.x >= (rightRect.left + (rightRect.right - rightRect.left) * 3 / 4)) {
				// On right side target is always right page's right border.
				mAnimationTarget.set(mDragStartPos);
				mAnimationTarget.x = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).right;
				mAnimationTargetEvent = SET_CURL_TO_RIGHT;
			} else {
				// On left side target depends on visible pages.
				mAnimationTarget.set(mDragStartPos);
				mAnimationTarget.x = leftRect.left;
				mAnimationTargetEvent = SET_CURL_TO_LEFT;
			}
			mAnimate = true;
			requestRender();
		} else if (mCurlState == CURL_LEFT) {
			mAnimationSource.set(x, y);
			mRenderer.translate(mAnimationSource);
			mAnimationStartTime = System.currentTimeMillis();

			// Given the explanation, here we decide whether to simulate
			// drag to left or right end.
			if ( mAnimationSource.x > (rightRect.left + (rightRect.right - rightRect.left) * 1 / 4)) {
				// On right side target is always right page's right border.
				mAnimationTarget.set(mDragStartPos);
				mAnimationTarget.x = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).right;
				mAnimationTargetEvent = SET_CURL_TO_RIGHT;
			} else {
				// On left side target depends on visible pages.
				mAnimationTarget.set(mDragStartPos);
				mAnimationTarget.x = rightRect.left;
				mAnimationTargetEvent = SET_CURL_TO_LEFT;
			}
			mAnimate = true;
			requestRender();
		}
	}

	private void init(Context ctx) {
		// next line prevent ignoring first onKeyDown DPad event
		// after any dialog was closed
		setFocusableInTouchMode(true);
		setDrawingCacheEnabled(false);
		setOnLongClickListener(this);

		mRenderer = new CurlRenderer(this);
		setRenderer(mRenderer);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

		// Even though left and right pages are static we have to allocate room
		// for curl on them too as we are switching meshes. Another way would be
		// to swap texture ids only.
		mPageLeft = new CurlMesh(10);
		mPageRight = new CurlMesh(10);
		mPageCurl = new CurlMesh(10);
		mPageLeft.setFlipTexture(true);
		mPageRight.setFlipTexture(false);
		
		mPageNextCache = new CurlMesh(10);
	}

	/**
	 * Sets mPageCurl curl position.
	 */
	private void setCurlPos(PointF curlPos, PointF curlDir, double radius) {

		// First reposition curl so that page doesn't 'rip off' from book.
		if (mCurlState == CURL_RIGHT || mCurlState == CURL_LEFT) {
			RectF pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT);
			if (curlPos.x >= pageRect.right) {
				mPageCurl.reset();
				requestRender();
				return;
			}
			if (curlPos.x < pageRect.left) {
				curlPos.x = pageRect.left;
			}
			if (curlDir.y != 0) {
				float diffX = curlPos.x - pageRect.left;
				float leftY = curlPos.y + (diffX * curlDir.x / curlDir.y);
				if (curlDir.y < 0 && leftY < pageRect.top) {
					curlDir.x = curlPos.y - pageRect.top;
					curlDir.y = pageRect.left - curlPos.x;
				} else if (curlDir.y > 0 && leftY > pageRect.bottom) {
					curlDir.x = pageRect.bottom - curlPos.y;
					curlDir.y = curlPos.x - pageRect.left;
				}
			}
		} else if (mCurlState == CURL_LEFT) {
			RectF pageRect = mRenderer.getPageRect(CurlRenderer.PAGE_LEFT);
			if (curlPos.x <= pageRect.left) {
				mPageCurl.reset();
				requestRender();
				return;
			}
			if (curlPos.x > pageRect.right) {
				curlPos.x = pageRect.right;
			}
			if (curlDir.y != 0) {
				float diffX = curlPos.x - pageRect.right;
				float rightY = curlPos.y + (diffX * curlDir.x / curlDir.y);
				if (curlDir.y < 0 && rightY < pageRect.top) {
					curlDir.x = pageRect.top - curlPos.y;
					curlDir.y = curlPos.x - pageRect.right;
				} else if (curlDir.y > 0 && rightY > pageRect.bottom) {
					curlDir.x = curlPos.y - pageRect.bottom;
					curlDir.y = pageRect.right - curlPos.x;
				}
			}
		}

		// Finally normalize direction vector and do rendering.
		double dist = Math.sqrt(curlDir.x * curlDir.x + curlDir.y * curlDir.y);
		if (dist != 0) {
			curlDir.x /= dist;
			curlDir.y /= dist;
			mPageCurl.curl(curlPos, curlDir, radius);
		} else {
			mPageCurl.reset();
		}

		requestRender();
	}

	/**
	 * Switches meshes and loads new bitmaps if available.
	 */
	private void startCurl(int page) {
		final ZLTextView view = FBReaderApp.Instance().getCurrentView();
		ZLTextView.PageIndex pageIndex = ZLTextView.PageIndex.current;
		if (page == CURL_LEFT) {
			pageIndex =  ZLTextView.PageIndex.previous;
		} else if (page == CURL_RIGHT) {
			pageIndex =  ZLTextView.PageIndex.next;
		}

		if (!view.canScroll(pageIndex)) {
			mCurlState = CURL_NONE;
			return;
		}

		switch (page) {

		// Once right side page is curled, first right page is assigned into
		// curled page. And if there are more bitmaps available new bitmap is
		// loaded into right side mesh.
		case CURL_RIGHT:
		{
			// Remove meshes from renderer.
			mRenderer.removeCurlMesh(mPageLeft);
			mRenderer.removeCurlMesh(mPageRight);
			mRenderer.removeCurlMesh(mPageCurl);

			// We are curling right page.
			CurlMesh curl = mPageRight;
			mPageRight = mPageNextCache;
			mPageNextCache = mPageCurl;
			mPageCurl = curl;

			// If there is new/next available, set it to right page.
			mPageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
			mPageRight.setFlipTexture(false);
			mPageRight.reset();
			mRenderer.addCurlMesh(mPageRight);

			// Add curled page to renderer.
			mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
			mPageCurl.setFlipTexture(false);
			mPageCurl.reset();
			mRenderer.addCurlMesh(mPageCurl);

			mCurlState = CURL_RIGHT;
			break;
		}

		// On left side curl, left page is assigned to curled page. And if
		// there are more bitmaps available before currentIndex, new bitmap
		// is loaded into left page.
		case CURL_LEFT:
		{
			// Remove meshes from renderer.
			mRenderer.removeCurlMesh(mPageLeft);
			mRenderer.removeCurlMesh(mPageRight);
			mRenderer.removeCurlMesh(mPageCurl);

			// We are curling left page.
			CurlMesh curl = mPageLeft;
			mPageLeft = mPageCurl;
			mPageCurl = curl;

			// If there is new/previous bitmap available load it to left page.
			
			mPageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
			mPageLeft.setFlipTexture(true);
			mPageLeft.reset();

			// If there is something to show on right page add it to renderer.
			mPageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
			mPageRight.reset();
			mRenderer.addCurlMesh(mPageRight);

			// How dragging previous page happens depends on view mode.
			Bitmap bitmap = getBitmap(ZLTextView.PageIndex.previous, false);
			mPageCurl.setBitmap(bitmap);
			mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
			mPageCurl.setFlipTexture(false);
			mPageCurl.reset();
			mRenderer.addCurlMesh(mPageCurl);

			mCurlState = CURL_LEFT;
			break;
		}

		}
	}

	/**
	 * Updates bitmaps for left and right meshes.
	 */
	private void updateBitmaps()
	{
		if (mPageBitmapWidth <= 0 || mPageBitmapHeight <= 0) {
			return;
		}

		// Remove meshes from renderer.
		mRenderer.removeCurlMesh(mPageCurl);
		mRenderer.removeCurlMesh(mPageRight);
		mRenderer.removeCurlMesh(mPageLeft);
		
		ZLTextView.PageIndex leftPageIndex = ZLTextView.PageIndex.current;
		ZLTextView.PageIndex rightPageIndex = ZLTextView.PageIndex.current;
		ZLTextView.PageIndex curlPageIndex = ZLTextView.PageIndex.current;
		
		if (mCurlState == CURL_LEFT) {
			curlPageIndex = ZLTextView.PageIndex.previous;
			rightPageIndex = ZLTextView.PageIndex.current;
		} else if (mCurlState == CURL_RIGHT) {
			leftPageIndex = ZLTextView.PageIndex.previous;
			curlPageIndex = ZLTextView.PageIndex.current;
			rightPageIndex = ZLTextView.PageIndex.next;
		} else if (mCurlState == CURL_NONE) {
			leftPageIndex = ZLTextView.PageIndex.previous;
			rightPageIndex = ZLTextView.PageIndex.current;
		}
		
		// 右侧页面放当前页文字
		final ZLTextView view = FBReaderApp.Instance().getCurrentView();
		
		if (mCurlState == CURL_LEFT || mCurlState == CURL_NONE) {
			if (view.canScroll(ZLTextView.PageIndex.next)) {
				mPageNextCache.setBitmap(getBitmap(ZLTextView.PageIndex.next, false));
				mPageNextCache.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
				mPageNextCache.setFlipTexture(false);
				mPageNextCache.reset();
			}
		}
		
		if (view.canScroll(rightPageIndex)) {
			Bitmap bitmapRight = getBitmap(rightPageIndex, false);
			
			mPageRight.setBitmap(bitmapRight);
			mPageRight.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));
			mPageRight.reset();
			mRenderer.addCurlMesh(mPageRight);
		}
		

		// 左侧页面放上一页（只有翻页的时候需要用到，这个页面并不参与显示）
		if (view.canScroll(leftPageIndex)) {
			Bitmap bitmapLeft = getBitmap(leftPageIndex, false);
			
			mPageLeft.setBitmap(bitmapLeft);
			mPageLeft.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));
			mPageLeft.reset();	
		}
		
		// 如果当前处于翻页动画状态，要更新翻卷页面
		if (mCurlState == CURL_RIGHT) {
			Bitmap bitmapCur = getBitmap(curlPageIndex, false);

			mPageCurl.setBitmap(bitmapCur);
			mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT));

			mPageCurl.reset();
			mRenderer.addCurlMesh(mPageCurl);
		} else if (mCurlState == CURL_LEFT) {
			Bitmap bitmapCur = getBitmap(curlPageIndex, false);

			mPageCurl.setBitmap(bitmapCur);
			mPageCurl.setRect(mRenderer.getPageRect(CurlRenderer.PAGE_LEFT));

			mPageCurl.reset();
			mRenderer.addCurlMesh(mPageCurl);
		}
	}

	/**
	 * Updates curl position.
	 */
	private void updateCurlPos(float x, float y) {

		// Default curl radius.
		double radius = mRenderer.getPageRect(CURL_RIGHT).width() / 3;
		// NOTE: Here we set pointerPos to mCurlPos. It might be a bit confusing
		// later to see e.g "mCurlPos.x - mDragStartPos.x" used. But it's
		// actually pointerPos we are doing calculations against. Why? Simply to
		// optimize code a bit with the cost of making it unreadable. Otherwise
		// we had to this in both of the next if-else branches.
		mCurlPos.set(x, y);

		// If curl happens on right page, or on left page on two page mode,
		// we'll calculate curl position from pointerPos.
		if (mCurlState == CURL_RIGHT) {

			mCurlDir.x = mCurlPos.x - mDragStartPos.x;
			mCurlDir.y = mCurlPos.y - mDragStartPos.y;
			float dist = (float) Math.sqrt(mCurlDir.x * mCurlDir.x + mCurlDir.y
					* mCurlDir.y);

			// Adjust curl radius so that if page is dragged far enough on
			// opposite side, radius gets closer to zero.
			float pageWidth = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT)
					.width();
			double curlLen = radius * Math.PI;
			if (dist > (pageWidth * 2) - curlLen) {
				curlLen = Math.max((pageWidth * 2) - dist, 0f);
				radius = curlLen / Math.PI;
			}

			// Actual curl position calculation.
			if (dist >= curlLen) {
				double translate = (dist - curlLen) / 2;
				mCurlPos.x -= mCurlDir.x * translate / dist;
				mCurlPos.y -= mCurlDir.y * translate / dist;
			} else {
				double angle = Math.PI * Math.sqrt(dist / curlLen);
				double translate = radius * Math.sin(angle);
				mCurlPos.x += mCurlDir.x * translate / dist;
				mCurlPos.y += mCurlDir.y * translate / dist;
			}

			setCurlPos(mCurlPos, mCurlDir, radius);
		}
		// Otherwise we'll let curl follow pointer position.
		else if (mCurlState == CURL_LEFT) {

			// Adjust radius regarding how close to page edge we are.
			float pageLeftX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).left;
			radius = Math.max(Math.min(mCurlPos.x - pageLeftX, radius), 0f);

			float pageRightX = mRenderer.getPageRect(CurlRenderer.PAGE_RIGHT).right;
			mCurlPos.x -= Math.min(pageRightX - mCurlPos.x, radius);
			mCurlDir.x = mCurlPos.x + mDragStartPos.x;
			mCurlDir.y = mCurlPos.y - mDragStartPos.y;

			setCurlPos(mCurlPos, mCurlDir, radius);
		}
	}
}
