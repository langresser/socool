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

		System.err.println("using plugin: " + plugin.supportedFileType() + "/" + plugin.type());

		final BookModel model = new BookModel(book);

//		Debug.startMethodTracing("socoolreader.trace");//calc为文件生成名
		plugin.readModel(model);
//		Debug.stopMethodTracing();
		return model;
	}

	protected BookModel(Book book) {
		Book = book;
		myId = null;
		myLanguage = book.getLanguage();
		
		m_paragraphStartIndex = new int[1024];
		myParagraphLengths = new int[1024];
		myTextSizes = new int[1024];
		myParagraphKinds = new byte[1024];
	}
	
	public Book Book = null;
	public final TOCTree TOCTree = new TOCTree();
	protected final HashMap<String,ZLImage> myImageMap = new HashMap<String,ZLImage>();
	public final String myId;
	public final String myLanguage;
	
	public boolean m_isStreamRead = false;			// 当前文件是否支持流读取(txt支持)
	public boolean m_supportRichText = true;		// 当前文件是否支持富文本显示(txt不支持)
	public int m_beginParagraph = 0;
	public int m_endParagraph = 0;
	public int m_allParagraphNumber = 0;
	public int m_allTextSize = 0;

	protected int[] m_paragraphStartIndex;
	protected int[] myParagraphLengths;
	protected int[] myTextSizes;
	protected byte[] myParagraphKinds;

	public int myParagraphsNumber;
	public ArrayList<ZLTextMark> myMarks;
	
	public HashMap<String, Label> myInternalHyperlinks = new HashMap<String, Label>();
	public Vector<Element> m_elements = new Vector<Element>();

	public void clearParagraphData()
	{
		myParagraphsNumber = 0;
		m_elements.clear();
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
		ZLSearchPattern pattern = new ZLSearchPattern(text, ignoreCase);
		myMarks = new ArrayList<ZLTextMark>();
		if (startIndex > myParagraphsNumber) {
			startIndex = myParagraphsNumber;
		}
		if (endIndex > myParagraphsNumber) {
			endIndex = myParagraphsNumber;
		}
		int index = startIndex;
		final EntryIterator it = new EntryIterator(index);
		while (true) {
			int offset = 0;
			while (it.hasNext()) {
				it.next();
				if (it.myType == ZLTextParagraph.Entry.TEXT) {
					char[] textData = it.myTextData;
					int textOffset = it.myTextOffset;
					int textLength = it.myTextLength;
					for (int pos = ZLSearchUtil.find(textData, textOffset, textLength, pattern); pos != -1;
						pos = ZLSearchUtil.find(textData, textOffset, textLength, pattern, pos + 1)) {
						myMarks.add(new ZLTextMark(index, offset + pos, pattern.getLength()));
						++count;
					}
					offset += textLength;
				}
			}
			if (++index >= endIndex) {
				break;
			}
			it.reset(index);
		}
		return count;
	}

	public final List<ZLTextMark> getMarks() {
		return (myMarks != null) ? myMarks : Collections.<ZLTextMark>emptyList();
	}
	
	public final byte getParagraphKind(int index)
	{
		final byte kind = myParagraphKinds[index - m_beginParagraph];
		return kind;
	}

	public final ZLTextParagraph getParagraph(int index) {
		Log.d("getParagraph", String.format("para: %1d   begin:%2d    end:%3d", index, m_beginParagraph, m_endParagraph));
		if (m_isStreamRead) {
			if (index < m_allParagraphNumber && (index >= m_endParagraph || index < m_beginParagraph)) {
				Book.getPlugin().readParagraph(index);
			}
		}

		final byte kind = myParagraphKinds[index - m_beginParagraph];
		return new ZLTextParagraph(this, index, kind);
	}

	public final int getTextLength(int index) {
		return myTextSizes[Math.max(Math.min(index - m_beginParagraph, myParagraphsNumber - 1), 0)];
	}

	private static int binarySearch(int[] array, int length, int value) {
		int lowIndex = 0;
		int highIndex = length - 1;

		while (lowIndex <= highIndex) {
			int midIndex = (lowIndex + highIndex) >>> 1;
			int midValue = array[midIndex];
			if (midValue > value) {
				highIndex = midIndex - 1;
			} else if (midValue < value) {
				lowIndex = midIndex + 1;
			} else {
				return midIndex;
			}
		}
		return -lowIndex - 1;
	}

	public final int findParagraphByTextLength(int length) {
		int index = binarySearch(myTextSizes, myParagraphsNumber, length);
		if (index >= 0) {
			return index;
		}
		return Math.min(-index - 1, myParagraphsNumber - 1);
	}

	public void createParagraph(byte kind) {
		final int index = myParagraphsNumber++;
		if (!m_isStreamRead) {
			m_allParagraphNumber = myParagraphsNumber;
		}

		if (index == myParagraphLengths.length) {
			final int size = myParagraphLengths.length;
			m_paragraphStartIndex = ZLArrayUtils.createCopy(m_paragraphStartIndex, size, size << 1);
			myParagraphLengths = ZLArrayUtils.createCopy(myParagraphLengths, size, size << 1);
			myTextSizes = ZLArrayUtils.createCopy(myTextSizes, size, size << 1);
			myParagraphKinds = ZLArrayUtils.createCopy(myParagraphKinds, size, size << 1);
		}
		if (index > 0) {
			myTextSizes[index] = myTextSizes[index - 1];
		}

		m_paragraphStartIndex[index] = m_elements.size();
		myParagraphLengths[index] = 0;
		myParagraphKinds[index] = kind;
	}
	
	class Element {
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
//			Log.d("holi", m_text);
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
		++myParagraphLengths[myParagraphsNumber - 1];
		myTextSizes[myParagraphsNumber - 1] += length;
		m_elements.add(new Element(text, offset, length));
	}

	public void addImage(String id, short vOffset, boolean isCover) {
		++myParagraphLengths[myParagraphsNumber - 1];
		m_elements.add(new Element(id, vOffset, isCover));
	}

	public void addControl(byte textKind, boolean isStart) {
		++myParagraphLengths[myParagraphsNumber - 1];
		short kind = textKind;
		if (isStart) {
			kind += 0x0100;
		}
		m_elements.add(new Element(kind, isStart));
	}

	public void addHyperlinkControl(byte textKind, byte hyperlinkType, String label) {
		++myParagraphLengths[myParagraphsNumber - 1];
		m_elements.add(new Element(textKind, hyperlinkType, label));
	}

	public void addStyleEntry(ZLTextStyleEntry entry) {
		++myParagraphLengths[myParagraphsNumber - 1];		
		m_elements.add(new Element(entry));
	}

	public void addFixedHSpace(short length) {
		++myParagraphLengths[myParagraphsNumber - 1];
		m_elements.add(new Element(ZLTextParagraph.Entry.FIXED_HSPACE, length));
	}	

	public void addBidiReset() {
		++myParagraphLengths[myParagraphsNumber - 1];
		m_elements.add(new Element(ZLTextParagraph.Entry.RESET_BIDI));
	}
	
	
	public final class EntryIterator {
		public int myCounter;
		public int myLength;
		public byte myType;
		
		public int m_index;

		// TextEntry data
		public char[] myTextData = null;
		public int myTextOffset;
		public int myTextLength;

		// ControlEntry data
		public byte myControlKind;
		public boolean myControlIsStart;
		// HyperlinkControlEntry data
		public byte myHyperlinkType;
		public String myHyperlinkId;

		// ImageEntry
		public ZLImageEntry myImageEntry;

		// StyleEntry
		public ZLTextStyleEntry myStyleEntry;

		// FixedHSpaceEntry data
		public short myFixedHSpaceLength;

		public EntryIterator(int index) {
			myLength = myParagraphLengths[index - m_beginParagraph];
			m_index = m_paragraphStartIndex[index - m_beginParagraph];
			myCounter = 0;
			Log.d("EntryIterator", "length:" + myLength + "index:" +  m_index);
		}

		void reset(int index) {
			myCounter = 0;
			myLength = myParagraphLengths[index - m_beginParagraph];
			m_index = m_paragraphStartIndex[index - m_beginParagraph];
			Log.d("reset", "length:" + myLength + "index:" +  m_index);
		}

		public boolean hasNext() {
			return myCounter < myLength;
		}

		public void next() {
			Log.d("next", String.format("index:%1d   all:%1d", m_index, m_elements.size()));
			Element element = m_elements.get(m_index);
			myType = (byte)element.m_type;
			switch (element.m_type) {
				case ZLTextParagraph.Entry.TEXT:
					myTextLength = element.m_text.length;
					myTextData = element.m_text;
					Log.d("next", new String(element.m_text));
					myTextOffset = 0;
					break;
				case ZLTextParagraph.Entry.CONTROL:
				{
					short kind = (short)element.m_kind;
					myControlKind = (byte)kind;
					myControlIsStart = (kind & 0x0100) == 0x0100;
					myHyperlinkType = 0;
					break;
				}
				case ZLTextParagraph.Entry.HYPERLINK_CONTROL:
				{
					myControlKind = (byte)element.m_kind;
					myControlIsStart = true;
					myHyperlinkType = (byte)element.m_hyperlinkType;
					myHyperlinkId = element.m_label;
					break;
				}
				case ZLTextParagraph.Entry.IMAGE:
				{
					final short vOffset = (short)element.m_imagevOffset;
					final String id = element.m_imageId;
					final boolean isCover = element.m_isCover;
					myImageEntry = new ZLImageEntry(myImageMap, id, vOffset, isCover);
					break;
				}
				case ZLTextParagraph.Entry.FIXED_HSPACE:
					myFixedHSpaceLength = (short)element.m_len;
					break;
				case ZLTextParagraph.Entry.STYLE:
					myStyleEntry = element.m_textStyle;
					break;
				case ZLTextParagraph.Entry.RESET_BIDI:
					// No data => skip
					break;
			}
			++m_index;
			++myCounter;
		}
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
