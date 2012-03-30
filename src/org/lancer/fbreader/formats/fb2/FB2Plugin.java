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

package org.lancer.fbreader.formats.fb2;


import org.lancer.fbreader.bookmodel.BookModel;
import org.lancer.fbreader.bookmodel.BookReadingException;
import org.lancer.fbreader.formats.*;
import org.lancer.fbreader.library.Book;
import org.lancer.zlibrary.core.encodings.AutoEncodingCollection;
import org.lancer.zlibrary.core.filesystem.ZLFile;
import org.lancer.zlibrary.core.image.ZLImage;

public class FB2Plugin extends JavaFormatPlugin {
	public FB2Plugin() {
		super("fb2");
	}

	@Override
	public void readMetaInfo(Book book) throws BookReadingException {
		new FB2MetaInfoReader(book).readMetaInfo();
	}

	@Override
	public void readModel(BookModel model) throws BookReadingException {
		new FB2Reader(model).readBook();
	}

	@Override
	public ZLImage readCover(ZLFile file) {
		return new FB2CoverReader().readCover(file);
	}

	@Override
	public String readAnnotation(ZLFile file) {
		return new FB2AnnotationReader().readAnnotation(file);
	}

	@Override
	public AutoEncodingCollection supportedEncodings() {
		return new AutoEncodingCollection();
	}

	@Override
	public void detectLanguageAndEncoding(Book book) {
		book.setEncoding("auto");
	}
}
