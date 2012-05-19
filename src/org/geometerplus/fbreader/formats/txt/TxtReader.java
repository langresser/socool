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
import org.geometerplus.zlibrary.text.ZLTextParagraph;

import android.util.Log;

public final class TxtReader extends BookReader {
	public final static int BREAK_PARAGRAPH_AT_NEW_LINE = 1;
	public final static int BREAK_PARAGRAPH_AT_EMPTY_LINE = 2;
	public final static int BREAK_PARAGRAPH_AT_LINE_WITH_INDENT = 4;

	public boolean myInitialized = false;
	public int myIgnoredIndent = 1;
	public int myEmptyLinesBeforeNewSection = 1;
	
	public final int BUFFER_SIZE = 1024 * 10;		// 假定文件缓存区有10k

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
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		m_bookModel = model;
		myCurrentContentsTree = model.TOCTree;
		
		String path = m_bookModel.Book.File.getPath();

		try {
			m_streamReader = new RandomAccessFile(path, "r").getChannel();
			initParagraphData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// TODO 考虑全是\r形式的换行符文件 测试 \n和\r\n形式的文件
	private void initParagraphData()
	{
		long startTime = System.currentTimeMillis();
		int count = 0;
		try {

		final long size = m_streamReader.size();
		int currentOffset = 0;
		final String encoding = m_bookModel.Book.getEncoding();
		
		int paraCount = 0;
		m_paraOffset.put(0, 0);
		byte lastReadByte = -1;
		byte[] byteBuffer = new byte[BUFFER_SIZE];
		
		ByteBuffer mapBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);
		do {
			++count;
			int readSize = (int)size / BUFFER_SIZE == 0 ? (int)size % BUFFER_SIZE : BUFFER_SIZE;
			mapBuffer.clear();
			readSize = m_streamReader.read(mapBuffer);
			mapBuffer.flip();
			mapBuffer.get(byteBuffer, 0, readSize);
	
			for (int i = 0; i < readSize; ++i) {
				byte c = byteBuffer[i];
				
				// 记录每个新段落对应的文件偏移(整个文件最后一个字符为换行符则忽略)
				if (c == 0x0a && currentOffset + i < size - 1) {
					// unicode需要判断前一个或后一个字节内容
					if (encoding.equalsIgnoreCase("utf-16le")) {
						 // 0d 00 0a 00
						if (i + 1 < readSize) {
							byte cn = byteBuffer[i + 1];
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

		m_bookModel.m_allParagraphNumber = paraCount;
		m_bookModel.m_allTextSize = (int)size;

		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		double lastTime = (System.currentTimeMillis() - startTime) / 1000.0;
		Log.d("ProfileTime", "init1: " + lastTime + "count:" + count);
		Log.d("InitPara", "Para: " + m_paraOffset.size() + "lastOffset: " + m_paraOffset.get(m_paraOffset.size() - 1));
	}
	
	public int getParagraphByOffset(int offset)
	{
		final int size = m_paraOffset.size();
		final int lastOffset = m_paraOffset.get(m_bookModel.m_allParagraphNumber - 1);
		if (offset >= lastOffset) {
			return m_bookModel.m_allParagraphNumber - 1;
		}

		for (int i = 0; i < size; ++i) {
			if (m_paraOffset.get(i) > offset) {
				Log.d("getParagraphByOffset", "para:" + (i - 1) + "offset:" + offset);
				return i - 1;
			}
		}

		return 0;
	}
		
	public void readDocument(int paragraph)
	{
		Log.d("readDocument", "read:" + paragraph);
		startDocumentHandler();

		try {
		
		final int fileOffset = m_paraOffset.get(paragraph);
		final long size = m_streamReader.size();

		int readOffset = 0;
		int maxSize = BUFFER_SIZE;
		int paraStart = 0;
		

		if (size <= BUFFER_SIZE) {
			// 如果文件比较小，就直接读取
			readOffset = 0;
			maxSize = (int)size;
			paraStart = 0;
		} else {
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
		}
		
		// 读取30k的内容，保留10k的最小缓存区，然后从开头结尾再截取出完整段落
		ByteBuffer mapBuffer = ByteBuffer.allocateDirect(maxSize);
		byte[] buffer = new byte[maxSize];
		m_streamReader.position(readOffset);
		m_streamReader.read(mapBuffer);
		mapBuffer.flip();
		mapBuffer.get(buffer);
//		MappedByteBuffer mappedBuffer = m_streamReader.map(FileChannel.MapMode.READ_ONLY, readOffset, maxSize);
		
		final String encoding = m_bookModel.Book.getEncoding();
		int ii = paraStart;
		for (; ii > 0; --ii) {
			byte c = buffer[ii];
			if (c == 0x0a) {
				if (encoding.equalsIgnoreCase("utf-16le")) {
					// 0d 00 0a 00
					byte cn = buffer[ii + 1];
					if (cn == 0) {
						ii += 2;
						break;
					}
				} else if (encoding.equalsIgnoreCase("utf-16be")) {
					// 00 0d 00 0a
					if (ii >= 1) {
						byte cp = buffer[ii - 1];
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
			byte c = buffer[jj];
			if (c == 0x0a) {
				if (encoding.equalsIgnoreCase("utf-16le")) {
					// 0d 00 0a 00
					if (jj + 1 < maxSize) {
						byte cn = buffer[jj + 1];
						if (cn == 0) {
							jj -= 3;
							break;
						}	
					}
				} else if (encoding.equalsIgnoreCase("utf-16be")) {
					// 00 0d 00 0a
					if (jj >= 1) {
						byte cp = buffer[jj - 1];
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
		
		ii = Math.max(0, ii);
		jj = Math.min(jj, maxSize);

		ByteBuffer textBuffer = ByteBuffer.wrap(buffer, ii, jj - ii);

		Charset cs = Charset.forName (encoding);
	    CharBuffer cb = cs.decode(textBuffer);
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


		m_bookModel.m_beginParagraph = getParagraphByOffset(ii + readOffset);
		m_bookModel.m_endParagraph = getParagraphByOffset(jj + readOffset);

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
