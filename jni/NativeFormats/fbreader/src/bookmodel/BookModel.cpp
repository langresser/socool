#include <AndroidUtil.h>

#include <ZLImage.h>
#include <ZLFile.h>

#include "BookModel.h"
#include "BookReader.h"

#include "../formats/FormatPlugin.h"
#include "../library/Book.h"
#include "../filesystem/ZLFSManager.h"

BookModel::BookModel(const shared_ptr<Book> book, jobject javaModel) : myBook(book) {
	myJavaModel = AndroidUtil::getEnv()->NewGlobalRef(javaModel);

	const std::string cacheDirectory = ZLFSManager::Instance().cacheDirectory();
	myBookTextModel = new ZLTextPlainModel(std::string(), book->language(), 131072, cacheDirectory, "ncache");
	myContentsModel = new ContentsModel(book->language(), cacheDirectory, "ncontents");
}

BookModel::~BookModel() {
	AndroidUtil::getEnv()->DeleteGlobalRef(myJavaModel);
}

void BookModel::setHyperlinkMatcher(shared_ptr<HyperlinkMatcher> matcher) {
	myHyperlinkMatcher = matcher;
}
	
BookModel::Label BookModel::label(const std::string &id) const {
	if (!myHyperlinkMatcher.isNull()) {
		return myHyperlinkMatcher->match(myInternalHyperlinks, id);
	}

	std::map<std::string,Label>::const_iterator it = myInternalHyperlinks.find(id);
	return (it != myInternalHyperlinks.end()) ? it->second : Label(0, -1);
}

ContentsModel::ContentsModel(const std::string &language,
		const std::string &directoryName, const std::string &fileExtension) :
	ZLTextTreeModel(std::string(), language, directoryName, fileExtension) {
}

void ContentsModel::setReference(const ZLTextTreeParagraph *paragraph, int reference) {
	myReferenceByParagraph[paragraph] = reference;
}

int ContentsModel::reference(const ZLTextTreeParagraph *paragraph) const {
	std::map<const ZLTextTreeParagraph*,int>::const_iterator it = myReferenceByParagraph.find(paragraph);
	return (it != myReferenceByParagraph.end()) ? it->second : -1;
}

const shared_ptr<Book> BookModel::book() const {
	return myBook;
}

bool BookModel::flush() {
	myBookTextModel->flush();
	if (myBookTextModel->allocator().failed()) {
		return false;
	}
	myContentsModel->flush();
	return true;
}
