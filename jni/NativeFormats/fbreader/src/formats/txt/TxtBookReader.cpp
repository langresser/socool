#include <cctype>
#include <ZLLogger.h>
#include "TxtBookReader.h"
#include "../../bookmodel/BookModel.h"

TxtBookReader::TxtBookReader(BookModel &model, const PlainTextFormat &format, const std::string &encoding) : BookReader(model), myFormat(format) {
	ZLEncodingCollection &collection = ZLEncodingCollection::Instance();
	myConverter = collection.converter(encoding);
	if (myConverter.isNull()) {
		myConverter = collection.defaultConverter();
	}

	if (strcasecmp(encoding.c_str(), "utf-8") == 0) {
		m_unicodeFlag = kUtf8;
	} else if (strcasecmp(encoding.c_str(), "utf-16") == 0) {
		m_unicodeFlag = kUtf16le;
	} else if (strcasecmp(encoding.c_str(), "utf-16be") == 0) {
		m_unicodeFlag = kUtf16be;
	} else if (strcasecmp(encoding.c_str(), "utf-16le") == 0) {
		m_unicodeFlag = kUtf16le;
	} else {
		m_unicodeFlag = kAnsi;
	}
}

void TxtBookReader::internalEndParagraph() {
	if (!myLastLineIsEmpty) {
		//myLineFeedCounter = 0;
		myLineFeedCounter = -1; /* Fixed by Hatred: zero value was break LINE INDENT formater -
		                           second line print with indent like new paragraf */
	}
	myLastLineIsEmpty = true;
	endParagraph();
}

bool TxtBookReader::characterDataHandler(std::string &str) {
	const char *ptr = str.data();
	const char *end = ptr + str.length();
	for (; ptr != end; ++ptr) {
		if (isspace((unsigned char)*ptr)) {
			if (*ptr != '\t') {
				++mySpaceCounter;
			} else {
				mySpaceCounter += myFormat.ignoredIndent() + 1; // TODO: implement single option in PlainTextFormat
			}
		} else {
			myLastLineIsEmpty = false;
			break;
		}
	}
	if (ptr != end) {
		if ((myFormat.breakType() & PlainTextFormat::BREAK_PARAGRAPH_AT_LINE_WITH_INDENT) &&
				myNewLine && (mySpaceCounter > myFormat.ignoredIndent())) {
			internalEndParagraph();
			beginParagraph();
		}
		addData(str);
		if (myInsideContentsParagraph) {
			addContentsData(str);
		}
		myNewLine = false;
	}
	return true;
}

bool TxtBookReader::newLineHandler() {
	if (!myLastLineIsEmpty) {
		myLineFeedCounter = -1;
	}
	myLastLineIsEmpty = true;
	++myLineFeedCounter;
	myNewLine = true;
	mySpaceCounter = 0;
	bool paragraphBreak =
		(myFormat.breakType() & PlainTextFormat::BREAK_PARAGRAPH_AT_NEW_LINE) ||
		((myFormat.breakType() & PlainTextFormat::BREAK_PARAGRAPH_AT_EMPTY_LINE) && (myLineFeedCounter > 0));

	if (myFormat.createContentsTable()) {
		if (!myInsideContentsParagraph && (myLineFeedCounter == myFormat.emptyLinesBeforeNewSection())) {
			myInsideContentsParagraph = true;
			internalEndParagraph();
			insertEndOfSectionParagraph();
			beginContentsParagraph();
			enterTitle();
			pushKind(SECTION_TITLE);
			beginParagraph();
			paragraphBreak = false;
		}
		if (myInsideContentsParagraph && (myLineFeedCounter == 1)) {
			exitTitle();
			endContentsParagraph();
			popKind();
			myInsideContentsParagraph = false;
			paragraphBreak = true;
		}
	}

	if (paragraphBreak) {
		internalEndParagraph();
		beginParagraph();
	}

	return true;
}

void TxtBookReader::startDocumentHandler() {
	setMainTextModel();
	pushKind(REGULAR);
	beginParagraph();
	myLineFeedCounter = 0;
	myInsideContentsParagraph = false;
	enterTitle();
	myLastLineIsEmpty = true;
	myNewLine = true;
	mySpaceCounter = 0;
}

void TxtBookReader::endDocumentHandler() {
	internalEndParagraph();
}


void TxtBookReader::readDocument(ZLInputStream &stream) {
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
	if (m_unicodeFlag == kUtf16le) {
		for (int i = parBegin; i < maxLength; ++i) {
			char c = inputBuffer[i];
			char cn = -1;
			if ((i + 1) < maxLength) {
				cn = inputBuffer[i + 1];
			}
			if ((c == '\n' || c == '\r') && cn == 0) {
				bool newPar = false;
				bool skipNewLine = false;
				if (c == '\r' && cn == 0
						&& (i + 3) < maxLength
						&& inputBuffer[i + 2] == '\n'
						&& inputBuffer[i + 3] == 0) {
					skipNewLine = true;
					inputBuffer[i] = '\n';
				}
				if (parBegin != i) {
					str.erase();
					myConverter->convert(str, inputBuffer + parBegin, inputBuffer + i + 2);
					characterDataHandler(str);
				}
				// 跳过'\n'(\r\n的情况)
				if (skipNewLine) {
					i += 3; // 0d 00 0a 00
				}
				parBegin = i + 1;
				newLineHandler();
			} else if (isspace(c) && cn == 0) {
				if (c != '\t') {
					inputBuffer[i] = ' ';
				}
			}
		}
	} else if (m_unicodeFlag == kUtf16be) {
		for (int i = parBegin; i < maxLength; ++i) {
			char c = inputBuffer[i];
			char cp = -1;
			if (i - 1 >= 0) {
				cp = inputBuffer[i - 1];
			}
			if ((c == '\n' || c == '\r') && cp == 0) {
				bool skipNewLine = false;
				if (c == '\r' && cp == 0
						&& (i + 2) < maxLength
						&& inputBuffer[i + 1] == 0
						&& inputBuffer[i + 2] == '\n') {
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
					i += 2; // 00 0d 00 0a
				}
				parBegin = i + 1;
				newLineHandler();
			} else if (isspace(c) && cp == 0) {
				if (c != '\t') {
					inputBuffer[i] = ' ';
				}
			}
		}
	} else {
		for (int i = parBegin; i < maxLength; ++i) {
			char c = inputBuffer[i];
			if (c == '\n' || c == '\r') {
				bool newPar = false;
				bool skipNewLine = false;
				if (c == '\r' && (i + 1) != maxLength && inputBuffer[i + 1] == '\n') {
					skipNewLine = true;
					inputBuffer[i] = '\n';
				}
				if (parBegin != i) {
					str.erase();
					myConverter->convert(str, inputBuffer + parBegin, inputBuffer + i + 1);
//					LOGD(str.c_str());
					characterDataHandler(str);
				}
				// 跳过'\n'(\r\n的情况)
				if (skipNewLine) {
					++i; // 0d 0a
				}
				parBegin = i + 1;
				newLineHandler();
			} else if (isspace(c)) {
				if (c != '\t') {
					inputBuffer[i] = ' ';
				}
			}
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
