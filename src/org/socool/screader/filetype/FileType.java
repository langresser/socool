/*
 * Copyright (C) 2012 Geometer Plus <contact@geometerplus.com>
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

package org.socool.screader.filetype;

import java.util.List;

import org.socool.zlibrary.filesystem.ZLFile;
import org.socool.zlibrary.util.MimeType;

public abstract class FileType {
	public final String Id;

	protected FileType(String id) {
		Id = id;
	}

	public abstract boolean acceptsFile(String filePath);

	//public abstract String extension(MimeType mimeType);
	public abstract List<MimeType> mimeTypes();
	public abstract MimeType mimeType(String filePath);
	public MimeType simplifiedMimeType(String filePath) {
		return mimeType(filePath);
	}
}
