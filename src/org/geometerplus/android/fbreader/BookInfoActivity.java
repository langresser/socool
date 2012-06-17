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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.*;

import org.socool.socoolreader.reader.R;

import org.geometerplus.fbreader.library.*;

public class BookInfoActivity extends Activity {
	public static final String CURRENT_BOOK_PATH_KEY = "CurrentBookPath";
	private String m_currentBookPath;

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Thread.setDefaultUncaughtExceptionHandler(
			new org.geometerplus.zlibrary.error.UncaughtExceptionHandler(this)
		);

		m_currentBookPath = getIntent().getStringExtra(CURRENT_BOOK_PATH_KEY);
		if (m_currentBookPath == null) {
			m_currentBookPath = "book/mcnxs";
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.book_info);
	}

	@Override
	protected void onStart() {
		super.onStart();

		final Book book = Book.getByPath(m_currentBookPath);
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
	}
}
