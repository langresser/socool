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

import org.geometerplus.zlibrary.encodings.EncodingCollection;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLImage;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Book;

public abstract class FormatPlugin {
	private final String myFileType;
	public final static int ANY = 0;
	public final static int JAVA = 1;
	public final static int NATIVE = 2;
	public final static int EXTERNAL = 3;
	public final static int NONE = 4;

	protected FormatPlugin(String fileType) {
		myFileType = fileType;
	}

	public final String supportedFileType() {
		return myFileType;
	}

	public ZLFile realBookFile(ZLFile file) {
		return file;
	}

	public abstract void readMetaInfo(Book book);				// 读取附加信息
	public abstract void readModel(BookModel model);			// 读取文本信息
	public abstract void detectLanguageAndEncoding(Book book);	// 监测编码和语言
	public abstract ZLImage readCover(ZLFile file);						// 读取封面信息
	public abstract String readAnnotation(ZLFile file);					// 读取简介信息

	public abstract int type();

	public abstract EncodingCollection supportedEncodings();
}
