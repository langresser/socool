package org.socool.screader.bookmodel;

import java.util.Vector;

import org.socool.screader.screader.FBReaderApp;
import org.socool.zlibrary.text.ZLTextStyleEntry;

import android.util.Log;

// 维护书籍加载后的段落数据
public class BookParagraph {
	static public final int PARAGRAPH_ELEMENT_TEXT = 1;
	static public final int PARAGRAPH_ELEMENT_IMAGE = 2;
	static public final int PARAGRAPH_ELEMENT_CONTROL = 3;
	static public final int PARAGRAPH_ELEMENT_HYPERLINK_CONTROL = 4;
	static public final int PARAGRAPH_ELEMENT_STYLE = 5;
	static public final int PARAGRAPH_ELEMENT_FIXED_HSPACE = 6;
	static public final int PARAGRAPH_ELEMENT_RESET_BIDI = 7;
	
	
	static public final int PARAGRAPH_KIND_TEXT_PARAGRAPH = 0;
	static public final int PARAGRAPH_KIND_EMPTY_LINE_PARAGRAPH = 2;
	static public final int PARAGRAPH_KIND_BEFORE_SKIP_PARAGRAPH = 3;
	static public final int PARAGRAPH_KIND_AFTER_SKIP_PARAGRAPH = 4;
	static public final int PARAGRAPH_KIND_END_OF_SECTION_PARAGRAPH = 5;
	static public final int PARAGRAPH_KIND_END_OF_TEXT_PARAGRAPH = 6;

	public class ParagraphData
	{
		public int m_kind;
		public int m_textSize = 0;
		public int m_index = 0;
		public Vector<Element> m_paragraphElement = new Vector<Element>();
		
		private ParagraphData(int kind)
		{
			m_kind = kind;
		}
	}

	public Vector<ParagraphData> m_paragraphs = new Vector<ParagraphData>();
	public ParagraphData m_currentParagraph = null;
	public int m_beginParagraph = 0;

	public void clearParagraphData()
	{
		m_paragraphs.clear();
	}
	
	public final byte getParagraphKind(int index)
	{
		final ParagraphData paragraph = getParagraph(index);
		return (byte)paragraph.m_kind;
	}
	
	public final ParagraphData getParagraph(int index) {
		if (FBReaderApp.Instance().Model.m_readType == BookModel.READ_TYPE_STREAM 
				|| FBReaderApp.Instance().Model.m_readType == BookModel.READ_TYPE_CHAPTER) {
			if (index >= m_beginParagraph + m_paragraphs.size() || index < m_beginParagraph) {
				FBReaderApp.Instance().Model.Book.getPlugin().readParagraph(index);
			}
		}

		ParagraphData paragraph = m_paragraphs.get(index - m_beginParagraph);
		paragraph.m_index = index;
		return paragraph;
	}
	
	public final String getText(int index)
	{
		index = index - m_beginParagraph;
		if (index < 0) {
			return "";
		}
		
		if (index >= m_paragraphs.size() - 1) {
			return "";
		}
		
		ParagraphData paragraph = m_paragraphs.get(index);
		if (paragraph.m_textSize > 0) {
			for (Element each :  paragraph.m_paragraphElement) {
				if (each.m_type == BookParagraph.PARAGRAPH_ELEMENT_TEXT) {
					return new String(each.m_text); 
				}
			}
		}
		
		return "";
	}

	public final int getTextLength(int index) {
		index = index - m_beginParagraph;
		if (index < 0) {
			index = 0;
		}
		
		if (index > m_paragraphs.size() - 1) {
			index = m_paragraphs.size() - 1;
		}

		ParagraphData paragraph = m_paragraphs.get(index);
		return paragraph.m_textSize;
	}

	public void createParagraph(int kind) {
//		if (FBReaderApp.Instance().Model.m_readType != BookModel.READ_TYPE_STREAM
//				&& FBReaderApp.Instance().Model.m_readType != BookModel.READ_TYPE_CHAPTER) {
//			++m_allParagraphNumber;
//		}
		
		if (m_currentParagraph != null) {
			m_paragraphs.add(m_currentParagraph);
		}

		m_currentParagraph = new ParagraphData(kind);
	}
	
	public void insertEndOfChapter()
	{
		if (m_currentParagraph != null) {
			m_paragraphs.add(m_currentParagraph);
		}

		m_currentParagraph = null;
		m_paragraphs.add(new ParagraphData(PARAGRAPH_KIND_END_OF_SECTION_PARAGRAPH));
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
			m_type = PARAGRAPH_ELEMENT_TEXT;
			m_text = new char[length];
			System.arraycopy(text, offset, m_text, 0, length);
		}

		Element(String id, short vOffset, boolean isCover)
		{
			m_type = PARAGRAPH_ELEMENT_IMAGE;
			m_imageId = id;
			m_imagevOffset = vOffset;
			m_isCover = isCover;
		}

		Element(short textKind, boolean isStart)
		{
			m_type = PARAGRAPH_ELEMENT_CONTROL;
			m_kind = textKind;
			m_isStart = isStart;
		}
		
		Element(byte textKind, byte hyperlinkType, String label)
		{
			m_type = PARAGRAPH_ELEMENT_HYPERLINK_CONTROL;
			m_kind = textKind;
			m_hyperlinkType = hyperlinkType;
			m_label = label;
		}
		
		Element(ZLTextStyleEntry entry)
		{
			m_type = PARAGRAPH_ELEMENT_STYLE;
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
		m_currentParagraph.m_paragraphElement.add(new Element(text, offset, length));
	}

	public void addImage(String id, short vOffset, boolean isCover) {
		m_currentParagraph.m_paragraphElement.add(new Element(id, vOffset, isCover));
	}

	public void addControl(byte textKind, boolean isStart) {
		short kind = textKind;
		if (isStart) {
			kind += 0x0100;
		}
		m_currentParagraph.m_paragraphElement.add(new Element(kind, isStart));
	}

	public void addHyperlinkControl(byte textKind, byte hyperlinkType, String label) {
		m_currentParagraph.m_paragraphElement.add(new Element(textKind, hyperlinkType, label));
	}

	public void addStyleEntry(ZLTextStyleEntry entry) {	
		m_currentParagraph.m_paragraphElement.add(new Element(entry));
	}

	public void addFixedHSpace(short length) {
		m_currentParagraph.m_paragraphElement.add(new Element(PARAGRAPH_ELEMENT_FIXED_HSPACE, length));
	}	

	public void addBidiReset() {
		m_currentParagraph.m_paragraphElement.add(new Element(PARAGRAPH_ELEMENT_RESET_BIDI));
	}
}
