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

import java.util.*;

import android.content.Context;
import android.preference.ListPreference;

import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.filesystem.ZLResource;
import org.geometerplus.zlibrary.options.ZLStringOption;

import org.geometerplus.fbreader.fbreader.ColorProfile;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.TextTheme;

class WallpaperPreference extends ListPreference {
	WallpaperPreference(Context context, ColorProfile profile, ZLResource resource, String resourceKey) {
		super(context);

		ZLResource myResource = resource.getResource(resourceKey);
		setTitle(myResource.getValue());
		
		FBReaderApp.Instance().initTheme();
		ArrayList<TextTheme> themes = FBReaderApp.Instance().m_themes;
		
		final int size = 1 + themes.size();
		final String[] values = new String[size];
		final String[] texts = new String[size];

		values[0] = "";
		texts[0] = resource.getResource(resourceKey).getResource("solidColor").getValue();
		
		int index = 1;
		for (TextTheme each : themes) {
			values[index] = each.m_path;
			texts[index] = each.m_title;
			index += 1;
		}

		setLists(values, texts);
		setInitialValue(FBReaderApp.Instance().getCurrentTheme());
	}

	@Override
	protected void onDialogClosed(boolean result) {
		super.onDialogClosed(result);
		if (result) {
			setSummary(getEntry());
		}
		FBReaderApp.Instance().changeTheme(getValue());
	}
	
	protected final void setLists(String[] values, String[] texts) {
		assert(values.length == texts.length);
		setEntries(texts);
		setEntryValues(values);
	}

	protected final boolean setInitialValue(String value) {
		int index = 0;
		boolean found = false;;
		final CharSequence[] entryValues = getEntryValues();
		if (value != null) {
			for (int i = 0; i < entryValues.length; ++i) {
				if (value.equals(entryValues[i])) {
					index = i;
					found = true;
					break;
				}
			}
		}
		
		if (found == false) {
			index = 0;
		}

		setValueIndex(index);
		final CharSequence entry = getEntry();
		if (entry != null) {
			setSummary(entry);
		}
		
		return found;
	}
}
