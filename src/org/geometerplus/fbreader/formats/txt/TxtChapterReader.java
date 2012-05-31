package org.geometerplus.fbreader.formats.txt;

import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.BookReader;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.zlibrary.text.ZLTextParagraph;

import android.util.Log;

public final class TxtChapterReader extends BookReader {
	public final static int BREAK_PARAGRAPH_AT_NEW_LINE = 1;
	public final static int BREAK_PARAGRAPH_AT_EMPTY_LINE = 2;
	public final static int BREAK_PARAGRAPH_AT_LINE_WITH_INDENT = 4;

	public boolean myInitialized = false;
	public int myIgnoredIndent = 1;
	public int myEmptyLinesBeforeNewSection = 1;
	
	public Vector<Integer> m_paraOfFile = new Vector<Integer>();
		
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
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "gb18030"));
			
			String line = "";

			int paraCount = 0;
			while ((line = reader.readLine()) != null) {
				String[] infos = line.split("@@");
				paraCount += Integer.parseInt(infos[1]); 
				m_paraOfFile.add(paraCount);
			}
			input.close();
			
			m_bookModel.m_allParagraphNumber = paraCount;
			m_bookModel.m_fileCount = m_paraOfFile.size();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public int getFileCount()
	{
		return m_paraOfFile.size();
	}
	
	public int getFileByParagraph(int para)
	{
		if (para == 0) {
			return 1;
		}
		
		if (m_paraOfFile.size() == 1) {
			return 1;
		}

		int i = 1;
		int currentPara = 0;
		for (Integer each : m_paraOfFile) {
			if (currentPara >= para) {
				break;
			}
			
			currentPara += each;
			++i;
		}
		return i;
	}
	
	private int getBeginParagraph(int fileNum)
	{
		if (fileNum == 1) {
			return 0;
		}

		int maxIndex = Math.min(fileNum - 1, m_paraOfFile.size());
		int paraCount = 0;
		for (int i = 0; i < maxIndex; ++i) {
			int currentPara = m_paraOfFile.get(i);
			paraCount += currentPara;
		}
		
		return paraCount;
	}
	
	private int getParagraphCount(int fileNum)
	{
		if (fileNum == 1) {
			return m_paraOfFile.get(0) - 1;
		}
		
		if (fileNum <= 0 || fileNum > m_paraOfFile.size()) {
			return 0;
		}
		
		return m_paraOfFile.get(fileNum - 1);
	}
	
	public void readChapter(int fileNum)
	{
		startDocumentHandler();

		try {
			m_bookModel.m_currentBookIndex = fileNum;

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
					newLineHandler();
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
		m_bookModel.clearParagraphData();
		int fileNum = getFileByParagraph(paraNumber);
		
		if (fileNum > 1) {
			readChapter(fileNum - 1);
			m_bookModel.m_beginParagraph = getBeginParagraph(fileNum - 1);
		} else {
			m_bookModel.m_beginParagraph = 0;
		}

		readChapter(fileNum);

		final int lastFile = m_paraOfFile.size();
		if (fileNum < lastFile) {
			readChapter(fileNum + 1);
		}
	
//		Log.d("readDocument", String.format("readover:%1d    begin:%2d    end:%3d", paragraph, m_bookModel.m_beginParagraph, m_bookModel.m_endParagraph));
	}

	protected void startDocumentHandler()
	{
		pushKind(BookModel.REGULAR);
		beginParagraph(ZLTextParagraph.Kind.TEXT_PARAGRAPH);
		myInsideTitle = true;
	}

	protected void endDocumentHandler()
	{
		popKind();
		endParagraph();
//		insertEndParagraph(ZLTextParagraph.Kind.END_OF_SECTION_PARAGRAPH);
	}
	
	protected boolean characterDataHandler(char[] ch, int start, int length)
	{
		addData(ch, start, length, false);
		return true;
	}
	
	protected boolean newLineHandler()
	{
		endParagraph();
		beginParagraph(ZLTextParagraph.Kind.TEXT_PARAGRAPH);

		return true;
	}
}
