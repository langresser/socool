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
import org.geometerplus.zlibrary.image.ZLImage;
import org.geometerplus.zlibrary.image.ZLLoadableImage;
import org.geometerplus.zlibrary.util.MimeType;

class OEBCoverReader {
	private static class OEBCoverImage extends ZLLoadableImage {
		private final ZLFile myFile;

		OEBCoverImage(ZLFile file) {
			super(MimeType.IMAGE_AUTO);
			myFile = file;
		}

		@Override
		public ZLImage getRealImage() {
			return new OEBCoverBackgroundReader().readCover(myFile);
		}

		@Override
		public int sourceType() {
			return SourceType.DISK;
		}

		@Override
		public String getId() {
			return myFile.getPath();
		}
	}

	public ZLLoadableImage readCover(ZLFile file) {
		return new OEBCoverImage(file);
	}
}
