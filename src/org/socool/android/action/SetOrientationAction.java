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

import android.app.Activity;
import android.content.pm.ActivityInfo;

import org.socool.zlibrary.util.ZLBoolean3;

import org.socool.android.SCReaderActivity;
import org.socool.screader.screader.FBReaderApp;

public class SetOrientationAction extends FBAndroidAction {
	public static void setOrientation(Activity activity, String optionValue) {
		int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		if (FBReaderApp.SCREEN_ORIENTATION_SYSTEM.equals(optionValue)) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
		} else if (FBReaderApp.SCREEN_ORIENTATION_PORTRAIT.equals(optionValue)) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
		} else if (FBReaderApp.SCREEN_ORIENTATION_LANDSCAPE.equals(optionValue)) {
			orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
		}
		activity.setRequestedOrientation(orientation);
	}

	private final String myOptionValue;

	public SetOrientationAction(SCReaderActivity baseActivity, FBReaderApp fbreader, String optionValue) {
		super(baseActivity, fbreader);
		myOptionValue = optionValue;
	}

	@Override
	public ZLBoolean3 isChecked() {
		return myOptionValue.equals(FBReaderApp.Instance().OrientationOption.getValue())
			? ZLBoolean3.B3_TRUE : ZLBoolean3.B3_FALSE;
	}

	@Override
	protected void run(Object ... params) {
		setOrientation(BaseActivity, myOptionValue);
		FBReaderApp.Instance().OrientationOption.setValue(myOptionValue);
		Reader.onRepaintFinished();
	}
	
	@Override
	public boolean isVisible() {
		return myOptionValue.equals(FBReaderApp.Instance().OrientationOption.getValue())
				? false : true;
	}
}
