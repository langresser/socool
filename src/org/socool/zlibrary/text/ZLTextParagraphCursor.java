package org.socool.zlibrary.text;

import java.util.*;


import org.socool.screader.bookmodel.BookChapter;
import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.bookmodel.BookParagraph;
import org.socool.screader.screader.FBReaderApp;
import org.socool.zlibrary.image.*;
import org.socool.zlibrary.util.LineBreaker;

import android.util.Log;

public final class ZLTextParagraphCursor {
	private static final class Processor {
		private final BookParagraph.ParagraphData myParagraph;
		private final ArrayList<ZLTextElement> myElements;
		private int myOffset;
		private int myFirstMark;
		private int myLastMark;
		private final List<ZLTextMark> myMarks;

		private Processor(BookParagraph.ParagraphData paragraph, List<ZLTextMark> marks, int paragraphIndex, ArrayList<ZLTextElement> elements) {
			myParagraph = paragraph;
			myElements = elements;
			myMarks = marks;
			final ZLTextMark mark = new ZLTextMark(paragraphIndex, 0, 0);
			int i;
			for (i = 0; i < myMarks.size(); i++) {
				if (((ZLTextMark)myMarks.get(i)).compareTo(mark) >= 0) {
					break;
				}
			}
			myFirstMark = i;
			myLastMark = myFirstMark;
			for (; (myLastMark != myMarks.size()) && (((ZLTextMark)myMarks.get(myLastMark)).ParagraphIndex == paragraphIndex); myLastMark++);
			myOffset = 0;
		}

		void fill() {
			int hyperlinkDepth = 0;
			ZLTextHyperlink hyperlink = null;

			final ArrayList<ZLTextElement> elements = myElements;
			final BookParagraph.ParagraphData paragraphData = myParagraph;
			final int elementCount = paragraphData.m_paragraphElement.size();
			for (int i = 0; i < elementCount; ++i) {
				BookParagraph.Element element = paragraphData.m_paragraphElement.get(i);
				switch (element.m_type) {
				case BookParagraph.PARAGRAPH_ELEMENT_TEXT:
					processTextEntry(element.m_text, 0, element.m_text.length, hyperlink);
					break;
				case BookParagraph.PARAGRAPH_ELEMENT_CONTROL:
					if (hyperlink != null) {
						hyperlinkDepth += ((element.m_kind & 0x0100) == 0x0100) ? 1 : -1;
						if (hyperlinkDepth == 0) {
							hyperlink = null;
						}
					}
					elements.add(ZLTextControlElement.get((byte)element.m_kind, (element.m_kind & 0x0100) == 0x0100));
					break;
				case BookParagraph.PARAGRAPH_ELEMENT_HYPERLINK_CONTROL:
				{
					final byte hyperlinkType = element.m_hyperlinkType;
					if (hyperlinkType != 0) {
						final ZLTextHyperlinkControlElement control =
							new ZLTextHyperlinkControlElement((byte)element.m_kind, hyperlinkType, element.m_label);
						elements.add(control);
						hyperlink = control.Hyperlink;
						hyperlinkDepth = 1;
					}
					break;
				}
				case BookParagraph.PARAGRAPH_ELEMENT_IMAGE:
					final ZLImageEntry imageEntry = new ZLImageEntry(FBReaderApp.Instance().Model.myImageMap, element.m_imageId, (short)element.m_imagevOffset, element.m_isCover);
					final ZLImage image = imageEntry.getImage();
					if (image != null) {
						ZLImageData data = ZLImageManager.Instance().getImageData(image);
						if (data != null) {
							if (hyperlink != null) {
								hyperlink.addElementIndex(elements.size());
							}
							elements.add(new ZLTextImageElement(imageEntry.Id, data, image.getURI(), imageEntry.IsCover));
						}
					}
					break;
				case BookParagraph.PARAGRAPH_ELEMENT_STYLE:
					// TODO: implement
					break;
				case BookParagraph.PARAGRAPH_ELEMENT_FIXED_HSPACE:
					elements.add(ZLTextFixedHSpaceElement.getElement((short)element.m_len));
					break;
				}
			}
		}

		private static byte[] ourBreaks = new byte[1024];
		private static final int NO_SPACE = 0;
		private static final int SPACE = 1;
		//private static final int NON_BREAKABLE_SPACE = 2;
		private void processTextEntry(final char[] data, final int offset, final int length, ZLTextHyperlink hyperlink) {
			if (length == 0) {
				return;
			}

			if (ourBreaks.length < length) {
				ourBreaks = new byte[length];
			}
//			String text = new String(data, offset, length);
//			Log.d("processTextEntry", text);
			final byte[] breaks = ourBreaks;
			LineBreaker.setLineBreaks(data, offset, length, breaks);
			
			final ZLTextElement hSpace = ZLTextElement.HSpace;
			final ArrayList<ZLTextElement> elements = myElements;
			char ch = 0;
			char previousChar = 0;
			int spaceState = NO_SPACE;
			int wordStart = 0;
			for (int index = 0; index < length; ++index) {
				previousChar = ch;
				ch = data[offset + index];
				if (Character.isSpace(ch)) {
					if (index > 0 && spaceState == NO_SPACE) {
						addWord(data, offset + wordStart, index - wordStart, myOffset + wordStart, hyperlink);
					}
					spaceState = SPACE;
				} else {
					switch (spaceState) {
						case SPACE:
							//if (breaks[index - 1] == LineBreak.NOBREAK || previousChar == '-') {
							//}
							elements.add(hSpace);
							wordStart = index;
							break;
						//case NON_BREAKABLE_SPACE:
							//break;
						case NO_SPACE:
							if (index > 0 &&
								breaks[index - 1] != LineBreaker.NOBREAK &&
								previousChar != '-' &&
								index != wordStart) {
								addWord(data, offset + wordStart, index - wordStart, myOffset + wordStart, hyperlink);
								wordStart = index;
							}
							break;
					}
					spaceState = NO_SPACE;
				}
			}
			switch (spaceState) {
				case SPACE:
					elements.add(hSpace);
					break;
				//case NON_BREAKABLE_SPACE:
					//break;
				case NO_SPACE:
					addWord(data, offset + wordStart, length - wordStart, myOffset + wordStart, hyperlink);
					break;
			}
			myOffset += length;
		}

		private final void addWord(char[] data, int offset, int len, int paragraphOffset, ZLTextHyperlink hyperlink) {
//			String text = new String(data, offset, len);
//			Log.d("addWord", text);
			ZLTextWord word = new ZLTextWord(data, offset, len, paragraphOffset);
			for (int i = myFirstMark; i < myLastMark; ++i) {
				final ZLTextMark mark = (ZLTextMark)myMarks.get(i);
				if ((mark.Offset < paragraphOffset + len) && (mark.Offset + mark.Length > paragraphOffset)) {
					word.addMark(mark.Offset - paragraphOffset, mark.Length);
				}
			}
			if (hyperlink != null) {
				hyperlink.addElementIndex(myElements.size());
			}
			myElements.add(word);
		}
	}

	public final int Index;
	public final BookModel Model;
	private final ArrayList<ZLTextElement> myElements = new ArrayList<ZLTextElement>();

	private ZLTextParagraphCursor(BookModel model, int index) {
		Model = model;
		Index = index;
		fill();
	}

	static ZLTextParagraphCursor cursor(BookModel model, int index) {
		ZLTextParagraphCursor result = ZLTextParagraphCursorCache.get(model, index);
		if (result == null) {
			result = new ZLTextParagraphCursor(model, index);
			ZLTextParagraphCursorCache.put(model, index, result);
		}
		return result;
	}

	private static final char[] SPACE_ARRAY = { ' ' };
	void fill() {
		BookParagraph.ParagraphData	paragraph = Model.m_paragraph.getParagraph(Index);
		switch (paragraph.m_kind) {
			case BookParagraph.PARAGRAPH_KIND_TEXT_PARAGRAPH:
				new Processor(paragraph, Model.getMarks(), Index, myElements).fill();
				break;
			case BookParagraph.PARAGRAPH_KIND_EMPTY_LINE_PARAGRAPH:
				myElements.add(new ZLTextWord(SPACE_ARRAY, 0, 1, 0));
				break;
			default:
				break;
		}
	}

	void clear() {
		myElements.clear();
	}

	public boolean isFirst() {
		return Index == 0;
	}

	public boolean isLast() {
		return (Index + 1 >= Model.getParagraphNumber());
	}
	
	public boolean isEndOfSection() {
//		final byte kind = Model.m_paragraph.getParagraphKind(Index);
//		Log.d("isEndOfSection", String.format("%1d  %2d  %3d", kind, Index, BookModel.PARAGRAPH_KIND_END_OF_SECTION_PARAGRAPH));
		return (Model.m_paragraph.getParagraphKind(Index) == BookParagraph.PARAGRAPH_KIND_END_OF_SECTION_PARAGRAPH);
	}

	int getParagraphLength() {
		return myElements.size();
	}

	public ZLTextParagraphCursor previous() {
		return isFirst() ? null : cursor(Model, Index - 1);
	}

	public ZLTextParagraphCursor next() {
		return isLast() ? null : cursor(Model, Index + 1);
	}

	ZLTextElement getElement(int index) {
		try {
			return myElements.get(index);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	BookParagraph.ParagraphData getParagraph() {
		return Model.m_paragraph.getParagraph(Index);
	}

	@Override
	public String toString() {
		return "ZLTextParagraphCursor [" + Index + " (0.." + myElements.size() + ")]";
	}
}
