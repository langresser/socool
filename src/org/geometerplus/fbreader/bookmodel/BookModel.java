/*
 * Copyright (C) 2007-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.fbreader.bookmodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.geometerplus.zlibrary.image.ZLImage;
import org.geometerplus.zlibrary.text.ZLImageEntry;
import org.geometerplus.zlibrary.text.ZLTextMark;
import org.geometerplus.zlibrary.text.ZLTextParagraph;
import org.geometerplus.zlibrary.text.ZLTextStyleEntry;
import org.geometerplus.zlibrary.util.ZLArrayUtils;
import org.geometerplus.zlibrary.util.ZLSearchPattern;
import org.geometerplus.zlibrary.util.ZLSearchUtil;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.formats.FormatPlugin;

import android.util.Log;


public class BookModel {
	public final static byte NONE = 0;
	public final static byte INTERNAL = 1;
	public final static byte EXTERNAL = 2;

	
	public final static byte REGULAR = 0;
	public final static byte TITLE = 1;
	public final static byte SECTION_TITLE = 2;
	public final static byte POEM_TITLE = 3;
	public final static byte SUBTITLE = 4;
	public final static byte ANNOTATION = 5;
	public final static byte EPIGRAPH = 6;
	public final static byte STANZA = 7;
	public final static byte VERSE = 8;
	public final static byte PREFORMATTED = 9;
	public final static byte IMAGE = 10;
	//byte END_OF_SECTION = 11;
	public final static byte CITE = 12;
	public final static byte AUTHOR = 13;
	public final static byte DATE = 14;
	public final static byte INTERNAL_HYPERLINK = 15;
	public final static byte FOOTNOTE = 16;
	public final static byte EMPHASIS = 17;
	public final static byte STRONG = 18;
	public final static byte SUB = 19;
	public final static byte SUP = 20;
	public final static byte CODE = 21;
	public final static byte STRIKETHROUGH = 22;
	//byte CONTENTS_TABLE_ENTRY = 23;
	//byte LIBRARY_AUTHOR_ENTRY = 24;
	//byte LIBRARY_BOOK_ENTRY = 25;
	//byte LIBRARY_ENTRY = 25;
	//byte RECENT_BOOK_LIST = 26;
	public final static byte ITALIC = 27;
	public final static byte BOLD = 28;
	public final static byte DEFINITION = 29;
	public final static byte DEFINITION_DESCRIPTION = 30;
	public final static byte H1 = 31;
	public final static byte H2 = 32;
	public final static byte H3 = 33;
	public final static byte H4 = 34;
	public final static byte H5 = 35;
	public final static byte H6 = 36;
	public final static byte EXTERNAL_HYPERLINK = 37;
	//byte BOOK_HYPERLINK = 38;
	
	
	public static final byte ALIGN_UNDEFINED = 0;
	public static final byte ALIGN_LEFT = 1;
	public static final byte ALIGN_RIGHT = 2;
	public static final byte ALIGN_CENTER = 3;
	public static final byte ALIGN_JUSTIFY = 4;
	public static final byte ALIGN_LINESTART = 5; // left for LTR languages and right for RTL
	
	

	public static BookModel createModel(Book book) {
		final FormatPlugin plugin = book.getPlugin();
		final BookModel model = new BookModel(book);

//		Debug.startMethodTracing("socoolreader.trace");//calc为文件生成名
		plugin.readModel(model);
//		Debug.stopMethodTracing();
		return model;
	}

	protected BookModel(Book book) {
		Book = book;
		myId = null;
	}
	
	public Book Book = null;
	public final TOCTree TOCTree = new TOCTree();
	public final HashMap<String,ZLImage> myImageMap = new HashMap<String,ZLImage>();
	public final String myId;
	
	public static int READ_TYPE_NORMAL = 0;		// 正常读取方式，全文本读取
	public static int READ_TYPE_STREAM = 1;		// 流读取，部分文件读取
	public static int READ_TYPE_CHAPTER = 2;	// 书籍分为n章，章节读取

	public int m_readType = READ_TYPE_NORMAL;
	public boolean m_supportRichText = true;		// 当前文件是否支持富文本显示(txt不支持)
	public int m_beginParagraph = 0;
	public int m_allParagraphNumber = 0;
	public int m_currentBookIndex = 0;			// 当前书籍序号，只有章节读取可用
	public int m_fileCount = 0;					// 书籍总共由多少文件组成

	public ArrayList<ZLTextMark> myMarks;
	
	public HashMap<String, Label> myInternalHyperlinks = new HashMap<String, Label>();
	public Vector<Element> m_elements = new Vector<Element>();
	public Vector<ParagraphData> m_paragraphs = new Vector<ParagraphData>();

	public class ParagraphData
	{
		public int m_kind;
		public int m_textSize = 0;
		public Vector<Element> m_currentPara = new Vector<Element>();
		
		private ParagraphData(int kind)
		{
			m_kind = kind;
		}
	}
	
	public ParagraphData m_currentParagraph = null;

	public void clearParagraphData()
	{
		m_elements.clear();
		m_paragraphs.clear();
	}

	public void addImage(String id, ZLImage image) {
		myImageMap.put(id, image);
	}

	public final ZLTextMark getFirstMark() {
		return ((myMarks == null) || myMarks.isEmpty()) ? null : myMarks.get(0);
	}

	public final ZLTextMark getLastMark() {
		return ((myMarks == null) || myMarks.isEmpty()) ? null : myMarks.get(myMarks.size() - 1);
	}

	public final ZLTextMark getNextMark(ZLTextMark position) {
		if ((position == null) || (myMarks == null)) {
			return null;
		}

		ZLTextMark mark = null;
		for (ZLTextMark current : myMarks) {
			if (current.compareTo(position) >= 0) {
				if ((mark == null) || (mark.compareTo(current) > 0)) {
					mark = current;
				}
			}
		}
		return mark;
	}

	public final ZLTextMark getPreviousMark(ZLTextMark position) {
		if ((position == null) || (myMarks == null)) {
			return null;
		}

		ZLTextMark mark = null;
		for (ZLTextMark current : myMarks) {
			if (current.compareTo(position) < 0) {
				if ((mark == null) || (mark.compareTo(current) < 0)) {
					mark = current;
				}
			}
		}
		return mark;
	}

	public final int search(final String text, int startIndex, int endIndex, boolean ignoreCase) {
		int count = 0;
//		ZLSearchPattern pattern = new ZLSearchPattern(text, ignoreCase);
//		myMarks = new ArrayList<ZLTextMark>();
//		if (startIndex > getParagraphNumber()) {
//			startIndex = getParagraphNumber();
//		}
//		if (endIndex > getParagraphNumber()) {
//			endIndex = getParagraphNumber();
//		}
//		int index = startIndex;
//		final EntryIterator it = new EntryIterator(index);
//		while (true) {
//			int offset = 0;
//			while (it.hasNext()) {
//				it.next();
//				if (it.myType == ZLTextParagraph.Entry.TEXT) {
//					char[] textData = it.myTextData;
//					int textOffset = it.myTextOffset;
//					int textLength = it.myTextLength;
//					for (int pos = ZLSearchUtil.find(textData, textOffset, textLength, pattern); pos != -1;
//						pos = ZLSearchUtil.find(textData, textOffset, textLength, pattern, pos + 1)) {
//						myMarks.add(new ZLTextMark(index, offset + pos, pattern.getLength()));
//						++count;
//					}
//					offset += textLength;
//				}
//			}
//			if (++index >= endIndex) {
//				break;
//			}
//			it.reset(index);
//		}
		return count;
	}

	public final List<ZLTextMark> getMarks() {
		return (myMarks != null) ? myMarks : Collections.<ZLTextMark>emptyList();
	}
	
	public final int getParagraphNumber()
	{
		return m_allParagraphNumber;
	}

	public final byte getParagraphKind(int index)
	{
		index = index - m_beginParagraph;
		if (index < 0) {
			index = 0;
		}
		
		if (index >= m_paragraphs.size() - 1) {
			index = m_paragraphs.size() - 1;
		}

		final ParagraphData paragraph = m_paragraphs.get(index);
		return (byte)paragraph.m_kind;
	}

	public final ZLTextParagraph getParagraph(int index) {
		Log.d("getParagraph", String.format("para: %1d   begin:%2d    size:%3d", index, m_beginParagraph, m_paragraphs.size()));
		if (m_readType == READ_TYPE_STREAM || m_readType == READ_TYPE_CHAPTER) {
			if (index < m_allParagraphNumber && (index >= m_beginParagraph + m_paragraphs.size() || index < m_beginParagraph)) {
				Book.getPlugin().readParagraph(index);
			}
		}

		ParagraphData paragraph = m_paragraphs.get(index - m_beginParagraph);
		return new ZLTextParagraph(this, index, (byte)paragraph.m_kind);
	}

	public final int getTextLength(int index) {
		index = index - m_beginParagraph;
		if (index < 0) {
			index = 0;
		}
		
		if (index >= m_paragraphs.size() - 1) {
			index = m_paragraphs.size() - 1;
		}

		ParagraphData paragraph = m_paragraphs.get(index);
		return paragraph.m_textSize;
	}

	public void createParagraph(byte kind) {
		if (m_readType != READ_TYPE_STREAM && m_readType != READ_TYPE_CHAPTER) {
			m_allParagraphNumber = getParagraphNumber();
		}
		
		if (m_currentParagraph != null) {
			m_paragraphs.add(m_currentParagraph);
		}
		
		m_currentParagraph = new ParagraphData(kind);
	}
	
	public class Element {
		public int m_type;
		public char[] m_text = null;

		public String m_imageId = null;
		public short m_imagevOffset = 0;
		public boolean m_isCover = false;

		public short m_kind = 0;
		public boolean m_isStart = false;
		
		public byte m_hyperlinkType = 0;
		public String m_label = null;
		
		public ZLTextStyleEntry m_textStyle = null;
		
		public int m_len = 0;
		
		Element(int type)
		{
			m_type = type;
		}

		Element(char[] text, int offset, int length)
		{
			m_type = ZLTextParagraph.Entry.TEXT;
			m_text = new char[length];
			System.arraycopy(text, offset, m_text, 0, length);
		}
		
		Element(String id, short vOffset, boolean isCover)
		{
			m_type = ZLTextParagraph.Entry.IMAGE;
			m_imageId = id;
			m_imagevOffset = vOffset;
			m_isCover = isCover;
		}
		
		Element(short textKind, boolean isStart)
		{
			m_type = ZLTextParagraph.Entry.CONTROL;
			m_kind = textKind;
			m_isStart = isStart;
		}
		
		Element(byte textKind, byte hyperlinkType, String label)
		{
			m_type = ZLTextParagraph.Entry.HYPERLINK_CONTROL;
			m_kind = textKind;
			m_hyperlinkType = hyperlinkType;
			m_label = label;
		}
		
		Element(ZLTextStyleEntry entry)
		{
			m_type = ZLTextParagraph.Entry.STYLE;
			m_textStyle = entry;
		}
		
		Element(int type, int len)
		{
			m_type = type;
			m_len = len;
		}
	}

	public void addText(char[] text, int offset, int length) {
		m_currentParagraph.m_textSize += length;
		m_currentParagraph.m_currentPara.add(new Element(text, offset, length));
	}

	public void addImage(String id, short vOffset, boolean isCover) {
		m_currentParagraph.m_currentPara.add(new Element(id, vOffset, isCover));
	}

	public void addControl(byte textKind, boolean isStart) {
		short kind = textKind;
		if (isStart) {
			kind += 0x0100;
		}
		m_currentParagraph.m_currentPara.add(new Element(kind, isStart));
	}

	public void addHyperlinkControl(byte textKind, byte hyperlinkType, String label) {
		m_currentParagraph.m_currentPara.add(new Element(textKind, hyperlinkType, label));
	}

	public void addStyleEntry(ZLTextStyleEntry entry) {	
		m_currentParagraph.m_currentPara.add(new Element(entry));
	}

	public void addFixedHSpace(short length) {
		m_currentParagraph.m_currentPara.add(new Element(ZLTextParagraph.Entry.FIXED_HSPACE, length));
	}	

	public void addBidiReset() {
		m_currentParagraph.m_currentPara.add(new Element(ZLTextParagraph.Entry.RESET_BIDI));
	}
	
	public interface LabelResolver {
		List<String> getCandidates(String id);
	}

	public static final class Label {
		public final String ModelId;
		public final int ParagraphIndex;

		public Label(String modelId, int paragraphIndex) {
			ModelId = modelId;
			ParagraphIndex = paragraphIndex;
		}
	}

	void addHyperlinkLabel(String label, int paragraphNumber) {
		myInternalHyperlinks.put(label, new Label(myId, paragraphNumber));
	}

	public LabelResolver myResolver;

	// 不直接显示，作为内部链接可以进行跳转
	public Label getLabel(String id) {
		Label label = myInternalHyperlinks.get(id);
		if (label == null && myResolver != null) {
			for (String candidate : myResolver.getCandidates(id)) {
				label = myInternalHyperlinks.get(candidate);
				if (label != null) {
					break;
				}
			}
		}
		return label;
	}
}
