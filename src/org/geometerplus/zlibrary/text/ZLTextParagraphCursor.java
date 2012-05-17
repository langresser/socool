package org.geometerplus.zlibrary.text;

import java.util.*;


import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.zlibrary.image.*;
import org.geometerplus.zlibrary.util.LineBreaker;

import android.util.Log;

public final class ZLTextParagraphCursor {
	private static final class Processor {
		private final ZLTextParagraph myParagraph;
		private final LineBreaker myLineBreaker;
		private final ArrayList<ZLTextElement> myElements;
		private int myOffset;
		private int myFirstMark;
		private int myLastMark;
		private final List<ZLTextMark> myMarks;

		private Processor(ZLTextParagraph paragraph, LineBreaker lineBreaker, List<ZLTextMark> marks, int paragraphIndex, ArrayList<ZLTextElement> elements) {
			myParagraph = paragraph;
			myLineBreaker = lineBreaker;
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
			for (BookModel.EntryIterator it = myParagraph.iterator(); it.hasNext(); ) {
				it.next();
				switch (it.myType) {
					case ZLTextParagraph.Entry.TEXT:
						processTextEntry(it.myTextData, it.myTextOffset, it.myTextLength, hyperlink);
						break;
					case ZLTextParagraph.Entry.CONTROL:
						if (hyperlink != null) {
							hyperlinkDepth += it.myControlIsStart ? 1 : -1;
							if (hyperlinkDepth == 0) {
								hyperlink = null;
							}
						}
						elements.add(ZLTextControlElement.get(it.myControlKind, it.myControlIsStart));
						break;
					case ZLTextParagraph.Entry.HYPERLINK_CONTROL:
					{
						final byte hyperlinkType = it.myHyperlinkType;
						if (hyperlinkType != 0) {
							final ZLTextHyperlinkControlElement control =
								new ZLTextHyperlinkControlElement(
									it.myControlKind, hyperlinkType, it.myHyperlinkId
								);
							elements.add(control);
							hyperlink = control.Hyperlink;
							hyperlinkDepth = 1;
						}
						break;
					}
					case ZLTextParagraph.Entry.IMAGE:
						final ZLImageEntry imageEntry = it.myImageEntry;
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
					case ZLTextParagraph.Entry.STYLE:
						// TODO: implement
						break;
					case ZLTextParagraph.Entry.FIXED_HSPACE:
						elements.add(ZLTextFixedHSpaceElement.getElement(it.myFixedHSpaceLength));
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
			String text = new String(data, offset, length);
//			Log.d("processTextEntry", text);
			final byte[] breaks = ourBreaks;
			myLineBreaker.setLineBreaks(data, offset, length, breaks);
			
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
		ZLTextParagraph	paragraph = Model.getParagraph(Index);
		switch (paragraph.getKind()) {
			case ZLTextParagraph.Kind.TEXT_PARAGRAPH:
				new Processor(paragraph, new LineBreaker(Model.myLanguage), Model.getMarks(), Index, myElements).fill();
				break;
			case ZLTextParagraph.Kind.EMPTY_LINE_PARAGRAPH:
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
		if (Model.m_isStreamRead) {
			return (Index + 1 >= Model.m_allParagraphNumber);
		} else {
			return (Index + 1 >= Model.myParagraphsNumber);
		}
	}

	public boolean isEndOfSection() {
		return (Model.getParagraphKind(Index) == ZLTextParagraph.Kind.END_OF_SECTION_PARAGRAPH);
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

	ZLTextParagraph getParagraph() {
		return Model.getParagraph(Index);
	}

	@Override
	public String toString() {
		return "ZLTextParagraphCursor [" + Index + " (0.." + myElements.size() + ")]";
	}
}
