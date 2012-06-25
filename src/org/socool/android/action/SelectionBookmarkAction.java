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

package org.socool.android.action;

import org.socool.zlibrary.filesystem.ZLResource;
import org.socool.zlibrary.text.ZLTextView;

import org.socool.screader.library.Bookmark;
import org.socool.screader.screader.FBReaderApp;

import org.socool.android.SCReaderActivity;
import org.socool.android.util.UIUtil;

public class SelectionBookmarkAction extends FBAndroidAction {
	public SelectionBookmarkAction(SCReaderActivity baseApplication, FBReaderApp fbreader) {
		super(baseApplication, fbreader);
	}

	@Override
    protected void run(Object ... params) {
		final ZLTextView fbview = Reader.getCurrentView();
		final String text = fbview.getSelectedText();

		// 创建新书摘
		// TODO add comment
		Bookmark bookmark = new Bookmark(
			Reader.Model.Book,
			fbview.getModel().myId, text,
			fbview.getStartCursor(), fbview.getSelectionStartPosition(),
			fbview.getSelectionEndPosition(), null, fbview.getCurrentPercent());
		bookmark.save();
		fbview.addBookmarkHighlight(bookmark);
		fbview.clearSelection();

		UIUtil.showMessageText(
			BaseActivity,
			ZLResource.resource("selection").getResource("bookmarkCreated").getValue().replace("%s", text)
		);
	}
}
