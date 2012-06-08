package org.geometerplus.android.fbreader.action;

import org.geometerplus.android.fbreader.ChangeFontSizePopup;
import org.geometerplus.android.fbreader.NavigationPopup;
import org.geometerplus.android.fbreader.SCReaderActivity;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class ShowFontAction extends FBAndroidAction {
	public ShowFontAction(SCReaderActivity baseActivity, FBReaderApp fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	public boolean isVisible() {
		final BookModel textModel = Reader.getCurrentView().getModel();
		return textModel != null && textModel.getParagraphNumber() != 0;
	}

	@Override
	protected void run(Object ... params) {
		FBReaderApp.Instance().getPopupById(ChangeFontSizePopup.ID).run();
	}
}