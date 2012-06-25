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

package org.socool.screader.bookmodel;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;

import org.socool.zlibrary.image.ZLImage;
import org.socool.zlibrary.util.ZLArrayUtils;

public class BookReader {
	public BookModel m_bookModel;

	public boolean myTextParagraphExists = false;
	public boolean myTextParagraphIsNonEmpty = false;

	private char[] myTextBuffer = new char[4096];
	private int myTextBufferLength;
	private StringBuilder myContentsBuffer = new StringBuilder();

	private byte[] myKindStack = new byte[20];
	private int myKindStackSize;

	private byte myHyperlinkKind;
	private String myHyperlinkReference = "";

	public boolean myInsideTitle = false;
	private boolean mySectionContainsRegularContents = false;

	protected TOCTree myCurrentContentsTree;

	private CharsetDecoder myByteDecoder;

	public BookReader(BookModel model) {
		if (model != null) {
			m_bookModel = model;
			myCurrentContentsTree = model.TOCTree;
		} else {
			m_bookModel = null;
			myCurrentContentsTree = null;
		}
	}

	public final void setByteDecoder(CharsetDecoder decoder) {
		myByteDecoder = decoder;
	}

	private final void flushTextBufferToParagraph() {
		if (myTextBufferLength > 0) {
			m_bookModel.m_paragraph.addText(myTextBuffer, 0, myTextBufferLength);
			myTextBufferLength = 0;
			if (myByteDecoder != null) {
				myByteDecoder.reset();
			}
		}
	}

	public final void addControl(byte kind, boolean start) {
		if (myTextParagraphExists) {
			flushTextBufferToParagraph();
			m_bookModel.m_paragraph.addControl(kind, start);
		}
		if (!start && myHyperlinkReference.length() != 0 && kind == myHyperlinkKind) {
			myHyperlinkReference = "";
		}
	}

	public final void pushKind(byte kind) {
		byte[] stack = myKindStack;
		if (stack.length == myKindStackSize) {
			stack = ZLArrayUtils.createCopy(stack, myKindStackSize, myKindStackSize << 1);
			myKindStack = stack;
		}
		stack[myKindStackSize++] = kind;
	}

	public final boolean popKind() {
		if (myKindStackSize != 0) {
			--myKindStackSize;
			return true;
		}
		return false;
	}

	public final void beginParagraph(int kind) {
		endParagraph();
		final BookModel textModel = m_bookModel;
		if (textModel != null) {
			textModel.m_paragraph.createParagraph((byte)kind);
			
			final byte[] stack = myKindStack;
			final int size = myKindStackSize;
			for (int i = 0; i < size; ++i) {
				textModel.m_paragraph.addControl(stack[i], true);
			}
			if (myHyperlinkReference.length() != 0) {
				textModel.m_paragraph.addHyperlinkControl(myHyperlinkKind, hyperlinkType(myHyperlinkKind), myHyperlinkReference);
			}
			
			myTextParagraphExists = true;
		}
	}

	public final void endParagraph() {
		if (myTextParagraphExists) {
			flushTextBufferToParagraph();
			myTextParagraphExists = false;
			myTextParagraphIsNonEmpty = false;
		}
	}

	public final void insertEndParagraph(int kind) {
		if (m_bookModel != null && mySectionContainsRegularContents) {
			int size = m_bookModel.getParagraphNumber();
			if (size > 0 && m_bookModel.m_paragraph.getParagraphKind(size - 1) != kind) {
				m_bookModel.m_paragraph.createParagraph((byte)kind);
				mySectionContainsRegularContents = false;
			}
		}
	}

	public final void addData(char[] data) {
		addData(data, 0, data.length, false);
	}

	public final void addData(char[] data, int offset, int length, boolean direct) {
		if (!myTextParagraphExists || length == 0) {
			return;
		}
//		if (!myInsideTitle && !mySectionContainsRegularContents) {
//			while (length > 0 && Character.isWhitespace(data[offset])) {
//				--length;
//				++offset;
//			}
//			if (length == 0) {
//				return;
//			}
//		}

		myTextParagraphIsNonEmpty = true;

		if (direct && myTextBufferLength == 0 && !myInsideTitle) {
			m_bookModel.m_paragraph.addText(data, offset, length);
		} else {
			final int oldLength = myTextBufferLength;
			final int newLength = oldLength + length;
			if (myTextBuffer.length < newLength) {
				myTextBuffer = ZLArrayUtils.createCopy(myTextBuffer, oldLength, newLength);
			}
			System.arraycopy(data, offset, myTextBuffer, oldLength, length);
			myTextBufferLength = newLength;
			if (myInsideTitle) {
				addContentsData(myTextBuffer, oldLength, length);
			}
		}
		if (!myInsideTitle) {
			mySectionContainsRegularContents = true;
		}
	}

	private byte[] myUnderflowByteBuffer = new byte[4];
	private int myUnderflowLength;

	public final void addByteData(byte[] data, int start, int length) {
		if (!myTextParagraphExists || length == 0) {
			return;
		}
		myTextParagraphIsNonEmpty = true;

		final int oldLength = myTextBufferLength;
		if (myTextBuffer.length < oldLength + length) {
			myTextBuffer = ZLArrayUtils.createCopy(myTextBuffer, oldLength, oldLength + length);
		}
		final CharBuffer cb = CharBuffer.wrap(myTextBuffer, myTextBufferLength, length);

		if (myUnderflowLength > 0) {
			int l = myUnderflowLength;
			while (length-- > 0 && l < 4) {
				myUnderflowByteBuffer[l++] = data[start++];
				final ByteBuffer ubb = ByteBuffer.wrap(myUnderflowByteBuffer);
				myByteDecoder.decode(ubb, cb, false);
				if (cb.position() != oldLength) {
					myUnderflowLength = 0;
					break;
				}
			}
			if (length == 0) {
				myUnderflowLength = l;
				return;
			}
		}

		ByteBuffer bb = ByteBuffer.wrap(data, start, length);
		myByteDecoder.decode(bb, cb, false);
		myTextBufferLength = cb.position();
		int rem = bb.remaining();
		if (rem > 0) {
			for (int i = 0, j = start + length - rem; i < rem;) {
				myUnderflowByteBuffer[i++] = data[j++];
			}
			myUnderflowLength = rem;
		}

		if (myInsideTitle) {
			addContentsData(myTextBuffer, oldLength, myTextBufferLength - oldLength);
		} else {
			mySectionContainsRegularContents = true;
		}
	}

	private static byte hyperlinkType(byte kind) {
		return (kind == BookModel.EXTERNAL_HYPERLINK) ?
				BookModel.EXTERNAL : BookModel.INTERNAL;
	}

	public final void addHyperlinkControl(byte kind, String label) {
		if (myTextParagraphExists) {
			flushTextBufferToParagraph();
			m_bookModel.m_paragraph.addHyperlinkControl(kind, hyperlinkType(kind), label);
		}
		myHyperlinkKind = kind;
		myHyperlinkReference = label;
	}

	public final void addHyperlinkLabel(String label) {
		if (m_bookModel != null) {
			int paragraphNumber = m_bookModel.getParagraphNumber();
			if (myTextParagraphExists) {
				--paragraphNumber;
			}
			m_bookModel.addHyperlinkLabel(label, paragraphNumber);
		}
	}

	public final void addHyperlinkLabel(String label, int paragraphIndex) {
		m_bookModel.addHyperlinkLabel(label, paragraphIndex);
	}

	public final void addContentsData(char[] data) {
		addContentsData(data, 0, data.length);
	}

	public final void addContentsData(char[] data, int offset, int length) {
		if ((length != 0) && (myCurrentContentsTree != null)) {
			myContentsBuffer.append(data, offset, length);
		}
	}

	public final boolean hasContentsData() {
		return myContentsBuffer.length() > 0;
	}

	public final void beginContentsParagraph(int referenceNumber) {
		if (referenceNumber == -1) {
			referenceNumber = m_bookModel.getParagraphNumber();
		}
		TOCTree parentTree = myCurrentContentsTree;
		if (parentTree.Level > 0) {
			if (myContentsBuffer.length() > 0) {
				parentTree.setText(myContentsBuffer.toString());
				myContentsBuffer.delete(0, myContentsBuffer.length());
			} else if (parentTree.getText() == null) {
				parentTree.setText("...");
			}
		} else {
			myContentsBuffer.delete(0, myContentsBuffer.length());
		}
		TOCTree tree = new TOCTree(parentTree);
		tree.setReference(m_bookModel, referenceNumber);
		myCurrentContentsTree = tree;
	}

	public final void endContentsParagraph() {
		final TOCTree tree = myCurrentContentsTree;
		if (tree.Level == 0) {
			myContentsBuffer.delete(0, myContentsBuffer.length());
			return;
		}
		if (myContentsBuffer.length() > 0) {
			tree.setText(myContentsBuffer.toString());
			myContentsBuffer.delete(0, myContentsBuffer.length());
		} else if (tree.getText() == null) {
			tree.setText("...");
		}
		myCurrentContentsTree = tree.Parent;
	}

	public final boolean contentsParagraphIsOpen() {
		return myCurrentContentsTree.Level > 0;
	}

	public final void beginContentsParagraph() {
		beginContentsParagraph(-1);
	}

	public final void addImageReference(String ref, boolean isCover) {
		addImageReference(ref, (short)0, isCover);
	}

	public final void addImageReference(String ref, short vOffset, boolean isCover) {
		if (m_bookModel != null) {
			mySectionContainsRegularContents = true;
			if (myTextParagraphExists) {
				flushTextBufferToParagraph();
				m_bookModel.m_paragraph.addImage(ref, vOffset, isCover);
			} else {
				beginParagraph(BookParagraph.PARAGRAPH_KIND_TEXT_PARAGRAPH);
				m_bookModel.m_paragraph.addControl(BookModel.IMAGE, true);
				m_bookModel.m_paragraph.addImage(ref, vOffset, isCover);
				m_bookModel.m_paragraph.addControl(BookModel.IMAGE, false);
				endParagraph();
			}
		}
	}

	public final void addImage(String id, ZLImage image) {
		m_bookModel.addImage(id, image);
	}

	public final void addFixedHSpace(short length) {
		if (myTextParagraphExists) {
			m_bookModel.m_paragraph.addFixedHSpace(length);
		}
	}
}
