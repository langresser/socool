#include <AndroidUtil.h>

#include <ZLibrary.h>
#include <ZLFile.h>

#include "FormatPlugin.h"

#include "../library/Book.h"

//#include "fb2/FB2Plugin.h"
////#include "docbook/DocBookPlugin.h"
//#include "html/HtmlPlugin.h"
#include "txt/TxtPlugin.h"
//#include "pdb/PdbPlugin.h"
//#include "tcr/TcrPlugin.h"
//#include "oeb/OEBPlugin.h"
//#include "chm/CHMPlugin.h"
#include "rtf/RtfPlugin.h"
//#include "openreader/OpenReaderPlugin.h"
////#include "pdf/PdfPlugin.h"

PluginCollection *PluginCollection::ourInstance = 0;

PluginCollection &PluginCollection::Instance() {
	if (ourInstance == 0) {
		ourInstance = new PluginCollection();
		//ourInstance->myPlugins.push_back(new FB2Plugin());
//		//ourInstance->myPlugins.push_back(new DocBookPlugin());
		//ourInstance->myPlugins.push_back(new HtmlPlugin());
		ourInstance->myPlugins.push_back(new TxtPlugin());
//		ourInstance->myPlugins.push_back(new PluckerPlugin());
//		ourInstance->myPlugins.push_back(new PalmDocPlugin());
//		ourInstance->myPlugins.push_back(new MobipocketPlugin());
//		ourInstance->myPlugins.push_back(new EReaderPlugin());
//		ourInstance->myPlugins.push_back(new ZTXTPlugin());
//		ourInstance->myPlugins.push_back(new TcrPlugin());
//		ourInstance->myPlugins.push_back(new CHMPlugin());
		//ourInstance->myPlugins.push_back(new OEBPlugin());
		ourInstance->myPlugins.push_back(new RtfPlugin());
//		ourInstance->myPlugins.push_back(new OpenReaderPlugin());
//		//ourInstance->myPlugins.push_back(new PdfPlugin());
	}
	return *ourInstance;
}

void PluginCollection::deleteInstance() {
	if (ourInstance != 0) {
		delete ourInstance;
		ourInstance = 0;
	}
}

PluginCollection::PluginCollection() {
	JNIEnv *env = AndroidUtil::getEnv();
	jclass cls = env->FindClass(AndroidUtil::Class_PluginCollection);
	jobject instance = env->CallStaticObjectMethod(cls, AndroidUtil::SMID_PluginCollection_Instance);
	myJavaInstance = env->NewGlobalRef(instance);
	env->DeleteLocalRef(instance);
	env->DeleteLocalRef(cls);
}

PluginCollection::~PluginCollection() {
	JNIEnv *env = AndroidUtil::getEnv();
	env->DeleteGlobalRef(myJavaInstance);
}

shared_ptr<FormatPlugin> PluginCollection::pluginByType(const std::string &fileType) const {
	for (std::vector<shared_ptr<FormatPlugin> >::const_iterator it = myPlugins.begin(); it != myPlugins.end(); ++it) {
		if (fileType == (*it)->supportedFileType()) {
			return *it;
		}
	}
	return 0;
}

bool PluginCollection::isLanguageAutoDetectEnabled() {
	return true;
}
