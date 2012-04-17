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
import org.geometerplus.zlibrary.application.ZLibrary;

import android.graphics.Bitmap;

class BitmapManager {
	private final int SIZE = 2;
	private final Bitmap[] myBitmaps = new Bitmap[SIZE];
	private final ZLTextView.PageIndex[] myIndexes = new ZLTextView.PageIndex[SIZE];

	private int myWidth;
	private int myHeight;

	BitmapManager() {
	}
	
	void setSize(int w, int h) {
		if (myWidth != w || myHeight != h) {
			myWidth = w;
			myHeight = h;
			for (int i = 0; i < SIZE; ++i) {
				myBitmaps[i] = null;
				myIndexes[i] = null;
			}
			System.gc();
			System.gc();
			System.gc();
		}
	}

	Bitmap getBitmap(ZLTextView.PageIndex index) {
		for (int i = 0; i < SIZE; ++i) {
			if (index == myIndexes[i]) {
				return myBitmaps[i];
			}
		}
		final int iIndex = getInternalIndex(index);
		myIndexes[iIndex] = index;
		if (myBitmaps[iIndex] == null) {
			try {
				myBitmaps[iIndex] = Bitmap.createBitmap(myWidth, myHeight, Bitmap.Config.RGB_565);
			} catch (OutOfMemoryError e) {
				System.gc();
				System.gc();
				myBitmaps[iIndex] = Bitmap.createBitmap(myWidth, myHeight, Bitmap.Config.RGB_565);
			}
		}
		
		if (ZLibrary.Instance().isUseGLView()) {
			ZLibrary.Instance().getWidgetGL().drawOnBitmap(myBitmaps[iIndex], index);
		} else {
			ZLibrary.Instance().getWidget().drawOnBitmap(myBitmaps[iIndex], index);
		}

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

	void reset() {
		for (int i = 0; i < SIZE; ++i) {
			myIndexes[i] = null;
		}
	}

	void shift(boolean forward) {
		for (int i = 0; i < SIZE; ++i) {
			if (myIndexes[i] == null) {
				continue;
			}
			myIndexes[i] = forward ? myIndexes[i].getPrevious() : myIndexes[i].getNext();
		}
	}
}
