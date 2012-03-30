#ifndef __ENCODEDTEXTREADER_H__
#define __ENCODEDTEXTREADER_H__

#include <string>

#include <ZLEncodingConverter.h>

class EncodedTextReader {

protected:
	EncodedTextReader(const std::string &encoding);
	virtual ~EncodedTextReader();

protected:
	shared_ptr<ZLEncodingConverter> myConverter;
};

#endif /* __ENCODEDTEXTREADER_H__ */
