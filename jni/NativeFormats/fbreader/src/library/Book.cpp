#include <algorithm>
#include <set>

#include <AndroidUtil.h>
#include <JniEnvelope.h>

#include <ZLStringUtil.h>
#include <ZLFile.h>
#include <ZLLanguageList.h>

#include "Book.h"

#include "../formats/FormatPlugin.h"
//#include "../migration/BookInfo.h"

const std::string Book::AutoEncoding = "auto";

Book::Book(const ZLFile &file, int id) : myBookId(id), myFile(file) {
}

Book::~Book() {
}

shared_ptr<Book> Book::createBook(
	const ZLFile &file,
	int id,
	const std::string &encoding,
	const std::string &language,
	const std::string &title
) {
	Book *book = new Book(file, id);
	book->setEncoding(encoding);
	book->setLanguage(language);
	book->setTitle(title);
	return book;
}

shared_ptr<Book> Book::loadFromJavaBook(JNIEnv *env, jobject javaBook) {
	jobject javaFile = AndroidUtil::Field_Book_File->value(javaBook);
	const std::string path = AndroidUtil::Method_ZLFile_getPath->callForCppString(javaFile);
	env->DeleteLocalRef(javaFile);

	const std::string title = AndroidUtil::Method_Book_getTitle->callForCppString(javaBook);
	const std::string language = AndroidUtil::Method_Book_getLanguage->callForCppString(javaBook);
	const std::string encoding = AndroidUtil::Method_Book_getEncodingNoDetection->callForCppString(javaBook);

	return createBook(ZLFile(path), 0, encoding, language, title);
}

void Book::setTitle(const std::string &title) {
	myTitle = title;
}

void Book::setLanguage(const std::string &language) {
	if (!myLanguage.empty()) {
		const std::vector<std::string> &codes = ZLLanguageList::languageCodes();
		std::vector<std::string>::const_iterator it =
			std::find(codes.begin(), codes.end(), myLanguage);
		std::vector<std::string>::const_iterator jt =
			std::find(codes.begin(), codes.end(), language);
		if (it != codes.end() && jt == codes.end()) {
			return;
		}
	}
	myLanguage = language;
}

void Book::setEncoding(const std::string &encoding) {
	myEncoding = encoding;
}

bool BookByFileNameComparator::operator() (
	const shared_ptr<Book> book0,
	const shared_ptr<Book> book1
) const {
	return book0->file() < book1->file();
}
