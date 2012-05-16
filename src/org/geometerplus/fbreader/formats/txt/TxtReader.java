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

import android.util.Log;

public final class TxtReader extends BookReader {
	public final static int BREAK_PARAGRAPH_AT_NEW_LINE = 1;
	public final static int BREAK_PARAGRAPH_AT_EMPTY_LINE = 2;
	public final static int BREAK_PARAGRAPH_AT_LINE_WITH_INDENT = 4;

	public boolean myInitialized = false;
	public int myBreakType = BREAK_PARAGRAPH_AT_NEW_LINE;
	public int myIgnoredIndent = 1;
	public int myEmptyLinesBeforeNewSection = 1;
	public boolean myCreateContentsTable = false;
	
	public final int BUFFER_SIZE = 1024 * 10;		// 假定文件缓存区有10k
	public final int MAX_BUFFER_SIZE = 1024 * 30;	// 最大缓存区有30k，实际读取的文件总字节数在10k~30k之间，前后会多一个段落的数据
	

	public int m_currentOffset = 0;

	public FileChannel m_streamReader = null;
	
	public HashMap<Integer, Integer> m_paraOffset = new HashMap<Integer, Integer>();
	
	public TxtReader(BookModel model)
	{
		super(model);
		
		if (model != null) {
			String path = model.Book.File.getPath();
			
			try {
				m_streamReader = new RandomAccessFile(path, "r").getChannel();
				initParagraphData();
			} catch (Exception e) {
				
			}
		}
	}
	
	public void setModel(BookModel model)
	{
		// 如果换文件，则关闭原文件
		if (m_streamReader != null && !m_bookModel.Book.File.getPath().equalsIgnoreCase(model.Book.File.getPath())) {
			try {
				m_streamReader.close();
			} catch (Exception e) {
				
			}
		}

		m_bookModel = model;
		myCurrentContentsTree = model.TOCTree;
		
		String path = m_bookModel.Book.File.getPath();

		try {
			m_streamReader = new RandomAccessFile(path, "r").getChannel();
			initParagraphData();
		} catch (IOException e) {
			
		}
	}

	// TODO 考虑全是\r形式的换行符文件 测试 \n和\r\n形式的文件
	private void initParagraphData()
	{
		long startTime = System.currentTimeMillis();
		int count = 0;
		long allTime = 0;
		try {

		final long size = m_streamReader.size();
		int currentOffset = 0;
		final String encoding = m_bookModel.Book.getEncoding();
		
//		ByteBuffer bb = ByteBuffer.allocate(BUFFER_SIZE);
//		byte[] buffer = new byte[BUFFER_SIZE];

		int paraCount = 0;
		m_paraOffset.put(0, 0);
		byte lastReadByte = -1;
		byte[] byteBuffer = new byte[BUFFER_SIZE];
		
		ByteBuffer mapBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		do {
			++count;
			int readSize = (int)size / BUFFER_SIZE == 0 ? (int)size % BUFFER_SIZE : BUFFER_SIZE;
//			MappedByteBuffer mapBuffer = m_streamReader.map(FileChannel.MapMode.READ_ONLY, currentOffset, readSize);
//			int count = m_streamReader.read(bb);
//			bb.flip();
//			bb.get(buffer);
			mapBuffer.clear();
			readSize = m_streamReader.read(mapBuffer);
			long start = System.currentTimeMillis();
			mapBuffer.flip();
			mapBuffer.get(byteBuffer, 0, readSize);
			allTime += System.currentTimeMillis() - start;
	
			for (int i = 0; i < readSize; ++i) {
				byte c = byteBuffer[i];//0;//mapBuffer.get(i);
				
				// 记录每个新段落对应的文件偏移(整个文件最后一个字符为换行符则忽略)
				if (c == 0x0a && currentOffset + i < size - 1) {
					// unicode需要判断前一个或后一个字节内容
					if (encoding.equalsIgnoreCase("utf-16le")) {
						 // 0d 00 0a 00
						if (i + 1 < readSize) {
							byte cn = mapBuffer.get(i + 1);
							if (cn == 0) {
								++paraCount;
								int offset = currentOffset + i + 2;
								m_paraOffset.put(paraCount, offset);
							}
						} else if (i == readSize - 1) {
							// 缓存区末尾恰好为\n，则要再读取一个字节，进行判断
							MappedByteBuffer mapBufferNext = m_streamReader.map(FileChannel.MapMode.READ_ONLY, currentOffset + readSize, 1);
							byte cn = mapBufferNext.get(0);
							if (cn == 0) {
								++paraCount;
								int offset = currentOffset + i + 2;
								m_paraOffset.put(paraCount, offset);
							}
						}
					} else if (encoding.equalsIgnoreCase("utf-16be")) {
						// 00 0d 00 0a
						if (i - 1 >= 0) {
							byte cp = byteBuffer[i - 1];
							if (cp == 0) {
								++paraCount;
								int offset = currentOffset + i + 1;
								m_paraOffset.put(paraCount, offset);
							}
						} else if (i == 0) {
							// 如果\n为文件开始，则要判断上次读取的缓存区的最后一个字符
							if (lastReadByte == 0) {
								++paraCount;
								int offset = currentOffset + i + 1;
								m_paraOffset.put(paraCount, offset);
							}
						}
					} else {
						// utf-8或ansi直接判断字符，也不需要考虑\n在缓存区头，或者缓存区末尾的情况
						++paraCount;
						int offset = currentOffset + i + 1;
						m_paraOffset.put(paraCount, offset);
					}
				}
			}
			
			lastReadByte = byteBuffer[readSize - 1];
			currentOffset += readSize;
		} while (currentOffset < size);

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		double lastTime = (System.currentTimeMillis() - startTime) / 1000.0;
		Log.d("ProfileTime", "init1: " + lastTime + "count:" + count + "all:" + allTime / 1000.0);
		Log.d("InitPara", "Para: " + m_paraOffset.size() + "lastOffset: " + m_paraOffset.get(m_paraOffset.size() - 1));
	}
		
	public void readDocument(int paragraph)
	{
		startDocumentHandler();

		try {
		
		final int fileOffset = m_paraOffset.get(paragraph);
		final long size = m_streamReader.size();

		int readOffset = 0;
		int maxSize = BUFFER_SIZE;
		int paraStart = fileOffset;
		

		if (fileOffset > BUFFER_SIZE) {
			readOffset = fileOffset - BUFFER_SIZE;
			maxSize += BUFFER_SIZE;
			paraStart = BUFFER_SIZE;
		} else {
			readOffset = 0;
			maxSize += fileOffset;
			paraStart = fileOffset;
		}
		
		if (fileOffset + BUFFER_SIZE > size) {
			maxSize += size - fileOffset;
		} else {
			maxSize += BUFFER_SIZE;
		}
		
		// 读取30k的内容，保留10k的最小缓存区，然后从开头结尾再截取出完整段落
		MappedByteBuffer mappedBuffer = m_streamReader.map(FileChannel.MapMode.READ_ONLY, readOffset, maxSize);
		
		final String encoding = m_bookModel.Book.getEncoding();
		int ii = paraStart;
		for (; ii > 0; --ii) {
			byte c = mappedBuffer.get(ii);
			if (c == 0x0a) {
				if (encoding.equalsIgnoreCase("utf-16le")) {
					// 0d 00 0a 00
					byte cn = mappedBuffer.get(ii + 1);
					if (cn == 0) {
						ii += 2;
						break;
					}
				} else if (encoding.equalsIgnoreCase("utf-16be")) {
					// 00 0d 00 0a
					if (ii >= 1) {
						byte cp = mappedBuffer.get(ii - 1);
						if (cp == 0) {
							++ii;
							break;
						}
					}
				} else {
					++ii;
					break;
				}
			}
		}
		
		int jj = paraStart + BUFFER_SIZE;
		for (; jj < maxSize; ++jj) {
			byte c = mappedBuffer.get(jj);
			if (c == 0x0a) {
				if (encoding.equalsIgnoreCase("utf-16le")) {
					// 0d 00 0a 00
					if (jj + 1 < maxSize) {
						byte cn = mappedBuffer.get(jj + 1);
						if (cn == 0) {
							jj -= 3;
							break;
						}	
					}
				} else if (encoding.equalsIgnoreCase("utf-16be")) {
					// 00 0d 00 0a
					if (jj >= 1) {
						byte cp = mappedBuffer.get(jj - 1);
						if (cp == 0) {
							jj -= 4;
							break;
						}
					}
				} else {
					--jj;
					break;
				}
			}
		}
		
		ByteBuffer buffer = ByteBuffer.allocate(jj - ii);
		for (int i = ii; i < jj; ++i) {
			buffer.put(i - ii, mappedBuffer.get(i));
		}

		Charset cs = Charset.forName (encoding);
	    CharBuffer cb = cs.decode(buffer);
		char[] text = cb.array();
		int maxLength = text.length;
		int parBegin = 0;

		for (int i = 0; i < maxLength; ++i) {
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

		m_streamReader.close();
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
