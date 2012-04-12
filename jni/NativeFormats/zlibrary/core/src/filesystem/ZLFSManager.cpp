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

#include <ZLibrary.h>
#include <ZLFileUtil.h>
#include <ZLStringUtil.h>
#include <AndroidUtil.h>
#include <JniEnvelope.h>
#include <ZLLogger.h>

#include <sys/stat.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>

#include <set>

#include "ZLFSManager.h"
#include "ZLFSDir.h"
#include "ZLUnixFSDir.h"
#include "ZLUnixFileInputStream.h"
#include "ZLUnixFileOutputStream.h"
#include "JavaInputStream.h"
#include "JavaFSDir.h"

//static std::string getPwdDir() {
//	char *pwd = getenv("PWD");
//	return (pwd != 0) ? pwd : "";
//}
//
//static std::string getHomeDir() {
//	char *home = getenv("HOME");
//	return (home != 0) ? home : "";
//}

ZLFSManager *ZLFSManager::ourInstance = 0;

void ZLFSManager::createInstance() {
	ourInstance = new ZLFSManager;
}

void ZLFSManager::deleteInstance() {
	if (ourInstance != 0) {
		delete ourInstance;
		ourInstance = 0;
	}
}

int ZLFSManager::findLastFileNameDelimiter(const std::string &path) const {
	int index = findArchiveFileNameDelimiter(path);
	if (index == -1) {
		index = path.rfind(ZLibrary::FileNameDelimiter);
	}
	return index;
}

std::string ZLFSDir::delimiter() const {
	return ZLibrary::FileNameDelimiter;
}

void ZLFSManager::normalize(std::string &path) const {
	int index = findArchiveFileNameDelimiter(path);
	if (index == -1) {
		normalizeRealPath(path);
	} else {
		std::string realPath = path.substr(0, index);
		normalizeRealPath(realPath);
		path = realPath + ':' + ZLFileUtil::normalizeUnixPath(path.substr(index + 1));
	}
}

std::string ZLFSManager::mimeType(const std::string &path) const {
	return std::string();
}

ZLFileInfo ZLFSManager::fileInfo(const std::string &path) const {
	if (!useNativeImplementation(path)) {
		ZLFileInfo info;

		JNIEnv *env = AndroidUtil::getEnv();
		jobject javaFile = AndroidUtil::createJavaFile(env, path);
		if (javaFile == 0) {
			return info;
		}

		info.IsDirectory = AndroidUtil::Method_ZLFile_isDirectory->call(javaFile);
		const jboolean exists = AndroidUtil::Method_ZLFile_exists->call(javaFile);
		if (exists) {
			info.Exists = true;
			info.Size = AndroidUtil::Method_ZLFile_size->call(javaFile);
		}
		env->DeleteLocalRef(javaFile);

		return info;
	}

	ZLFileInfo info;
	struct stat fileStat;
	info.Exists = stat(path.c_str(), &fileStat) == 0;
	if (info.Exists) {
		info.Size = fileStat.st_size;
		info.IsDirectory = S_ISDIR(fileStat.st_mode);
	}
	return info;
}

std::string ZLFSManager::resolveSymlink(const std::string &path) const {
	if (!useNativeImplementation(path)) {
		return path;
	}

	std::set<std::string> names;
	std::string current = path;
	for (int i = 0; i < 256; ++i) {
		names.insert(current);

		std::string buffer(2048, '\0');
		int len = readlink(current.c_str(), (char*)buffer.data(), 2048);
		if ((len == 2048) || (len <= 0)) {
			return current;
		}
		buffer.erase(len);
		if (buffer[0] != '/') {
			buffer = parentPath(current) + '/' + buffer;
		}
		normalizeRealPath(buffer);
		if (names.find(buffer) != names.end()) {
			return buffer;
		}
		current = buffer;
	}
	return "";
}

void ZLFSManager::normalizeRealPath(std::string &path) const {
	if (path.empty()) {
		return;
	} else if (path[0] == '~') {
		if (path.length() == 1 || path[1] == '/') {
			path.erase(0, 1);
		}
	}
	int last = path.length() - 1;
	while ((last > 0) && (path[last] == '/')) {
		--last;
	}
	if (last < (int)path.length() - 1) {
		path = path.substr(0, last + 1);
	}

	int index;
	while ((index = path.find("/../")) != -1) {
		int prevIndex = std::max((int)path.rfind('/', index - 1), 0);
		path.erase(prevIndex, index + 3 - prevIndex);
	}
	int len = path.length();
	if ((len >= 3) && (path.substr(len - 3) == "/..")) {
		int prevIndex = std::max((int)path.rfind('/', len - 4), 0);
		path.erase(prevIndex);
	}
	while ((index = path.find("/./")) != -1) {
		path.erase(index, 2);
	}
	while (path.length() >= 2 &&
				 path.substr(path.length() - 2) == "/.") {
		path.erase(path.length() - 2);
	}
	while ((index = path.find("//")) != -1) {
		path.erase(index, 1);
	}
}

ZLFSDir *ZLFSManager::createNewDirectory(const std::string &path) const {
	if (!useNativeImplementation(path)) {
		return 0;
	}

	std::vector<std::string> subpaths;
	std::string current = path;

	while (current.length() > 1) {
		struct stat fileStat;
		if (stat(current.c_str(), &fileStat) == 0) {
			if (!S_ISDIR(fileStat.st_mode)) {
				return 0;
			}
			break;
		} else {
			subpaths.push_back(current);
			int index = current.rfind('/');
			if (index == -1) {
				return 0;
			}
			current.erase(index);
		}
	}

	for (int i = subpaths.size() - 1; i >= 0; --i) {
		if (mkdir(subpaths[i].c_str(), 0x1FF) != 0) {
			return 0;
		}
	}
	return createPlainDirectory(path);
}

ZLFSDir *ZLFSManager::createPlainDirectory(const std::string &path) const {
	if (!useNativeImplementation(path)) {
		return new JavaFSDir(path);
	}

	return new ZLUnixFSDir(path);
}

ZLInputStream *ZLFSManager::createPlainInputStream(const std::string &path) const {
	if (!useNativeImplementation(path)) {
		 return new JavaInputStream(path);
	}

	return new ZLUnixFileInputStream(path);
}

ZLOutputStream *ZLFSManager::createOutputStream(const std::string &path) const {
	return new ZLUnixFileOutputStream(path);
}

bool ZLFSManager::removeFile(const std::string &path) const {
	if (!useNativeImplementation(path)) {
		return false;
	}

	return unlink(path.c_str()) == 0;
}

int ZLFSManager::findArchiveFileNameDelimiter(const std::string &path) const {
	return path.rfind(':');
}

static const std::string RootPath = "/";

shared_ptr<ZLDir> ZLFSManager::rootDirectory() const {
	return createPlainDirectory(RootPath);
}

const std::string &ZLFSManager::rootDirectoryPath() const {
	return RootPath;
}

std::string ZLFSManager::parentPath(const std::string &path) const {
	if (path == RootPath) {
		return path;
	}
	int index = findLastFileNameDelimiter(path);
	return (index <= 0) ? RootPath : path.substr(0, index);
}

bool ZLFSManager::canRemoveFile(const std::string &path) const {
	if (!useNativeImplementation(path)) {
		return false;
	}

	return access(parentPath(path).c_str(), W_OK) == 0;
}
