#include <jni.h>

#include <ZLInputStream.h>
#include <ZLLanguageDetector.h>
#include <ZLImage.h>
#include <ZLLogger.h>

#include "FormatPlugin.h"

#include "../library/Book.h"

void FormatPlugin::detectEncodingAndLanguage(Book &book, ZLInputStream &stream) {
	std::string language = book.language();
	std::string encoding = book.encoding();

	if (!encoding.empty()) {
		return;
	}

	PluginCollection &collection = PluginCollection::Instance();
	if (encoding.empty()) {
		encoding = "utf-8";
	}
	if (collection.isLanguageAutoDetectEnabled() && stream.open()) {
		static const int BUFSIZE = 65536;
		char *buffer = new char[BUFSIZE];
		const size_t size = stream.read(buffer, BUFSIZE);
		stream.close();
		shared_ptr<ZLLanguageDetector::LanguageInfo> info = ZLLanguageDetector().findInfo(buffer, size);
		delete[] buffer;
		if (!info.isNull()) {
			if (!info->Language.empty()) {
				language = info->Language;
			}
			encoding = info->Encoding;
			if ((encoding == "us-ascii") || (encoding == "iso-8859-1")) {
				encoding = "windows-1252";
			}
		}
	}
	book.setEncoding(encoding);
	book.setLanguage(language);
}

void FormatPlugin::detectLanguage(Book &book, ZLInputStream &stream) {
	std::string language = book.language();
	if (!language.empty()) {
		return;
	}

	PluginCollection &collection = PluginCollection::Instance();
	if (collection.isLanguageAutoDetectEnabled() && stream.open()) {
		static const int BUFSIZE = 65536;
		char *buffer = new char[BUFSIZE];
		const size_t size = stream.read(buffer, BUFSIZE);
		stream.close();
		shared_ptr<ZLLanguageDetector::LanguageInfo> info =
			ZLLanguageDetector().findInfoForEncoding(book.encoding(), buffer, size, -20000);
		delete[] buffer;
		if (!info.isNull()) {
			if (!info->Language.empty()) {
				language = info->Language;
			}
		}
	}
	book.setLanguage(language);
}

const std::string &FormatPlugin::tryOpen(const ZLFile&) const {
	static const std::string EMPTY = "";
	return EMPTY;
}

