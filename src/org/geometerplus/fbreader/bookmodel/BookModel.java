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

import org.geometerplus.zlibrary.image.ZLImage;
import org.geometerplus.zlibrary.text.CachedCharStorageBase;
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

	public static final class Label {
		public final String ModelId;
		public final int ParagraphIndex;

		public Label(String modelId, int paragraphIndex) {
			ModelId = modelId;
			ParagraphIndex = paragraphIndex;
		}
	}

	protected BookModel(Book book) {
		Book = book;
		myInternalHyperlinks = new CachedCharStorageBase(32768, Paths.cacheDirectory(), "links", false);
		myId = null;
		myLanguage = book.getLanguage();
		myStartEntryIndices = new int[1024];
		myStartEntryOffsets = new int[1024];
		myParagraphLengths = new int[1024];
		myTextSizes = new int[1024];
		myParagraphKinds = new byte[1024];
		myStorage = new CachedCharStorageBase(65535, Paths.cacheDirectory(), "cache", false);
	}

	private char[] myCurrentDataBlock;
	private int myBlockOffset;
	private void extend() {
		final int size = myStartEntryIndices.length;
		myStartEntryIndices = ZLArrayUtils.createCopy(myStartEntryIndices, size, size << 1);
		myStartEntryOffsets = ZLArrayUtils.createCopy(myStartEntryOffsets, size, size << 1);
		myParagraphLengths = ZLArrayUtils.createCopy(myParagraphLengths, size, size << 1);
		myTextSizes = ZLArrayUtils.createCopy(myTextSizes, size, size << 1);
		myParagraphKinds = ZLArrayUtils.createCopy(myParagraphKinds, size, size << 1);
	}

	public interface LabelResolver {
		List<String> getCandidates(String id);
	}

	public LabelResolver myResolver;

	public Label getLabel(String id) {
		Label label = getLabelInternal(id);
		if (label == null && myResolver != null) {
			for (String candidate : myResolver.getCandidates(id)) {
				label = getLabelInternal(candidate);
				if (label != null) {
					break;
				}
			}
		}
		return label;
	}

	protected Label getLabelInternal(String id) {
		final int len = id.length();
		final int size = myInternalHyperlinks.myArray.size();

		for (int i = 0; i < size; ++i) {
			final char[] block = myInternalHyperlinks.block(i);
			for (int offset = 0; offset < block.length; ) {
				final int labelLength = (int)block[offset++];
				if (labelLength == 0) {
					break;
				}
				final int idLength = (int)block[offset + labelLength];
				if ((labelLength != len) || !id.equals(new String(block, offset, labelLength))) {
					offset += labelLength + idLength + 3;
					continue;
				}
				offset += labelLength + 1;
				final String modelId = (idLength > 0) ? new String(block, offset, idLength) : null;
				offset += idLength;
				final int paragraphNumber = (int)block[offset] + (((int)block[offset + 1]) << 16);
				return new Label(modelId, paragraphNumber);
			}
		}
		return null;
	}

	public void addImage(String id, ZLImage image) {
		myImageMap.put(id, image);
	}
	
	public Book Book = null;
	public final TOCTree TOCTree = new TOCTree();
	protected CachedCharStorageBase myInternalHyperlinks;
	protected final HashMap<String,ZLImage> myImageMap = new HashMap<String,ZLImage>();
	private char[] myCurrentLinkBlock;
	private int myCurrentLinkBlockOffset;

	void addHyperlinkLabel(String label, int paragraphNumber) {
		final String modelId = myId;
		final int labelLength = label.length();
		final int idLength = (modelId != null) ? modelId.length() : 0;
		final int len = 4 + labelLength + idLength;

		char[] block = myCurrentLinkBlock;
		int offset = myCurrentLinkBlockOffset;
		if ((block == null) || (offset + len > block.length)) {
			if (block != null) {
				myInternalHyperlinks.freezeLastBlock();
			}
			block = myInternalHyperlinks.createNewBlock(len);
			myCurrentLinkBlock = block;
			offset = 0;
		}
		block[offset++] = (char)labelLength;
		label.getChars(0, labelLength, block, offset);
		offset += labelLength;
		block[offset++] = (char)idLength;
		if (idLength > 0) {
			modelId.getChars(0, idLength, block, offset);
			offset += idLength;
		}
		block[offset++] = (char)paragraphNumber;
		block[offset++] = (char)(paragraphNumber >> 16);
		myCurrentLinkBlockOffset = offset;
	}

	public final String myId;
	public final String myLanguage;

	protected int[] myStartEntryIndices;
	protected int[] myStartEntryOffsets;
	protected int[] myParagraphLengths;
	protected int[] myTextSizes;
	protected byte[] myParagraphKinds;

	public int myParagraphsNumber;

	protected final CachedCharStorageBase myStorage;

	public ArrayList<ZLTextMark> myMarks;
	
	

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

	public final ZLTextParagraph getParagraph(int index) {
		final byte kind = myParagraphKinds[index];
		return new ZLTextParagraph(this, index, kind);
	}

	public final int getTextLength(int index) {
		return myTextSizes[Math.max(Math.min(index, myParagraphsNumber - 1), 0)];
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
		int[] startEntryIndices = myStartEntryIndices;
		if (index == startEntryIndices.length) {
			extend();
			startEntryIndices = myStartEntryIndices;
		}
		if (index > 0) {
			myTextSizes[index] = myTextSizes[index - 1];
		}
		final int dataSize = myStorage.myArray.size();
		startEntryIndices[index] = (dataSize == 0) ? 0 : (dataSize - 1);
		myStartEntryOffsets[index] = myBlockOffset;
		myParagraphLengths[index] = 0;
		myParagraphKinds[index] = kind;
	}

	private char[] getDataBlock(int minimumLength) {
		char[] block = myCurrentDataBlock;
		if ((block == null) || (minimumLength > block.length - myBlockOffset)) {
			if (block != null) {
				myStorage.freezeLastBlock();
			}
			block = myStorage.createNewBlock(minimumLength);
			myCurrentDataBlock = block;
			myBlockOffset = 0;
		}
		return block;
	}

	public void addText(char[] text, int offset, int length) {
		char[] block = getDataBlock(3 + length);
		++myParagraphLengths[myParagraphsNumber - 1];
		int blockOffset = myBlockOffset;
		block[blockOffset++] = (char)ZLTextParagraph.Entry.TEXT;
		block[blockOffset++] = (char)length;
		block[blockOffset++] = (char)(length >> 16);
		System.arraycopy(text, offset, block, blockOffset, length);
		myBlockOffset = blockOffset + length;
		myTextSizes[myParagraphsNumber - 1] += length;
	}

	public void addImage(String id, short vOffset, boolean isCover) {
		final int len = id.length();
		final char[] block = getDataBlock(4 + len);
		++myParagraphLengths[myParagraphsNumber - 1];
		int blockOffset = myBlockOffset;
		block[blockOffset++] = (char)ZLTextParagraph.Entry.IMAGE;
		block[blockOffset++] = (char)vOffset;
		block[blockOffset++] = (char)len;
		id.getChars(0, len, block, blockOffset);
		blockOffset += len;
		block[blockOffset++] = (char)(isCover ? 1 : 0);
		myBlockOffset = blockOffset;
	}

	public void addControl(byte textKind, boolean isStart) {
		final char[] block = getDataBlock(2);
		++myParagraphLengths[myParagraphsNumber - 1];
		block[myBlockOffset++] = (char)ZLTextParagraph.Entry.CONTROL;
		short kind = textKind;
		if (isStart) {
			kind += 0x0100;
		}
		block[myBlockOffset++] = (char)kind;
	}

	public void addHyperlinkControl(byte textKind, byte hyperlinkType, String label) {
		final short labelLength = (short)label.length();
		final char[] block = getDataBlock(3 + labelLength);
		++myParagraphLengths[myParagraphsNumber - 1];
		int blockOffset = myBlockOffset;
		block[blockOffset++] = (char)ZLTextParagraph.Entry.HYPERLINK_CONTROL;
		block[blockOffset++] = (char)((hyperlinkType << 8) + textKind);
		block[blockOffset++] = (char)labelLength;
		label.getChars(0, labelLength, block, blockOffset);
		myBlockOffset = blockOffset + labelLength;
	}

	public void addStyleEntry(ZLTextStyleEntry entry) {
		int len = 2;
		for (int mask = entry.getMask(); mask != 0; mask >>= 1) {
			len += mask & 1;
		}
		final char[] block = getDataBlock(len);
		++myParagraphLengths[myParagraphsNumber - 1];
		block[myBlockOffset++] = (char)ZLTextParagraph.Entry.STYLE;
		block[myBlockOffset++] = (char)entry.getMask();
		if (entry.isLeftIndentSupported()) {
			block[myBlockOffset++] = (char)entry.getLeftIndent();
		}
		if (entry.isRightIndentSupported()) {
			block[myBlockOffset++] = (char)entry.getRightIndent();
		}
		if (entry.isAlignmentTypeSupported()) {
			block[myBlockOffset++] = (char)entry.getAlignmentType();
		}
	}

	public void addFixedHSpace(short length) {
		final char[] block = getDataBlock(2);
		++myParagraphLengths[myParagraphsNumber - 1];
		block[myBlockOffset++] = (char)ZLTextParagraph.Entry.FIXED_HSPACE;
		block[myBlockOffset++] = (char)length;
	}	

	public void addBidiReset() {
		final char[] block = getDataBlock(1);
		++myParagraphLengths[myParagraphsNumber - 1];
		block[myBlockOffset++] = (char)ZLTextParagraph.Entry.RESET_BIDI;
	}
	
	
	
	
	public final class EntryIterator {
		public int myCounter;
		public int myLength;
		public byte myType;

		int myDataIndex;
		int myDataOffset;

		// TextEntry data
		public char[] myTextData;
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
			myLength = myParagraphLengths[index];
			myDataIndex = myStartEntryIndices[index];
			myDataOffset = myStartEntryOffsets[index];
		}

		void reset(int index) {
			myCounter = 0;
			myLength = myParagraphLengths[index];
			myDataIndex = myStartEntryIndices[index];
			myDataOffset = myStartEntryOffsets[index];
		}

		public boolean hasNext() {
			return myCounter < myLength;
		}

		public void next() {
			int dataOffset = myDataOffset;
			char[] data = myStorage.block(myDataIndex);
			if (dataOffset == data.length) {
				data = myStorage.block(++myDataIndex);
				dataOffset = 0;
			}
			byte type = (byte)data[dataOffset];
			if (type == 0) {
				data = myStorage.block(++myDataIndex);
				dataOffset = 0;
				type = (byte)data[0];
			}
			myType = type;
			++dataOffset;
			switch (type) {
				case ZLTextParagraph.Entry.TEXT:
					myTextLength =
						(int)data[dataOffset++] +
						(((int)data[dataOffset++]) << 16);
					myTextData = data;
					myTextOffset = dataOffset;
					dataOffset += myTextLength;
					break;
				case ZLTextParagraph.Entry.CONTROL:
				{
					short kind = (short)data[dataOffset++];
					myControlKind = (byte)kind;
					myControlIsStart = (kind & 0x0100) == 0x0100;
					myHyperlinkType = 0;
					break;
				}
				case ZLTextParagraph.Entry.HYPERLINK_CONTROL:
				{
					short kind = (short)data[dataOffset++];
					myControlKind = (byte)kind;
					myControlIsStart = true;
					myHyperlinkType = (byte)(kind >> 8);
					short labelLength = (short)data[dataOffset++];
					myHyperlinkId = new String(data, dataOffset, labelLength);
					dataOffset += labelLength;
					break;
				}
				case ZLTextParagraph.Entry.IMAGE:
				{
					final short vOffset = (short)data[dataOffset++];
					final short len = (short)data[dataOffset++];
					final String id = new String(data, dataOffset, len);
					dataOffset += len;
					final boolean isCover = data[dataOffset++] != 0;
					myImageEntry = new ZLImageEntry(myImageMap, id, vOffset, isCover);
					break;
				}
				case ZLTextParagraph.Entry.FIXED_HSPACE:
					myFixedHSpaceLength = (short)data[dataOffset++];
					break;
				case ZLTextParagraph.Entry.STYLE:
				{
					final int mask = (int)data[dataOffset++];
					final ZLTextStyleEntry entry = new ZLTextStyleEntry();
					if ((mask & ZLTextStyleEntry.SUPPORTS_LEFT_INDENT) ==
								ZLTextStyleEntry.SUPPORTS_LEFT_INDENT) {
						entry.setLeftIndent((short)data[dataOffset++]);
					}
					if ((mask & ZLTextStyleEntry.SUPPORTS_RIGHT_INDENT) ==
								ZLTextStyleEntry.SUPPORTS_RIGHT_INDENT) {
						entry.setRightIndent((short)data[dataOffset++]);
					}
					if ((mask & ZLTextStyleEntry.SUPPORTS_ALIGNMENT_TYPE) ==
								ZLTextStyleEntry.SUPPORTS_ALIGNMENT_TYPE) {
						entry.setAlignmentType((byte)data[dataOffset++]);
					}
					myStyleEntry = entry;
				}
				case ZLTextParagraph.Entry.RESET_BIDI:
					// No data => skip
					break;
			}
			++myCounter;
			myDataOffset = dataOffset;
		}
	}
}
