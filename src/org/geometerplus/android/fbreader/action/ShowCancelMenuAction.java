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

package org.geometerplus.android.fbreader.action;

import java.util.List;

import android.content.Intent;

import org.geometerplus.android.fbreader.BookShelfActivity;
import org.geometerplus.android.fbreader.SCReaderActivity;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class ShowCancelMenuAction extends FBAndroidAction {
	public ShowCancelMenuAction(SCReaderActivity baseActivity, FBReaderApp fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	protected void run(Object ... params) {
		Intent intent = new Intent(BaseActivity, BookShelfActivity.class);
		BaseActivity.startActivity(intent);
	}
}
