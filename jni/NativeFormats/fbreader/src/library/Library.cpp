#include <AndroidUtil.h>

#include "Library.h"


shared_ptr<Library> Library::ourInstance;

Library &Library::Instance() {
	if (ourInstance.isNull()) {
		ourInstance = new Library();
	}
	return *ourInstance;
}

Library::Library() {
	JNIEnv *env = AndroidUtil::getEnv();
	jclass paths = env->FindClass(AndroidUtil::Class_Paths);
	myPathsClass = (jclass)env->NewGlobalRef(paths);
	env->DeleteLocalRef(paths);
}

Library::~Library() {
	JNIEnv *env = AndroidUtil::getEnv();
	env->DeleteGlobalRef(myPathsClass);
}

std::string Library::cacheDirectory() const {
	JNIEnv *env = AndroidUtil::getEnv();
	jstring res = (jstring)env->CallStaticObjectMethod(myPathsClass, AndroidUtil::SMID_Paths_cacheDirectory);
	const char *data = env->GetStringUTFChars(res, 0);
	std::string str(data);
	env->ReleaseStringUTFChars(res, data);
	env->DeleteLocalRef(res);
	return str;
}
