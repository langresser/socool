package org.socool.screader.screader;

import org.socool.zlibrary.text.ZLTextView;

public class VolumeKeyTurnPageAction extends FBAction {
	private final boolean myForward;

	public VolumeKeyTurnPageAction(FBReaderApp fbreader, boolean forward) {
		super(fbreader);
		myForward = forward;
	}

	@Override
	protected void run(Object ... params) {
		final ScrollingPreferences preferences = ScrollingPreferences.Instance();
		if (FBReaderApp.Instance().isUseGLView()) {
			FBReaderApp.Instance().getWidgetGL().startAnimatedScrolling(
				myForward ? ZLTextView.PageIndex.next : ZLTextView.PageIndex.previous,
				preferences.AnimationSpeedOption.getValue()
			);
		} else {
			FBReaderApp.Instance().getWidget().startAnimatedScrolling(
					myForward ? ZLTextView.PageIndex.next : ZLTextView.PageIndex.previous,
							-1, -1, preferences.AnimationSpeedOption.getValue()
				);
		}
	}
}
