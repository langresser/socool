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

package org.socool.screader.screader;

import java.util.*;

import android.view.KeyEvent;
import org.socool.screader.Paths;

import org.socool.zlibrary.filesystem.ZLFile;
import org.socool.zlibrary.options.ZLBooleanOption;
import org.socool.zlibrary.options.ZLStringListOption;
import org.socool.zlibrary.options.ZLStringOption;
import org.socool.zlibrary.xml.ZLStringMap;
import org.socool.zlibrary.xml.ZLXMLReaderAdapter;

public final class ZLKeyBindings {
	private static final String ACTION = "Action";
	private static final String LONG_PRESS_ACTION = "LongPressAction";

	private final String myName;
	private final ZLStringListOption myKeysOption;
	private final TreeMap<Integer,ZLStringOption> myActionMap = new TreeMap<Integer,ZLStringOption>();
	private final TreeMap<Integer,ZLStringOption> myLongPressActionMap = new TreeMap<Integer,ZLStringOption>();

	public ZLKeyBindings(String name) {
		myName = name;
		final Set<String> keys = new TreeSet<String>();
		new Reader(keys).readQuietly(ZLFile.createFileByPath("default/keymap.xml"));
		try {
			new Reader(keys).readQuietly(ZLFile.createFileByPath(Paths.systemShareDirectory() + "/keymap.xml"));
		} catch (Exception e) {
			// ignore
		}
		try {
			new Reader(keys).readQuietly(ZLFile.createFileByPath(Paths.BooksDirectoryOption().getValue() + "/keymap.xml"));
		} catch (Exception e) {
			// ignore
		}
 		myKeysOption = new ZLStringListOption(name, "KeyList", new ArrayList<String>(keys), ",");

		// this code is for migration from SCReaderActivity versions <= 1.1.2
		ZLStringOption oldBackKeyOption = new ZLStringOption(myName + ":" + ACTION, "<Back>", "");
		if (!"".equals(oldBackKeyOption.getValue())) {
			bindKey(KeyEvent.KEYCODE_BACK, false, oldBackKeyOption.getValue());
			oldBackKeyOption.setValue("");
		}
		oldBackKeyOption = new ZLStringOption(myName + ":" + LONG_PRESS_ACTION, "<Back>", "");
		if (!"".equals(oldBackKeyOption.getValue())) {
			bindKey(KeyEvent.KEYCODE_BACK, true, oldBackKeyOption.getValue());
			oldBackKeyOption.setValue("");
		}

		if (FBReaderApp.Instance().SoundTurnOption.getValue()) {
			bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, ActionCode.VOLUME_KEY_SCROLL_FORWARD);
			bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, ActionCode.VOLUME_KEY_SCROLL_BACK);
		} else {
			bindKey(KeyEvent.KEYCODE_VOLUME_UP, false, FBReaderApp.NoAction);
			bindKey(KeyEvent.KEYCODE_VOLUME_DOWN, false, FBReaderApp.NoAction);
		}
		// end of migration code
	}

	private ZLStringOption createOption(int key, boolean longPress, String defaultValue) {
		final String group = myName + ":" + (longPress ? LONG_PRESS_ACTION : ACTION);
		return new ZLStringOption(group, String.valueOf(key), defaultValue);
	}

	public ZLStringOption getOption(int key, boolean longPress) {
		final TreeMap<Integer,ZLStringOption> map = longPress ? myLongPressActionMap : myActionMap;
		ZLStringOption option = map.get(key);
		if (option == null) {
			option = createOption(key, longPress, FBReaderApp.NoAction);
			map.put(key, option);
		}
		return option;
	}

	public void bindKey(int key, boolean longPress, String actionId) {
		final String stringKey = String.valueOf(key);
		List<String> keys = myKeysOption.getValue();
		if (!keys.contains(stringKey)) {
			keys = new ArrayList<String>(keys);
			keys.add(stringKey);
			Collections.sort(keys);
			myKeysOption.setValue(keys);
		}
		getOption(key, longPress).setValue(actionId);
	}

	public String getBinding(int key, boolean longPress) {
		return getOption(key, longPress).getValue();
	}

	private class Reader extends ZLXMLReaderAdapter {
		private final Set<String> myKeySet;

		Reader(Set<String> keySet) {
			myKeySet = keySet;
		}

		@Override
		public boolean dontCacheAttributeValues() {
			return true;
		}

		@Override
		public boolean startElementHandler(String tag, ZLStringMap attributes) {
			if ("binding".equals(tag)) {
				final String stringKey = attributes.getValue("key");
				final String actionId = attributes.getValue("action");
				if (stringKey != null && actionId != null) {
					try {
						final int key = Integer.parseInt(stringKey);
						myKeySet.add(stringKey);
						myActionMap.put(key, createOption(key, false, actionId));
					} catch (NumberFormatException e) {
					}
				}
			}
			return false;
		}
	}
}
