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

#ifndef __ZLIBRARY_H__
#define __ZLIBRARY_H__

#include <string>

class ZLibrary {

public:
	static const std::string FileNameDelimiter;
	static const std::string PathDelimiter;
	static const std::string EndOfLine;
	static std::string Language();
	static const std::string &ZLibraryDirectory();
	static const std::string &ApplicationDirectory();

public:
	static bool init(int &argc, char **&argv);
	static void parseArguments(int &argc, char **&argv);
	static void shutdown();

private:
	static std::string ourZLibraryDirectory;
	static std::string ourApplicationName;
	static std::string ourApplicationDirectory;
public:
	static void initApplication(const std::string &name);
	static ZLibrary *Instance;
private:
	ZLibrary();

friend class ZLApplicationBase;
};

inline const std::string &ZLibrary::ZLibraryDirectory() { return ourZLibraryDirectory; }
inline const std::string &ZLibrary::ApplicationDirectory() { return ourApplicationDirectory; }

extern "C" {
	void initLibrary();
}

#endif /* __ZLIBRARY_H__ */
