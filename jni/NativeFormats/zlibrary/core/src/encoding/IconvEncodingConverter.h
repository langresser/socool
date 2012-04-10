#pragma once

#include "ZLEncodingConverter.h"
#include "ZLEncodingConverterProvider.h"

class IconvEncodingConverterProvider : public ZLEncodingConverterProvider {

public:
	bool providesConverter(const std::string &encoding);
	shared_ptr<ZLEncodingConverter> createConverter(const std::string &encoding);
};
