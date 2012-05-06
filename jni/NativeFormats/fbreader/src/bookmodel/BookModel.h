#ifndef __BOOKMODEL_H__
#define __BOOKMODEL_H__

#include <jni.h>

#include <map>
#include <string>

#include <ZLTextModel.h>
#include <ZLTextParagraph.h>

class ZLImage;
class Book;

class ContentsModel : public ZLTextTreeModel {

public:
	ContentsModel(const std::string &language, const std::string &directoryName, const std::string &fileExtension);
	void setReference(const ZLTextTreeParagraph *paragraph, int reference);
	int reference(const ZLTextTreeParagraph *paragraph) const;

private:
	std::map<const ZLTextTreeParagraph*,int> myReferenceByParagraph;
};

class BookModel {

public:
	struct Label {
		Label(shared_ptr<ZLTextModel> model, int paragraphNumber) : Model(model), ParagraphNumber(paragraphNumber) {}

		const shared_ptr<ZLTextModel> Model;
		const int ParagraphNumber;
	};

public:
	class HyperlinkMatcher {

	public:
		virtual Label match(const std::map<std::string,Label> &lMap, const std::string &id) const = 0;
	};

public:
	BookModel(const shared_ptr<Book> book, jobject javaModel);
	~BookModel();

	void setHyperlinkMatcher(shared_ptr<HyperlinkMatcher> matcher);

	shared_ptr<ZLTextModel> bookTextModel() const;
	shared_ptr<ZLTextModel> contentsModel() const;

	Label label(const std::string &id) const;
	const std::map<std::string,Label> &internalHyperlinks() const;

	const shared_ptr<Book> book() const;

	bool flush();

private:
	const shared_ptr<Book> myBook;
	jobject myJavaModel;
	shared_ptr<ZLTextModel> myBookTextModel;
	shared_ptr<ZLTextModel> myContentsModel;
	std::map<std::string,Label> myInternalHyperlinks;
	shared_ptr<HyperlinkMatcher> myHyperlinkMatcher;

friend class BookReader;
};

inline shared_ptr<ZLTextModel> BookModel::bookTextModel() const { return myBookTextModel; }
inline shared_ptr<ZLTextModel> BookModel::contentsModel() const { return myContentsModel; }
inline const std::map<std::string,BookModel::Label> &BookModel::internalHyperlinks() const { return myInternalHyperlinks; }

#endif /* __BOOKMODEL_H__ */
