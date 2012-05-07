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
		// txt��ʽû�и�����Ϣ
	}

	@Override
	public void readModel(BookModel model) {
		new TxtReader(model).readDocument();
	}

	@Override
	public ZLImage readCover(ZLFile file) {
		// Ĭ�Ϸ��棬�û�����ָ��ͼƬ�����������ݿ��С������ı���
		return null;
	}

	@Override
	public String readAnnotation(ZLFile file) {
		// �޸�����Ϣ
		return null;
	}

	@Override
	public AutoEncodingCollection supportedEncodings() {
		return new AutoEncodingCollection();
	}

	@Override
	public void detectLanguageAndEncoding(Book book) {
		// TODO �������
		book.setEncoding("auto");
	}
}
