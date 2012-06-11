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
import org.geometerplus.fbreader.fbreader.WallpapersUtil;

class WallpaperPreference extends ListPreference {
	private final ZLStringOption myOption;
	private final ZLResource myResource;

	WallpaperPreference(Context context, ColorProfile profile, ZLResource resource, String resourceKey) {
		super(context);

		myOption = profile.WallpaperOption;
		myResource = resource.getResource(resourceKey);
		setTitle(myResource.getValue());
		final List<ZLFile> predefined = WallpapersUtil.predefinedWallpaperFiles();
		final List<ZLFile> external = WallpapersUtil.externalWallpaperFiles();
		
		final int size = 1 + predefined.size() + external.size();
		final String[] values = new String[size];
		final String[] texts = new String[size];

		// 第一个配置是单色
		final ZLResource optionResource = resource.getResource(resourceKey);
		values[0] = "";
		texts[0] = optionResource.getResource("solidColor").getValue();
		int index = 1;
		for (ZLFile f : predefined) {
			values[index] = f.getPath();
			
			final String name = f.getShortName();
			if (name.indexOf(".") == -1) {
				texts[index] = f.getPath();
				++index;
				continue;
			}

			final String key = name.substring(0, name.indexOf("."));
			texts[index] = optionResource.getResource(key).getValue();
			++index;
		}
		for (ZLFile f : external) {
			values[index] = f.getPath();
			texts[index] = f.getShortName();
			++index;
		}
		setLists(values, texts);

		setInitialValue(myOption.getValue());
	}

	@Override
	protected void onDialogClosed(boolean result) {
		super.onDialogClosed(result);
		if (result) {
			setSummary(getEntry());
		}
		myOption.setValue(getValue());
	}
	
	protected final void setList(String[] values) {
		String[] texts = new String[values.length];
		for (int i = 0; i < values.length; ++i) {
			final ZLResource resource = myResource.getResource(values[i]);
			texts[i] = (resource != null && resource.hasValue()) ? resource.getValue() : values[i];
		}
		setLists(values, texts);
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
		setValueIndex(index);
		setSummary(getEntry());
		return found;
	}
}
