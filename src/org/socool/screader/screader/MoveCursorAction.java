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

package org.socool.screader.screader;

import org.socool.zlibrary.text.ZLTextRegion;
import org.socool.zlibrary.text.ZLTextView;
import org.socool.zlibrary.text.ZLTextWordRegionSoul;

public class MoveCursorAction extends FBAction {
	private final ZLTextView.Direction myDirection;

	public MoveCursorAction(FBReaderApp fbreader, ZLTextView.Direction direction) {
		super(fbreader);
		myDirection = direction;
	}

	@Override
	public void run(Object ... params) {
		final ZLTextView fbView = Reader.getCurrentView();
		ZLTextRegion region = fbView.getSelectedRegion();
		final ZLTextRegion.Filter filter =
			(region != null && region.getSoul() instanceof ZLTextWordRegionSoul)
				|| Reader.NavigateAllWordsOption.getValue()
					? ZLTextRegion.AnyRegionFilter : ZLTextRegion.ImageOrHyperlinkFilter;
		region = fbView.nextRegion(myDirection, filter);
		if (region != null) {
			fbView.selectRegion(region);
		} else {
			switch (myDirection) {
				case down:
					fbView.scrollPage(true, ZLTextView.ScrollingMode.SCROLL_LINES, 1);
					break;
				case up:
					fbView.scrollPage(false, ZLTextView.ScrollingMode.SCROLL_LINES, 1);
					break;
			}
		}

		FBReaderApp.Instance().resetWidget();
		FBReaderApp.Instance().repaintWidget();
	}
}
