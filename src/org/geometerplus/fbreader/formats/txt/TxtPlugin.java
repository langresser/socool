package org.geometerplus.fbreader.formats.txt;

import org.geometerplus.zlibrary.encodings.AutoEncodingCollection;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLImage;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.formats.*;

public class TxtPlugin extends JavaFormatPlugin {
	public TxtPlugin() {
		super("plain text");
	}

	@Override
	public ZLFile realBookFile(ZLFile file) {
		return file;
	}

	@Override
	public void readMetaInfo(Book book){
		// txt格式没有附加信息
	}

	@Override
	public void readModel(BookModel model) {
		new TxtReader(model).readDocument();
	}

	@Override
	public ZLImage readCover(ZLFile file) {
		// 默认封面，用户可以指定图片，保存在数据库中。而非文本中
		return null;
	}

	@Override
	public String readAnnotation(ZLFile file) {
		// 无附加信息
		return null;
	}

	@Override
	public AutoEncodingCollection supportedEncodings() {
		return new AutoEncodingCollection();
	}

	@Override
	public void detectLanguageAndEncoding(Book book) {
		// TODO 编码侦测
		book.setEncoding("auto");
	}
}
