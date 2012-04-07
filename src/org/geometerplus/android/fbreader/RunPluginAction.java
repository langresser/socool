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

package org.geometerplus.android.fbreader;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;

import org.geometerplus.fbreader.fbreader.FBReaderApp;

class RunPluginAction extends FBAndroidAction {
	private final Uri myUri;

	RunPluginAction(SCReader baseActivity, FBReaderApp fbreader, Uri uri) {
		super(baseActivity, fbreader);
		myUri = uri;
	}

	@Override
	protected void run(Object ... params) {
		try {
			BaseActivity.startActivity(new Intent("android.fbreader.action.plugin.RUN", myUri));
		} catch (ActivityNotFoundException e) {
		}
	}
}
