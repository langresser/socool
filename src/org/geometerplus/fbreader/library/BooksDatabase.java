/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.fbreader.library;

import java.math.BigDecimal;
import java.util.*;

import org.geometerplus.android.fbreader.util.SQLiteUtil;
import org.geometerplus.android.fbreader.util.UIUtil;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.filesystem.ZLPhysicalFile;
import org.geometerplus.zlibrary.options.ZLIntegerOption;
import org.geometerplus.zlibrary.options.ZLStringOption;

import org.geometerplus.zlibrary.text.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.ZLTextPosition;
import org.geometerplus.zlibrary.util.ZLConfig;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

public class BooksDatabase {
	private final SQLiteDatabase myDatabase;

	public Book createBook(long id, ZLFile file, String title, String encoding, String language) {
		return (file != null) ? new Book(id, file, title, encoding, language) : null;
	}

	public BooksDatabase(Context context) {
		myDatabase = context.openOrCreateDatabase("books.db", Context.MODE_PRIVATE, null);
		updateDatabase();
	}

	public void executeAsATransaction(Runnable actions) {
		boolean transactionStarted = false;
		try {
			myDatabase.beginTransaction();
			transactionStarted = true;
		} catch (Throwable t) {
		}
		try {
			actions.run();
			if (transactionStarted) {
				myDatabase.setTransactionSuccessful();
			}
		} finally {
			if (transactionStarted) {
				myDatabase.endTransaction();
			}
		}
	}

	private void updateDatabase() {
		final int version = myDatabase.getVersion();
		final int currentVersion = 1;
		if (version >= currentVersion) {
			return;
		}

		myDatabase.beginTransaction();

		switch (version) {
			case 0:
				createTables();
				break;
		}
		myDatabase.setTransactionSuccessful();
		myDatabase.endTransaction();

		myDatabase.execSQL("VACUUM");
		myDatabase.setVersion(currentVersion);
	}

	protected Book loadBook(long bookId) {
		Book book = null;
		final Cursor cursor = myDatabase.rawQuery("SELECT file_path,title,encoding,language FROM Books WHERE book_id = " + bookId, null);
		if (cursor.moveToNext()) {
			book = createBook(
				bookId, ZLFile.createFileByPath(cursor.getString(0)), cursor.getString(1), cursor.getString(2), cursor.getString(3)
			);
		}
		cursor.close();
		return book;
	}

	protected void reloadBook(Book book) {
		final Cursor cursor = myDatabase.rawQuery("SELECT title,encoding,language FROM Books WHERE book_id = " + book.myId, null);
		if (cursor.moveToNext()) {
			book.setTitle(cursor.getString(0));
			book.setEncoding(cursor.getString(1));
			book.setLanguage(cursor.getString(2));
		}
		cursor.close();
	}

	protected Book loadBookByFile(ZLFile file) {
		Book book = null;
		final Cursor cursor = myDatabase.rawQuery("SELECT book_id,title,encoding,language,author,file_size FROM Books WHERE file_path = '" + file.getPath() + "'", null);
		if (cursor.moveToNext()) {
			book = createBook(
				cursor.getLong(0), file, cursor.getString(1), cursor.getString(2), cursor.getString(3)
			);
			book.m_filePath = file.getPath();
			book.m_bookAuthor = cursor.getString(4);
			book.m_fileSize = (int)cursor.getLong(5);
		}
		cursor.close();
		return book;
	}

	public Map<Long,Book> loadBooks() {
		Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id,title,encoding,language,author,file_path,file_size FROM Books" , null
		);
		final HashMap<Long,Book> booksById = new HashMap<Long,Book>();
		while (cursor.moveToNext()) {
			final long id = cursor.getLong(0);
			final Book book = createBook(id, ZLFile.createFileByPath(cursor.getString(5)),
					cursor.getString(1), cursor.getString(2), cursor.getString(3));
			book.m_filePath = cursor.getString(5);
			book.m_fileSize = (int)cursor.getLong(6);
			book.m_bookAuthor = cursor.getString(4);
			if (book != null) {
				booksById.put(id, book);
			}
		}
		cursor.close();
		return booksById;
	}

	private SQLiteStatement myUpdateBookInfoStatement;
	protected void updateBookInfo(long bookId, ZLFile file, String encoding, String language, String title) {
		if (myUpdateBookInfoStatement == null) {
			myUpdateBookInfoStatement = myDatabase.compileStatement(
				"UPDATE Books SET file_path = ?, encoding = ?, language = ?, title = ? WHERE book_id = ?"
			);
		}
		myUpdateBookInfoStatement.bindString(1, file.getPath());
		SQLiteUtil.bindString(myUpdateBookInfoStatement, 2, encoding);
		SQLiteUtil.bindString(myUpdateBookInfoStatement, 3, language);
		myUpdateBookInfoStatement.bindString(4, title);
		myUpdateBookInfoStatement.bindLong(5, bookId);
		myUpdateBookInfoStatement.execute();
	}

	private SQLiteStatement myInsertBookInfoStatement;
	protected long insertBookInfo(ZLFile file, String encoding, String language, String title) {
		if (myInsertBookInfoStatement == null) {
			myInsertBookInfoStatement = myDatabase.compileStatement(
				"INSERT OR IGNORE INTO Books (author,encoding,language,title,access_time,"+
				"pages_full,page_current,file_path,file_size) VALUES (?,?,?,?,?,?,?,?,?)"
			);
		}
		// TODO 添加作者等信息
		myInsertBookInfoStatement.bindString(1, "");
		myInsertBookInfoStatement.bindString(2, encoding == null ? "" : encoding);
		myInsertBookInfoStatement.bindString(3, language == null ? "" : language);
		myInsertBookInfoStatement.bindString(4, title);
		myInsertBookInfoStatement.bindLong(5, 0);
		myInsertBookInfoStatement.bindLong(6, 0);
		myInsertBookInfoStatement.bindLong(7, 0);
		myInsertBookInfoStatement.bindString(8, file.getPath());
		myInsertBookInfoStatement.bindLong(9, 0);
		long bookId = myInsertBookInfoStatement.executeInsert();
		return bookId;
	}

	private SQLiteStatement mySaveRecentBookStatement;
	public void saveRecentBookIds(final List<Long> ids) {
		if (mySaveRecentBookStatement == null) {
			mySaveRecentBookStatement = myDatabase.compileStatement(
				"INSERT OR IGNORE INTO RecentBooks (book_id) VALUES (?)"
			);
		}
		executeAsATransaction(new Runnable() {
			public void run() {
				myDatabase.delete("RecentBooks", null, null);
				for (long id : ids) {
					mySaveRecentBookStatement.bindLong(1, id);
					mySaveRecentBookStatement.execute();
				}
			}
		});
	}

	public List<Long> loadRecentBookIds() {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id FROM RecentBooks ORDER BY book_index", null
		);
		final LinkedList<Long> ids = new LinkedList<Long>();
		while (cursor.moveToNext()) {
			ids.add(cursor.getLong(0));
		}
		cursor.close();
		return ids;
	}

	private SQLiteStatement myAddToFavoritesStatement;
	public void addToFavorites(long bookId) {
		if (myAddToFavoritesStatement == null) {
			myAddToFavoritesStatement = myDatabase.compileStatement(
				"INSERT OR IGNORE INTO Favorites(book_id) VALUES (?)"
			);
		}
		myAddToFavoritesStatement.bindLong(1, bookId);
		myAddToFavoritesStatement.execute();
	}

	private SQLiteStatement myRemoveFromFavoritesStatement;
	public void removeFromFavorites(long bookId) {
		if (myRemoveFromFavoritesStatement == null) {
			myRemoveFromFavoritesStatement = myDatabase.compileStatement(
				"DELETE FROM Favorites WHERE book_id = ?"
			);
		}
		myRemoveFromFavoritesStatement.bindLong(1, bookId);
		myRemoveFromFavoritesStatement.execute();
	}

	public List<Long> loadFavoritesIds() {
		final Cursor cursor = myDatabase.rawQuery(
			"SELECT book_id FROM Favorites", null
		);
		final LinkedList<Long> ids = new LinkedList<Long>();
		while (cursor.moveToNext()) {
			ids.add(cursor.getLong(0));
		}
		cursor.close();
		return ids;
	}

	public List<Bookmark> loadBookmarks(long bookId) {
		LinkedList<Bookmark> list = new LinkedList<Bookmark>();
		myDatabase.execSQL("DELETE FROM Bookmarks WHERE book_id = -1");
		Cursor cursor = myDatabase.rawQuery(
			"SELECT Bookmarks.bookmark_id, Bookmarks.book_id, Books.title, Bookmarks.bookmark_text, Bookmarks.model_id,"+	// 5
			"Bookmarks.modification_time, Bookmarks.paragraph, Bookmarks.word, Bookmarks.char,"+//4
			"Bookmarks.type, Bookmarks.comment,"+
			"Bookmarks.begin_paragraph, Bookmarks.begin_word, Bookmarks.begin_char,"+//5
			"Bookmarks.end_paragraph,Bookmarks.end_word,Bookmarks.end_char "+//3
			"FROM Bookmarks INNER JOIN Books ON Books.book_id = Bookmarks.book_id WHERE Bookmarks.book_id = ?",
			new String[] { "" + bookId});

		while (cursor.moveToNext()) {
			int type = (int)cursor.getLong(9);
			if (type == Bookmark.BOOKMARK_TYPE_COMMENT) {
				ZLTextFixedPosition page = new ZLTextFixedPosition((int)cursor.getLong(6), (int)cursor.getLong(7), (int)cursor.getLong(8));
				ZLTextFixedPosition begin = new ZLTextFixedPosition((int)cursor.getLong(11), (int)cursor.getLong(12), (int)cursor.getLong(13));
				ZLTextFixedPosition end = new ZLTextFixedPosition((int)cursor.getLong(14), (int)cursor.getLong(15), (int)cursor.getLong(16));
				Bookmark bookmark = new Bookmark(
						cursor.getLong(0),				// Bookmarks.bookmark_id
						cursor.getLong(1),				// Bookmarks.book_id
						cursor.getString(2),			// Books.title
						cursor.getString(3),			// Bookmarks.bookmark_text
						cursor.getString(4),			// Bookmarks.model_id
						SQLiteUtil.getDate(cursor, 5),	// date
						page, begin, end,				// page
						cursor.getString(10));			// Bookmarks.comment
				list.add(bookmark);
			} else {
				ZLTextFixedPosition page = new ZLTextFixedPosition((int)cursor.getLong(6), (int)cursor.getLong(7), (int)cursor.getLong(8));
				Bookmark bookmark = new Bookmark(
						cursor.getLong(0),				// Bookmarks.bookmark_id
						cursor.getLong(1),				// Bookmarks.book_id
						cursor.getString(2),			// Books.title
						cursor.getString(3),			// Bookmarks.bookmark_text
						cursor.getString(8),			// Bookmarks.model_id
						SQLiteUtil.getDate(cursor, 4),	// date
						page, null, null,				// page
						null);							// Bookmarks.comment
				list.add(bookmark);
			}
		}
		cursor.close();
		return list;
	}

	public List<Bookmark> loadAllBookmarks() {
		LinkedList<Bookmark> list = new LinkedList<Bookmark>();
		myDatabase.execSQL("DELETE FROM Bookmarks WHERE book_id = -1");
		Cursor cursor = myDatabase.rawQuery(
			"SELECT Bookmarks.bookmark_id, Bookmarks.book_id, Books.title, Bookmarks.bookmark_text, Bookmarks.model_id,"+	// 5
			"Bookmarks.modification_time, Bookmarks.paragraph, Bookmarks.word, Bookmarks.char,"+//4
			"Bookmarks.type, Bookmarks.comment,"+
			"Bookmarks.begin_paragraph, Bookmarks.begin_word, Bookmarks.begin_char,"+//5
			"Bookmarks.end_paragraph,Bookmarks.end_word,Bookmarks.end_char "+//3
			"FROM Bookmarks INNER JOIN Books ON Books.book_id = Bookmarks.book_id", null
		);
		while (cursor.moveToNext()) {
			int type = (int)cursor.getLong(9);
			if (type == Bookmark.BOOKMARK_TYPE_COMMENT) {
				ZLTextFixedPosition page = new ZLTextFixedPosition((int)cursor.getLong(6), (int)cursor.getLong(7), (int)cursor.getLong(8));
				ZLTextFixedPosition begin = new ZLTextFixedPosition((int)cursor.getLong(11), (int)cursor.getLong(12), (int)cursor.getLong(13));
				ZLTextFixedPosition end = new ZLTextFixedPosition((int)cursor.getLong(14), (int)cursor.getLong(15), (int)cursor.getLong(16));
				Bookmark bookmark = new Bookmark(
						cursor.getLong(0),				// Bookmarks.bookmark_id
						cursor.getLong(1),				// Bookmarks.book_id
						cursor.getString(2),			// Books.title
						cursor.getString(3),			// Bookmarks.bookmark_text
						cursor.getString(4),			// Bookmarks.model_id
						SQLiteUtil.getDate(cursor, 5),	// date
						page, begin, end,				// page
						cursor.getString(10));			// Bookmarks.comment
				list.add(bookmark);
			} else {
				ZLTextFixedPosition page = new ZLTextFixedPosition((int)cursor.getLong(6), (int)cursor.getLong(7), (int)cursor.getLong(8));
				Bookmark bookmark = new Bookmark(
						cursor.getLong(0),				// Bookmarks.bookmark_id
						cursor.getLong(1),				// Bookmarks.book_id
						cursor.getString(2),			// Books.title
						cursor.getString(3),			// Bookmarks.bookmark_text
						cursor.getString(8),			// Bookmarks.model_id
						SQLiteUtil.getDate(cursor, 4),	// date
						page, null, null,				// page
						null);							// Bookmarks.comment
				list.add(bookmark);
			}
		}
		cursor.close();
		return list;
	}

	private SQLiteStatement myInsertBookmarkStatement;
	private SQLiteStatement myUpdateBookmarkStatement;
	protected long saveBookmark(Bookmark bookmark) {
		SQLiteStatement statement;
		if (bookmark.getId() == -1) {
			if (myInsertBookmarkStatement == null) {
				myInsertBookmarkStatement = myDatabase.compileStatement(
					"INSERT OR IGNORE INTO Bookmarks (book_id,bookmark_text,"+
					"modification_time,model_id,"+
					"paragraph,word,char,type,comment,begin_paragraph,begin_word,begin_char,end_paragraph,end_word,end_char) " +
					"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
				);
			}
			statement = myInsertBookmarkStatement;
		} else {
			if (myUpdateBookmarkStatement == null) {
				myUpdateBookmarkStatement = myDatabase.compileStatement(
					"UPDATE Bookmarks SET book_id = ?, bookmark_text = ?,"+
					"modification_time = ?, model_id = ?, "+
					"paragraph = ?, word = ?, char = ?, type = ?,comment = ?,"+
					"begin_paragraph = ?,begin_word = ?,begin_char = ?,end_paragraph = ?,end_word = ?,end_char = ?"+
					"WHERE bookmark_id = ?");
			}
			statement = myUpdateBookmarkStatement;
		}

		statement.bindLong(1, bookmark.getBookId());
		statement.bindString(2, bookmark.getText());						// 书摘的所选文本  书签保留20个字符
		SQLiteUtil.bindDate(statement, 3, bookmark.getTime());				// 书摘的修改时间 书签的创建时间
		SQLiteUtil.bindString(statement, 4, bookmark.ModelId);
		statement.bindLong(5, bookmark.m_posCurrentPage.ParagraphIndex);	// 书签所在页
		statement.bindLong(6, bookmark.m_posCurrentPage.ElementIndex);
		statement.bindLong(7, bookmark.m_posCurrentPage.CharIndex);
		statement.bindLong(8, bookmark.m_bookmarkType);					// 书签类型（书签 书摘）
		
		if (bookmark.m_bookmarkType == Bookmark.BOOKMARK_TYPE_COMMENT) {
			if (bookmark.m_bookmarkComment == null) {
				statement.bindString(9, "");				// 书摘注释内容
			} else {
				statement.bindString(9, bookmark.m_bookmarkComment);				// 书摘注释内容
			}
			
			statement.bindLong(10, bookmark.m_posBegin.ParagraphIndex);			// 书摘的起始和结束
			statement.bindLong(11, bookmark.m_posBegin.ElementIndex);
			statement.bindLong(12, bookmark.m_posBegin.CharIndex);
			statement.bindLong(13, bookmark.m_posEnd.ParagraphIndex);
			statement.bindLong(14, bookmark.m_posEnd.ElementIndex);
			statement.bindLong(15, bookmark.m_posEnd.CharIndex);
		} else {
			statement.bindString(9, "");				// 书摘注释内容
			statement.bindLong(10, -1);			// 书摘的起始和结束
			statement.bindLong(11, -1);
			statement.bindLong(12, -1);
			statement.bindLong(13, -1);
			statement.bindLong(14, -1);
			statement.bindLong(15, -1);
		}
		
		if (statement == myInsertBookmarkStatement) {
			return statement.executeInsert();
		} else {
			final long id = bookmark.getId();
			statement.bindLong(16, id);
			statement.execute();
			return id;
		}
	}

	private SQLiteStatement myDeleteBookmarkStatement;
	protected void deleteBookmark(Bookmark bookmark) {
		if (myDeleteBookmarkStatement == null) {
			myDeleteBookmarkStatement = myDatabase.compileStatement(
				"DELETE FROM Bookmarks WHERE bookmark_id = ?"
			);
		}
		myDeleteBookmarkStatement.bindLong(1, bookmark.getId());
		myDeleteBookmarkStatement.execute();
	}

	protected ZLTextPosition getStoredPosition(long bookId) {
		ZLTextPosition position = null;
		Cursor cursor = myDatabase.rawQuery(
			"SELECT paragraph,word,char FROM BookState WHERE book_id = " + bookId, null
		);
		if (cursor.moveToNext()) {
			position = new ZLTextFixedPosition(
				(int)cursor.getLong(0),
				(int)cursor.getLong(1),
				(int)cursor.getLong(2)
			);
		}
		cursor.close();
		return position;
	}

	private SQLiteStatement myStorePositionStatement;
	protected void storePosition(long bookId, ZLTextPosition position) {
		if (myStorePositionStatement == null) {
			myStorePositionStatement = myDatabase.compileStatement(
				"INSERT OR REPLACE INTO BookState (book_id,paragraph,word,char) VALUES (?,?,?,?)"
			);
		}
		myStorePositionStatement.bindLong(1, bookId);
		myStorePositionStatement.bindLong(2, position.getParagraphIndex());
		myStorePositionStatement.bindLong(3, position.getElementIndex());
		myStorePositionStatement.bindLong(4, position.getCharIndex());
		myStorePositionStatement.execute();
	}

	private SQLiteStatement myDeleteVisitedHyperlinksStatement;
	private void deleteVisitedHyperlinks(long bookId) {
		if (myDeleteVisitedHyperlinksStatement == null) {
			myDeleteVisitedHyperlinksStatement = myDatabase.compileStatement(
				"DELETE FROM VisitedHyperlinks WHERE book_id = ?"
			);
		}

		myDeleteVisitedHyperlinksStatement.bindLong(1, bookId);
		myDeleteVisitedHyperlinksStatement.execute();
	}

	private SQLiteStatement myStoreVisitedHyperlinksStatement;
	protected void addVisitedHyperlink(long bookId, String hyperlinkId) {
		if (myStoreVisitedHyperlinksStatement == null) {
			myStoreVisitedHyperlinksStatement = myDatabase.compileStatement(
				"INSERT OR IGNORE INTO VisitedHyperlinks(book_id,hyperlink_id) VALUES (?,?)"
			);
		}

		myStoreVisitedHyperlinksStatement.bindLong(1, bookId);
		myStoreVisitedHyperlinksStatement.bindString(2, hyperlinkId);
		myStoreVisitedHyperlinksStatement.execute();
	}

	protected Collection<String> loadVisitedHyperlinks(long bookId) {
		final TreeSet<String> links = new TreeSet<String>();
		final Cursor cursor = myDatabase.rawQuery("SELECT hyperlink_id FROM VisitedHyperlinks WHERE book_id = ?", new String[] { "" + bookId });
		while (cursor.moveToNext()) {
			links.add(cursor.getString(0));
		}
		cursor.close();
		return links;
	}


	private void createTables() {
		// 书本数据
		myDatabase.execSQL(
				"CREATE TABLE Books(" +
					"book_id INTEGER PRIMARY KEY," +			// 唯一id
					"author TEXT," +							// 作者
					"encoding TEXT," +							// 文本编码
					"language TEXT," +							// 书本语言
					"title TEXT NOT NULL," +					// 书名（可自定义）
					"access_time INTEGER," +			// 上次访问时间
					"pages_full INTEGER," +			// 总页数
					"page_current INTEGER," +			// 当前阅读进度
					"file_path TEXT UNIQUE NOT NULL," +			// 文件路径
					"file_size INTERGER)");						// 文件大小

	
		// 最近阅读
		myDatabase.execSQL("CREATE TABLE RecentBooks(" +
				"book_index INTEGER PRIMARY KEY," +
				"book_id INTEGER REFERENCES Books(book_id))");

		// 书签
		myDatabase.execSQL("CREATE TABLE Bookmarks(" +
				"bookmark_id INTEGER PRIMARY KEY," +
				"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
				"bookmark_text TEXT NOT NULL," +
				"modification_time INTEGER," +
				"model_id TEXT," +
				"paragraph INTEGER NOT NULL," +
				"word INTEGER NOT NULL," +
				"char INTEGER NOT NULL," +
				"type INTEGER NOT NULL," +
				"comment TEXT NOT NULL," +
				"begin_paragraph INTEGER NOT NULL," +
				"begin_word INTEGER NOT NULL," +
				"begin_char INTEGER NOT NULL," +
				"end_paragraph INTEGER NOT NULL," +
				"end_word INTEGER NOT NULL," +
				"end_char INTEGER NOT NULL)");

		// 阅读进度
		myDatabase.execSQL("CREATE TABLE BookState(" +
				"book_id INTEGER UNIQUE NOT NULL REFERENCES Books(book_id)," +
				"paragraph INTEGER NOT NULL," +
				"word INTEGER NOT NULL," +
				"char INTEGER NOT NULL)");
		
		myDatabase.execSQL("CREATE TABLE IF NOT EXISTS Favorites(" +
					"book_id INTEGER UNIQUE NOT NULL REFERENCES Books(book_id))");
	
		myDatabase.execSQL("CREATE TABLE IF NOT EXISTS VisitedHyperlinks(" +
					"book_id INTEGER NOT NULL REFERENCES Books(book_id)," +
					"hyperlink_id TEXT NOT NULL," +
					"CONSTRAINT VisitedHyperlinks_Unique UNIQUE (book_id, hyperlink_id))");
	}
}
