#ifndef __TXTPLUGIN_H__
#define __TXTPLUGIN_H__

#include "../FormatPlugin.h"

class TxtPlugin : public FormatPlugin {

public:
	~TxtPlugin() {};
	bool providesMetaInfo() const {return false;};
	const std::string supportedFileType() const {return "plain text";};
	bool readMetaInfo(Book &book) const {return true;};
	bool readLanguageAndEncoding(Book &book) const;
	bool readModel(BookModel &model) const;
};

#endif /* __TXTPLUGIN_H__ */
