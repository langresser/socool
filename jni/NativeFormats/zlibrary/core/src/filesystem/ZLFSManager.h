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

#ifndef __ZLFSMANAGER_H__
#define __ZLFSMANAGER_H__

#include <string>
#include <map>

#include <ZLFileInfo.h>
#include <ZLFile.h>

class ZLDir;
class ZLFSDir;
class ZLInputStream;
class ZLOutputStream;

class ZLFSManager {

public:
	static void createInstance();
	static void deleteInstance();
	static ZLFSManager &Instance();
protected:
	static ZLFSManager *ourInstance;
	
protected:
	ZLFSManager();
	virtual ~ZLFSManager();
	
public:
	void normalize(std::string &path) const;
	std::string cacheDirectory() const;
	std::string normalizeUnixPath(const std::string &path) const;
protected:
	void normalizeRealPath(std::string &path) const;
	bool useNativeImplementation(const std::string &path) const;
	std::string mimeType(const std::string &path) const;
protected:
	std::string resolveSymlink(const std::string &path) const;
	ZLFSDir *createNewDirectory(const std::string &path) const;
	ZLFSDir *createPlainDirectory(const std::string &path) const;
	ZLInputStream *createPlainInputStream(const std::string &path) const;
	ZLOutputStream *createOutputStream(const std::string &path) const;
	bool removeFile(const std::string &path) const;

	ZLFileInfo fileInfo(const std::string &path) const;

	int findLastFileNameDelimiter(const std::string &path) const;
	int findArchiveFileNameDelimiter(const std::string &path) const;
	shared_ptr<ZLDir> rootDirectory() const;
	const std::string &rootDirectoryPath() const;
	std::string parentPath(const std::string &path) const;

	bool canRemoveFile(const std::string &path) const;

private:
	std::map<std::string,ZLFile::ArchiveType> myForcedFiles;

friend class ZLFile;
friend class ZLDir;
};

// 返回true代表读取的是实际目录资源，如sdcard；返回false代表读取的是apk包内资源
inline bool ZLFSManager::useNativeImplementation(const std::string &path) const {
	return path.length() > 0 && path[0] == '/';
}

inline ZLFSManager &ZLFSManager::Instance() { return *ourInstance; }
inline ZLFSManager::ZLFSManager() {}
inline ZLFSManager::~ZLFSManager() {}

#endif /* __ZLFSMANAGER_H__ */
