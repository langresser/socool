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

import org.socool.zlibrary.util.ZLArrayUtils;
import org.socool.zlibrary.xml.ZLStringMap;

import org.socool.screader.bookmodel.*;

class XHTMLTagHyperlinkAction extends XHTMLTagAction {
	private byte[] myHyperlinkStack = new byte[10];
	private int myHyperlinkStackSize;

	private static boolean isReference(String text) {
		switch (text.charAt(0)) {
			default:
				return false;
			case 'f':
				return
					text.startsWith("fbreader-action://") ||
					text.startsWith("ftp://");
			case 'h':
				return
					text.startsWith("http://") ||
					text.startsWith("https://");
			case 'm':
				return
					text.startsWith("mailto:");
		}
	}

	protected void doAtStart(XHTMLReader reader, ZLStringMap xmlattributes) {
		final BookReader modelReader = reader.getModelReader();
		final String href = xmlattributes.getValue("href");
		if (myHyperlinkStackSize == myHyperlinkStack.length) {
			myHyperlinkStack = ZLArrayUtils.createCopy(myHyperlinkStack, myHyperlinkStackSize, 2 * myHyperlinkStackSize);
		}
		if (href != null && href.length() > 0) {
			String link = href;
			final byte hyperlinkType;
			if (isReference(link)) {
				hyperlinkType = BookModel.EXTERNAL_HYPERLINK;
			} else {
				hyperlinkType = BookModel.INTERNAL_HYPERLINK;
				final int index = href.indexOf('#');
				if (index == 0) {
					link =
						new StringBuilder(reader.myReferencePrefix)
							.append(href, 1, href.length())
							.toString();
				} else if (index > 0) {
					link =
						new StringBuilder(reader.getLocalFileAlias(href.substring(0, index)))
							.append(href, index, href.length())
							.toString();
				} else {
					link = reader.getLocalFileAlias(href);
				}
			}
			myHyperlinkStack[myHyperlinkStackSize++] = hyperlinkType;
			modelReader.addHyperlinkControl(hyperlinkType, link);
		} else {
			myHyperlinkStack[myHyperlinkStackSize++] = BookModel.REGULAR;
		}
		final String name = xmlattributes.getValue("name");
		if (name != null) {
			modelReader.addHyperlinkLabel(reader.myReferencePrefix + name);
		}
	}

	protected void doAtEnd(XHTMLReader reader) {
		byte kind = myHyperlinkStack[--myHyperlinkStackSize];
		if (kind != BookModel.REGULAR) {
			reader.getModelReader().addControl(kind, false);
		}
	}
}
