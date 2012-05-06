package org.geometerplus.fbreader.formats.txt;

import org.geometerplus.zlibrary.encodings.AutoEncodingCollection;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLImage;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.BookReadingException;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.formats.*;

public class TxtPlugin extends JavaFormatPlugin {
	public TxtPlugin() {
		super("fb2");
	}

	@Override
	public ZLFile realBookFile(ZLFile file) {
		return null;
	}

	@Override
	public void readMetaInfo(Book book) throws BookReadingException {
//		new FB2MetaInfoReader(book).readMetaInfo();
	}

	@Override
	public void readModel(BookModel model) throws BookReadingException {
//		new FB2Reader(model).readBook();
	}

	@Override
	public ZLImage readCover(ZLFile file) {
//		return new FB2CoverReader().readCover(file);
		return null;
	}

	@Override
	public String readAnnotation(ZLFile file) {
//		return new FB2AnnotationReader().readAnnotation(file);
		return null;
	}

	@Override
	public AutoEncodingCollection supportedEncodings() {
		return new AutoEncodingCollection();
	}

	@Override
	public void detectLanguageAndEncoding(Book book) {
		book.setEncoding("auto");
	}
}
