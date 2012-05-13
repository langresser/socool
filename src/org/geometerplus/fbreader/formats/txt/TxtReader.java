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
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.text.ZLTextParagraph;

public final class TxtReader extends BookReader {
	public final static int BREAK_PARAGRAPH_AT_NEW_LINE = 1;
	public final static int BREAK_PARAGRAPH_AT_EMPTY_LINE = 2;
	public final static int BREAK_PARAGRAPH_AT_LINE_WITH_INDENT = 4;

	public boolean myInitialized = false;
	public int myBreakType = BREAK_PARAGRAPH_AT_NEW_LINE;
	public int myIgnoredIndent = 1;
	public int myEmptyLinesBeforeNewSection = 1;
	public boolean myCreateContentsTable = false;
	
	public final static int	kAnsi = 0;
	public final static int	kUtf8 = 1;
	public final static int	kUtf16le = 2;
	public final static int	kUtf16be = 3; 
	
	public int m_currentOffset = 0;
	
	public TxtReader(BookModel model)
	{
		super(model);
		
		String encoding = model.Book.getEncoding();
		if (encoding.equalsIgnoreCase("utf-8")) {
			m_unicodeFlag = kUtf8;
		} else if (encoding.equalsIgnoreCase("utf-16")) {
			m_unicodeFlag = kUtf16le;
		} else if (encoding.equalsIgnoreCase("utf-16be")) {
			m_unicodeFlag = kUtf16be;
		} else if (encoding.equalsIgnoreCase("utf-16le")) {
			m_unicodeFlag = kUtf16le;
		} else {
			m_unicodeFlag = kAnsi;
		}
	}
	
	public void readDocument()
	{
		startDocumentHandler();

		try {
		String path = m_bookModel.Book.File.getPath();
		
		FileChannel streamReader = new RandomAccessFile(path, "r").getChannel();
		MappedByteBuffer mbb = streamReader.map(FileChannel.MapMode.READ_ONLY, 0, streamReader.size());
//		BufferedRandomAccessFile streamReader = new BufferedRandomAccessFile(file, "r");
		ByteBuffer bb = ByteBuffer.allocate((int)streamReader.size());
		Charset cs = Charset.forName (m_bookModel.Book.getEncoding());

		bb.clear();
		int count = streamReader.read(bb);
		bb.flip();
	    CharBuffer cb = cs.decode(bb);
		char[] text = cb.array();
		int maxLength = text.length;
		int parBegin = 0;

		for (int i = parBegin; i < maxLength; ++i) {
			char c = text[i];
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
				parBegin = i + 1;
				newLineHandler();
			}
		}
		
		if (parBegin != maxLength) {
			characterDataHandler(text, parBegin, maxLength - parBegin);
		}

		streamReader.close();
		} catch (IOException e) {
		}
		
		endDocumentHandler();
	}

	protected void startDocumentHandler()
	{
		pushKind(BookModel.REGULAR);
		beginParagraph(ZLTextParagraph.Kind.TEXT_PARAGRAPH);
		myLineFeedCounter = 0;
		myInsideContentsParagraph = false;
		myInsideTitle = true;
		myLastLineIsEmpty = true;
		myNewLine = true;
		mySpaceCounter = 0;
	}

	protected void endDocumentHandler()
	{
		internalEndParagraph();
	}
	
	protected boolean characterDataHandler(char[] ch, int start, int length)
	{
		int i = 0;
		for (i = start; i < start + length; ++i) {
			char c = ch[i];
			if (c == ' ' || c == '\t') {
				if (c != '\t') {
					++mySpaceCounter;
				} else {
					mySpaceCounter += myIgnoredIndent + 1; // TODO: implement single option in PlainTextFormat
				}
			} else {
				myLastLineIsEmpty = false;
				break;
			}
		}

		if (i != start + length) {
			if ((myBreakType & BREAK_PARAGRAPH_AT_LINE_WITH_INDENT) != 0 &&
					myNewLine && (mySpaceCounter > myIgnoredIndent)) {
				internalEndParagraph();
				beginParagraph(ZLTextParagraph.Kind.TEXT_PARAGRAPH);
			}
			addData(ch, start, length, false);
			if (myInsideContentsParagraph) {
				addContentsData(ch, start, length);
			}
			myNewLine = false;
		}
		return true;
	}
	
	protected boolean newLineHandler()
	{
		if (!myLastLineIsEmpty) {
			myLineFeedCounter = -1;
		}
		myLastLineIsEmpty = true;
		++myLineFeedCounter;
		myNewLine = true;
		mySpaceCounter = 0;
		boolean paragraphBreak =
			(myBreakType & BREAK_PARAGRAPH_AT_NEW_LINE) != 0 ||
			((myBreakType & BREAK_PARAGRAPH_AT_EMPTY_LINE) != 0 && (myLineFeedCounter > 0));

		if (myCreateContentsTable) {
			if (!myInsideContentsParagraph && (myLineFeedCounter == myEmptyLinesBeforeNewSection)) {
				myInsideContentsParagraph = true;
				internalEndParagraph();
				insertEndOfSectionParagraph();
				beginContentsParagraph();
				myInsideTitle = true;
				pushKind(BookModel.SECTION_TITLE);
				beginParagraph(ZLTextParagraph.Kind.TEXT_PARAGRAPH);
				paragraphBreak = false;
			}
			if (myInsideContentsParagraph && (myLineFeedCounter == 1)) {
				myInsideTitle = false;
				endContentsParagraph();
				popKind();
				myInsideContentsParagraph = false;
				paragraphBreak = true;
			}
		}

		if (paragraphBreak) {
			internalEndParagraph();
			beginParagraph(ZLTextParagraph.Kind.TEXT_PARAGRAPH);
		}

		return true;
	}

	private	int m_unicodeFlag;
//		shared_ptr<ZLEncodingConverter> myConverter;
	private	void internalEndParagraph()
	{
		if (!myLastLineIsEmpty) {
			//myLineFeedCounter = 0;
			myLineFeedCounter = -1; /* Fixed by Hatred: zero value was break LINE INDENT formater -
			                           second line print with indent like new paragraf */
		}
		myLastLineIsEmpty = true;
		endParagraph();
	}

	private	int myLineFeedCounter;
	private	boolean myInsideContentsParagraph;
	private	boolean myLastLineIsEmpty;
	private	boolean myNewLine;
	private	int mySpaceCounter;
	
}
