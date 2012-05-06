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

package org.geometerplus.android.fbreader.action;

import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;

import org.geometerplus.zlibrary.text.ZLTextHyperlink;
import org.geometerplus.zlibrary.text.ZLTextHyperlinkRegionSoul;
import org.geometerplus.zlibrary.text.ZLTextImageRegionSoul;
import org.geometerplus.zlibrary.text.ZLTextRegion;
import org.geometerplus.zlibrary.text.ZLTextWordRegionSoul;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.network.NetworkLibrary;

import org.geometerplus.android.fbreader.network.BookDownloader;
import org.geometerplus.android.fbreader.network.BookDownloaderService;
import org.geometerplus.android.fbreader.DictionaryUtil;
import org.geometerplus.android.fbreader.ImageViewActivity;
import org.geometerplus.android.fbreader.SCReaderActivity;

public class ProcessHyperlinkAction extends FBAndroidAction {
	public ProcessHyperlinkAction(SCReaderActivity baseActivity, FBReaderApp fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	public boolean isEnabled() {
		return Reader.getCurrentView().getSelectedRegion() != null;
	}

	@Override
	protected void run(Object ... params) {
		final ZLTextRegion region = Reader.getCurrentView().getSelectedRegion();
		if (region == null) {
			return;
		}

		final ZLTextRegion.Soul soul = region.getSoul();
		if (soul instanceof ZLTextHyperlinkRegionSoul) {
			Reader.getCurrentView().hideSelectedRegionBorder();
			FBReaderApp.Instance().repaintWidget();
			final ZLTextHyperlink hyperlink = ((ZLTextHyperlinkRegionSoul)soul).Hyperlink;
			switch (hyperlink.Type) {
				case BookModel.EXTERNAL:
					openInBrowser(hyperlink.Id);
					break;
				case BookModel.INTERNAL:
					Reader.Model.Book.markHyperlinkAsVisited(hyperlink.Id);
					Reader.tryOpenFootnote(hyperlink.Id);
					break;
			}
		} else if (soul instanceof ZLTextImageRegionSoul) {
			Reader.getCurrentView().hideSelectedRegionBorder();
			FBReaderApp.Instance().repaintWidget();
			final String uriString = ((ZLTextImageRegionSoul)soul).ImageElement.URI;
			if (uriString != null) {
				try {
					final Intent intent = new Intent();
					intent.setClass(BaseActivity, ImageViewActivity.class);
					intent.setData(Uri.parse(uriString));
					intent.putExtra(
						ImageViewActivity.BACKGROUND_COLOR_KEY,
						Reader.ImageViewBackgroundOption.getValue().getIntValue()
					);
					BaseActivity.startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else if (soul instanceof ZLTextWordRegionSoul) {
			DictionaryUtil.openWordInDictionary(
				BaseActivity, ((ZLTextWordRegionSoul)soul).Word, region
			);
		}
	}

	private void openInBrowser(final String urlString) {
		final Intent intent = new Intent(Intent.ACTION_VIEW);
		final boolean externalUrl;
		if (BookDownloader.acceptsUri(Uri.parse(urlString))) {
			intent.setClass(BaseActivity, BookDownloader.class);
			intent.putExtra(BookDownloaderService.SHOW_NOTIFICATIONS_KEY, BookDownloaderService.Notifications.ALL);
			externalUrl = false;
		} else {
			externalUrl = true;
		}
		final NetworkLibrary nLibrary = NetworkLibrary.Instance();
		new Thread(new Runnable() {
			public void run() {
				nLibrary.initialize();
				intent.setData(Uri.parse(nLibrary.rewriteUrl(urlString, externalUrl)));
				BaseActivity.runOnUiThread(new Runnable() {
					public void run() {
						try {
							BaseActivity.startActivity(intent);
						} catch (ActivityNotFoundException e) {
							e.printStackTrace();
						}
					}
				});
			}
		}).start();
	}
}
