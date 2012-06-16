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

package org.geometerplus.android.fbreader;

import java.text.DateFormat;
import java.util.*;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.widget.*;

import org.geometerplus.zlibrary.filesystem.ZLResource;
import org.geometerplus.zlibrary.image.ZLImage;
import org.geometerplus.zlibrary.image.ZLImageData;
import org.geometerplus.zlibrary.image.ZLImageManager;
import org.geometerplus.zlibrary.image.ZLLoadableImage;
import org.geometerplus.zlibrary.util.ZLLanguageUtil;

import org.socool.socoolreader.reader.R;

import org.geometerplus.fbreader.library.*;
import org.geometerplus.fbreader.network.HtmlUtil;

public class BookInfoActivity extends Activity {
	public static final String CURRENT_BOOK_PATH_KEY = "CurrentBookPath";

	private final ZLResource myResource = ZLResource.resource("bookInfo");
	private String m_currentBookPath;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Thread.setDefaultUncaughtExceptionHandler(
			new org.geometerplus.zlibrary.error.UncaughtExceptionHandler(this)
		);

		m_currentBookPath = getIntent().getStringExtra(CURRENT_BOOK_PATH_KEY);
		if (m_currentBookPath == null) {
			m_currentBookPath = "book/wxkb";
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.book_info);
	}

	@Override
	protected void onStart() {
		super.onStart();

		final Book book = Book.getByPath(m_currentBookPath);

		if (book != null) {
			setupCover(book);
			setupBookInfo(book);
			setupAnnotation(book);
			setupFileInfo(book);
		}
		
		final Button btnOpen = (Button)findViewById(R.id.book_info_button_open);
		final Button btnBook = (Button)findViewById(R.id.book_info_button_book);
		final Button btnApp = (Button)findViewById(R.id.book_info_button_app);
		
		View.OnClickListener listener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Button btn = (Button)v;
				if (btn == btnOpen) {
					startActivity(
							new Intent(getApplicationContext(), SCReaderActivity.class)
								.setAction(Intent.ACTION_VIEW)
								.putExtra(SCReaderActivity.BOOK_PATH_KEY, m_currentBookPath)
								.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
						);
				} else if (btn == btnBook) {
					Intent intent = new Intent(getApplicationContext(), LibraryActivity.class);
					startActivity(intent);
				} else if (btn == btnApp) {
					
				}
			}
		};
		
		btnOpen.setOnClickListener(listener);
		btnBook.setOnClickListener(listener);
		btnApp.setOnClickListener(listener);

		final View root = findViewById(R.id.book_info_root);
		root.invalidate();
		root.requestLayout();
	}

	private void setupInfoPair(int id, String key, CharSequence value) {
		final LinearLayout layout = (LinearLayout)findViewById(id);
		if (value == null || value.length() == 0) {
			layout.setVisibility(View.GONE);
			return;
		}
		layout.setVisibility(View.VISIBLE);
		((TextView)layout.findViewById(R.id.book_info_key)).setText(myResource.getResource(key).getValue(0));
		((TextView)layout.findViewById(R.id.book_info_value)).setText(value);
	}

	private void setupCover(Book book) {
		if (book == null) {
			return;
		}

		final ImageView coverView = (ImageView)findViewById(R.id.book_cover);

		final DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		final int maxHeight = metrics.heightPixels * 2 / 3;
		final int maxWidth = maxHeight * 2 / 3;

		coverView.setVisibility(View.GONE);
		coverView.setImageDrawable(null);

		final ZLImage image = book.getCover();

		if (image == null) {
			return;
		}

		if (image instanceof ZLLoadableImage) {
			final ZLLoadableImage loadableImage = (ZLLoadableImage)image;
			if (!loadableImage.isSynchronized()) {
				loadableImage.synchronize();
			}
		}
		final ZLImageData data = ZLImageManager.Instance().getImageData(image);
		if (data == null) {
			return;
		}

		final Bitmap coverBitmap = data.getBitmap(2 * maxWidth, 2 * maxHeight);
		if (coverBitmap == null) {
			return;
		}

		coverView.setVisibility(View.VISIBLE);
		coverView.getLayoutParams().width = maxWidth;
		coverView.getLayoutParams().height = maxHeight;
		coverView.setImageBitmap(coverBitmap);
	}

	private void setupBookInfo(Book book) {
		((TextView)findViewById(R.id.book_info_title)).setText(myResource.getResource("bookInfo").getValue());

		setupInfoPair(R.id.book_title, "title", book.myTitle);
		setupInfoPair(R.id.book_authors, "authors", book.authors());
	}

	private void setupAnnotation(Book book) {
		final TextView titleView = (TextView)findViewById(R.id.book_info_annotation_title);
		final TextView bodyView = (TextView)findViewById(R.id.book_info_annotation_body);
		final String annotation = book.getPlugin().readAnnotation(book);	
		if (annotation == null) {
			titleView.setVisibility(View.GONE);
			bodyView.setVisibility(View.GONE);
		} else {
			titleView.setText(myResource.getResource("annotation").getValue());
			bodyView.setText(HtmlUtil.getHtmlText(annotation));
			bodyView.setMovementMethod(new LinkMovementMethod());
			bodyView.setTextColor(ColorStateList.valueOf(bodyView.getTextColors().getDefaultColor()));
		}
	}

	private void setupFileInfo(Book book) {
		((TextView)findViewById(R.id.file_info_title)).setText(myResource.getResource("fileInfo").getValue());

		setupInfoPair(R.id.file_name, "name", book.m_filePath);
		setupInfoPair(R.id.file_type, "type", null);
		setupInfoPair(R.id.file_size, "size", null);
		setupInfoPair(R.id.file_time, "time", null);
	}

	private String formatSize(long size) {
		if (size <= 0) {
			return null;
		}
		final int kilo = 1024;
		if (size < kilo) { // less than 1 kilobyte
			return myResource.getResource("sizeInBytes").getValue((int)size).replaceAll("%s", String.valueOf(size));
		}
		final String value;
		if (size < kilo * kilo) { // less than 1 megabyte
			value = String.format("%.2f", ((float)size) / kilo);
		} else {
			value = String.valueOf(size / kilo);
		}
		return myResource.getResource("sizeInKiloBytes").getValue().replaceAll("%s", value);
	}

	private String formatDate(long date) {
		if (date == 0) {
			return null;
		}
		return DateFormat.getDateTimeInstance().format(new Date(date));
	}
}
