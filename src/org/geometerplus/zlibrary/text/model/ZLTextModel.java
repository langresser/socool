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

package org.geometerplus.zlibrary.text.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geometerplus.zlibrary.image.ZLImageMap;
import org.geometerplus.zlibrary.util.ZLArrayUtils;
import org.geometerplus.zlibrary.util.ZLSearchPattern;
import org.geometerplus.zlibrary.util.ZLSearchUtil;

public class ZLTextModel {
	public static final byte ALIGN_UNDEFINED = 0;
	public static final byte ALIGN_LEFT = 1;
	public static final byte ALIGN_RIGHT = 2;
	public static final byte ALIGN_CENTER = 3;
	public static final byte ALIGN_JUSTIFY = 4;
	public static final byte ALIGN_LINESTART = 5; // left for LTR languages and right for RTL

	private final String myId;
	private final String myLanguage;

	protected int[] myStartEntryIndices;
	protected int[] myStartEntryOffsets;
	protected int[] myParagraphLengths;
	protected int[] myTextSizes;
	protected byte[] myParagraphKinds;

	protected int myParagraphsNumber;

	protected final CachedCharStorageBase myStorage;
	protected final ZLImageMap myImageMap;

	private ArrayList<ZLTextMark> myMarks;
	
	public boolean m_isNavite = false;

	final class EntryIteratorImpl implements ZLTextParagraph.EntryIterator {
		private int myCounter;
		private int myLength;
		private byte myType;

		int myDataIndex;
		int myDataOffset;

		// TextEntry data
		private char[] myTextData;
		private int myTextOffset;
		private int myTextLength;

		// ControlEntry data
		private byte myControlKind;
		private boolean myControlIsStart;
		// HyperlinkControlEntry data
		private byte myHyperlinkType;
		private String myHyperlinkId;

		// ImageEntry
		private ZLImageEntry myImageEntry;

		// StyleEntry
		private ZLTextStyleEntry myStyleEntry;

		// FixedHSpaceEntry data
		private short myFixedHSpaceLength;

		EntryIteratorImpl(int index) {
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

		public byte getType() {
			return myType;
		}

		public char[] getTextData() {
			return myTextData;
		}
		public int getTextOffset() {
			return myTextOffset;
		}
		public int getTextLength() {
			return myTextLength;
		}

		public byte getControlKind() {
			return myControlKind;
		}
		public boolean getControlIsStart() {
			return myControlIsStart;
		}
		public byte getHyperlinkType() {
			return myHyperlinkType;
		}
		public String getHyperlinkId() {
			return myHyperlinkId;
		}

		public ZLImageEntry getImageEntry() {
			return myImageEntry;
		}

		public ZLTextStyleEntry getStyleEntry() {
			return myStyleEntry;
		}

		public short getFixedHSpaceLength() {
			return myFixedHSpaceLength;
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

	public ZLTextModel(
			String id, String language, int paragraphsNumber,
			int[] entryIndices, int[] entryOffsets,
			int[] paragraphLengths, int[] textSizes,
			byte[] paragraphKinds,
			String directoryName, String fileExtension, int blocksNumber,
			ZLImageMap imageMap
		) {
		this(
				id, language,
				entryIndices, entryOffsets, paragraphLengths, textSizes, paragraphKinds,
				new CachedCharStorageBase(blocksNumber, directoryName, fileExtension, true),
				imageMap, true
			);
			myParagraphsNumber = paragraphsNumber;
	}

	protected ZLTextModel(
		String id,
		String language,
		int[] entryIndices,
		int[] entryOffsets,
		int[] paragraphLenghts,
		int[] textSizes,
		byte[] paragraphKinds,
		CachedCharStorageBase storage,
		ZLImageMap imageMap,
		boolean isNative
	) {
		myId = id;
		myLanguage = language;
		myStartEntryIndices = entryIndices;
		myStartEntryOffsets = entryOffsets;
		myParagraphLengths = paragraphLenghts;
		myTextSizes = textSizes;
		myParagraphKinds = paragraphKinds;
		myStorage = storage;
		myImageMap = imageMap;
		m_isNavite = isNative;
	}

	public final String getId() {
		return myId;
	}

	public final String getLanguage() {
		return myLanguage;
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
		final EntryIteratorImpl it = new EntryIteratorImpl(index);
		while (true) {
			int offset = 0;
			while (it.hasNext()) {
				it.next();
				if (it.getType() == ZLTextParagraph.Entry.TEXT) {
					char[] textData = it.getTextData();
					int textOffset = it.getTextOffset();
					int textLength = it.getTextLength();
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

	public final void removeAllMarks() {
		myMarks = null;
	}

	public final int getParagraphsNumber() {
		return myParagraphsNumber;
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
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// write able
	public ZLTextModel(String id, String language, int arraySize, int dataBlockSize, String directoryName, String extension, ZLImageMap imageMap) {
		this(
			id, language,
			new int[arraySize], new int[arraySize],
			new int[arraySize], new int[arraySize],
			new byte[arraySize],
			new CachedCharStorageBase(dataBlockSize, directoryName, extension, false),
			imageMap, false
		);
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
		final int dataSize = myStorage.size();
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

	public void addText(char[] text) {
		addText(text, 0, text.length);
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

	public void stopReading() {
	}
}
