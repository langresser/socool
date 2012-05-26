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

import org.geometerplus.fbreader.Paths;

public class Book {
	public static Book getById(long bookId) {
		final Book book = FBReaderApp.Instance().getDatabase().loadBook(bookId);
		if (book == null) {
			return null;
		}

		final ZLFile bookFile = ZLFile.createFileByPath(book.m_filePath);
		final ZLPhysicalFile physicalFile = bookFile.getPhysicalFile();
		if (physicalFile == null) {
			return book;
		}
		if (!physicalFile.exists()) {
			// TODO 再次从sd卡中搜索
			return null;
		}

		book.readMetaInfo();
		return book;
	}

	public static Book getByFile(ZLFile bookFile) {
		if (bookFile == null) {
			return null;
		}

		final ZLPhysicalFile physicalFile = bookFile.getPhysicalFile();
		if (physicalFile != null && !physicalFile.exists()) {
			return null;
		}

		final FormatPlugin plugin = PluginCollection.Instance().getPlugin(
				FileTypeCollection.Instance.typeForFile(bookFile), FormatPlugin.ANY);

		if (plugin == null) {
			return null;
		}

		Book book = FBReaderApp.Instance().getDatabase().loadBookByFile(bookFile.getPath());

		if (book == null) {
			book = new Book(bookFile.getPath());
		} else {
			book.readMetaInfo();
		}

		book.save();
		return book;
	}
	
	public static Book getByPath(String path) {
		if (path == null) {
			return null;
		}

		Book book = new Book(path);;//FBReaderApp.Instance().getDatabase().loadBookByFile(bookFile);

//		if (book == null) {
//			book = new Book(path);
//		} else {
//			book.readMetaInfo();
//		}

		book.save();
		return book;
	}

	public String m_filePath = "";		// 如果是单文件，则对应文件全路径；如果是一组文件，则对应文件夹路径
	public int m_fileSize = 0;
	public String m_bookAuthor = "";

	public volatile long myId;

	public volatile String myEncoding;
	public volatile String myLanguage;
	public volatile String myTitle;

	private volatile boolean myIsSaved;

	private static final WeakReference<ZLImage> NULL_IMAGE = new WeakReference<ZLImage>(null);
	private WeakReference<ZLImage> myCover;

	Book(long id, String filePath, String title, String encoding, String language) {
		myId = id;
		myTitle = title;
		myEncoding = encoding;
		myLanguage = language;
		myIsSaved = true;
		m_filePath = filePath;
	}

	public Book(String filePath) {
		myId = -1;
		final FormatPlugin plugin = getPlugin(filePath);
		if (plugin != null) {
			readMetaInfo(plugin);
			m_filePath = filePath;
		}
	}
	
	public void reloadInfoFromFile() {
		readMetaInfo();
		save();
	}

	public void reloadInfoFromDatabase() {
		final BooksDatabase database = FBReaderApp.Instance().getDatabase();
		database.reloadBook(this);
		myIsSaved = true;
	}

	private FormatPlugin getPlugin(String filePath) {
		final ZLFile file = ZLFile.createFileByPath(filePath);
		final FormatPlugin plugin = PluginCollection.Instance().getPlugin(
				FileTypeCollection.Instance.typeForFile(file), FormatPlugin.ANY);
		if (plugin == null) {
			// TODO logmsg
			return null;
		}
		return plugin;
	}

	public FormatPlugin getPlugin() {
		return PluginCollection.Instance().getPlugin();
	}

	public void readMetaInfo() {
		readMetaInfo(getPlugin());
	}

	private void readMetaInfo(FormatPlugin plugin) {
		myEncoding = null;
		myLanguage = null;
		myTitle = null;
		m_bookAuthor = null;

		myIsSaved = false;

		plugin.readMetaInfo(this);

		if (myTitle == null || myTitle.length() == 0) {
			final String fileName = m_filePath.substring(m_filePath.lastIndexOf('/'));
			final int index = fileName.lastIndexOf('.');
			setTitle(index > 0 ? fileName.substring(0, index) : fileName);
		}
	}

	public String authors() {
		return m_bookAuthor == null ? "" : m_bookAuthor;
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
			getPlugin().detectLanguageAndEncoding(this);
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

	public boolean save() {
		if (myIsSaved) {
			return false;
		}
		final BooksDatabase database = FBReaderApp.Instance().getDatabase();
		database.executeAsATransaction(new Runnable() {
			public void run() {
				if (myId >= 0) {
					database.updateBookInfo(myId, m_filePath, myEncoding, myLanguage, myTitle);
				} else {
					myId = database.insertBookInfo(m_filePath, myEncoding, myLanguage, myTitle);
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

	public String getContentHashCode() {
		InputStream stream = null;

		try {
			final MessageDigest hash = MessageDigest.getInstance("SHA-256");
			ZLFile file = ZLFile.createFileByPath(m_filePath);
			stream = file.getInputStream();

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
		ZLImage image = getPlugin().readCover(this);
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
		return m_filePath.equals(((Book)o).m_filePath);
	}
}
