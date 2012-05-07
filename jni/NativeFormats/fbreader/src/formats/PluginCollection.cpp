#include <AndroidUtil.h>
#include <JniEnvelope.h>

#include <ZLibrary.h>
#include <ZLFile.h>

#include "FormatPlugin.h"

#include "../library/Book.h"
#include "rtf/RtfPlugin.h"

PluginCollection *PluginCollection::ourInstance = 0;

PluginCollection &PluginCollection::Instance() {
	if (ourInstance == 0) {
		ourInstance = new PluginCollection();
		ourInstance->myPlugins.push_back(new RtfPlugin());
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
	jobject instance = AndroidUtil::StaticMethod_PluginCollection_Instance->call();
	myJavaInstance = env->NewGlobalRef(instance);
	env->DeleteLocalRef(instance);
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
