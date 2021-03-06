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

class FileTypeHtml extends FileType {
	FileTypeHtml() {
		super("HTML");
	}

	@Override
	public boolean acceptsFile(String filePath) {
		final String extension = FileUtil.getExtension(filePath);
		return extension.endsWith("html") || "htm".equals(extension);
	}

	/*
	@Override
	public String extension() {
		return "html";
	}
	*/

	@Override
	public List<MimeType> mimeTypes() {
		return MimeType.TYPES_HTML;
	}

	@Override
	public MimeType mimeType(String filePath) {
		return acceptsFile(filePath) ? MimeType.TEXT_HTML : MimeType.NULL;
	}
}
