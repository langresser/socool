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
	
	public final int BUFFER_SIZE = 1024 * 20;		// 假定文件缓存区有10k

	public FileChannel m_streamReader = null;
	public Vector<Integer> m_paraOfFile = new Vector<Integer>();
	public String m_currentPath = null;
		
	public TxtChapterReader()
	{
		super(null);
	}
	
	public void setModel(BookModel model)
	{
		// 如果换文件，则关闭原文件
		m_bookModel = model;
		myCurrentContentsTree = model.TOCTree;
		m_currentPath = m_bookModel.Book.m_filePath;
		
		initData();
	}
	
	private void initData()
	{
		try {
			InputStream input = FBReaderApp.Instance().getBookFile(m_currentPath + "/data.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "gb18030"));
			
			String line = "";
			String titleAuthor = reader.readLine();
			String infoBook = reader.readLine();
			String infoAuthor = reader.readLine();

			int paraCount = 0;
			while ((line = reader.readLine()) != null) {
				String[] infos = line.split("@@");
				m_paraOfFile.add(paraCount);
				paraCount += Integer.parseInt(infos[1]); 
			}
			input.close();
			
			m_bookModel.m_allParagraphNumber = paraCount;
			m_bookModel.m_allTextSize = 1024 * 1024;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private int getFileByParagraph(int para)
	{
		int i = 1;
		for (Integer each : m_paraOfFile) {
			if (para >= each) {
				break;
			}
			
			++i;
		}
		return i;
	}

	public void readDocument(int paragraph)
	{
		Log.d("readDocument", "read:" + paragraph);
		startDocumentHandler();

		try {
		int fileNum = getFileByParagraph(paragraph);
		String filePath = m_currentPath + "/" + fileNum + ".txt";
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


		m_bookModel.m_beginParagraph = 0;
		m_bookModel.m_endParagraph = 100;

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		endDocumentHandler();
		Log.d("readDocument", String.format("readover:%1d    begin:%2d    end:%3d", paragraph, m_bookModel.m_beginParagraph, m_bookModel.m_endParagraph));
	}

	protected void startDocumentHandler()
	{
		m_bookModel.clearParagraphData();
		pushKind(BookModel.REGULAR);
		beginParagraph(ZLTextParagraph.Kind.TEXT_PARAGRAPH);
		myInsideTitle = true;
	}

	protected void endDocumentHandler()
	{
		popKind();
		endParagraph();
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
