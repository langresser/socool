package org.socool.android.action;

import org.socool.android.ChangeLightPopup;
import org.socool.android.SCReaderActivity;
import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.screader.FBReaderApp;

public class ShowLightAction extends FBAndroidAction {
	public ShowLightAction(SCReaderActivity baseActivity, FBReaderApp fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	public boolean isVisible() {
		final BookModel textModel = Reader.getCurrentView().getModel();
		return textModel != null && textModel.getParagraphNumber() != 0;
	}

	@Override
	protected void run(Object ... params) {
		FBReaderApp.Instance().getPopupById(ChangeLightPopup.ID).run();
	}
}
