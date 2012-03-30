/*
 * Copyright (C) 2007-2012 Geometer Plus <wangjiatc@gmail.com>
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

package org.lancer.android.fbreader;

import android.content.Intent;


import org.lancer.android.fbreader.library.LibraryActivity;
import org.lancer.fbreader.bookmodel.BookModel;
import org.lancer.fbreader.fbreader.FBReaderApp;

class ShowLibraryAction extends FBAndroidAction {
	ShowLibraryAction(FBReader baseActivity, FBReaderApp fbreader) {
		super(baseActivity, fbreader);
	}

	@Override
	protected void run(Object ... params) {
		final BookModel model = Reader.Model;
		Intent intent = new Intent(BaseActivity.getApplicationContext(), LibraryActivity.class);
		if (model != null && model.Book != null) {
			intent.putExtra(LibraryActivity.SELECTED_BOOK_PATH_KEY, model.Book.File.getPath());
		}
		BaseActivity.startActivity(intent);
	}
}
