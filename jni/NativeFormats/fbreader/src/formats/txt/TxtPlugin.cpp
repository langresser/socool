#include <ZLFile.h>
#include <ZLInputStream.h>

#include "TxtPlugin.h"
#include "TxtBookReader.h"
#include "PlainTextFormat.h"

#include "../../bookmodel/BookModel.h"
#include "../../library/Book.h"

bool TxtPlugin::readModel(BookModel &model) const {
	Book &book = *model.book();
	const ZLFile &file = book.file();
	shared_ptr<ZLInputStream> stream = file.inputStream();
	if (stream.isNull()) {
		return false;
	}

	PlainTextFormat format(file);
	if (!format.initialized()) {
		PlainTextFormatDetector detector;
		detector.detect(*stream, format);
	}

	readLanguageAndEncoding(book);
	TxtBookReader(model, format, book.encoding()).readDocument(*stream);
	return true;
}

bool TxtPlugin::readLanguageAndEncoding(Book &book) const {
	shared_ptr<ZLInputStream> stream = book.file().inputStream();
	if (stream.isNull()) {
		return false;
	}
	detectEncodingAndLanguage(book, *stream);
	return !book.encoding().empty();
}
