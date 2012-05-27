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
		// txt格式没有附加信息
		book.myTitle = "无限恐怖";
	}
	
	@Override
	public boolean supportStreamRead()						// 是否支持文件部分读取(暂时只有txt读取支持)
	{
		return true;
	}

	@Override
	public void readParagraph(int paragraph)				// 读取某一段落（部分读取）
	{
		m_reader.readDocument(paragraph);
	}
	
	@Override
	public void readPercent(double percent)					// 读取文件百分比（部分读取）
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
		// 默认封面，用户可以指定图片，保存在数据库中。而非文本中
		return null;
	}

	@Override
	public String readAnnotation(Book book) {
		// 无附加信息
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
