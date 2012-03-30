/*
 * Copyright (C) 2007-2012 Geometer Plus <wangjiatc@gmail.com>
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

package org.lancer.fbreader.library;


import org.lancer.fbreader.formats.*;
import org.lancer.zlibrary.core.filesystem.ZLFile;
import org.lancer.zlibrary.core.image.ZLImage;
import org.lancer.zlibrary.core.resources.ZLResource;

public abstract class LibraryUtil {
	public static ZLResource resource() {
		return ZLResource.resource("library");
	}

	public static ZLImage getCover(Book book) {
		return book != null ? book.getCover() : null;
	}

	public static String getAnnotation(ZLFile file) {
		final FormatPlugin plugin = PluginCollection.Instance().getPlugin(file);
		return plugin != null ? plugin.readAnnotation(file) : null;
	}
}
