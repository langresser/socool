#include <jni.h>

#include <AndroidUtil.h>

#include <ZLibrary.h>

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
	if (AndroidUtil::init(jvm)) {
		int argc = 0;
		char **argv = 0;
		ZLibrary::init(argc, argv);
		ZLibrary::initApplication("FBReader");
	}
	return JNI_VERSION_1_2;
}
