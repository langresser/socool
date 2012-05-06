#ifndef __TXTBOOKREADER_H__
#define __TXTBOOKREADER_H__

#include <stack>

#include "TxtReader.h"
#include "PlainTextFormat.h"
#include "../../bookmodel/BookReader.h"

class BookModel;

class TxtBookReader : public TxtReader, public BookReader {

public:
	TxtBookReader(BookModel &model, const PlainTextFormat &format, const std::string &encoding);
	~TxtBookReader() {};

protected:
	void startDocumentHandler();
	void endDocumentHandler();

	bool characterDataHandler(std::string &str);
	bool newLineHandler();

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
