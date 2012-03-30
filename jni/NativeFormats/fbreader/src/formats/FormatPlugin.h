#ifndef __FORMATPLUGIN_H__
#define __FORMATPLUGIN_H__

#include <jni.h>

#include <string>
#include <vector>

#include <shared_ptr.h>

class Book;
class BookModel;
//class ZLOptionsDialog;
//class ZLOptionsDialogTab;
class ZLFile;
class ZLInputStream;
class ZLImage;

/*class FormatInfoPage {

protected:
	FormatInfoPage();

public:
	virtual ~FormatInfoPage();
};*/

class FormatPlugin {

protected:
	FormatPlugin();

public:
	virtual ~FormatPlugin();

	virtual bool providesMetaInfo() const = 0;
	virtual const std::string supportedFileType() const = 0;
	//virtual FormatInfoPage *createInfoPage(ZLOptionsDialog &dialog, const ZLFile &file);

	virtual const std::string &tryOpen(const ZLFile &file) const;
	virtual bool readMetaInfo(Book &book) const = 0;
	virtual bool readLanguageAndEncoding(Book &book) const = 0;
	virtual bool readModel(BookModel &model) const = 0;
	virtual shared_ptr<ZLImage> coverImage(const ZLFile &file) const;

protected:
	static void detectEncodingAndLanguage(Book &book, ZLInputStream &stream);
	static void detectLanguage(Book &book, ZLInputStream &stream);
};

class PluginCollection {

public:
	static PluginCollection &Instance();
	static void deleteInstance();

private:
	PluginCollection();

public:
	~PluginCollection();

public:
	std::vector<shared_ptr<FormatPlugin> > plugins() const;
	shared_ptr<FormatPlugin> pluginByType(const std::string &fileType) const;

	bool isLanguageAutoDetectEnabled();

private:
	static PluginCollection *ourInstance;

	jobject myJavaInstance;

	std::vector<shared_ptr<FormatPlugin> > myPlugins;
};

//inline FormatInfoPage::FormatInfoPage() {}
//inline FormatInfoPage::~FormatInfoPage() {}
inline FormatPlugin::FormatPlugin() {}
inline FormatPlugin::~FormatPlugin() {}
//inline FormatInfoPage *FormatPlugin::createInfoPage(ZLOptionsDialog&, const ZLFile&) { return 0; }

inline std::vector<shared_ptr<FormatPlugin> > PluginCollection::plugins() const {
	return myPlugins;
}

#endif /* __FORMATPLUGIN_H__ */
