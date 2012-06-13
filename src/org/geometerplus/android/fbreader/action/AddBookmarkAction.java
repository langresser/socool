package org.geometerplus.android.fbreader.action;

import org.geometerplus.zlibrary.text.ZLTextView;
import org.geometerplus.zlibrary.text.ZLTextWordCursor;

import org.geometerplus.fbreader.library.Bookmark;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

import org.geometerplus.android.fbreader.SCReaderActivity;
import org.geometerplus.android.fbreader.util.UIUtil;

public class AddBookmarkAction extends FBAndroidAction {
	public AddBookmarkAction(SCReaderActivity baseApplication, FBReaderApp fbreader) {
		super(baseApplication, fbreader);
	}

	@Override
    protected void run(Object ... params) {
		// 创建新书摘
		// TODO add comment
		final FBReaderApp fbreader = FBReaderApp.Instance();
		final ZLTextView view = fbreader.getCurrentView();
		final ZLTextWordCursor cursor = view.getStartCursor();

		if (cursor.isNull()) {
			return;
		}
		
		final int percent = view.getCurrentPercent();
		final Bookmark bookmark = new Bookmark(fbreader.Model.Book, view.getModel().myId, cursor, 20, percent);
		bookmark.save();

		UIUtil.showMessageText(
			BaseActivity,
			"添加书签成功"
		);
	}
}
