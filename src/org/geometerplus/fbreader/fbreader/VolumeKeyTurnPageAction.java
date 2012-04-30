package org.geometerplus.fbreader.fbreader;

import org.geometerplus.zlibrary.application.ZLibrary;
import org.geometerplus.zlibrary.text.view.ZLTextView;

class VolumeKeyTurnPageAction extends FBAction {
	private final boolean myForward;

	VolumeKeyTurnPageAction(FBReaderApp fbreader, boolean forward) {
		super(fbreader);
		myForward = forward;
	}

	@Override
	protected void run(Object ... params) {
		final ScrollingPreferences preferences = ScrollingPreferences.Instance();
		if (ZLibrary.Instance().isUseGLView()) {
			ZLibrary.Instance().getWidgetGL().startAnimatedScrolling(
				myForward ? ZLTextView.PageIndex.next : ZLTextView.PageIndex.previous,
				preferences.AnimationSpeedOption.getValue()
			);
		} else {
			ZLibrary.Instance().getWidget().startAnimatedScrolling(
					myForward ? ZLTextView.PageIndex.next : ZLTextView.PageIndex.previous,
							-1, -1, preferences.AnimationSpeedOption.getValue()
				);
		}
	}
}
