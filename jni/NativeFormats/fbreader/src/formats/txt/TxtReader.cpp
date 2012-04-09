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

#include <cctype>

#include <ZLInputStream.h>

#include "TxtReader.h"

#include <android/log.h>
#define LOG_TAG "show infomation"
#define LOGW(a )  __android_log_write(ANDROID_LOG_WARN,LOG_TAG,a)
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)

TxtReader::TxtReader(const std::string &encoding) : EncodedTextReader(encoding) {
}

TxtReader::~TxtReader() {
}

void TxtReader::readDocument(ZLInputStream &stream) {
	if (!stream.open()) {
		return;
	}

	startDocumentHandler();

	const size_t BUFSIZE = 2048;
	char *buffer = new char[BUFSIZE + 1];
	std::string str;
	std::string inputBuffer;
	size_t length;
	do {
		length = stream.read(buffer, BUFSIZE);
		buffer[length] = 0;

		inputBuffer += buffer;
	} while (length == BUFSIZE);
	delete[] buffer;
	stream.close();

	int maxLength = inputBuffer.size();
	int parBegin = 0;
	for (int i = parBegin; i < maxLength; ++i) {
		char c = inputBuffer[i];
//TODO: 支持多种换行符
		if (c == '\n' || c == '\r') {
			bool skipNewLine = false;
			if (c == '\r' && (i + 1) != maxLength && inputBuffer[i + 1] == '\n') {
				skipNewLine = true;
				inputBuffer[i] = '\n';
			}
			if (parBegin != i) {
				str.erase();
				myConverter->convert(str, inputBuffer.c_str() + parBegin, inputBuffer.c_str() + i + 1);
				LOGD(str.c_str());
				characterDataHandler(str);
			}
			// 跳过'\n'
			if (skipNewLine) {
				++i;
			}
			parBegin = i + 1;
			newLineHandler();
		} else if (isspace(c)) {
			if (c != '\t') {
				inputBuffer[i] = ' ';
			}
		} else {
		}
	}
	if (parBegin != maxLength) {
		str.erase();
		myConverter->convert(str, inputBuffer.c_str() + parBegin, inputBuffer.c_str() + maxLength);
		characterDataHandler(str);
	}
//	printf(str.c_str());

	endDocumentHandler();
}
