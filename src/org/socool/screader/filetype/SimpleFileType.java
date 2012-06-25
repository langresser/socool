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

import org.socool.screader.FileUtil;
import org.socool.zlibrary.filesystem.ZLFile;
import org.socool.zlibrary.util.MimeType;

class SimpleFileType extends FileType {
	private final String myExtension;
	private final List<MimeType> myMimeTypes;

	SimpleFileType(String id, String extension, List<MimeType> mimeTypes) {
		super(id);
		myExtension = extension;
		myMimeTypes = mimeTypes;
	}

	@Override
	public boolean acceptsFile(String filePath) {
		return myExtension.equalsIgnoreCase(FileUtil.getExtension(filePath));
	}

	@Override
	public List<MimeType> mimeTypes() {
		return myMimeTypes;
	}

	@Override
	public MimeType mimeType(String filePath) {
		return acceptsFile(filePath) ? myMimeTypes.get(0) : MimeType.NULL;
	}
}
