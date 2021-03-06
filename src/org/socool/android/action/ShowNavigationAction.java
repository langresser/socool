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

import org.socool.android.NavigationPopup;
import org.socool.android.SCReaderActivity;
import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.screader.FBReaderApp;

public class ShowNavigationAction extends FBAndroidAction {
	public ShowNavigationAction(SCReaderActivity baseActivity, FBReaderApp fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	public boolean isVisible() {
		final BookModel textModel = Reader.getCurrentView().getModel();
		return textModel != null && textModel.getParagraphNumber() != 0;
	}

	@Override
	protected void run(Object ... params) {
		FBReaderApp.Instance().getPopupById(NavigationPopup.ID).run();
	}
}
