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

package org.geometerplus.fbreader.formats.oeb;

import org.geometerplus.zlibrary.encodings.AutoEncodingCollection;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLImage;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.formats.*;

public class OEBPlugin extends FormatPlugin {
	public OEBPlugin() {
		super("ePub");
	}

	private ZLFile getOpfFile(ZLFile oebFile) {
		if ("opf".equals(oebFile.getExtension())) {
			return oebFile;
		}

		final ZLFile containerInfoFile = ZLFile.createFile(oebFile, "META-INF/container.xml");
		if (containerInfoFile.exists()) {
			final ContainerFileReader reader = new ContainerFileReader();
			reader.readQuietly(containerInfoFile);
			final String opfPath = reader.getRootPath();
			if (opfPath != null) {
				return ZLFile.createFile(oebFile, opfPath);
			}
		}

		for (ZLFile child : oebFile.children()) {
			if (child.getExtension().equals("opf")) {
				return child;
			}
		}
		
		return null;
	}

	@Override
	public void readMetaInfo(Book book){
		final ZLFile file = ZLFile.createFileByPath(book.m_filePath);
		new OEBMetaInfoReader(book).readMetaInfo(getOpfFile(file));
	}
	
	@Override
	public void readModel(BookModel model) {
		final ZLFile file = ZLFile.createFileByPath(model.Book.m_filePath);
		file.setCached(true);
		new OEBBookReader(model).readBook(getOpfFile(file));
	}

	@Override
	public ZLImage readCover(Book book) {
		final ZLFile file = ZLFile.createFileByPath(book.m_filePath);
		return new OEBCoverReader().readCover(getOpfFile(file)); 
	}

	@Override
	public String readAnnotation(Book book) {
		final ZLFile file = ZLFile.createFileByPath(book.m_filePath);
		return new OEBAnnotationReader().readAnnotation(getOpfFile(file)); 
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
