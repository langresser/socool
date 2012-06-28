package org.socool.screader.formats.txt;

import android.annotation.SuppressLint;
import java.io.*;
import java.util.ArrayList;

import org.socool.screader.bookmodel.BookChapter;
import org.socool.screader.bookmodel.BookChapter.BookChapterData;
import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.bookmodel.BookParagraph;
import org.socool.screader.bookmodel.BookReader;
import org.socool.screader.screader.FBReaderApp;

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

		initData();
	}
	
	public void initData()
	{
		if (m_bookModel.m_chapter.m_chapterData.size() > 0) {
			return;
		}

		try {
//			long startTime = System.currentTimeMillis();

			InputStream input = FBReaderApp.Instance().getBookFile(m_bookModel.Book.m_filePath + "/chapter.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, m_bookModel.Book.getEncoding()));
			
			String line = "";

			int paraCount = 0;
			int textSize = 0;
			int currentJuanIndex = -1;
			int currentChapterIndex = -1;
			final BookChapter chapter = m_bookModel.m_chapter;
			BookChapter.JuanData juanData = null;

			while ((line = reader.readLine()) != null) {
				if (line.charAt(0) == 'j') {
					if (juanData != null) {
						chapter.m_juanData.add(juanData);
					}

					juanData = new BookChapter.JuanData();
					juanData.m_juanTitle = line.substring(1);
					++currentJuanIndex;
				} else {
					++currentChapterIndex;

					// 加1是补上隐藏的段落终结符
					final int firstA = line.indexOf("@@");
					final int count = Integer.parseInt(line.substring(0, firstA)) + 1;
					BookChapter.BookChapterData data = new BookChapter.BookChapterData();
					data.m_startOffset = paraCount;
					data.m_paragraphCount = count;
					final int secondA = line.indexOf("@@", firstA + 2);
					data.m_textSize = Integer.parseInt(line.substring(firstA + 2, secondA));
					data.m_startTxtOffset = textSize;
					data.m_title = line.substring(secondA + 2);
					data.m_juanIndex = currentJuanIndex;
					chapter.addChapterData(data);
					paraCount += count;
					textSize += data.m_textSize;
					
					juanData.m_juanChapter.add(currentChapterIndex);
				}
			}
			input.close();
			
			if (juanData != null) {
				chapter.m_juanData.add(juanData);
			}
			
			chapter.m_allParagraphNumber = paraCount;
			chapter.m_allTextSize = textSize;
			
//			long time1 = System.currentTimeMillis() - startTime;
			
			input = FBReaderApp.Instance().getBookFile(m_bookModel.Book.m_filePath + "/data.db");
			int size = input.available();
			byte[] buffer = new byte[size];
			BufferedInputStream bis = new BufferedInputStream(input, size);
			bis.read(buffer);
			input.close();

			ArrayList<Integer> currentParagraphOffset = null;
			int startTxtOffset = 0;
			int paraTxtSize = 0;
			currentChapterIndex = -1;
			for (int i = 0; i < size; ++i) {
				final int ps = buffer[i] & 0xff;
				if (ps == 0) {
					++currentChapterIndex;
					final BookChapterData data = chapter.m_chapterData.get(currentChapterIndex);
					currentParagraphOffset = data.paragraphOffset;
					startTxtOffset = data.m_startTxtOffset;
					paraTxtSize = 0;
				} else if (ps == 255) {
					int plus = ps;
					for (int j = i + 1; j < size; ++j) {
						final int ps2 = buffer[j] & 0xff;
						plus += ps2;

						if (ps2 != 255) {
							i = j;
							break;
						}
					}
					
					currentParagraphOffset.add(startTxtOffset + paraTxtSize);
					paraTxtSize += plus;
				} else {
					if (currentParagraphOffset != null) {
						currentParagraphOffset.add(startTxtOffset + paraTxtSize);
						paraTxtSize += ps;
					}
				}
			}
			
//			long time2 = System.currentTimeMillis() - startTime;
			
//			Log.d("init cost:", String.format("%1d   %2d", time1, time2));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressLint({ "ParserError", "ParserError" })
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
	}

	protected void startDocumentHandler()
	{
		pushKind(BookModel.REGULAR);
		pushKind(BookModel.SECTION_TITLE);
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
