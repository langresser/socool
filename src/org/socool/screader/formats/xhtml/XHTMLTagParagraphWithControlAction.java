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

import org.socool.zlibrary.xml.ZLStringMap;

import org.socool.screader.bookmodel.*;

class XHTMLTagParagraphWithControlAction extends XHTMLTagAction {
	final byte myControl;

	XHTMLTagParagraphWithControlAction(byte control) {
		myControl = control;
	}

	protected void doAtStart(XHTMLReader reader, ZLStringMap xmlattributes) {
		final BookReader modelReader = reader.getModelReader();
		switch (myControl) {
			case BookModel.TITLE:
			case BookModel.H1:
			case BookModel.H2:
				if (modelReader.m_bookModel.getParagraphNumber() > 1) {
					modelReader.insertEndParagraph(BookParagraph.PARAGRAPH_KIND_END_OF_SECTION_PARAGRAPH);
				}
				modelReader.myInsideTitle = true;
				break;
		}
		modelReader.pushKind(myControl);
		modelReader.beginParagraph(BookParagraph.PARAGRAPH_KIND_TEXT_PARAGRAPH);
	}

	protected void doAtEnd(XHTMLReader reader) {
		final BookReader modelReader = reader.getModelReader();
		modelReader.endParagraph();
		modelReader.popKind();
		switch (myControl) {
			case BookModel.TITLE:
			case BookModel.H1:
			case BookModel.H2:
				modelReader.myInsideTitle = false;
				break;
		}
	}
}
