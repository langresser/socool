package org.socool.screader.formats.txt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.socool.zlibrary.encodings.AutoEncodingCollection;
import org.socool.zlibrary.image.ZLImage;

import org.socool.android.covers.CoverManager;
import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.library.Book;
import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.formats.*;

public class TxtChapterPlugin extends FormatPlugin {
	private TxtChapterReader m_reader;
	public TxtChapterPlugin() {
		super("plain text");
		
		m_reader = new TxtChapterReader();
	}

	@Override
	public void readMetaInfo(Book book){
		detectLanguageAndEncoding(book);

		try {
			InputStream input = FBReaderApp.Instance().getBookFile(book.m_filePath + "/info.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, book.getEncoding()));
			String line = reader.readLine();
			String[] infos = line.split("@@");
			book.myTitle = infos[0];
			book.m_bookAuthor = infos[1];
			if (infos.length >= 3) {
				book.m_coverId = CoverManager.getCoverResIdByIndex(Integer.valueOf(infos[2]));
			}
			
			book.m_bookIntro = reader.readLine();
			book.m_bookAuthorIntro = reader.readLine();

			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean supportStreamRead()						// �Ƿ�֧���ļ����ֶ�ȡ(��ʱֻ��txt��ȡ֧��)
	{
		return true;
	}

	@Override
	public void readParagraph(int paragraph)				// ��ȡĳһ���䣨���ֶ�ȡ��
	{
		if (paragraph < 0) {
			return;
		}

		m_reader.readDocument(paragraph);
	}
	
	@Override
	public void readChapter(int chapter)
	{
	}
	
	@Override
	public void readPercent(double percent)					// ��ȡ�ļ��ٷֱȣ����ֶ�ȡ��
	{
	}

	@Override
	public void readModel(BookModel model) {
		model.m_readType = BookModel.READ_TYPE_CHAPTER;

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
		try {
			InputStream input = FBReaderApp.Instance().getBookFile(book.m_filePath + "/encoding.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "gbk"));
			String encoding = reader.readLine();
			book.setEncoding(encoding);
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
