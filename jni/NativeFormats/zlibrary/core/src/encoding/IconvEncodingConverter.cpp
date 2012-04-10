#include <ZLUnicodeUtil.h>
#include <ZLLogger.h>
#include "IconvEncodingConverter.h"
#include <iconv.h>

class IconvEncodingConverter : public ZLEncodingConverter {

private:
	IconvEncodingConverter(const std::string &encoding);

	std::string m_encoding;
	iconv_t m_converter;
public:
	~IconvEncodingConverter();
	std::string name() const;
	void convert(std::string &dst, const char *srcStart, const char *srcEnd);
	void reset();
	bool fillTable(int *map);

friend class IconvEncodingConverterProvider;
};

bool IconvEncodingConverterProvider::providesConverter(const std::string &encoding) {
	if (encoding.empty()) {
		return false;
	}

	iconv_t cd = iconv_open("utf-8", encoding.c_str());
	iconv_close(cd);
	return cd != 0;
}

shared_ptr<ZLEncodingConverter> IconvEncodingConverterProvider::createConverter(const std::string &encoding) {
	return new IconvEncodingConverter(encoding);
}

IconvEncodingConverter::IconvEncodingConverter(const std::string &encoding) {
	m_encoding = encoding;
	m_converter = iconv_open(encoding.c_str(), encoding.c_str());
}

IconvEncodingConverter::~IconvEncodingConverter() {
	iconv_close(m_converter);
	m_converter = 0;
}

std::string IconvEncodingConverter::name() const {
	return m_encoding;
}

void IconvEncodingConverter::convert(std::string &dst, const char *srcStart, const char *srcEnd) {
	if (m_converter == 0) {
		return;
	}

	int srcLen = srcEnd - srcStart;
	int outLen = srcLen * 2 + 1;

	char* inputBuffer = new char[srcLen + 1];
	memcpy(inputBuffer, srcStart, srcLen);
	inputBuffer[srcLen] = 0;
	char* outBuffer = new char[outLen];
	memset(outBuffer, 0, outLen);

	char **pin = &inputBuffer;
	char **pout = &outBuffer;

	LOGD("beg iconv %d", srcLen);
	size_t ret = iconv(m_converter, pin, (size_t *)&srcLen, pout, (size_t *)&outLen);
	LOGD("end iconv %d   %d    %d   %x", srcLen, outLen, ret, (int)outBuffer);
//	dst = outBuffer;
//	LOGD(dst.c_str());
//	delete[] inputBuffer;
//	delete[] outBuffer;
}

void IconvEncodingConverter::reset() {
}

bool IconvEncodingConverter::fillTable(int *map) {
	char in;
	std::string out;
	for (int i = 0; i < 256; ++i) {
		in = i;
		convert(out, &in, (&in)+1);
		reset();
		if (out.size() != 0) {
			ZLUnicodeUtil::Ucs4Char ch;
			ZLUnicodeUtil::firstChar(ch, out.data());
			map[i] = ch;
			out.clear();
		} else {
			map[i] = i;
		}
	}
	return true;
}
