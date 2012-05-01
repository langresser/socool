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

package org.geometerplus.zlibrary.image;

import java.io.InputStream;

import org.geometerplus.zlibrary.util.MimeType;

public abstract class ZLLoadableImage extends ZLSingleImage {
	private volatile boolean myIsSynchronized;
	private ZLSingleImage myImage;

	public ZLLoadableImage(MimeType mimeType) {
		super(mimeType);
	}

	public final boolean isSynchronized() {
		return myIsSynchronized;
	}

	protected final void setSynchronized() {
		myIsSynchronized = true;
	}

	public void startSynchronization(Runnable postSynchronizationAction) {
		ZLImageManager.Instance().startImageLoading(this, postSynchronizationAction);
	}

	public String getURI() {
		final ZLImage image = getRealImage();
		return image != null ? image.getURI() : "image proxy";
	}

	public InputStream inputStream() {
		return myImage != null ? myImage.inputStream() : null;
	}

	public synchronized void synchronize() {
		myImage = getRealImage();
		setSynchronized();
	}

	public void synchronizeFast() {
		setSynchronized();
	}

	public static interface SourceType {
		int DISK = 0;
		int NETWORK = 1;
	};
	public abstract int sourceType();
	public abstract String getId();
	public abstract ZLSingleImage getRealImage();
}
