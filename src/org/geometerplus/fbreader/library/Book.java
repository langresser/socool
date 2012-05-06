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

package org.geometerplus.fbreader.library;

import java.lang.ref.WeakReference;
import java.util.*;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.filesystem.ZLPhysicalFile;
import org.geometerplus.zlibrary.image.ZLImage;

import org.geometerplus.zlibrary.text.ZLTextPosition;
import org.geometerplus.zlibrary.util.ZLMiscUtil;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.filetype.FileTypeCollection;
import org.geometerplus.fbreader.formats.*;
import org.geometerplus.fbreader.bookmodel.BookReadingException;

import org.geometerplus.fbreader.Paths;

public class Book {
	public static Book getById(long bookId) {
		final Book book = FBReaderApp.Instance().getDatabase().loadBook(bookId);
		if (book == null) {
			return null;
		}

		final ZLFile bookFile = book.File;
		final ZLPhysicalFile physicalFile = bookFile.getPhysicalFile();
		if (physicalFile == null) {
			return book;
		}
		if (!physicalFile.exists()) {
			// TODO 再次从sd卡中搜索
			return null;
		}

		try {
			book.readMetaInfo();
			return book;
		} catch (BookReadingException e) {
			return null;
		}
	}

	public static Book getByFile(ZLFile bookFile) {
		if (bookFile == null) {
			return null;
		}

		final ZLPhysicalFile physicalFile = bookFile.getPhysicalFile();
		if (physicalFile != null && !physicalFile.exists()) {
			return null;
		}

		// TODO 先查看下数据库
		Book book = null;//FBReaderApp.Instance().getDatabase().loadBookByFile(bookFile);

		try {
			if (book == null) {
				book = new Book(bookFile);
			} else {
				book.readMetaInfo();
			}
		} catch (BookReadingException e) {
			return null;
		}

		book.save();
		return book;
	}

	public final ZLFile File;
	public String m_filePath = "";
	public int m_fileSize = 0;
	public String m_bookAuthor = "";

	private volatile long myId;

	private volatile String myEncoding;
	private volatile String myLanguage;
	private volatile String myTitle;

	private volatile boolean myIsSaved;

	private static final WeakReference<ZLImage> NULL_IMAGE = new WeakReference<ZLImage>(null);
	private WeakReference<ZLImage> myCover;

	Book(long id, ZLFile file, String title, String encoding, String language) {
		myId = id;
		File = file;
		myTitle = title;
		myEncoding = encoding;
		myLanguage = language;
		myIsSaved = true;
	}

	public Book(ZLFile file) throws BookReadingException {
		myId = -1;
		final FormatPlugin plugin = getPlugin(file);
		File = plugin.realBookFile(file);
		readMetaInfo(plugin);
	}

	public void reloadInfoFromFile() {
		try {
			readMetaInfo();
			save();
		} catch (BookReadingException e) {
			// ignore
		}
	}

	public void reloadInfoFromDatabase() {
		final BooksDatabase database = FBReaderApp.Instance().getDatabase();
		database.reloadBook(this);
		myIsSaved = true;
	}

	private FormatPlugin getPlugin(ZLFile file) throws BookReadingException {
		final FormatPlugin plugin = PluginCollection.Instance().getPlugin(
				FileTypeCollection.Instance.typeForFile(file), FormatPlugin.ANY);
		if (plugin == null) {
			throw new BookReadingException("pluginNotFound", file);
		}
		return plugin;
	}

	public FormatPlugin getPlugin() throws BookReadingException {
		return getPlugin(File);
	}

	public void readMetaInfo() throws BookReadingException {
		readMetaInfo(getPlugin());
	}

	private void readMetaInfo(FormatPlugin plugin) throws BookReadingException {
		myEncoding = null;
		myLanguage = null;
		myTitle = null;
		m_bookAuthor = null;

		myIsSaved = false;

		plugin.readMetaInfo(this);
		if (myTitle == null || myTitle.length() == 0) {
			final String fileName = File.getShortName();
			final int index = fileName.lastIndexOf('.');
			setTitle(index > 0 ? fileName.substring(0, index) : fileName);
		}
	}

	public String authors() {
		return m_bookAuthor == null ? "" : m_bookAuthor;
	}

	public long getId() {
		return myId;
	}

	public String getTitle() {
		return myTitle;
	}

	public void setTitle(String title) {
		if (!ZLMiscUtil.equals(myTitle, title)) {
			myTitle = title;
			myIsSaved = false;
		}
	}

	public String getLanguage() {
		return myLanguage;
	}

	public void setLanguage(String language) {
		if (!ZLMiscUtil.equals(myLanguage, language)) {
			myLanguage = language;
			myIsSaved = false;
		}
	}

	public String getEncoding() {
		if (myEncoding == null) {
			try {
				getPlugin().detectLanguageAndEncoding(this);
			} catch (BookReadingException e) {
			}
			if (myEncoding == null) {
				setEncoding("utf-8");
			}
		}
		return myEncoding;
	}

	public String getEncodingNoDetection() {
		return myEncoding;
	}

	public void setEncoding(String encoding) {
		if (!ZLMiscUtil.equals(myEncoding, encoding)) {
			myEncoding = encoding;
			myIsSaved = false;
		}
	}

	public boolean matches(String pattern) {
		if (myTitle != null && ZLMiscUtil.matchesIgnoreCase(myTitle, pattern)) {
			return true;
		}

		if (ZLMiscUtil.matchesIgnoreCase(File.getLongName(), pattern)) {
			return true;
		}
		return false;
	}

	public boolean save() {
		if (myIsSaved) {
			return false;
		}
		final BooksDatabase database = FBReaderApp.Instance().getDatabase();
		database.executeAsATransaction(new Runnable() {
			public void run() {
				if (myId >= 0) {
					database.updateBookInfo(myId, File, myEncoding, myLanguage, myTitle);
				} else {
					myId = database.insertBookInfo(File, myEncoding, myLanguage, myTitle);
					storeAllVisitedHyperinks();
				}
			}
		});

		myIsSaved = true;
		return true;
	}

	public ZLTextPosition getStoredPosition() {
		return FBReaderApp.Instance().getDatabase().getStoredPosition(myId);
	}

	public void storePosition(ZLTextPosition position) {
		if (myId != -1) {
			FBReaderApp.Instance().getDatabase().storePosition(myId, position);
		}
	}

	private Set<String> myVisitedHyperlinks;
	private void initHyperlinkSet() {
		if (myVisitedHyperlinks == null) {
			myVisitedHyperlinks = new TreeSet<String>();
			if (myId != -1) {
				myVisitedHyperlinks.addAll(FBReaderApp.Instance().getDatabase().loadVisitedHyperlinks(myId));
			}
		}
	}

	public boolean isHyperlinkVisited(String linkId) {
		initHyperlinkSet();
		return myVisitedHyperlinks.contains(linkId);
	}

	public void markHyperlinkAsVisited(String linkId) {
		initHyperlinkSet();
		if (!myVisitedHyperlinks.contains(linkId)) {
			myVisitedHyperlinks.add(linkId);
			if (myId != -1) {
				FBReaderApp.Instance().getDatabase().addVisitedHyperlink(myId, linkId);
			}
		}
	}

	private void storeAllVisitedHyperinks() {
		if (myId != -1 && myVisitedHyperlinks != null) {
			for (String linkId : myVisitedHyperlinks) {
				FBReaderApp.Instance().getDatabase().addVisitedHyperlink(myId, linkId);
			}
		}
	}

	public void insertIntoBookList() {
		if (myId != -1) {
			FBReaderApp.Instance().getDatabase().insertIntoBookList(myId);
		}
	}

	public String getContentHashCode() {
		InputStream stream = null;

		try {
			final MessageDigest hash = MessageDigest.getInstance("SHA-256");
			stream = File.getInputStream();

			final byte[] buffer = new byte[2048];
			while (true) {
				final int nread = stream.read(buffer);
				if (nread == -1) {
					break;
				}
				hash.update(buffer, 0, nread);
			}

			final Formatter f = new Formatter();
			for (byte b : hash.digest()) {
				f.format("%02X", b & 0xFF);
			}
			return f.toString();
		} catch (IOException e) {
			return null;
		} catch (NoSuchAlgorithmException e) {
			return null;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public synchronized ZLImage getCover() {
		if (myCover == NULL_IMAGE) {
			return null;
		} else if (myCover != null) {
			final ZLImage image = myCover.get();
			if (image != null) {
				return image;
			}
		}
		ZLImage image = null;
		try {
			image = getPlugin().readCover(File);
		} catch (BookReadingException e) {
			// ignore
		}
		myCover = image != null ? new WeakReference<ZLImage>(image) : NULL_IMAGE;
		return image;
	}

	@Override
	public int hashCode() {
		return (int)myId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Book)) {
			return false;
		}
		return File.equals(((Book)o).File);
	}
}
