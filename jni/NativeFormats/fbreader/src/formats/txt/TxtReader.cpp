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
#include <ZLLogger.h>

#include "TxtReader.h"

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
	size_t currentLen = 0;
	size_t currentBufferSize = BUFSIZE * 2 + 1;
	char* inputBuffer = new char[currentBufferSize];
	memset(inputBuffer, 0, currentBufferSize);

	size_t length;
	do {
		length = stream.read(buffer, BUFSIZE);
		buffer[length] = 0;

		if (currentLen + length >= currentBufferSize) {
			currentBufferSize = currentBufferSize * 2 + 1;
			char* temp = new char[currentBufferSize];
			memcpy(temp, inputBuffer, currentLen);
			delete[] inputBuffer;
			inputBuffer = temp;
		}

		memcpy(inputBuffer + currentLen, buffer, length);
		currentLen += length;
	} while (length == BUFSIZE);
	delete[] buffer;
	stream.close();

	int maxLength = currentLen;
	int parBegin = 0;
	for (int i = parBegin; i < maxLength; ++i) {
		char c = inputBuffer[i];
		if (c == '\n' || c == '\r') {
			bool skipNewLine = false;
			if (c == '\r' && (i + 1) != maxLength && inputBuffer[i + 1] == '\n') {
				skipNewLine = true;
				inputBuffer[i] = '\n';
			}
			if (parBegin != i) {
				str.erase();
				myConverter->convert(str, inputBuffer + parBegin, inputBuffer + i + 1);
				characterDataHandler(str);
			}
			// 跳过'\n'(\r\n的情况)
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
		myConverter->convert(str, inputBuffer + parBegin, inputBuffer + maxLength);
		characterDataHandler(str);
	}

	delete[] inputBuffer;
	endDocumentHandler();
}
