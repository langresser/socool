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

package org.socool.screader.library;

import org.socool.screader.FBTree;
import org.socool.zlibrary.image.ZLImage;

public class BookTree extends LibraryTree {
	public final Book Book;
	private final boolean myShowAuthors;

	public BookTree(Book book, boolean showAuthors) {
		Book = book;
		myShowAuthors = showAuthors;
	}

	public BookTree(LibraryTree parent, Book book, boolean showAuthors) {
		super(parent);
		Book = book;
		myShowAuthors = showAuthors;
	}

	public BookTree(LibraryTree parent, Book book, boolean showAuthors, int position) {
		super(parent, position);
		Book = book;
		myShowAuthors = showAuthors;
	}

	@Override
	public String getName() {
		return Book.myTitle;
	}

	@Override
	public Book getBook() {
		return Book;
	}

	@Override
	protected String getStringId() {
		return "@BookTree " + getName();
	}

	@Override
	public String getSummary() {
		if (!myShowAuthors) {
			return super.getSummary();
		}

		return Book.authors();
	}

	@Override
	protected ZLImage createCover() {
		return Book.getCover();
	}

	@Override
	public boolean containsBook(Book book) {
		return book != null && book.equals(Book);
	}

	@Override
	protected String getSortKey() {
		return "BSK:" + super.getSortKey();
	}

	@Override
	public int compareTo(FBTree tree) {
		final int cmp = super.compareTo(tree);
		if (cmp == 0 && tree instanceof BookTree) {
			final Book b = ((BookTree)tree).Book;
			if (Book != null && b != null) {
				return Book.m_filePath.compareTo(b.m_filePath);
			}
		}
		return cmp;
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) {
			return true;
		}
		if (!(object instanceof BookTree)) {
			return false;
		}
		return Book.equals(((BookTree)object).Book);
	}
}
