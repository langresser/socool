/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.android.fbreader.preferences;

import org.geometerplus.zlibrary.filesystem.ZLResource;
import org.geometerplus.zlibrary.options.ZLStringOption;

import android.content.Context;
import android.preference.EditTextPreference;

class ZLStringOptionPreference extends EditTextPreference {
	private final ZLStringOption myOption;
	private String myValue;

	ZLStringOptionPreference(Context context, ZLStringOption option, ZLResource rootResource, String resourceKey) {
		super(context);
		
		ZLResource resource = rootResource.getResource(resourceKey);
		setTitle(resource.getValue());

		myOption = option;
		
		setValue(myOption.getValue());
	}

	protected void setValue(String value) {
		setSummary(value);
		setText(value);
		myValue = value;
		myOption.setValue(value);
	}
	
	protected final String getValue() {
		return myValue;
	}
	
	@Override
	protected void onDialogClosed(boolean result) {
		if (result) {
			setValue(getEditText().getText().toString());
		}
		super.onDialogClosed(result);
	}
}
