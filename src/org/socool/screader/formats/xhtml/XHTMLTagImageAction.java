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

package org.socool.screader.formats.xhtml;

import org.socool.zlibrary.filesystem.ZLFile;
import org.socool.zlibrary.image.ZLFileImage;
import org.socool.zlibrary.util.MimeType;
import org.socool.zlibrary.xml.ZLStringMap;

import org.socool.screader.formats.html.MiscUtil;
import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.bookmodel.BookParagraph;
import org.socool.screader.bookmodel.BookReader;

class XHTMLTagImageAction extends XHTMLTagAction {
	private final String myNamespace;
	private final String myNameAttribute;

	XHTMLTagImageAction(String namespace, String nameAttribute) {
		myNamespace = namespace;
		myNameAttribute = nameAttribute;
	}

	protected void doAtStart(XHTMLReader reader, ZLStringMap xmlattributes) {
		String fileName = reader.getAttributeValue(xmlattributes, myNamespace, myNameAttribute);
		if (fileName != null) {
			fileName = MiscUtil.decodeHtmlReference(fileName);
			final ZLFile imageFile = ZLFile.createFileByPath(reader.myPathPrefix + fileName);
			if (imageFile != null) {
				final BookReader modelReader = reader.getModelReader();
				boolean flag = modelReader.myTextParagraphExists && !modelReader.myTextParagraphIsNonEmpty;
				if (flag) {
					modelReader.endParagraph();
				}
				final String imageName = imageFile.getLongName();
				modelReader.addImageReference(imageName, (short)0, false);
				modelReader.addImage(imageName, new ZLFileImage(MimeType.IMAGE_AUTO, imageFile));
				if (flag) {
					modelReader.beginParagraph(BookParagraph.PARAGRAPH_KIND_TEXT_PARAGRAPH);
				}
			}
		}
	}

	protected void doAtEnd(XHTMLReader reader) {
	}
}
