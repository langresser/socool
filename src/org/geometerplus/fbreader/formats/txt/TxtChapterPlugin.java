package org.geometerplus.fbreader.formats.txt;

import java.io.BufferedInputStream;

import info.monitorenter.cpdetector.io.CodepageDetectorProxy;

import org.geometerplus.zlibrary.encodings.AutoEncodingCollection;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLImage;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.formats.*;

public class TxtChapterPlugin extends FormatPlugin {
	private TxtChapterReader m_reader;
	public TxtChapterPlugin() {
		super("plain text");
		
		m_reader = new TxtChapterReader();
	}

	@Override
	public void readMetaInfo(Book book){
		// txt��ʽû�и�����Ϣ
		book.myTitle = "���޿ֲ�";
	}
	
	@Override
	public boolean supportStreamRead()						// �Ƿ�֧���ļ����ֶ�ȡ(��ʱֻ��txt��ȡ֧��)
	{
		return true;
	}

	@Override
	public void readParagraph(int paragraph)				// ��ȡĳһ���䣨���ֶ�ȡ��
	{
		m_reader.readDocument(paragraph);
	}
	
	@Override
	public void readPercent(double percent)					// ��ȡ�ļ��ٷֱȣ����ֶ�ȡ��
	{
		
	}

	@Override
	public void readModel(BookModel model) {
		model.m_isStreamRead = true;
		model.m_supportRichText = false;

		detectLanguageAndEncoding(model.Book);
		m_reader.setModel(model);
		m_reader.readDocument(0);
	}

	@Override
	public ZLImage readCover(Book book) {
		// Ĭ�Ϸ��棬�û�����ָ��ͼƬ�����������ݿ��С������ı���
		return null;
	}

	@Override
	public String readAnnotation(Book book) {
		// �޸�����Ϣ
		return null;
	}

	@Override
	public AutoEncodingCollection supportedEncodings() {
		return new AutoEncodingCollection();
	}

	@Override
	public void detectLanguageAndEncoding(Book book) {
	}
}
