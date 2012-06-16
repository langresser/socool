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

import java.util.*;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.text.ZLTextElement;
import org.geometerplus.zlibrary.text.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.ZLTextPosition;
import org.geometerplus.zlibrary.text.ZLTextWord;
import org.geometerplus.zlibrary.text.ZLTextWordCursor;


public final class Bookmark {
	public final static int CREATION = 0;
	public final static int MODIFICATION = 1;
	public final static int ACCESS = 2;
	public final static int LATEST = 3;
	
	public final static int BOOKMARK_TYPE_BOOKMARK = 0;
	public final static int BOOKMARK_TYPE_COMMENT = 1;
	
	private long myId;
	private final long myBookId;
	private final String myBookTitle;
	public String myText;

	public final String ModelId;
	
	public int m_bookmarkType = BOOKMARK_TYPE_BOOKMARK;
	public String m_bookmarkComment = null;

	public Date myModificationDate = null;
	public ZLTextFixedPosition m_posCurrentPage = null;	// 书签或者书摘在哪一页
	public ZLTextFixedPosition m_posBegin = null;			// 书摘开始字符
	public ZLTextFixedPosition m_posEnd = null;			// 书摘结束字符
	public int m_percent = 0;							// 阅读进度(仅用于显示)

	private boolean myIsChanged;
	
	// 通过数据库载入
	Bookmark(long id, long bookId, String bookTitle, String text, String modelId, Date date,
			ZLTextPosition page, ZLTextPosition begin, ZLTextPosition end, String comment, int percent) {
		myId = id;
		myBookId = bookId;
		myBookTitle = bookTitle;
		myText = text;
		ModelId = modelId;
		myIsChanged = false;
		m_percent = percent;
		myModificationDate = date;
		m_posCurrentPage = new ZLTextFixedPosition(page);

		if (begin == null && end == null) {
			m_posBegin = null;
			m_posEnd = null;
			m_bookmarkComment = null;

			m_bookmarkType = BOOKMARK_TYPE_BOOKMARK;
		} else {
			m_posBegin = new ZLTextFixedPosition(begin);
			m_posEnd = new ZLTextFixedPosition(end);
			m_bookmarkComment = comment;

			m_bookmarkType = BOOKMARK_TYPE_COMMENT;
		}
	}

	// 新创建书签
	public Bookmark(Book book, String modelId, ZLTextWordCursor cursor, int maxLength, int percent) {
		this(book, modelId, createBookmarkText(cursor, maxLength), cursor, null, null, null, percent);
	}

	// 代码中创建书签或书摘，如果没有指定选中文字的话，为书签
	public Bookmark(Book book, String modelId, String text,
			ZLTextPosition page, ZLTextPosition begin, ZLTextPosition end, String comment, int percent) {
		myId = -1;
		myBookId = book.myId;
		myBookTitle = book.myTitle;
		myText = text;
		myModificationDate = new Date();
		ModelId = modelId;
		myIsChanged = true;
		m_percent = percent;
		
		m_posCurrentPage = new ZLTextFixedPosition(page);
		
		if (begin == null && end == null) {
			m_posBegin = null;
			m_posEnd = null;
			m_bookmarkComment = null;

			m_bookmarkType = BOOKMARK_TYPE_BOOKMARK;
		} else {
			m_posBegin = new ZLTextFixedPosition(begin);
			m_posEnd = new ZLTextFixedPosition(end);
			m_bookmarkComment = comment;

			m_bookmarkType = BOOKMARK_TYPE_COMMENT;
		}
	}

	public long getId() {
		return myId;
	}

	public long getBookId() {
		return myBookId;
	}

	public String getText() {
		return myText;
	}

	public String getBookTitle() {
		return myBookTitle;
	}

	public Date getTime() {
		return myModificationDate;
	}

	public void setText(String text) {
		if (!text.equals(myText)) {
			myText = text;
			myModificationDate = new Date();
			myIsChanged = true;
		}
	}

	public void onOpen() {
		myIsChanged = true;
	}

	public void save() {
		if (myIsChanged) {
			myId = FBReaderApp.Instance().getDatabase().saveBookmark(this);
			myIsChanged = false;
		}
	}

	public void delete() {
		if (myId != -1) {
			FBReaderApp.Instance().getDatabase().deleteBookmark(this);
		}
	}

	private static String createBookmarkText(ZLTextWordCursor cursor, int maxWords) {
		cursor = new ZLTextWordCursor(cursor);

		final StringBuilder builder = new StringBuilder();
		final StringBuilder sentenceBuilder = new StringBuilder();
		final StringBuilder phraseBuilder = new StringBuilder();

		int wordCounter = 0;
		int sentenceCounter = 0;
		int storedWordCounter = 0;
		boolean lineIsNonEmpty = false;
		boolean appendLineBreak = false;
mainLoop:
		while (wordCounter < maxWords && sentenceCounter < 3) {
			while (cursor.isEndOfParagraph()) {
				if (!cursor.nextParagraph()) {
					break mainLoop;
				}
				if ((builder.length() > 0) && cursor.getParagraphCursor().isEndOfSection()) {
					break mainLoop;
				}
				if (phraseBuilder.length() > 0) {
					sentenceBuilder.append(phraseBuilder);
					phraseBuilder.delete(0, phraseBuilder.length());
				}
				if (sentenceBuilder.length() > 0) {
					if (appendLineBreak) {
						builder.append("\n");
					}
					builder.append(sentenceBuilder);
					sentenceBuilder.delete(0, sentenceBuilder.length());
					++sentenceCounter;
					storedWordCounter = wordCounter;
				}
				lineIsNonEmpty = false;
				if (builder.length() > 0) {
					appendLineBreak = true;
				}
			}
			final ZLTextElement element = cursor.getElement();
			if (element instanceof ZLTextWord) {
				final ZLTextWord word = (ZLTextWord)element;
				if (lineIsNonEmpty) {
					phraseBuilder.append(" ");
				}
				phraseBuilder.append(word.Data, word.Offset, word.Length);
				++wordCounter;
				lineIsNonEmpty = true;
				switch (word.Data[word.Offset + word.Length - 1]) {
					case ',':
					case ':':
					case ';':
					case ')':
						sentenceBuilder.append(phraseBuilder);
						phraseBuilder.delete(0, phraseBuilder.length());
						break;
					case '.':
					case '!':
					case '?':
						++sentenceCounter;
						if (appendLineBreak) {
							builder.append("\n");
							appendLineBreak = false;
						}
						sentenceBuilder.append(phraseBuilder);
						phraseBuilder.delete(0, phraseBuilder.length());
						builder.append(sentenceBuilder);
						sentenceBuilder.delete(0, sentenceBuilder.length());
						storedWordCounter = wordCounter;
						break;
				}
			}
			cursor.nextWord();
		}
		if (storedWordCounter < 4) {
			if (sentenceBuilder.length() == 0) {
				sentenceBuilder.append(phraseBuilder);
			}
			if (appendLineBreak) {
				builder.append("\n");
			}
			builder.append(sentenceBuilder);
		}
		return builder.toString();
	}
}
