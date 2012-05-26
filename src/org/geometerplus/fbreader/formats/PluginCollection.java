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

package org.geometerplus.fbreader.formats;

import java.util.*;

import org.geometerplus.zlibrary.filesystem.ZLFile;

import org.geometerplus.fbreader.formats.fb2.FB2Plugin;
import org.geometerplus.fbreader.formats.oeb.OEBPlugin;
import org.geometerplus.fbreader.formats.pdb.MobipocketPlugin;
import org.geometerplus.fbreader.formats.txt.TxtChapterPlugin;
import org.geometerplus.fbreader.formats.txt.TxtPlugin;
import org.geometerplus.fbreader.filetype.*;

public class PluginCollection {
	private static PluginCollection ourInstance;

	private final Map<Integer, List<FormatPlugin>> myPlugins = new HashMap<Integer, List<FormatPlugin>>();
	
	private TxtChapterPlugin m_txtPlugin;

	public static PluginCollection Instance() {
		if (ourInstance == null) {
			ourInstance = new PluginCollection();
		}
		return ourInstance;
	}

	public static void deleteInstance() {
		if (ourInstance != null) {
			ourInstance = null;
		}
	}

	private PluginCollection() {
		addPlugin(new FB2Plugin());
		addPlugin(new MobipocketPlugin());
		addPlugin(new OEBPlugin());
		addPlugin(new TxtPlugin());

		m_txtPlugin = new TxtChapterPlugin();
		addPlugin(m_txtPlugin);
	}

	private void addPlugin(FormatPlugin plugin) {
		final int type = plugin.type();
		List<FormatPlugin> list = myPlugins.get(type);
		if (list == null) {
			list = new ArrayList<FormatPlugin>();
			myPlugins.put(type, list);
		}
		list.add(plugin);
	}
	
	public TxtChapterPlugin getPlugin()
	{
		return m_txtPlugin;
	}

	public FormatPlugin getPlugin(FileType fileType, int formatType) {
		if (fileType == null) {
			return null;
		}

		if (formatType == FormatPlugin.ANY) {
			FormatPlugin p = getPlugin(fileType, FormatPlugin.NATIVE);
			if (p == null) {
				p = getPlugin(fileType, FormatPlugin.JAVA);
			}
			return p;
		} else {
			final List<FormatPlugin> list = myPlugins.get(formatType);
			if (list == null) {
				return null;
			}
			for (FormatPlugin p : list) {
				if (fileType.Id.equalsIgnoreCase(p.supportedFileType())) {
					return p;
				}
			}
			return null;
		}
	}
}
