#ifndef __TXTREADER_H__
#define __TXTREADER_H__

#include <string>

#include <ZLEncodingConverter.h>

class ZLInputStream;

enum {
	kAnsi,
	kUtf8,
	kUtf16le,
	kUtf16be,
};

class TxtReader {

public:
	void readDocument(ZLInputStream &stream);

protected:
	TxtReader(const std::string &encoding);
	virtual ~TxtReader();

protected:
	virtual void startDocumentHandler() = 0;
	virtual void endDocumentHandler() = 0;

	virtual bool characterDataHandler(std::string &str) = 0;
	virtual bool newLineHandler() = 0;

	int m_unicodeFlag;
	shared_ptr<ZLEncodingConverter> myConverter;
};

#endif /* __TXTREADER_H__ */
