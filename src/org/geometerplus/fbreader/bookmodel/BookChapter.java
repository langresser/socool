package org.geometerplus.fbreader.bookmodel;

import java.util.Vector;

// 维护了章节信息，与data.txt配置相关
public class BookChapter {
	public class BookChapterData
	{
		public String m_fileName;
		public int m_startOffset;
		public int m_paragraphCount;
		public int m_textSize;
		public String m_title;
		
		public BookChapterData(String fileName, int startOffset, int paragraphCount, int textSize, String title)
		{
			m_fileName = fileName;
			m_startOffset = startOffset;
			m_paragraphCount = paragraphCount;
			m_textSize = textSize;
			m_title = title;
		}
	}
	
	public BookChapter()
	{
		
	}
	
	public void addChapterData(String fileName, int startOffset, int paragraphCount, int textSize, String title)
	{
		m_chapterData.add(new BookChapterData(fileName, startOffset, paragraphCount, textSize, title));
	}
	
	public int getChapterCount()
	{
		return m_chapterData.size();
	}
	
	public int getChapterOffset(int fileNum)
	{
		if (fileNum <= 0 || fileNum > m_chapterData.size()) {
			return 0;
		}

		return m_chapterData.get(fileNum - 1).m_startOffset;
	}
	
	public int getParagraphCount(int fileNum)
	{
		if (fileNum <= 0 || fileNum > m_chapterData.size()) {
			return 0;
		}

		return m_chapterData.get(fileNum - 1).m_paragraphCount;
	}
	
	public int getChapterIndexByParagraph(int para)
	{
		if (para <= 1) {
			return 1;
		}
		
		final int amount = m_chapterData.size();
		int lastPara = m_chapterData.get(amount - 1).m_startOffset;
		if (para >= lastPara) {
			return amount;
		}

		int low = 0;
		int high = amount - 1;
		int mid = 0;

		while (low <= high) {
			mid = low + (high - low) / 2;
			final int midOffset = m_chapterData.get(mid).m_startOffset;
			if (midOffset == para) {
				return mid + 1;
			} else if (midOffset > para) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}
		
		if (low > high) {
			final int loffset = m_chapterData.get(high).m_startOffset;
			final int roffset = m_chapterData.get(low).m_startOffset;
			if (para >= loffset && para < roffset) {
				return high + 1;
			}
		}
		
		return 1;
	}
	
	public Vector<BookChapterData> m_chapterData = new Vector<BookChapterData>();
}
