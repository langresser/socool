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

	private final ArrayList<FormatPlugin> myPlugins = new ArrayList<FormatPlugin>();
	
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
		myPlugins.add(new FB2Plugin());
		myPlugins.add(new MobipocketPlugin());
		myPlugins.add(new OEBPlugin());
		myPlugins.add(new TxtPlugin());

		m_txtPlugin = new TxtChapterPlugin();
		myPlugins.add(m_txtPlugin);
	}

	public TxtChapterPlugin getPlugin()
	{
		return m_txtPlugin;
	}

	public FormatPlugin getPlugin(FileType fileType) {
		if (fileType == null) {
			return null;
		}

		for (FormatPlugin p : myPlugins) {
			if (fileType.Id.equalsIgnoreCase(p.supportedFileType())) {
				return p;
			}
		}
		return null;
	}
}
