/*
 * Copyright (C) 2010-2012 Geometer Plus <contact@geometerplus.com>
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

package org.socool.android;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.socool.zlibrary.text.ZLTextWordCursor;

import org.socool.screader.screader.FBReaderApp;

public abstract class PopupPanel {
	public ZLTextWordCursor StartPosition;

	protected volatile PopupWindow myWindow;
	private volatile SCReaderActivity myActivity;
	private volatile RelativeLayout myRoot;

	PopupPanel() {
		FBReaderApp.Instance().myPopups.put(getId(), this);
	}
	
	public abstract String getId();
	public abstract void run();
	public abstract void update();

	public void show_() {
		if (myActivity != null) {
			createControlPanel(myActivity, myRoot);
		}
		if (myWindow != null) {
			myWindow.show();
		}
	}

	public void hide_() {
		if (myWindow != null) {
			myWindow.hide();
		}
	}

	public final void removeWindow(Activity activity) {
		if (myWindow != null && activity == myWindow.getActivity()) {
			ViewGroup root = (ViewGroup)myWindow.getParent();
			myWindow.hide();
			root.removeView(myWindow);
			myWindow = null;
		}
	}

	public static void removeAllWindows(Activity activity) {
		for (PopupPanel popup : FBReaderApp.Instance().popupPanels()) {
			((PopupPanel)popup).removeWindow(activity);
		}
	}

	public static void restoreVisibilities() {
		final PopupPanel popup = (PopupPanel)FBReaderApp.Instance().getActivePopup();
		if (popup != null) {
			popup.show_();
		}
	}

	public final void initPosition() {
		if (StartPosition == null) {
			StartPosition = new ZLTextWordCursor(FBReaderApp.Instance().getCurrentView().getStartCursor());
		}
	}

	public final void storePosition() {
		if (StartPosition != null &&
			!StartPosition.equals(FBReaderApp.Instance().getCurrentView().getStartCursor())) {
		}
	}

	public void setPanelInfo(SCReaderActivity activity, RelativeLayout root) {
		myActivity = activity;
		myRoot = root;
	}

	public abstract void createControlPanel(SCReaderActivity activity, RelativeLayout root);
}
