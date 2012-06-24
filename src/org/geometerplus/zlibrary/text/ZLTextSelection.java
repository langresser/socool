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

package org.geometerplus.zlibrary.text;

import org.geometerplus.fbreader.fbreader.FBReaderApp;

class ZLTextSelection {
	static class Point {
		int X;
		int Y;

		Point(int x, int y) {
			X = x;
			Y = y;
		}
	}

	private final ZLTextView myView;

	private ZLTextRegion.Soul myLeftMostRegionSoul;
	private ZLTextRegion.Soul myRightMostRegionSoul;

	private ZLTextSelectionCursor myCursorInMovement = ZLTextSelectionCursor.None;
	private final Point myCursorInMovementPoint = new Point(-1, -1);

	ZLTextSelection(ZLTextView view) {
		myView = view;
	}

	public boolean isEmpty() {
		return myLeftMostRegionSoul == null;
	}

	public boolean clear() {
		if (isEmpty()) {
			return false;
		}

		stop();
		myLeftMostRegionSoul = null;
		myRightMostRegionSoul = null;
		myCursorInMovement = ZLTextSelectionCursor.None;
		return true;
	}

	void setCursorInMovement(ZLTextSelectionCursor cursor, int x, int y) {
		myCursorInMovement = cursor;
		myCursorInMovementPoint.X = x;
		myCursorInMovementPoint.Y = y;
	}

	ZLTextSelectionCursor getCursorInMovement() {
		return myCursorInMovement;
	}

	Point getCursorInMovementPoint() {
		return myCursorInMovementPoint;
	}

	boolean start(int x, int y) {
		clear();

		final ZLTextRegion region = myView.findRegion(
			x, y, ZLTextView.MAX_SELECTION_DISTANCE, ZLTextRegion.AnyRegionFilter
		);
		if (region == null) {
			return false;
		}

		myRightMostRegionSoul = myLeftMostRegionSoul = region.getSoul();
		return true;
	}

	void stop() {
		myCursorInMovement = ZLTextSelectionCursor.None;
	}

	void  expandTo(int x, int y) {
		if (isEmpty()) {
			return;
		}

		final ZLTextElementAreaVector vector = myView.myCurrentPage.TextElementMap;

		ZLTextRegion region = myView.findRegion(x, y, ZLTextView.MAX_SELECTION_DISTANCE, ZLTextRegion.AnyRegionFilter);
		if (region == null) {
			region = myView.findRegion(x, y, ZLTextRegion.AnyRegionFilter);
		}
		if (region == null) {
			return;
		}

		final ZLTextRegion.Soul soul = region.getSoul();
		if (myCursorInMovement == ZLTextSelectionCursor.Right) {
			if (myLeftMostRegionSoul.compareTo(soul) <= 0) {
				myRightMostRegionSoul = soul;
			} else {
				myRightMostRegionSoul = myLeftMostRegionSoul;
				myLeftMostRegionSoul = soul;
				myCursorInMovement = ZLTextSelectionCursor.Left;
			}
		} else {
			if (myRightMostRegionSoul.compareTo(soul) >= 0) {
				myLeftMostRegionSoul = soul;
			} else {
				myLeftMostRegionSoul = myRightMostRegionSoul;
				myRightMostRegionSoul = soul;
				myCursorInMovement = ZLTextSelectionCursor.Right;
			}
		}

		if (myCursorInMovement == ZLTextSelectionCursor.Right) {
			if (hasAPartAfterPage(myView.myCurrentPage)) {
				myView.scrollPage(true, ZLTextView.ScrollingMode.SCROLL_LINES, 1);
				FBReaderApp.Instance().resetWidget();
				myView.preparePaintInfo();
			}
		} else {
			if (hasAPartBeforePage(myView.myCurrentPage)) {
				myView.scrollPage(false, ZLTextView.ScrollingMode.SCROLL_LINES, 1);
				FBReaderApp.Instance().resetWidget();
				myView.preparePaintInfo();
			}
		}
	}

	boolean isAreaSelected(ZLTextElementArea area) {
		return
			!isEmpty()
			&& myLeftMostRegionSoul.compareTo(area) <= 0
			&& myRightMostRegionSoul.compareTo(area) >= 0;
	}

	public ZLTextPosition getStartPosition() {
		if (isEmpty()) {
			return null;
		}
		return new ZLTextFixedPosition(
			myLeftMostRegionSoul.ParagraphIndex,
			myLeftMostRegionSoul.StartElementIndex,
			0
		);
	}

	public ZLTextPosition getEndPosition() {
		if (isEmpty()) {
			return null;
		}
		return new ZLTextFixedPosition(
			myRightMostRegionSoul.ParagraphIndex,
			myRightMostRegionSoul.EndElementIndex,
			0
		);
	}

	public ZLTextElementArea getStartArea(ZLTextPage page) {
		if (isEmpty()) {
			return null;
		}
		final ZLTextElementAreaVector vector = page.TextElementMap;
		final ZLTextRegion region = vector.getRegion(myLeftMostRegionSoul);
		if (region != null) {
			return region.getFirstArea();
		}
		final ZLTextElementArea firstArea = vector.getFirstArea();
		if (firstArea != null && myLeftMostRegionSoul.compareTo(firstArea) <= 0) {
			return firstArea;
		}
		return null;
	}

	public ZLTextElementArea getEndArea(ZLTextPage page) {
		if (isEmpty()) {
			return null;
		}
		final ZLTextElementAreaVector vector = page.TextElementMap;
		final ZLTextRegion region = vector.getRegion(myRightMostRegionSoul);
		if (region != null) {
			return region.getLastArea();
		}
		final ZLTextElementArea lastArea = vector.getLastArea();
		if (lastArea != null && myRightMostRegionSoul.compareTo(lastArea) >= 0) {
			return lastArea;
		}
		return null;
	}

	boolean hasAPartBeforePage(ZLTextPage page) {
		if (isEmpty()) {
			return false;
		}
		final ZLTextElementArea firstPageArea = page.TextElementMap.getFirstArea();
		if (firstPageArea == null) {
			return false;
		}
		final int cmp = myLeftMostRegionSoul.compareTo(firstPageArea);
		return cmp < 0 || (cmp == 0 && !firstPageArea.isFirstInElement());
	}

	boolean hasAPartAfterPage(ZLTextPage page) {
		if (isEmpty()) {
			return false;
		}
		final ZLTextElementArea lastPageArea = page.TextElementMap.getLastArea();
		if (lastPageArea == null) {
			return false;
		}
		final int cmp = myRightMostRegionSoul.compareTo(lastPageArea);
		return cmp > 0 || (cmp == 0 && !lastPageArea.isLastInElement());
	}
}
