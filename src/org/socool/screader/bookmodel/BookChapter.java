package org.socool.screader.bookmodel;

import java.util.ArrayList;
import java.util.Vector;

import org.socool.screader.screader.FBReaderApp;

import android.util.Log;

// 维护了章节信息，与data.txt配置相关
public class BookChapter {
	static public class BookChapterData
	{
		public int m_startOffset;
		public int m_paragraphCount;

		public int m_textSize;
		public int m_startTxtOffset;

		public String m_title;
		public int m_juanIndex;
		
		public ArrayList<Integer> paragraphOffset = new ArrayList<Integer>();
	}
	
	public BookChapter()
	{
		
	}
	
	public void addChapterData(BookChapterData data)
	{
		m_chapterData.add(data);
	}
	
	public int getChapterCount()
	{
		return m_chapterData.size();
	}
	
	public int getChapterOffset(int fileNum)
	{
		if (fileNum < 0 || fileNum >= m_chapterData.size()) {
			return 0;
		}

		return m_chapterData.get(fileNum).m_startOffset;
	}
	
	public BookChapterData getChapter(int fileNum)
	{
		if (fileNum < 0 || fileNum >= m_chapterData.size()) {
			return null;
		}

		return m_chapterData.get(fileNum);
	}
	
	public int getChapterTextOffset(int fileNum)
	{
		if (fileNum < 0 || fileNum >= m_chapterData.size()) {
			return 0;
		}

		return m_chapterData.get(fileNum).m_startTxtOffset;
	}
	
	public int getParagraphTextOffset(int paragraph)
	{
		int chapterIndex = getChapterIndexByParagraph(paragraph);
		if (chapterIndex < 0) {
			Log.e("chapterIndex", String.format("%1d  %2d  %3d", paragraph, chapterIndex, m_chapterData.size()));
			chapterIndex = 0;
		}
		
		if (chapterIndex > m_chapterData.size() - 1) {
			Log.e("chapterIndex", String.format("%1d  %2d  %3d", paragraph, chapterIndex, m_chapterData.size()));
			chapterIndex = m_chapterData.size() - 1;
		}

		final BookChapterData data = m_chapterData.get(chapterIndex);
		int paragraphIndex = paragraph - data.m_startOffset;
		if (paragraphIndex < 0) {
			Log.e("paragraphIndex", String.format("%1d  chapter:%2d  %3d  para:%4d %5d", paragraph, chapterIndex, m_chapterData.size(), paragraphIndex, data.paragraphOffset.size()));
			paragraphIndex = 0;
		}
		
		if (paragraphIndex > data.paragraphOffset.size() - 1) {
			Log.e("paragraphIndex", String.format("%1d  chapter:%2d  %3d  para:%4d %5d", paragraph, chapterIndex, m_chapterData.size(), paragraphIndex, data.paragraphOffset.size()));
			paragraphIndex = data.paragraphOffset.size() - 1;
		}

		return data.paragraphOffset.get(paragraphIndex);
	}
	
	public int getParagraphCount(int fileNum)
	{
		if (fileNum < 0 || fileNum >= m_chapterData.size()) {
			return 0;
		}

		return m_chapterData.get(fileNum).m_paragraphCount;
	}
	
	public String getChapterTitle(int fileNum)
	{
		if (fileNum < 0 || fileNum >= m_chapterData.size()) {
			return "";
		}

		return m_chapterData.get(fileNum).m_title;
	}
	
	public void gotoPositionByOffset(int offset)
	{
		final int chapter = getChapterByTxtOffset(offset);
		final int chapterStart = m_chapterData.get(chapter).m_startOffset;
		final int paragraph = getParagraphIndexByTxtOffset(chapter, offset);
		final int paragraphStart = m_chapterData.get(chapter).paragraphOffset.get(paragraph);
		final int word = offset - paragraphStart;

		Log.d("goto", String.format("%1d   %2d  %3d", offset, paragraph + chapterStart, word));
		FBReaderApp.Instance().BookTextView.gotoPosition(paragraph + chapterStart, word, 0);
	}
	
	public int getParagraphIndexByTxtOffset(int chapter, int offset)
	{
		final ArrayList<Integer> paraOffset = m_chapterData.get(chapter).paragraphOffset;
		final int amount = paraOffset.size();
		int low = 0;
		int high = amount - 1;
		int mid = 0;
		
		while (low < high) {
			mid = low + (high - low) / 2;
			final int midOffset = paraOffset.get(mid);
			if (midOffset == offset) {
				return mid;
			} else if (midOffset > offset) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}
		
		if (low < 0) {
			return 0;
		}
		
		if (low > amount - 1) {
			return amount - 1;
		}
		
		final int loffset = paraOffset.get(low);
		if (offset < loffset) {
			if (low == 0) {
				return 0;
			} else {
				final int lloffset = paraOffset.get(low - 1);
				if (offset < lloffset) {
					Log.e("errorl", String.format("getChapterByTxtOffset: offset: %1d  ll:%2d  l:%3d", offset, lloffset, loffset));
				}
				return low - 1;
			}
		} else {
			if (low == amount -1) {
				return amount - 1;
			} else {
				final int hoffset = paraOffset.get(low + 1);
				if (offset >= hoffset) {
					Log.e("errorh", String.format("getChapterByTxtOffset: offset %1d h: %2d  l:%3d", offset, hoffset, loffset));
					return low + 1;
				} else {
					return low;
				}
			}
		}
	}
	
	public int getChapterByTxtOffset(int offset)
	{
		if (offset <= 1) {
			return 0;
		}
		
		final int amount = m_chapterData.size();
		int lastPara = m_chapterData.get(amount - 1).m_startTxtOffset;
		if (offset >= lastPara) {
			return amount - 1;
		}

		int low = 0;
		int high = amount - 1;
		int mid = 0;

		while (low < high) {
			mid = low + (high - low) / 2;
			final int midOffset = m_chapterData.get(mid).m_startTxtOffset;
			if (midOffset == offset) {
				return mid;
			} else if (midOffset > offset) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}
		
		if (low < 0) {
			return 0;
		}
		
		if (low > amount - 1) {
			return amount - 1;
		}
		
		final int loffset = m_chapterData.get(low).m_startTxtOffset;
		if (offset < loffset) {
			if (low == 0) {
				return 0;
			} else {
				final int lloffset = m_chapterData.get(low - 1).m_startTxtOffset;
				if (offset < lloffset) {
					Log.e("errorl", String.format("getChapterByTxtOffset: offset: %1d  ll:%2d  l:%3d", offset, lloffset, loffset));
				}
				return low - 1;
			}
		} else {
			if (low == amount -1) {
				return amount - 1;
			} else {
				final int hoffset = m_chapterData.get(low + 1).m_startTxtOffset;
				if (offset >= hoffset) {
					Log.e("errorh", String.format("getChapterByTxtOffset: offset %1d h: %2d  l:%3d", offset, hoffset, loffset));
					return low + 1;
				} else {
					return low;
				}
			}
		}
	}
	
	public int getChapterIndexByParagraph(int para)
	{
		if (para <= 1) {
			return 0;
		}
		
		final int amount = m_chapterData.size();
		int lastPara = m_chapterData.get(amount - 1).m_startOffset;
		if (para >= lastPara) {
			return amount - 1;
		}

		int low = 0;
		int high = amount - 1;
		int mid = 0;

		while (low < high) {
			mid = low + (high - low) / 2;
			final int midOffset = m_chapterData.get(mid).m_startOffset;
			if (midOffset == para) {
				return mid;
			} else if (midOffset > para) {
				high = mid - 1;
			} else {
				low = mid + 1;
			}
		}
		
		if (low < 0) {
			return 0;
		}
		
		if (low > amount - 1) {
			return amount - 1;
		}
		
		final int loffset = m_chapterData.get(low).m_startOffset;
		if (para < loffset) {
			if (low == 0) {
				return 0;
			} else {
				final int lloffset = m_chapterData.get(low - 1).m_startOffset;
				if (para < lloffset) {
					Log.e("errorl", String.format("getChapterIndexByParagraph: offset: %1d  ll:%2d  l:%3d", para, lloffset, loffset));
				}
				return low - 1;
			}
		} else {
			if (low == amount -1) {
				return amount - 1;
			} else {
				final int hoffset = m_chapterData.get(low + 1).m_startOffset;
				if (para >= hoffset) {
					Log.e("errorh", String.format("getChapterIndexByParagraph: offset %1d h: %2d  l:%3d", para, hoffset, loffset));
					return low + 1;
				} else {
					return low;
				}
			}
		}
	}
	
	public ArrayList<BookChapterData> m_chapterData = new ArrayList<BookChapterData>();
	
	static public class JuanData
	{
		public String m_juanTitle;
		public ArrayList<Integer> m_juanChapter = new ArrayList<Integer>();
	}
	
	public String getJuanTitle(int index)
	{
		if (index < 0) {
			index = 0;
		}
		
		if (index > m_juanData.size() - 1) {
			index = m_juanData.size() - 1;
		}
		
		return m_juanData.get(index).m_juanTitle;
	}

	public ArrayList<JuanData> m_juanData = new ArrayList<JuanData>();
	public int m_allParagraphNumber = 0;
	public int m_allTextSize = 0;
	
	public int m_currentJuanIndex = -1;
}
