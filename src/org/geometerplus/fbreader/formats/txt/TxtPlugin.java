package org.geometerplus.fbreader.formats.txt;

import java.io.BufferedInputStream;

import info.monitorenter.cpdetector.io.CodepageDetectorProxy;

import org.geometerplus.zlibrary.encodings.AutoEncodingCollection;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLImage;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.formats.*;

public class TxtPlugin extends JavaFormatPlugin {
	private TxtReader m_reader;
	public TxtPlugin() {
		super("plain text");
		
		m_reader = new TxtReader(null);
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
		detectLanguageAndEncoding(model.Book);
		m_reader.setModel(model);
		m_reader.readDocument(0);
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
		java.nio.charset.Charset charset = null;   
		try {   
		  charset = CodepageDetectorProxy.getInstance().detectCodepage(
				  new BufferedInputStream(book.File.getInputStream()), 200);   
		} catch (Exception ex) {	
		}

		if(charset!=null){   
			book.setEncoding(charset.name());
		}else{
			book.setEncoding("auto");
		}
	}
}
