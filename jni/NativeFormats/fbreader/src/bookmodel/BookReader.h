#ifndef __BOOKREADER_H__
#define __BOOKREADER_H__

#include <vector>
#include <stack>
#include <string>

#include <ZLTextParagraph.h>

#include "FBHyperlinkType.h"
#include "FBTextKind.h"

class BookModel;
class ZLTextModel;
class ZLInputStream;
class ZLCachedMemoryAllocator;

class BookReader {

public:
	BookReader(BookModel &model);
	virtual ~BookReader();

	void setMainTextModel();
	void unsetTextModel();

	void insertEndOfSectionParagraph();
	void insertEndOfTextParagraph();

	void pushKind(FBTextKind kind);
	bool popKind();
	bool isKindStackEmpty() const;

	void beginParagraph(ZLTextParagraph::Kind kind = ZLTextParagraph::TEXT_PARAGRAPH);
	void endParagraph();
	bool paragraphIsOpen() const;
	void addControl(FBTextKind kind, bool start);
	void addControl(const ZLTextStyleEntry &entry);
	void addHyperlinkControl(FBTextKind kind, const std::string &label);
	void addHyperlinkLabel(const std::string &label);
	void addHyperlinkLabel(const std::string &label, int paragraphNumber);
	void addFixedHSpace(unsigned char length);

	void addImageReference(const std::string &id, short vOffset, bool isCover);
	void addImage(const std::string &id, shared_ptr<const ZLImage> image);

	void beginContentsParagraph(int referenceNumber = -1);
	void endContentsParagraph();
	bool contentsParagraphIsOpen() const;
	void setReference(size_t contentsParagraphNumber, int referenceNumber);

	void addData(const std::string &data);
	void addContentsData(const std::string &data);

	void enterTitle() { myInsideTitle = true; }
	void exitTitle() { myInsideTitle = false; }

	const BookModel &model() const { return myModel; }

	void reset();

private:
	void insertEndParagraph(ZLTextParagraph::Kind kind);
	void flushTextBufferToParagraph();

private:
	BookModel &myModel;
	shared_ptr<ZLTextModel> myCurrentTextModel;

	std::vector<FBTextKind> myKindStack;

	bool myTextParagraphExists;
	bool myContentsParagraphExists;
	std::stack<ZLTextTreeParagraph*> myTOCStack;
	bool myLastTOCParagraphIsEmpty;

	bool mySectionContainsRegularContents;
	bool myInsideTitle;

	std::vector<std::string> myBuffer;
	std::vector<std::string> myContentsBuffer;

	std::string myHyperlinkReference;
	FBHyperlinkType myHyperlinkType;
	FBTextKind myHyperlinkKind;

	shared_ptr<ZLCachedMemoryAllocator> myFootnotesAllocator;
};

inline bool BookReader::paragraphIsOpen() const {
	return myTextParagraphExists;
}

inline bool BookReader::contentsParagraphIsOpen() const {
	return myContentsParagraphExists;
}

#endif /* __BOOKREADER_H__ */
