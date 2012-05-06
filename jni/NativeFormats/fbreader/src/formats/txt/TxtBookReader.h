#ifndef __TXTBOOKREADER_H__
#define __TXTBOOKREADER_H__

#include <stack>
#include <shared_ptr.h>
#include "PlainTextFormat.h"
#include "../../bookmodel/BookReader.h"

#include <ZLEncodingConverter.h>

class ZLInputStream;

enum {
	kAnsi,
	kUtf8,
	kUtf16le,
	kUtf16be,
};

class BookModel;

class TxtBookReader : public BookReader {

public:
	TxtBookReader(BookModel &model, const PlainTextFormat &format, const std::string &encoding);
	~TxtBookReader() {};

	void readDocument(ZLInputStream &stream);
protected:
	void startDocumentHandler();
	void endDocumentHandler();

	bool characterDataHandler(std::string &str);
	bool newLineHandler();


	int m_unicodeFlag;
	shared_ptr<ZLEncodingConverter> myConverter;
private:
	void internalEndParagraph();

private:
	const PlainTextFormat &myFormat;

	int myLineFeedCounter;
	bool myInsideContentsParagraph;
	bool myLastLineIsEmpty;
	bool myNewLine;
	int mySpaceCounter;
};

#endif /* __TXTBOOKREADER_H__ */
