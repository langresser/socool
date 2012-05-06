/*
 * Copyright (C) 2004-2012 Geometer Plus <contact@geometerplus.com>
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

#ifndef __ZLINPUTSTREAM_H__
#define __ZLINPUTSTREAM_H__

#include <string>
#include <map>
#include <shared_ptr.h>

class ZLUserData {

public:
	virtual ~ZLUserData() {};
};

class ZLInputStream{

protected:
	ZLInputStream();

public:
	virtual ~ZLInputStream();
	virtual bool open() = 0;
	virtual size_t read(char *buffer, size_t maxSize) = 0;
	virtual void close() = 0;

	virtual void seek(int offset, bool absoluteOffset) = 0;
	virtual size_t offset() const = 0;
	virtual size_t sizeOfOpened() = 0;

private:
	// disable copying
	ZLInputStream(const ZLInputStream&);
	const ZLInputStream &operator = (const ZLInputStream&);

public:
	inline void addUserData(const std::string &key, shared_ptr<ZLUserData> data);
	inline void removeUserData(const std::string &key);
	inline shared_ptr<ZLUserData> getUserData(const std::string &key) const;

private:
	std::map<std::string,shared_ptr<ZLUserData> > myDataMap;
};

inline void ZLInputStream::addUserData(const std::string &key, shared_ptr<ZLUserData> data) {
	myDataMap[key] = data;
}

inline void ZLInputStream::removeUserData(const std::string &key) {
	myDataMap.erase(key);
}

inline shared_ptr<ZLUserData> ZLInputStream::getUserData(const std::string &key) const {
	std::map<std::string,shared_ptr<ZLUserData> >::const_iterator it = myDataMap.find(key);
	return (it != myDataMap.end()) ? it->second : 0;
}

class ZLInputStreamDecorator : public ZLInputStream {

public:
	ZLInputStreamDecorator(shared_ptr<ZLInputStream> decoratee);

private:
	bool open();
	size_t read(char *buffer, size_t maxSize);
	void close();

	void seek(int offset, bool absoluteOffset);
	size_t offset() const;
	size_t sizeOfOpened();

private:
	shared_ptr<ZLInputStream> myBaseStream;
	size_t myBaseOffset;
};

inline ZLInputStream::ZLInputStream() {}
inline ZLInputStream::~ZLInputStream() {}

#endif /* __ZLINPUTSTREAM_H__ */
