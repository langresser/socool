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

#ifndef __BOOK_H__
#define __BOOK_H__

#include <jni.h>

#include <string>
#include <set>
#include <vector>

#include <shared_ptr.h>

#include <ZLFile.h>

class Book {

public:
	static const std::string AutoEncoding;

public:
	static shared_ptr<Book> createBook(
		const ZLFile &file,
		int id,
		const std::string &encoding,
		const std::string &language,
		const std::string &title
	);

	static shared_ptr<Book> loadFromFile(const ZLFile &file);
	static shared_ptr<Book> loadFromJavaBook(JNIEnv *env, jobject javaBook);

public:
	Book(const ZLFile &file, int id);
	~Book();

public: // unmodifiable book methods
	const std::string &title() const;
	const ZLFile &file() const;
	const std::string &language() const;
	const std::string &encoding() const;

	const std::string &authors() const;

public: // modifiable book methods
	void setTitle(const std::string &title);
	void setLanguage(const std::string &language);
	void setEncoding(const std::string &encoding);

public:
	int bookId() const;
	void setBookId(int bookId);

private:
	int myBookId;

	const ZLFile myFile;
	std::string myTitle;
	std::string myLanguage;
	std::string myEncoding;
	std::string m_bookAuthor;

private: // disable copying
	Book(const Book &);
	const Book &operator = (const Book &);
};

class BookComparator {
};

class BookByFileNameComparator {

public:
	bool operator () (
		const shared_ptr<Book> book0,
		const shared_ptr<Book> book1
	) const;
};

inline const std::string &Book::title() const { return myTitle; }
inline const ZLFile &Book::file() const { return myFile; }
inline const std::string &Book::language() const { return myLanguage; }
inline const std::string &Book::encoding() const { return myEncoding; }
inline const std::string &Book::authors() const { return m_bookAuthor; }

inline int Book::bookId() const { return myBookId; }
inline void Book::setBookId(int bookId) { myBookId = bookId; }

typedef std::vector<shared_ptr<Book> > BookList;
typedef std::set<shared_ptr<Book>,BookByFileNameComparator> BookSet;
#endif /* __BOOK_H__ */
