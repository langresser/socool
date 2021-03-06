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

package org.socool.android;

import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;

import org.socool.zlibrary.filesystem.ZLFile;
import org.socool.zlibrary.filesystem.ZLResource;
import org.socool.zlibrary.options.ZLStringOption;

import org.socool.socoolreader.mcnxs.R;

import org.socool.screader.FBTree;
import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.library.Book;
import org.socool.screader.library.BooksDatabase;
import org.socool.screader.library.FileTree;
import org.socool.screader.library.LibraryTree;

import org.socool.android.util.UIUtil;
import org.socool.android.tree.TreeActivity;

public class LibraryActivity extends TreeActivity implements MenuItem.OnMenuItemClickListener, View.OnCreateContextMenuListener, FBReaderApp.ChangeListener {
	static volatile boolean ourToBeKilled = false;

	public static final String SELECTED_BOOK_PATH_KEY = "SelectedBookPath";

	private Book mySelectedBook;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		FBReaderApp.Instance().addChangeListener(this);
		FBReaderApp.Instance().startBuild();

		final String selectedBookPath = getIntent().getStringExtra(SELECTED_BOOK_PATH_KEY);
		mySelectedBook = null;
		if (selectedBookPath != null) {
			final ZLFile file = ZLFile.createFileByPath(selectedBookPath);
			if (file != null) {
				mySelectedBook = Book.getByFile(file);
			}
		}

		new LibraryTreeAdapter(this);

		init(getIntent());

		getListView().setTextFilterEnabled(true);
		getListView().setOnCreateContextMenuListener(this);
	}

	@Override
	protected FBTree getTreeByKey(FBTree.Key key) {
		return key != null ? FBReaderApp.Instance().getLibraryTree(key) : FBReaderApp.Instance().getRootTree();
	}

	@Override
	public void onPause() {
		super.onPause();
		ourToBeKilled = true;
	}

	@Override
	protected void onDestroy() {
		FBReaderApp.Instance().removeChangeListener(this);
		super.onDestroy();
	}

	@Override
	public boolean isTreeSelected(FBTree tree) {
		final LibraryTree lTree = (LibraryTree)tree;
		return lTree.isSelectable() && lTree.containsBook(mySelectedBook);
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position, long rowId) {
		final LibraryTree tree = (LibraryTree)getListAdapter().getItem(position);
		final Book book = tree.getBook();
		if (book != null) {
			showBookInfo(book);
		} else {
			openTree(tree);
		}
	}

	//
	// show BookInfoActivity
	//
	private static final int BOOK_INFO_REQUEST = 1;

	protected void showBookInfo(Book book) {
		startActivityForResult(
			new Intent(getApplicationContext(), BookInfoActivity.class)
				.putExtra(BookInfoActivity.CURRENT_BOOK_PATH_KEY, book.m_filePath),
			BOOK_INFO_REQUEST
		);
	}

	@Override
	protected void onActivityResult(int requestCode, int returnCode, Intent intent) {
		if (requestCode == BOOK_INFO_REQUEST && intent != null) {
			final String path = intent.getStringExtra(BookInfoActivity.CURRENT_BOOK_PATH_KEY);
			final Book book = Book.getByFile(ZLFile.createFileByPath(path));
			FBReaderApp.Instance().refreshBookInfo(book);
			getListView().invalidateViews();
		} else {
			super.onActivityResult(requestCode, returnCode, intent);
		}
	} 

	//
	// Search
	//
	static final ZLStringOption BookSearchPatternOption =
		new ZLStringOption("BookSearch", "Pattern", "");

	private void openSearchResults() {
		final FBTree tree = FBReaderApp.Instance().getRootTree().getSubTree(FBReaderApp.ROOT_FOUND);
		if (tree != null) {
			openTree(tree);
		}
	}

	@Override
	public boolean onSearchRequested() {
		startSearch(BookSearchPatternOption.getValue(), true, null, false);
		return true;
	}

	//
	// Context menu
	//
	private static final int OPEN_BOOK_ITEM_ID = 0;
	private static final int SHOW_BOOK_INFO_ITEM_ID = 1;
	private static final int SHARE_BOOK_ITEM_ID = 2;
	private static final int ADD_TO_FAVORITES_ITEM_ID = 3;
	private static final int REMOVE_FROM_FAVORITES_ITEM_ID = 4;
	private static final int DELETE_BOOK_ITEM_ID = 5;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		final int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
		final Book book = ((LibraryTree)getListAdapter().getItem(position)).getBook();
		if (book != null) {
			createBookContextMenu(menu, book); 
		}
	}

	private void createBookContextMenu(ContextMenu menu, Book book) {
		final ZLResource resource = ZLResource.resource("library");
		menu.setHeaderTitle(book.myTitle);
		menu.add(0, OPEN_BOOK_ITEM_ID, 0, resource.getResource("openBook").getValue());
		menu.add(0, SHOW_BOOK_INFO_ITEM_ID, 0, resource.getResource("showBookInfo").getValue());
		menu.add(0, SHARE_BOOK_ITEM_ID, 0, resource.getResource("shareBook").getValue());
		if (FBReaderApp.Instance().isBookInFavorites(book)) {
			menu.add(0, REMOVE_FROM_FAVORITES_ITEM_ID, 0, resource.getResource("removeFromFavorites").getValue());
		} else {
			menu.add(0, ADD_TO_FAVORITES_ITEM_ID, 0, resource.getResource("addToFavorites").getValue());
		}
		if (FBReaderApp.Instance().canRemoveBookFile(book)) {
			menu.add(0, DELETE_BOOK_ITEM_ID, 0, resource.getResource("deleteBook").getValue());
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		final Book book = ((LibraryTree)getListAdapter().getItem(position)).getBook();
		if (book != null) {
			return onContextItemSelected(item.getItemId(), book);
		}
		return super.onContextItemSelected(item);
	}

	private boolean onContextItemSelected(int itemId, Book book) {
		switch (itemId) {
			case OPEN_BOOK_ITEM_ID:
				openBook(book);
				return true;
			case SHOW_BOOK_INFO_ITEM_ID:
				showBookInfo(book);
				return true;
			case SHARE_BOOK_ITEM_ID:
				UIUtil.shareBook(this, book);
				return true;
			case ADD_TO_FAVORITES_ITEM_ID:
				FBReaderApp.Instance().addBookToFavorites(book);
				return true;
			case REMOVE_FROM_FAVORITES_ITEM_ID:
				FBReaderApp.Instance().removeBookFromFavorites(book);
				getListView().invalidateViews();
				return true;
			case DELETE_BOOK_ITEM_ID:
				tryToDeleteBook(book);
				return true;
		}
		return false;
	}

	private void openBook(Book book) {
		startActivity(
			new Intent(getApplicationContext(), SCReaderActivity.class)
				.setAction(Intent.ACTION_VIEW)
				.putExtra(SCReaderActivity.BOOK_PATH_KEY, book.m_filePath)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		);
	}

	//
	// Options menu
	//

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
//		addMenuItem(menu, 1, "localSearch", R.drawable.ic_menu_search);
		return true;
	}

	private MenuItem addMenuItem(Menu menu, int index, String resourceKey, int iconId) {
		final String label = ZLResource.resource("library").getResource("menu").getResource(resourceKey).getValue();
		final MenuItem item = menu.add(0, index, Menu.NONE, label);
		item.setOnMenuItemClickListener(this);
		item.setIcon(iconId);
		return item;
	}

	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case 1:
				return onSearchRequested();
			default:
				return true;
		}
	}

	//
	// Book deletion
	//
	private class BookDeleter implements DialogInterface.OnClickListener {
		private final Book myBook;
		private final int myMode;

		BookDeleter(Book book, int removeMode) {
			myBook = book;
			myMode = removeMode;
		}

		public void onClick(DialogInterface dialog, int which) {
			deleteBook(myBook, myMode);
		}
	}

	private void tryToDeleteBook(Book book) {
		final ZLResource dialogResource = ZLResource.resource("dialog");
		final ZLResource buttonResource = dialogResource.getResource("button");
		final ZLResource boxResource = dialogResource.getResource("deleteBookBox");
		new AlertDialog.Builder(this)
			.setTitle(book.myTitle)
			.setMessage(boxResource.getResource("message").getValue())
			.setIcon(0)
			.setPositiveButton(buttonResource.getResource("yes").getValue(), new BookDeleter(book, FBReaderApp.REMOVE_FROM_DISK))
			.setNegativeButton(buttonResource.getResource("no").getValue(), null)
			.create().show();
	}

	private void deleteBook(Book book, int mode) {
		FBReaderApp.Instance().removeBook(book, mode);

		if (getCurrentTree() instanceof FileTree) {
			ZLFile file = ZLFile.createFileByPath(book.m_filePath);
			getListAdapter().remove(new FileTree((FileTree)getCurrentTree(), file));
		} else {
			getListAdapter().replaceAll(getCurrentTree().subTrees());
		}
		getListView().invalidateViews();
	}

	public void onLibraryChanged(final Code code) {
		runOnUiThread(new Runnable() {
			public void run() {
				switch (code) {
					default:
						getListAdapter().replaceAll(getCurrentTree().subTrees());
						break;
					case StatusChanged:
						setProgressBarIndeterminateVisibility(!FBReaderApp.Instance().isUpToDate());
						break;
					case Found:
						openSearchResults();
						break;
					case NotFound:
						UIUtil.showErrorMessage(LibraryActivity.this, "bookNotFound");
						break;
				}
			}
		});
	}
}
