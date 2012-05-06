#include <ZLUnicodeUtil.h>
#include <ZLLogger.h>
#include <JniEnvelope.h>
#include "ZLibrary.h"
#include <AndroidUtil.h>
#include "../filesystem/ZLFSManager.h"

std::string ZLibrary::ourZLibraryDirectory;

std::string ZLibrary::ourApplicationName;
std::string ZLibrary::ourApplicationDirectory;

const std::string ZLibrary::FileNameDelimiter("/");
const std::string ZLibrary::PathDelimiter(":");
const std::string ZLibrary::EndOfLine("\n");

ZLibrary *ZLibrary::Instance = 0;

ZLibrary::ZLibrary() {
	Instance = this;
}
std::string ZLibrary::Language() {
	JNIEnv *env = AndroidUtil::getEnv();
	jobject locale = AndroidUtil::StaticMethod_java_util_Locale_getDefault->call();
	std::string lang = AndroidUtil::Method_java_util_Locale_getLanguage->callForCppString(locale);
	env->DeleteLocalRef(locale);
	return lang;
}

bool ZLibrary::init(int &argc, char **&argv) {
	if (ZLibrary::Instance == 0) {
		new ZLibrary();
	}

	ZLibrary::parseArguments(argc, argv);
	ZLFSManager::createInstance();
	return true;
}


void ZLibrary::parseArguments(int &argc, char **&argv) {
	static const std::string LANGUAGE_OPTION = "-lang";
	static const std::string LOGGER_OPTION = "-log";
	while ((argc > 2) && (argv[1] != 0) && (argv[2] != 0)) {
		const std::string argument = argv[1];
		if (LOGGER_OPTION == argument) {
			std::string loggerClasses = argv[2];
			while (size_t index = loggerClasses.find(':') != std::string::npos) {
				ZLLogger::Instance().registerClass(loggerClasses.substr(0, index));
				loggerClasses.erase(0, index + 1);
			}
			ZLLogger::Instance().registerClass(loggerClasses);
		} else {
			ZLLogger::Instance().println(ZLLogger::DEFAULT_CLASS, "unknown argument: " + argument);
		}
		argc -= 2;
		argv += 2;
	}
	ourZLibraryDirectory = ".";
}

void ZLibrary::shutdown() {
	ZLFSManager::deleteInstance();
}

void ZLibrary::initApplication(const std::string &name) {
	ourApplicationName = name;
	ourApplicationDirectory = ".";
}

