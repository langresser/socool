#ifndef __PLAINTEXTFORMAT_H__
#define __PLAINTEXTFORMAT_H__

#include <ZLInputStream.h>

#include "../FormatPlugin.h"

class PlainTextFormat {

public:
	enum ParagraphBreakType {
		BREAK_PARAGRAPH_AT_NEW_LINE = 1,
		BREAK_PARAGRAPH_AT_EMPTY_LINE = 2,
		BREAK_PARAGRAPH_AT_LINE_WITH_INDENT = 4,
	};

	PlainTextFormat(const ZLFile &file);
	~PlainTextFormat() {}

	bool initialized() const { return myInitialized; }
	int breakType() const { return myBreakType; }
	int ignoredIndent() const { return myIgnoredIndent; }
	int emptyLinesBeforeNewSection() const { return myEmptyLinesBeforeNewSection; }
	bool createContentsTable() const { return myCreateContentsTable; }

private:
	bool myInitialized;
	int myBreakType;
	int myIgnoredIndent;
	int myEmptyLinesBeforeNewSection;
	bool myCreateContentsTable;

friend class PlainTextInfoPage;
friend class PlainTextFormatDetector;
};

class PlainTextFormatDetector {

public:
	PlainTextFormatDetector() {}
	~PlainTextFormatDetector() {}

	void detect(ZLInputStream &stream, PlainTextFormat &format);
};
#endif /* __PLAINTEXTFORMAT_H__ */
