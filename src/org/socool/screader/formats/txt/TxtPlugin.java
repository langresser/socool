package org.socool.screader.formats.txt;

import java.io.BufferedInputStream;

import info.monitorenter.cpdetector.io.CodepageDetectorProxy;

import org.socool.zlibrary.encodings.AutoEncodingCollection;
import org.socool.zlibrary.filesystem.ZLFile;
import org.socool.zlibrary.image.ZLImage;

import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.library.Book;
import org.socool.screader.formats.*;

public class TxtPlugin extends FormatPlugin {
	private TxtReader m_reader;
	public TxtPlugin() {
		super("plain text");
		
		m_reader = new TxtReader(null);
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
		model.m_readType = BookModel.READ_TYPE_STREAM;

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
		java.nio.charset.Charset charset = null;   
		try {
			ZLFile file = ZLFile.createFileByPath(book.m_filePath);
		  charset = CodepageDetectorProxy.getInstance().detectCodepage(
				  new BufferedInputStream(file.getInputStream()), 200);   
		} catch (Exception ex) {	
		}

		if(charset!=null){   
			book.setEncoding(charset.name());
		}else{
			book.setEncoding("auto");
		}
	}
}
