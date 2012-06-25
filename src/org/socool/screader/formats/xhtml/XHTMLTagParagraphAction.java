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

import org.socool.screader.bookmodel.BookParagraph;
import org.socool.zlibrary.xml.ZLStringMap;

class XHTMLTagParagraphAction extends XHTMLTagAction {
	protected void doAtStart(XHTMLReader reader, ZLStringMap xmlattributes) {
		reader.getModelReader().beginParagraph(BookParagraph.PARAGRAPH_KIND_TEXT_PARAGRAPH);
	}

	protected void doAtEnd(XHTMLReader reader) {
		reader.getModelReader().endParagraph();
	}
}
