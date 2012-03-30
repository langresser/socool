#include "EncodedTextReader.h"

EncodedTextReader::EncodedTextReader(const std::string &encoding) {
	ZLEncodingCollection &collection = ZLEncodingCollection::Instance();
	myConverter = collection.converter(encoding);
	if (myConverter.isNull()) {
		myConverter = collection.defaultConverter();
	}
}

EncodedTextReader::~EncodedTextReader() {
}
