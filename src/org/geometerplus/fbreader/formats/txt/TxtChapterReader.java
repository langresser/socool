package org.geometerplus.fbreader.formats.txt;

import java.io.*;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.BookParagraph;
import org.geometerplus.fbreader.bookmodel.BookReader;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

import android.util.Log;


public final class TxtChapterReader extends BookReader {
	public final static int BREAK_PARAGRAPH_AT_NEW_LINE = 1;
	public final static int BREAK_PARAGRAPH_AT_EMPTY_LINE = 2;
	public final static int BREAK_PARAGRAPH_AT_LINE_WITH_INDENT = 4;

	public boolean myInitialized = false;
	public int myIgnoredIndent = 1;
	public int myEmptyLinesBeforeNewSection = 1;
		
	public TxtChapterReader()
	{
		super(null);
	}
	
	public void setModel(BookModel model)
	{
		m_bookModel = model;
		myCurrentContentsTree = model.TOCTree;
		
		initData();
	}
	
	private void initData()
	{
		try {
			InputStream input = FBReaderApp.Instance().getBookFile(m_bookModel.Book.m_filePath + "/data.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, m_bookModel.Book.getEncoding()));
			
			String line = "";

			int paraCount = 0;
			while ((line = reader.readLine()) != null) {
				String[] infos = line.split("@@");
				final int count = Integer.parseInt(infos[1]) + 1;
				m_bookModel.m_chapter.addChapterData(infos[0], paraCount, count, Integer.parseInt(infos[2]), infos[3], infos[4]);
				paraCount += count;
			}
			input.close();
			
			m_bookModel.m_paragraph.m_allParagraphNumber = paraCount;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public void readChapter(int fileNum)
	{
		startDocumentHandler();

		try {
			String filePath = m_bookModel.Book.m_filePath + "/" + fileNum + ".txt";
			InputStream input = FBReaderApp.Instance().getBookFile(filePath);

			int size = input.available();

			byte[] buffer = new byte[size];
			input.read(buffer);
			input.close();
			final String encoding = m_bookModel.Book.getEncoding();
			char[] text = (new String(buffer, encoding)).toCharArray();

			int maxLength = text.length;
			int parBegin = 0;
			
			// 过滤最开始的空格
			for (int i = 0; i < maxLength; ++i) {
				final char c = text[i];
				if (c == ' ' || c == '\t' || c == '　' || c == '	') {
					++parBegin;
				} else {
					break;
				}
			}

			for (int i = parBegin; i < maxLength; ++i) {
				final char c = text[i];
				if (c == '\n' || c == '\r') {
					boolean skipNewLine = false;
					if (c == '\r' && (i + 1) != maxLength && text[i + 1] == '\n') {
						skipNewLine = true;
						text[i] = '\n';
					}
					if (parBegin != i) {			
						characterDataHandler(text, parBegin, i - parBegin);
					}
					// 跳过'\n'(\r\n的情况)
					if (skipNewLine) {
						++i; // 0d 0a
					}
					
					// 过滤掉段首空格
					if ((i + 1 < maxLength)) {
						final char css = text[i + 1];
						// 分别对应半角和全角的空格
						if (css == ' ' || css == '\t' || css == '　' || css == '	') {
							++i;
						}
					}
					
					if ((i + 1 < maxLength)) {
						final char css = text[i + 1];
						if (css == ' ' || css == '\t' || css == '　' || css == '	') {
							++i;
						}
					}

					parBegin = i + 1;
					
					// 最后一个回车不需要重启段落
					if (i == maxLength - 1) {
						endParagraph();
					} else {
						newLineHandler();
					}
				}
			}
			
			if (parBegin != maxLength) {
				characterDataHandler(text, parBegin, maxLength - parBegin);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		endDocumentHandler();
	}

	public void readDocument(int paraNumber)
	{
//		Log.d("readDocument", "read:" + paragraph);
		m_bookModel.m_paragraph.clearParagraphData();
		int fileNum = m_bookModel.m_chapter.getChapterIndexByParagraph(paraNumber);
		
		if (fileNum >= 1) {
			readChapter(fileNum - 1);
			m_bookModel.m_paragraph.m_beginParagraph = m_bookModel.m_chapter.getChapterOffset(fileNum - 1);
		} else {
			m_bookModel.m_paragraph.m_beginParagraph = 0;
		}

		readChapter(fileNum);

		final int lastFile = m_bookModel.m_chapter.getChapterCount();
		if (fileNum < lastFile - 1) {
			readChapter(fileNum + 1);
		}
	
//		Log.d("readDocument", String.format("readover:%1d    begin:%2d    end:%3d", paragraph, m_bookModel.m_beginParagraph, m_bookModel.m_endParagraph));
	}

	protected void startDocumentHandler()
	{
		pushKind(BookModel.REGULAR);
		pushKind(BookModel.TITLE);
		beginParagraph(BookParagraph.PARAGRAPH_KIND_TEXT_PARAGRAPH);
		myInsideTitle = true;
	}

	protected void endDocumentHandler()
	{
		popKind();
		endParagraph();

		m_bookModel.m_paragraph.insertEndOfChapter();
	}
	
	protected boolean characterDataHandler(char[] ch, int start, int length)
	{
//		String text = new String(ch, start, length);
//		Log.d("characterDataHandler", String.format(" %1d  %2d   %3s", start, length, text));
		addData(ch, start, length, false);
		return true;
	}
	
	protected boolean newLineHandler()
	{
		if (myInsideTitle) {
			myInsideTitle = false;
			popKind();
		}

		endParagraph();
		beginParagraph(BookParagraph.PARAGRAPH_KIND_TEXT_PARAGRAPH);

		return true;
	}
}
