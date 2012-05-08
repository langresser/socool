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

public final class TxtReader extends BookReader {
	public final static int BREAK_PARAGRAPH_AT_NEW_LINE = 1;
	public final static int BREAK_PARAGRAPH_AT_EMPTY_LINE = 2;
	public final static int BREAK_PARAGRAPH_AT_LINE_WITH_INDENT = 4;
	public final static int BUFFER_SIZE = 4096;

	public boolean myInitialized = false;
	public int myBreakType = BREAK_PARAGRAPH_AT_NEW_LINE;
	public int myIgnoredIndent = 1;
	public int myEmptyLinesBeforeNewSection = 1;
	public boolean myCreateContentsTable = false;
	
	public final static int	kAnsi = 0;
	public final static int	kUtf8 = 1;
	public final static int	kUtf16le = 2;
	public final static int	kUtf16be = 3; 
	
	void detectFormat(InputStream stream) {

		final int tableSize = 10;

		int lineCounter = 0;
		int emptyLineCounter = -1;
		int stringsWithLengthLessThan81Counter = 0;
		int[] stringIndentTable = new int[tableSize];
		int[] emptyLinesTable = new int[tableSize];
		int[] emptyLinesBeforeShortStringTable = new int[tableSize];

		boolean currentLineIsEmpty = true;
		int currentLineLength = 0;
		int currentLineIndent = 0;
		int currentNumberOfEmptyLines = -1;
		
		byte[] buffer = new byte[BUFFER_SIZE];
		int length = 0;;
		do {
			try {
				length = stream.read(buffer, length, BUFFER_SIZE);
			} catch (IOException e) {
				
			}

			for (int i = 0; i < length; ++i) {
				++currentLineLength;
				char c = (char)buffer[i];
				if (c == '\n') {
					++lineCounter;
					if (currentLineIsEmpty) {
						++emptyLineCounter;
						++currentNumberOfEmptyLines;
					} else {
						if (currentNumberOfEmptyLines >= 0) {
							int index = Math.min(currentNumberOfEmptyLines, (int)tableSize - 1);
							emptyLinesTable[index]++;
							if (currentLineLength < 51) {
								emptyLinesBeforeShortStringTable[index]++;
							}
						}
						currentNumberOfEmptyLines = -1;
					}
					if (currentLineLength < 81) {
						++stringsWithLengthLessThan81Counter;
					}
					if (!currentLineIsEmpty) {
						stringIndentTable[Math.min(currentLineIndent, tableSize - 1)]++;
					}
					
					currentLineIsEmpty = true;
					currentLineLength = 0;
					currentLineIndent = 0;
				} else if (c == '\r') {
					continue;
				} else if (c == ' ' || c == '\t') {
					if (currentLineIsEmpty) {
						++currentLineIndent;
					}
				} else {
					currentLineIsEmpty = false;
				}
			}
		} while (length == BUFFER_SIZE);

		int nonEmptyLineCounter = lineCounter - emptyLineCounter;

		{
			int indent = 0;
			int lineWithIndent = 0;
			for (; indent < tableSize; ++indent) {
				lineWithIndent += stringIndentTable[indent];
				if (lineWithIndent > 0.1 * nonEmptyLineCounter) {
					break;
				}
			}
			myIgnoredIndent = (indent + 1);
		}

		{
			int breakType = 0;
			breakType |= BREAK_PARAGRAPH_AT_EMPTY_LINE;
	// TODO 测试下是否会有问题。默认情况下\n认定为换行
			breakType |= BREAK_PARAGRAPH_AT_NEW_LINE;
			if (stringsWithLengthLessThan81Counter < 0.3 * nonEmptyLineCounter) {
				breakType |= BREAK_PARAGRAPH_AT_NEW_LINE;
			} else {
				breakType |= BREAK_PARAGRAPH_AT_LINE_WITH_INDENT;
			}
			myBreakType = (breakType);
		}

		{
			int max = 0;
			int index;
			int emptyLinesBeforeNewSection = -1;
			for (index = 2; index < tableSize; ++index) {
				if (max < emptyLinesBeforeShortStringTable[index]) {
					max = emptyLinesBeforeShortStringTable[index];
					emptyLinesBeforeNewSection = index;
				}
			}
			if (emptyLinesBeforeNewSection > 0) {
				for (index = tableSize - 1; index > 0; --index) {
					emptyLinesTable[index - 1] += emptyLinesTable[index];	
					emptyLinesBeforeShortStringTable[index - 1] += emptyLinesBeforeShortStringTable[index];	
				}
				for (index = emptyLinesBeforeNewSection; index < tableSize; ++index) {
					if ((emptyLinesBeforeShortStringTable[index] > 2) &&
							(emptyLinesBeforeShortStringTable[index] > 0.7 * emptyLinesTable[index])) {
						break;
					}
				}
				emptyLinesBeforeNewSection = (index == tableSize) ? -1 : (int)index;
			}
			myEmptyLinesBeforeNewSection = (emptyLinesBeforeNewSection);
			myCreateContentsTable = (emptyLinesBeforeNewSection > 0);
		}

		myInitialized = (true);
	}
	
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
		final int BUFSIZE = 2048;
		
		String path = m_bookModel.Book.File.getPath();
		File file = new File(path);
		
		FileChannel streamReader = new RandomAccessFile(path, "r").getChannel();
		MappedByteBuffer mbb = streamReader.map(FileChannel.MapMode.READ_ONLY, 0, streamReader.size());
//		BufferedRandomAccessFile streamReader = new BufferedRandomAccessFile(file, "r");
		ByteBuffer bb = ByteBuffer.allocate(BUFSIZE);
		Charset cs = Charset.forName (m_bookModel.Book.getEncoding());
		while (true) {
			bb.clear();
			int count = streamReader.read(bb);
			if (count == -1 || count == 0) {
				break;
			}
			
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
	//					str.erase();
	//					myConverter->convert(str, inputBuffer + parBegin, inputBuffer + i + 1);
//							LOGD(str.c_str());
						
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
				//		str.erase();
				//		myConverter->convert(str, inputBuffer + parBegin, inputBuffer + maxLength);
				characterDataHandler(text, parBegin, maxLength - parBegin);
			}
		}

		streamReader.close();
		} catch (IOException e) {
		}
		
		endDocumentHandler();
	}

	protected void startDocumentHandler()
	{
		pushKind(BookModel.REGULAR);
		beginParagraph();
		myLineFeedCounter = 0;
		myInsideContentsParagraph = false;
		enterTitle();
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
				beginParagraph();
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
				enterTitle();
				pushKind(BookModel.SECTION_TITLE);
				beginParagraph();
				paragraphBreak = false;
			}
			if (myInsideContentsParagraph && (myLineFeedCounter == 1)) {
				exitTitle();
				endContentsParagraph();
				popKind();
				myInsideContentsParagraph = false;
				paragraphBreak = true;
			}
		}

		if (paragraphBreak) {
			internalEndParagraph();
			beginParagraph();
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
