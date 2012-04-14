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

	iconv_t cd = iconv_open("utf-8//TRANSLIT", encoding.c_str());
	iconv_close(cd);
	return cd != 0;
}

shared_ptr<ZLEncodingConverter> IconvEncodingConverterProvider::createConverter(const std::string &encoding) {
	return new IconvEncodingConverter(encoding);
}

IconvEncodingConverter::IconvEncodingConverter(const std::string &encoding) {
	LOGD(encoding.c_str());
	m_encoding = encoding;

	// 直接传utf-16，如果没有bom信息的话，会当做be来读，这里改为le
	if (strcasecmp(m_encoding.c_str(), "utf-16") == 0) {
		m_encoding += "le";
	}

	m_converter = iconv_open("utf-8//TRANSLIT", m_encoding.c_str());
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
	int outLen = srcLen * 2;

	if (m_encoding == "utf-8") {
		char* outBuffer = new char[srcLen + 1];
		memcpy(outBuffer, srcStart, srcLen);
		outBuffer[srcLen] = 0;
		dst = outBuffer;
		return;
	}

	if (outLen < 1024) {
		outLen = 1024;
		static char s_outBuffer[1024] = {0};
		memset(s_outBuffer, 0, outLen);

		// iconv会写tempOutBuffer指针，最终其会指向转换未完成的部分
		char* tempOutBuffer = s_outBuffer;
		size_t ret = iconv(m_converter, (const char**)&srcStart, (size_t *)&srcLen, &tempOutBuffer, (size_t *)&outLen);
		dst = s_outBuffer;
//		LOGD(dst.c_str());
	} else {
		char* outBuffer = new char[outLen];
		memset(outBuffer, 0, outLen);

		// iconv会写tempOutBuffer指针，最终其会指向转换未完成的部分
		char* tempOutBuffer = outBuffer;
		size_t ret = iconv(m_converter, (const char**)&srcStart, (size_t *)&srcLen, &tempOutBuffer, (size_t *)&outLen);
		dst = outBuffer;
	//	LOGD(dst.c_str());
		delete[] outBuffer;
	}

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
