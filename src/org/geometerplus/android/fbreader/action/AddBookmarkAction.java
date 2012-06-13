package org.geometerplus.android.fbreader.action;

import org.geometerplus.zlibrary.filesystem.ZLResource;
import org.geometerplus.zlibrary.text.ZLTextView;

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
		final ZLTextView fbview = Reader.getCurrentView();
		final String text = fbview.getSelectedText();

		// 创建新书摘
		// TODO add comment
		Bookmark bookmark = new Bookmark(
			Reader.Model.Book,
			fbview.getModel().myId, text,
			fbview.getStartCursor(), fbview.getSelectionStartPosition(),
			fbview.getSelectionEndPosition(), null);
		bookmark.save();
		fbview.addBookmarkHighlight(bookmark);
		fbview.clearSelection();

		UIUtil.showMessageText(
			BaseActivity,
			ZLResource.resource("selection").getResource("bookmarkCreated").getValue().replace("%s", text)
		);
	}
}
