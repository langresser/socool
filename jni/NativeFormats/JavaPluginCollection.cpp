#include <jni.h>

#include <vector>

#include <AndroidUtil.h>

#include "fbreader/src/formats/FormatPlugin.h"

extern "C"
JNIEXPORT jobjectArray JNICALL Java_org_lancer_fbreader_formats_PluginCollection_nativePlugins(JNIEnv* env, jobject thiz) {
	const std::vector<shared_ptr<FormatPlugin> > plugins = PluginCollection::Instance().plugins();
	const size_t size = plugins.size();
	jclass cls = env->FindClass(AndroidUtil::Class_NativeFormatPlugin);
	jobjectArray javaPlugins = env->NewObjectArray(size, cls, 0);

	for (size_t i = 0; i < size; ++i) {
		jstring fileType = AndroidUtil::createJavaString(env, plugins[i]->supportedFileType());
		env->SetObjectArrayElement(
			javaPlugins, i,
			env->NewObject(cls, AndroidUtil::MID_NativeFormatPlugin_init, fileType)
		);
		env->DeleteLocalRef(fileType);
	}
	return javaPlugins;
}

extern "C"
JNIEXPORT void JNICALL Java_org_lancer_fbreader_formats_PluginCollection_free(JNIEnv* env, jobject thiz) {
}
