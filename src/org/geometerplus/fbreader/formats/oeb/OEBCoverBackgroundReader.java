/*
 * Copyright (C) 2010-2012 Geometer Plus <contact@geometerplus.com>
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

import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLFileImage;
import org.geometerplus.zlibrary.util.MimeType;
import org.geometerplus.zlibrary.xml.XMLNamespaces;
import org.geometerplus.zlibrary.xml.ZLStringMap;
import org.geometerplus.zlibrary.xml.ZLXMLReaderAdapter;

import org.geometerplus.fbreader.formats.util.MiscUtil;

class OEBCoverBackgroundReader extends ZLXMLReaderAdapter implements XMLNamespaces {
	private ZLFileImage myImage;
	private String myPathPrefix;
	private boolean myReadGuide;

	public ZLFileImage readCover(ZLFile file) {
		myPathPrefix = MiscUtil.htmlDirectoryPrefix(file);
		myReadGuide = false;
		myImage = null;
		readQuietly(file);
		return myImage;
	}

	private static final String GUIDE = "guide";
	private static final String REFERENCE = "reference";
	private static final String COVER = "cover";
	private static final String COVER_IMAGE = "other.ms-coverimage-standard";

	@Override
	public boolean startElementHandler(String tag, ZLStringMap attributes) {
		tag = tag.toLowerCase().intern();
		if (GUIDE == tag) {
			myReadGuide = true;
		} else if (myReadGuide && REFERENCE == tag) {
			final String type = attributes.getValue("type");
			if (COVER == type) {
				final String href = attributes.getValue("href");
				if (href != null) {
					final ZLFile coverFile =
						ZLFile.createFileByPath(myPathPrefix + MiscUtil.decodeHtmlReference(href));
					myImage = XHTMLImageFinder.getCoverImage(coverFile);
					return true;
				}
			} else if (COVER_IMAGE == type) {
				final String href = attributes.getValue("href");
				if (href != null) {
					myImage = new ZLFileImage(
						MimeType.IMAGE_AUTO,
						ZLFile.createFileByPath(myPathPrefix + MiscUtil.decodeHtmlReference(href))
					);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean endElementHandler(String tag) {
		tag = tag.toLowerCase();
		if (GUIDE == tag) {
			myReadGuide = false;
			return true;
		}
		return false;
	}
}
