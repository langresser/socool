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

package org.geometerplus.zlibrary.text;

import java.util.*;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.ColorProfile;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.MoveCursorAction;
import org.geometerplus.fbreader.fbreader.ScrollingPreferences;
import org.geometerplus.fbreader.fbreader.TapZoneMap;
import org.geometerplus.fbreader.fbreader.TextBuildTraverser;
import org.geometerplus.fbreader.fbreader.WordCountTraverser;
import org.geometerplus.fbreader.library.Bookmark;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.filesystem.ZLResourceFile;

import org.geometerplus.zlibrary.util.ZLColor;
import org.geometerplus.zlibrary.view.ZLGLWidget;
import org.geometerplus.zlibrary.view.ZLPaintContext;
import org.geometerplus.zlibrary.view.ZLViewWidget;

import android.util.Log;

public class ZLTextView {
	// paint state
	public static final int NOTHING_TO_PAINT = 0;
	public static final int READY = 1;
	public static final int START_IS_KNOWN = 2;
	public static final int END_IS_KNOWN = 3;
	public static final int TO_SCROLL_FORWARD = 4;
	public static final int TO_SCROLL_BACKWARD = 5;

	public static final int MAX_SELECTION_DISTANCE = 10;

	public interface ScrollingMode {
		int NO_OVERLAPPING = 0;
		int KEEP_LINES = 1;
		int SCROLL_LINES = 2;
		int SCROLL_PERCENTAGE = 3;
	};

	private BookModel myModel;

	private interface SizeUnit {
		int PIXEL_UNIT = 0;
		int LINE_UNIT = 1;
	};

	private int myScrollingMode;
	private int myOverlappingValue;

	private ZLTextPage myPreviousPage = new ZLTextPage();
	ZLTextPage myCurrentPage = new ZLTextPage();
	private ZLTextPage myNextPage = new ZLTextPage();

	private final HashMap<ZLTextLineInfo,ZLTextLineInfo> myLineInfoCache = new HashMap<ZLTextLineInfo,ZLTextLineInfo>();

	private ZLTextRegion.Soul mySelectedRegionSoul;
	private boolean myHighlightSelectedRegion = true;

	private ZLTextSelection mySelection;

	protected ZLPaintContext myContext = null;
	ArrayList<ZLTextHighlighting> m_bookMarkHighlighting;

	public ZLTextView() {
		mySelection = new ZLTextSelection(this);
		m_bookMarkHighlighting = new ArrayList<ZLTextHighlighting>();
	}

	public synchronized void setModel(BookModel model) {
		ZLTextParagraphCursorCache.clear();

		myModel = model;
		
		myCurrentPage.reset();
		myPreviousPage.reset();
		myNextPage.reset();
		if (myModel != null) {
			resetTextStyle();

			final int paragraphsNumber = myModel.getParagraphNumber();
			if (paragraphsNumber > 0) {
				myCurrentPage.moveStartCursor(ZLTextParagraphCursor.cursor(myModel, 0));
			}
			
			loadBookMark();
		}

		FBReaderApp.Instance().resetWidget();
		
		if (myFooter != null) {
			myFooter.resetTOCMarks();
		}
	}
	
	private void loadBookMark()
	{
		m_bookMarkHighlighting.clear();
//		Collections.sort(m_allBooksBookmarks, new Bookmark.ByTimeComparator());

		if (FBReaderApp.Instance().Model != null) {
			final long bookId = FBReaderApp.Instance().Model.Book.myId;
			List<Bookmark> bookmarks = FBReaderApp.Instance().getDatabase().loadBookmarks(bookId);
			for (Bookmark bookmark : bookmarks) {
				addBookmarkHighlight(bookmark);
			}
		}
	}
	
	public void addBookmarkHighlight(Bookmark bookmark)
	{		
		if (bookmark.m_posBegin == null || bookmark.m_posEnd == null) {
			return;
		}

		ZLTextHighlighting highlighting = new ZLTextHighlighting();
		ZLTextFixedPosition begin = new ZLTextFixedPosition(
				bookmark.m_posBegin.getParagraphIndex(),
				bookmark.m_posBegin.getElementIndex(),
				bookmark.m_posBegin.getCharIndex());
		ZLTextFixedPosition end = new ZLTextFixedPosition(
				bookmark.m_posEnd.getParagraphIndex(), 
				bookmark.m_posEnd.getElementIndex(), 
				bookmark.m_posEnd.getCharIndex());

		highlighting.setup(begin, end);
		m_bookMarkHighlighting.add(highlighting);
	}

	public BookModel getModel() {
		return myModel;
	}

	public ZLTextWordCursor getStartCursor() {
		if (myCurrentPage.StartCursor.isNull()) {
			preparePaintInfo(myCurrentPage);
		}
		return myCurrentPage.StartCursor;
	}

	public ZLTextWordCursor getEndCursor() {
		if (myCurrentPage.EndCursor.isNull()) {
			preparePaintInfo(myCurrentPage);
		}
		return myCurrentPage.EndCursor;
	}

	private synchronized void gotoMark(ZLTextMark mark) {
		if (mark == null) {
			return;
		}

		myPreviousPage.reset();
		myNextPage.reset();
		boolean doRepaint = false;
		if (myCurrentPage.StartCursor.isNull()) {
			doRepaint = true;
			preparePaintInfo(myCurrentPage);
		}
		if (myCurrentPage.StartCursor.isNull()) {
			return;
		}
		if (myCurrentPage.StartCursor.getParagraphIndex() != mark.ParagraphIndex ||
			myCurrentPage.StartCursor.getMark().compareTo(mark) > 0) {
			doRepaint = true;
			gotoPosition(mark.ParagraphIndex, 0, 0);
			preparePaintInfo(myCurrentPage);
		}
		if (myCurrentPage.EndCursor.isNull()) {
			preparePaintInfo(myCurrentPage);
		}
		while (mark.compareTo(myCurrentPage.EndCursor.getMark()) > 0) {
			doRepaint = true;
			scrollPage(true, ScrollingMode.NO_OVERLAPPING, 0);
			preparePaintInfo(myCurrentPage);
		}
		if (doRepaint) {
			if (myCurrentPage.StartCursor.isNull()) {
				preparePaintInfo(myCurrentPage);
			}
			FBReaderApp.Instance().resetWidget();
			FBReaderApp.Instance().repaintWidget();
		}
	}

	public synchronized int search(final String text, boolean ignoreCase, boolean wholeText, boolean backward, boolean thisSectionOnly) {
		if (text.length() == 0) {
			return 0;
		}
		int startIndex = 0;
		int endIndex = myModel.getParagraphNumber();
		if (thisSectionOnly) {
			// TODO: implement
		}
		int count = myModel.search(text, startIndex, endIndex, ignoreCase);
		myPreviousPage.reset();
		myNextPage.reset();
		if (!myCurrentPage.StartCursor.isNull()) {
			rebuildPaintInfo();
			if (count > 0) {
				ZLTextMark mark = myCurrentPage.StartCursor.getMark();
				gotoMark(wholeText ?
					(backward ? myModel.getLastMark() : myModel.getFirstMark()) :
					(backward ? myModel.getPreviousMark(mark) : myModel.getNextMark(mark)));
			}
			FBReaderApp.Instance().resetWidget();
			FBReaderApp.Instance().repaintWidget();
		}
		return count;
	}

	public boolean canFindNext() {
		final ZLTextWordCursor end = myCurrentPage.EndCursor;
		return !end.isNull() && (myModel != null) && (myModel.getNextMark(end.getMark()) != null);
	}

	public synchronized void findNext() {
		final ZLTextWordCursor end = myCurrentPage.EndCursor;
		if (!end.isNull()) {
			gotoMark(myModel.getNextMark(end.getMark()));
		}
	}

	public boolean canFindPrevious() {
		final ZLTextWordCursor start = myCurrentPage.StartCursor;
		return !start.isNull() && (myModel != null) && (myModel.getPreviousMark(start.getMark()) != null);
	}

	public synchronized void findPrevious() {
		final ZLTextWordCursor start = myCurrentPage.StartCursor;
		if (!start.isNull()) {
			gotoMark(myModel.getPreviousMark(start.getMark()));
		}
	}

	public void clearFindResults() {
		if (!findResultsAreEmpty()) {
			myModel.myMarks = null;
			rebuildPaintInfo();
			FBReaderApp.Instance().resetWidget();
			FBReaderApp.Instance().repaintWidget();
		}
	}

	public boolean findResultsAreEmpty() {
		return (myModel == null) || myModel.getMarks().isEmpty();
	}

	public synchronized void onScrollingFinished(PageIndex pageIndex) {
		switch (pageIndex) {
			case current:
				break;
			case previous:
			{
				final ZLTextPage swap = myNextPage;
				myNextPage = myCurrentPage;
				myCurrentPage = myPreviousPage;
				myPreviousPage = swap;
				myPreviousPage.reset();

				if (myCurrentPage.PaintState == NOTHING_TO_PAINT) {
					preparePaintInfo(myNextPage);
					myCurrentPage.EndCursor.setCursor(myNextPage.StartCursor);
					myCurrentPage.PaintState = END_IS_KNOWN;
				} else if (!myCurrentPage.EndCursor.isNull() &&
						   !myNextPage.StartCursor.isNull() &&
						   !myCurrentPage.EndCursor.samePositionAs(myNextPage.StartCursor)) {
					myNextPage.reset();
					myNextPage.StartCursor.setCursor(myCurrentPage.EndCursor);
					myNextPage.PaintState = START_IS_KNOWN;
					FBReaderApp.Instance().resetWidget();
				}
				break;
			}
			case next:
			{
				final ZLTextPage swap = myPreviousPage;
				myPreviousPage = myCurrentPage;
				myCurrentPage = myNextPage;
				myNextPage = swap;
				myNextPage.reset();

				if (myCurrentPage.PaintState == NOTHING_TO_PAINT) {
					preparePaintInfo(myPreviousPage);
					myCurrentPage.StartCursor.setCursor(myPreviousPage.EndCursor);
					myCurrentPage.PaintState = START_IS_KNOWN;
				}
				break;
			}
		}
	}

	protected void moveSelectionCursorTo(ZLTextSelectionCursor cursor, int x, int y) {
		y -= ZLTextSelectionCursor.getHeight() / 2 + ZLTextSelectionCursor.getAccent() / 2;
		mySelection.setCursorInMovement(cursor, x, y);
		mySelection.expandTo(x, y);
		FBReaderApp.Instance().resetWidget();
		FBReaderApp.Instance().repaintWidget();
	}

	protected void releaseSelectionCursor() {
		mySelection.stop();
		FBReaderApp.Instance().resetWidget();
		FBReaderApp.Instance().repaintWidget();
		
		if (getCountOfSelectedWords() > 0) {
			FBReaderApp.Instance().runAction(ActionCode.SELECTION_SHOW_PANEL);
		}
	}

	protected ZLTextSelectionCursor getSelectionCursorInMovement() {
		return mySelection.getCursorInMovement();
	}

	private ZLTextSelection.Point getSelectionCursorPoint(ZLTextPage page, ZLTextSelectionCursor cursor) {
		if (cursor == ZLTextSelectionCursor.None) {
			return null;
		}

		if (cursor == mySelection.getCursorInMovement()) {
			return mySelection.getCursorInMovementPoint();
		}

		if (cursor == ZLTextSelectionCursor.Left) {	
			if (mySelection.hasAPartBeforePage(page)) {
				return null;
			}
			final ZLTextElementArea selectionStartArea = mySelection.getStartArea(page);
			if (selectionStartArea != null) {
				return new ZLTextSelection.Point(selectionStartArea.XStart, selectionStartArea.YEnd);
			}
		} else {
			if (mySelection.hasAPartAfterPage(page)) {
				return null;
			}
			final ZLTextElementArea selectionEndArea = mySelection.getEndArea(page);
			if (selectionEndArea != null) {
				return new ZLTextSelection.Point(selectionEndArea.XEnd, selectionEndArea.YEnd);
			}
		}
		return null;
	}

	private int distanceToCursor(int x, int y, ZLTextSelection.Point cursorPoint) {
		if (cursorPoint == null) {
			return Integer.MAX_VALUE;
		}

		final int dX, dY;

		final int w = ZLTextSelectionCursor.getWidth() / 2;
		if (x < cursorPoint.X - w) {
			dX = cursorPoint.X - w - x;
		} else if (x > cursorPoint.X + w) {
			dX = x - cursorPoint.X - w;
		} else {
			dX = 0;
		}

		final int h = ZLTextSelectionCursor.getHeight();
		if (y < cursorPoint.Y) {
			dY = cursorPoint.Y - y;
		} else if (y > cursorPoint.Y + h) {
			dY = y - cursorPoint.Y - h;
		} else {
			dY = 0;
		}

		return Math.max(dX, dY);
	}

	protected ZLTextSelectionCursor findSelectionCursor(int x, int y) {
		return findSelectionCursor(x, y, Integer.MAX_VALUE);
	}

	protected ZLTextSelectionCursor findSelectionCursor(int x, int y, int maxDistance) {
		if (mySelection.isEmpty()) {
			return ZLTextSelectionCursor.None;
		}

		final int leftDistance = distanceToCursor(
			x, y, getSelectionCursorPoint(myCurrentPage, ZLTextSelectionCursor.Left)
		);
		final int rightDistance = distanceToCursor(
			x, y, getSelectionCursorPoint(myCurrentPage, ZLTextSelectionCursor.Right)
		);

		if (rightDistance < leftDistance) {
			return rightDistance <= maxDistance ? ZLTextSelectionCursor.Right : ZLTextSelectionCursor.None;
		} else {
			return leftDistance <= maxDistance ? ZLTextSelectionCursor.Left : ZLTextSelectionCursor.None;
		}
	}

	private void drawSelectionCursor(ZLPaintContext context, ZLTextSelection.Point pt) {
		if (pt == null) {
			return;
		}

		final int w = ZLTextSelectionCursor.getWidth() / 2;
		final int h = ZLTextSelectionCursor.getHeight();
		final int a = ZLTextSelectionCursor.getAccent();
		final int[] xs = { pt.X, pt.X + w, pt.X + w, pt.X - w, pt.X - w };
		final int[] ys = { pt.Y - a, pt.Y, pt.Y + h, pt.Y + h, pt.Y };
		context.setFillColor(context.getBackgroundColor(), 192);
		context.fillPolygon(xs, ys);
		context.setLineColor(getTextColor(ZLTextHyperlink.NO_LINK));
		context.drawPolygonalLine(xs, ys);
	}

	public synchronized void paint(ZLPaintContext context, PageIndex pageIndex) {
		myContext = context;
		final ZLFile wallpaper = getWallpaperFile();
		if (wallpaper != null) {
			context.clear(wallpaper, wallpaper instanceof ZLResourceFile);
		} else {
			context.clear(getBackgroundColor());
		}

		if (myModel == null || myModel.getParagraphNumber() == 0) {
			return;
		}

		ZLTextPage page;
		switch (pageIndex) {
			default:
			case current:
				page = myCurrentPage;
				break;
			case previous:
				page = myPreviousPage;
				if (myPreviousPage.PaintState == NOTHING_TO_PAINT) {
					preparePaintInfo(myCurrentPage);
					myPreviousPage.EndCursor.setCursor(myCurrentPage.StartCursor);
					myPreviousPage.PaintState = END_IS_KNOWN;
				}
				break;
			case next:
				page = myNextPage;
				if (myNextPage.PaintState == NOTHING_TO_PAINT) {
					preparePaintInfo(myCurrentPage);
					myNextPage.StartCursor.setCursor(myCurrentPage.EndCursor);
					myNextPage.PaintState = START_IS_KNOWN;
				}
		}

		page.TextElementMap.clear();

		preparePaintInfo(page);

		if (page.StartCursor.isNull() || page.EndCursor.isNull()) {
			return;
		}

		final ArrayList<ZLTextLineInfo> lineInfos = page.LineInfos;
		final int[] labels = new int[lineInfos.size() + 1];
		int y = FBReaderApp.Instance().getTopMargin();
		int index = 0;
		for (ZLTextLineInfo info : lineInfos) {
			prepareTextLine(page, info, y);
			y += info.Height + info.Descent + info.VSpaceAfter;
			labels[++index] = page.TextElementMap.size();
		}

		y = FBReaderApp.Instance().getTopMargin();
		index = 0;
		for (ZLTextLineInfo info : lineInfos) {
			drawTextLine(page, info, labels[index], labels[index + 1], y);
			y += info.Height + info.Descent + info.VSpaceAfter;
			++index;
		}

		final ZLTextRegion selectedElementRegion = getSelectedRegion(page);
		if (selectedElementRegion != null && myHighlightSelectedRegion) {
			selectedElementRegion.draw(context);
		}
		
		drawSelectionCursor(context, getSelectionCursorPoint(page, ZLTextSelectionCursor.Left));
		drawSelectionCursor(context, getSelectionCursorPoint(page, ZLTextSelectionCursor.Right));
	}
	
	private ZLTextPage getPage(PageIndex pageIndex) {
		switch (pageIndex) {
			default:
			case current:
				return myCurrentPage;
			case previous:
				return myPreviousPage;
			case next:
				return myNextPage;
		}
	}

	public static final int SCROLLBAR_HIDE = 0;
	public static final int SCROLLBAR_SHOW = 1;
	public static final int SCROLLBAR_SHOW_AS_PROGRESS = 2;

	public final boolean isScrollbarShown() {
		return scrollbarType() == SCROLLBAR_SHOW || scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS;
	}

	protected final synchronized int sizeOfTextBeforeParagraph(int paragraphIndex) {
		return myModel != null ? myModel.getTextLength(paragraphIndex - 1) : 0;
	}

	protected final synchronized int sizeOfFullText() {
		if (myModel == null || myModel.getParagraphNumber() == 0) {
			return 1;
		}
		return myModel.getTextLength(myModel.getParagraphNumber() - 1);
	}

	protected final synchronized int getCurrentCharNumber(PageIndex pageIndex, boolean startNotEndOfPage) {
		if (myModel == null || myModel.getParagraphNumber() == 0) {
			return 0;
		}
		final ZLTextPage page = getPage(pageIndex);
		preparePaintInfo(page);
		if (startNotEndOfPage) {
			return Math.max(0, sizeOfTextBeforeCursor(page.StartCursor));
		} else {
			int end = sizeOfTextBeforeCursor(page.EndCursor);
			if (end == -1) {
				end = myModel.getTextLength(myModel.getParagraphNumber() - 1) - 1;
			}
			return Math.max(1, end);
		}
	}

	public final synchronized int getScrollbarFullSize() {
		return sizeOfFullText();
	}

	public final synchronized int getScrollbarThumbPosition(PageIndex pageIndex) {
		return scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS ? 0 : getCurrentCharNumber(pageIndex, true);
	}

	public final synchronized int getScrollbarThumbLength(PageIndex pageIndex) {
		int start = scrollbarType() == SCROLLBAR_SHOW_AS_PROGRESS
			? 0 : getCurrentCharNumber(pageIndex, true);
		int end = getCurrentCharNumber(pageIndex, false);
		return Math.max(1, end - start);
	}

	private int sizeOfTextBeforeCursor(ZLTextWordCursor wordCursor) {
		final ZLTextParagraphCursor paragraphCursor = wordCursor.getParagraphCursor();
		if (paragraphCursor == null) {
			return -1;
		}
		final int paragraphIndex = paragraphCursor.Index;
		if (paragraphIndex == 0) {
			return 0;
		}

		int sizeOfText = myModel.getTextLength(paragraphIndex - 1);
		final int paragraphLength = paragraphCursor.getParagraphLength();
		if (paragraphLength > 0) {
			sizeOfText +=
				(myModel.getTextLength(paragraphIndex) - sizeOfText)
				* wordCursor.getElementIndex()
				/ paragraphLength;
		}
		return sizeOfText;
	}

	public static class PagePosition {
		public final int Current;
		public final int Total;

		PagePosition(int current, int total) {
			Current = current;
			Total = total;
		}
	}

	public void gotoHome() {
		final ZLTextWordCursor cursor = getStartCursor();
		if (!cursor.isNull() && cursor.isStartOfParagraph() && cursor.getParagraphIndex() == 0) {
			return;
		}
		gotoPosition(0, 0, 0);
		preparePaintInfo();
	}

	private void drawBackgroung(ZLTextSelection highligting, ZLColor color,
							ZLTextPage page, ZLTextLineInfo info, int from, int to, int y
	) {
		if (myContext == null) {
			return;
		}

		if (!highligting.isEmpty() && from != to) {
			final ZLTextElementArea fromArea = page.TextElementMap.get(from);
			final ZLTextElementArea toArea = page.TextElementMap.get(to - 1);
			final ZLTextElementArea selectionStartArea = highligting.getStartArea(page);
			final ZLTextElementArea selectionEndArea = highligting.getEndArea(page);
			if (selectionStartArea != null
				&& selectionEndArea != null
				&& selectionStartArea.compareTo(toArea) <= 0
				&& selectionEndArea.compareTo(fromArea) >= 0) {
				final int top = y + 1;
				int left, right, bottom = y + info.Height + info.Descent;
				if (selectionStartArea.compareTo(fromArea) < 0) {
					left = FBReaderApp.Instance().getLeftMargin();
				} else {
					left = selectionStartArea.XStart;
				}
				if (selectionEndArea.compareTo(toArea) > 0) {
					right = getRightLine();
					bottom += info.VSpaceAfter;
				} else {
					right = selectionEndArea.XEnd;
				}
				myContext.setFillColor(color);
				myContext.fillRectangle(left, top, right, bottom);
			}
		}
	}

	private static final char[] SPACE = new char[] { ' ' };
	private void drawTextLine(ZLTextPage page, ZLTextLineInfo info, int from, int to, int y) {
		if (myContext == null) {
			return;
		}

		drawBackgroung(mySelection, getSelectedBackgroundColor(), page, info, from, to, y);
//		for (ZLTextHighlighting highlighting : m_bookMarkHighlighting) {
//			drawBackgroung(highlighting, getSelectedBackgroundColor(), page, info, from, to, y);
//		}

//		drawBackgroung(myHighlighting, getHighlightingColor(), page, info, from, to, y);

		final ZLTextParagraphCursor paragraph = info.ParagraphCursor;
		int index = from;
		final int endElementIndex = info.EndElementIndex;
		int charIndex = info.RealStartCharIndex;
		for (int wordIndex = info.RealStartElementIndex; wordIndex != endElementIndex && index < to; ++wordIndex, charIndex = 0) {
			final ZLTextElement element = paragraph.getElement(wordIndex);
			final ZLTextElementArea area = page.TextElementMap.get(index);
			if (element == area.Element) {
				++index;
				if (area.ChangeStyle) {
					setTextStyle(area.Style);
				}
				final int areaX = area.XStart;
				final int areaY = area.YEnd - getElementDescent(element) - myTextStyle.getVerticalShift();
				if (element instanceof ZLTextWord) {
					drawWord(
						areaX, areaY, (ZLTextWord)element, charIndex, -1, false,
						mySelection.isAreaSelected(area)
							? getSelectedForegroundColor() : getTextColor(myTextStyle.Hyperlink)
					);
				} else if (element instanceof ZLTextImageElement) {
					final ZLTextImageElement imageElement = (ZLTextImageElement)element;
					myContext.drawImage(
						areaX, areaY,
						imageElement.ImageData,
						getTextAreaSize(),
						imageElement.IsCover
							? ZLPaintContext.ScalingType.FitMaximum
							: ZLPaintContext.ScalingType.IntegerCoefficient
					);
				} else if (element == ZLTextElement.HSpace) {
					final int cw = myContext.getSpaceWidth();
					/*
					context.setFillColor(getHighlightingColor());
					context.fillRectangle(
						area.XStart, areaY - context.getStringHeight(),
						area.XEnd - 1, areaY + context.getDescent()
					);
					*/
					for (int len = 0; len < area.XEnd - area.XStart; len += cw) {
						myContext.drawString(areaX + len, areaY, SPACE, 0, 1);
					}
				}
			}
		}
		if (index != to) {
			ZLTextElementArea area = page.TextElementMap.get(index++);
			if (area.ChangeStyle) {
				setTextStyle(area.Style);
			}
			final int start = info.StartElementIndex == info.EndElementIndex
				? info.StartCharIndex : 0;
			final int len = info.EndCharIndex - start;
			final ZLTextWord word = (ZLTextWord)paragraph.getElement(info.EndElementIndex);
			drawWord(
				area.XStart, area.YEnd - myContext.getDescent() - myTextStyle.getVerticalShift(),
				word, start, len, area.AddHyphenationSign,
				mySelection.isAreaSelected(area)
					? getSelectedForegroundColor() : getTextColor(myTextStyle.Hyperlink)
			);
		}
	}

	private void buildInfos(ZLTextPage page, ZLTextWordCursor start, ZLTextWordCursor result) {
		result.setCursor(start);
		int textAreaHeight = getTextAreaHeight();
		page.LineInfos.clear();
		int counter = 0;
		// 纯txt显示，只需要在每页开始的时候初始化格式
		if (!myModel.m_supportRichText) {
			final ZLTextStyleDecoration decoration =
					ZLTextStyleCollection.Instance().getDecoration(BookModel.NONE);
			setTextStyle(decoration.createDecoratedStyle(ZLTextStyleCollection.Instance().getBaseStyle()));
		}

		do {
			// 富文本显示每个段落都进行格式初始化
			if (myModel.m_supportRichText) {
				resetTextStyle();
			}
			
			final ZLTextParagraphCursor paragraphCursor = result.getParagraphCursor();
			final int wordIndex = result.getElementIndex();
			if (wordIndex != 0) {
				applyControls(paragraphCursor, 0, wordIndex);
			}

			ZLTextLineInfo info = new ZLTextLineInfo(paragraphCursor, wordIndex, result.getCharIndex(), myTextStyle);
			final int endIndex = info.ParagraphCursorLength;
			while (info.EndElementIndex != endIndex) {
				info = processTextLine(paragraphCursor, info.EndElementIndex, info.EndCharIndex, endIndex);
				textAreaHeight -= info.Height + info.Descent;
				if (textAreaHeight < 0 && counter > 0) {
					break;
				}
				textAreaHeight -= info.VSpaceAfter;
				result.moveTo(info.EndElementIndex, info.EndCharIndex);
				page.LineInfos.add(info);
				if (textAreaHeight < 0) {
					break;
				}
				counter++;
			}
		} while (result.isEndOfParagraph() && result.nextParagraph() && !result.getParagraphCursor().isEndOfSection() && (textAreaHeight >= 0));
		
		// 排版完毕，进行段落初始化
		resetTextStyle();
	}

	private boolean isHyphenationPossible() {
		return ZLTextStyleCollection.Instance().getBaseStyle().AutoHyphenationOption.getValue()
			&& myTextStyle.allowHyphenations();
	}

	private ZLTextLineInfo processTextLine(
		ZLTextParagraphCursor paragraphCursor,
		final int startIndex,
		final int startCharIndex,
		final int endIndex
	) {
		final ZLTextLineInfo info = new ZLTextLineInfo(paragraphCursor, startIndex, startCharIndex, myTextStyle);
		final ZLTextLineInfo cachedInfo = myLineInfoCache.get(info);
		if (cachedInfo != null) {
			applyControls(paragraphCursor, startIndex, cachedInfo.EndElementIndex);
			return cachedInfo;
		}

		int currentElementIndex = startIndex;
		int currentCharIndex = startCharIndex;
		final boolean isFirstLine = startIndex == 0 && startCharIndex == 0;

		if (isFirstLine) {
			ZLTextElement element = paragraphCursor.getElement(currentElementIndex);
			while (element instanceof ZLTextControlElement) {
				applyControl((ZLTextControlElement)element);
				++currentElementIndex;
				currentCharIndex = 0;
				if (currentElementIndex == endIndex) {
					break;
				}
				element = paragraphCursor.getElement(currentElementIndex);
			}
			info.StartStyle = myTextStyle;
			info.RealStartElementIndex = currentElementIndex;
			info.RealStartCharIndex = currentCharIndex;
		}

		ZLTextStyle storedStyle = myTextStyle;

		info.LeftIndent = myTextStyle.getLeftIndent();
		// TODO 智能排版，自动过滤空格，但是要考虑某些文件有意使用空格进行排版
		if (isFirstLine) {
			info.LeftIndent += myTextStyle.getFirstLineIndentDelta();
//			Log.d("isFirstLine", "Indent: " + info.LeftIndent + "style: " + myTextStyle.Base.getClass().getName());
		}

		info.Width = info.LeftIndent;

		if (info.RealStartElementIndex == endIndex) {
			info.EndElementIndex = info.RealStartElementIndex;
			info.EndCharIndex = info.RealStartCharIndex;
			return info;
		}

		int newWidth = info.Width;
		int newHeight = info.Height;
		int newDescent = info.Descent;
		int maxWidth = getTextAreaWidth() - myTextStyle.getRightIndent();
		boolean wordOccurred = false;
		boolean isVisible = false;
		int lastSpaceWidth = 0;
		int internalSpaceCounter = 0;
		boolean removeLastSpace = false;

		do {
			ZLTextElement element = paragraphCursor.getElement(currentElementIndex);
			newWidth += getElementWidth(element, currentCharIndex);
			newHeight = Math.max(newHeight, getElementHeight(element));
			newDescent = Math.max(newDescent, getElementDescent(element));
			if (element == ZLTextElement.HSpace) {
				if (wordOccurred) {
					wordOccurred = false;
					internalSpaceCounter++;
					lastSpaceWidth = myContext.getSpaceWidth();
					newWidth += lastSpaceWidth;
				}
			} else if (element instanceof ZLTextWord) {
				wordOccurred = true;
				isVisible = true;
			} else if (element instanceof ZLTextControlElement) {
				applyControl((ZLTextControlElement)element);
			} else if (element instanceof ZLTextImageElement) {
				wordOccurred = true;
				isVisible = true;
			}
			if (newWidth > maxWidth) {
				if (info.EndElementIndex != startIndex || element instanceof ZLTextWord) {
					break;
				}
			}
			ZLTextElement previousElement = element;
			++currentElementIndex;
			currentCharIndex = 0;
			boolean allowBreak = currentElementIndex == endIndex;
			if (!allowBreak) {
				element = paragraphCursor.getElement(currentElementIndex);
				allowBreak = ((!(element instanceof ZLTextWord) || previousElement instanceof ZLTextWord) &&
						!(element instanceof ZLTextImageElement) &&
						!(element instanceof ZLTextControlElement));
			}
			if (allowBreak) {
				info.IsVisible = isVisible;
				info.Width = newWidth;
				if (info.Height < newHeight) {
					info.Height = newHeight;
				}
				if (info.Descent < newDescent) {
					info.Descent = newDescent;
				}
				info.EndElementIndex = currentElementIndex;
				info.EndCharIndex = currentCharIndex;
				info.SpaceCounter = internalSpaceCounter;
				storedStyle = myTextStyle;
				removeLastSpace = !wordOccurred && (internalSpaceCounter > 0);
			}
		} while (currentElementIndex != endIndex);

		if (currentElementIndex != endIndex &&
			(isHyphenationPossible() || info.EndElementIndex == startIndex)) {
			ZLTextElement element = paragraphCursor.getElement(currentElementIndex);
			if (element instanceof ZLTextWord) {
				final ZLTextWord word = (ZLTextWord)element;
				newWidth -= getWordWidth(word, currentCharIndex);
				int spaceLeft = maxWidth - newWidth;
				if ((word.Length > 3 && spaceLeft > 2 * myContext.getSpaceWidth())
					|| info.EndElementIndex == startIndex) {
					ZLTextHyphenationInfo hyphenationInfo = ZLTextHyphenator.Instance().getInfo(word);
					int hyphenationPosition = word.Length - 1;
					int subwordWidth = 0;
					for(; hyphenationPosition > currentCharIndex; hyphenationPosition--) {
						if (hyphenationInfo.isHyphenationPossible(hyphenationPosition)) {
							subwordWidth = getWordWidth(
								word,
								currentCharIndex,
								hyphenationPosition - currentCharIndex,
								word.Data[word.Offset + hyphenationPosition - 1] != '-'
							);
							if (subwordWidth <= spaceLeft) {
								break;
							}
						}
					}
					if (hyphenationPosition == currentCharIndex && info.EndElementIndex == startIndex) {
						hyphenationPosition = word.Length == currentCharIndex + 1 ? word.Length : word.Length - 1;
						subwordWidth = getWordWidth(word, currentCharIndex, word.Length - currentCharIndex, false);
						for(; hyphenationPosition > currentCharIndex + 1; hyphenationPosition--) {
							subwordWidth = getWordWidth(
								word,
								currentCharIndex,
								hyphenationPosition - currentCharIndex,
								word.Data[word.Offset + hyphenationPosition - 1] != '-'
							);
							if (subwordWidth <= spaceLeft) {
								break;
							}
						}
					}
					if (hyphenationPosition > currentCharIndex) {
						info.IsVisible = true;
						info.Width = newWidth + subwordWidth;
						if (info.Height < newHeight) {
							info.Height = newHeight;
						}
						if (info.Descent < newDescent) {
							info.Descent = newDescent;
						}
						info.EndElementIndex = currentElementIndex;
						info.EndCharIndex = hyphenationPosition;
						info.SpaceCounter = internalSpaceCounter;
						storedStyle = myTextStyle;
						removeLastSpace = false;
					}
				}
			}
		}

		if (removeLastSpace) {
			info.Width -= lastSpaceWidth;
			info.SpaceCounter--;
		}

		if (myModel.m_supportRichText) {
			setTextStyle(storedStyle);
		}

		if (isFirstLine) {
			info.Height += info.StartStyle.getSpaceBefore();
		}
		if (info.isEndOfParagraph()) {
			info.VSpaceAfter = myTextStyle.getSpaceAfter();
		}

		if (info.EndElementIndex != endIndex || endIndex == info.ParagraphCursorLength) {
			myLineInfoCache.put(info, info);
		}

		return info;
	}

	private void prepareTextLine(ZLTextPage page, ZLTextLineInfo info, int y) {
		y = Math.min(y + info.Height, getBottomLine());

		final ZLTextParagraphCursor paragraphCursor = info.ParagraphCursor;

		setTextStyle(info.StartStyle);
		
		int spaceCounter = info.SpaceCounter;
		int fullCorrection = 0;
		final boolean endOfParagraph = info.isEndOfParagraph();
		boolean wordOccurred = false;
		boolean changeStyle = true;

		int x = FBReaderApp.Instance().getLeftMargin() + info.LeftIndent;
		final int maxWidth = getTextAreaWidth();
		switch (myTextStyle.getAlignment()) {
			case BookModel.ALIGN_RIGHT:
				x += maxWidth - myTextStyle.getRightIndent() - info.Width;
				break;
			case BookModel.ALIGN_CENTER:
				x += (maxWidth - myTextStyle.getRightIndent() - info.Width) / 2;
				break;
			case BookModel.ALIGN_JUSTIFY:
				if (!endOfParagraph && (paragraphCursor.getElement(info.EndElementIndex) != ZLTextElement.AfterParagraph)) {
					fullCorrection = maxWidth - myTextStyle.getRightIndent() - info.Width;
				}
				break;
			case BookModel.ALIGN_LEFT:
			case BookModel.ALIGN_UNDEFINED:
				break;
		}

		final ZLTextParagraphCursor paragraph = info.ParagraphCursor;
		final int paragraphIndex = paragraph.Index;
		final int endElementIndex = info.EndElementIndex;
		int charIndex = info.RealStartCharIndex;
		ZLTextElementArea spaceElement = null;
		for (int wordIndex = info.RealStartElementIndex; wordIndex != endElementIndex; ++wordIndex, charIndex = 0) {
			final ZLTextElement element = paragraph.getElement(wordIndex);
			final int width = getElementWidth(element, charIndex);
			if (element == ZLTextElement.HSpace) {
				if (wordOccurred && (spaceCounter > 0)) {
					final int correction = fullCorrection / spaceCounter;
					final int spaceLength = myContext.getSpaceWidth() + correction;
					if (myTextStyle.isUnderline()) {
						spaceElement = new ZLTextElementArea(
							paragraphIndex, wordIndex, 0,
							0, // length
							true, // is last in element
							false, // add hyphenation sign
							false, // changed style
							myTextStyle, element, x, x + spaceLength, y, y
						);
					} else {
						spaceElement = null;
					}
					x += spaceLength;
					fullCorrection -= correction;
					wordOccurred = false;
					--spaceCounter;
				}
			} else if (element instanceof ZLTextWord || element instanceof ZLTextImageElement) {
				final int height = getElementHeight(element);
				final int descent = getElementDescent(element);
				final int length = element instanceof ZLTextWord ? ((ZLTextWord)element).Length : 0;
				if (spaceElement != null) {
					page.TextElementMap.add(spaceElement);
					spaceElement = null;
				}
				page.TextElementMap.add(new ZLTextElementArea(
					paragraphIndex, wordIndex, charIndex,
					length - charIndex,
					true, // is last in element
					false, // add hyphenation sign
					changeStyle, myTextStyle, element,
					x, x + width - 1, y - height + 1, y + descent
				));
				changeStyle = false;
				wordOccurred = true;
			} else if (element instanceof ZLTextControlElement) {
				applyControl((ZLTextControlElement)element);
				changeStyle = true;
			}
			x += width;
		}
		if (!endOfParagraph) {
			final int len = info.EndCharIndex;
			if (len > 0) {
				final int wordIndex = info.EndElementIndex;
				final ZLTextWord word = (ZLTextWord)paragraph.getElement(wordIndex);
				final boolean addHyphenationSign = word.Data[word.Offset + len - 1] != '-';
				final int width = getWordWidth(word, 0, len, addHyphenationSign);
				final int height = getElementHeight(word);
				final int descent = myContext.getDescent();
				page.TextElementMap.add(
					new ZLTextElementArea(
						paragraphIndex, wordIndex, 0,
						len,
						false, // is last in element
						addHyphenationSign,
						changeStyle, myTextStyle, word,
						x, x + width - 1, y - height + 1, y + descent
					)
				);
			}
		}
	}

	public synchronized final void scrollPage(boolean forward, int scrollingMode, int value) {
		preparePaintInfo(myCurrentPage);
		myPreviousPage.reset();
		myNextPage.reset();
		if (myCurrentPage.PaintState == READY) {
			myCurrentPage.PaintState = forward ? TO_SCROLL_FORWARD : TO_SCROLL_BACKWARD;
			myScrollingMode = scrollingMode;
			myOverlappingValue = value;
		}
	}

	public final synchronized void gotoPosition(int paragraphIndex, int wordIndex, int charIndex) {
		if (myModel != null) {
			FBReaderApp.Instance().resetWidget();
			myCurrentPage.moveStartCursor(paragraphIndex, wordIndex, charIndex);
			myPreviousPage.reset();
			myNextPage.reset();
			preparePaintInfo(myCurrentPage);
			if (myCurrentPage.isEmptyPage()) {
				scrollPage(true, ScrollingMode.NO_OVERLAPPING, 0);
			}
		}
	}

	private final synchronized void gotoPositionByEnd(int paragraphIndex, int wordIndex, int charIndex) {
		if (myModel != null && myModel.getParagraphNumber() > 0) {
			myCurrentPage.moveEndCursor(paragraphIndex, wordIndex, charIndex);
			myPreviousPage.reset();
			myNextPage.reset();
			preparePaintInfo(myCurrentPage);
			if (myCurrentPage.isEmptyPage()) {
				scrollPage(false, ScrollingMode.NO_OVERLAPPING, 0);
			}
		}
	}

	protected synchronized void preparePaintInfo() {
		myPreviousPage.reset();
		myNextPage.reset();
		preparePaintInfo(myCurrentPage);
	}

	private synchronized void preparePaintInfo(ZLTextPage page) {
		int newWidth = getTextAreaWidth();
		int newHeight = getTextAreaHeight();
		if (newWidth != page.OldWidth || newHeight != page.OldHeight) {
			page.OldWidth = newWidth;
			page.OldHeight = newHeight;
			if (page.PaintState != NOTHING_TO_PAINT) {
				page.LineInfos.clear();
				if (page == myPreviousPage) {
					if (!page.EndCursor.isNull()) {
						page.StartCursor.reset();
						page.PaintState = END_IS_KNOWN;
					} else if (!page.StartCursor.isNull()) {
						page.EndCursor.reset();
						page.PaintState = START_IS_KNOWN;
					}
				} else {
					if (!page.StartCursor.isNull()) {
						page.EndCursor.reset();
						page.PaintState = START_IS_KNOWN;
					} else if (!page.EndCursor.isNull()) {
						page.StartCursor.reset();
						page.PaintState = END_IS_KNOWN;
					}
				}
			}
		}

		if (page.PaintState == NOTHING_TO_PAINT || page.PaintState == READY) {
			return;
		}

		final HashMap<ZLTextLineInfo,ZLTextLineInfo> cache = myLineInfoCache;
		for (ZLTextLineInfo info : page.LineInfos) {
			cache.put(info, info);
		}

		switch (page.PaintState) {
			default:
				break;
			case TO_SCROLL_FORWARD:
				if (!page.EndCursor.getParagraphCursor().isLast() || !page.EndCursor.isEndOfParagraph()) {
					final ZLTextWordCursor startCursor = new ZLTextWordCursor();
					switch (myScrollingMode) {
						case ScrollingMode.NO_OVERLAPPING:
							break;
						case ScrollingMode.KEEP_LINES:
							page.findLineFromEnd(startCursor, myOverlappingValue);
							break;
						case ScrollingMode.SCROLL_LINES:
							page.findLineFromStart(startCursor, myOverlappingValue);
							if (startCursor.isEndOfParagraph()) {
								startCursor.nextParagraph();
							}
							break;
						case ScrollingMode.SCROLL_PERCENTAGE:
							page.findPercentFromStart(startCursor, getTextAreaHeight(), myOverlappingValue);
							break;
					}

					if (!startCursor.isNull() && startCursor.samePositionAs(page.StartCursor)) {
						page.findLineFromStart(startCursor, 1);
					}

					if (!startCursor.isNull()) {
						final ZLTextWordCursor endCursor = new ZLTextWordCursor();
						buildInfos(page, startCursor, endCursor);
						if (!page.isEmptyPage() && (myScrollingMode != ScrollingMode.KEEP_LINES || !endCursor.samePositionAs(page.EndCursor))) {
							page.StartCursor.setCursor(startCursor);
							page.EndCursor.setCursor(endCursor);
							break;
						}
					}

					page.StartCursor.setCursor(page.EndCursor);
					buildInfos(page, page.StartCursor, page.EndCursor);
				}
				break;
			case TO_SCROLL_BACKWARD:
				if (!page.StartCursor.getParagraphCursor().isFirst() || !page.StartCursor.isStartOfParagraph()) {
					switch (myScrollingMode) {
						case ScrollingMode.NO_OVERLAPPING:
							page.StartCursor.setCursor(findStart(page.StartCursor, SizeUnit.PIXEL_UNIT, getTextAreaHeight()));
							break;
						case ScrollingMode.KEEP_LINES:
						{
							ZLTextWordCursor endCursor = new ZLTextWordCursor();
							page.findLineFromStart(endCursor, myOverlappingValue);
							if (!endCursor.isNull() && endCursor.samePositionAs(page.EndCursor)) {
								page.findLineFromEnd(endCursor, 1);
							}
							if (!endCursor.isNull()) {
								ZLTextWordCursor startCursor = findStart(endCursor, SizeUnit.PIXEL_UNIT, getTextAreaHeight());
								if (startCursor.samePositionAs(page.StartCursor)) {
									page.StartCursor.setCursor(findStart(page.StartCursor, SizeUnit.PIXEL_UNIT, getTextAreaHeight()));
								} else {
									page.StartCursor.setCursor(startCursor);
								}
							} else {
								page.StartCursor.setCursor(findStart(page.StartCursor, SizeUnit.PIXEL_UNIT, getTextAreaHeight()));
							}
							break;
						}
						case ScrollingMode.SCROLL_LINES:
							page.StartCursor.setCursor(findStart(page.StartCursor, SizeUnit.LINE_UNIT, myOverlappingValue));
							break;
						case ScrollingMode.SCROLL_PERCENTAGE:
							page.StartCursor.setCursor(findStart(page.StartCursor, SizeUnit.PIXEL_UNIT, getTextAreaHeight() * myOverlappingValue / 100));
							break;
					}
					buildInfos(page, page.StartCursor, page.EndCursor);
					if (page.isEmptyPage()) {
						page.StartCursor.setCursor(findStart(page.StartCursor, SizeUnit.LINE_UNIT, 1));
						buildInfos(page, page.StartCursor, page.EndCursor);
					}
				}
				break;
			case START_IS_KNOWN:
				buildInfos(page, page.StartCursor, page.EndCursor);
				break;
			case END_IS_KNOWN:
				// 先设置好字体，然后才能找到正确的起始字符
				if (!myModel.m_supportRichText) {
					final ZLTextStyleDecoration decoration =
							ZLTextStyleCollection.Instance().getDecoration(BookModel.NONE);
					setTextStyle(decoration.createDecoratedStyle(ZLTextStyleCollection.Instance().getBaseStyle()));
				}
				page.StartCursor.setCursor(findStart(page.EndCursor, SizeUnit.PIXEL_UNIT, getTextAreaHeight()));
				buildInfos(page, page.StartCursor, page.EndCursor);
				break;
		}
		page.PaintState = READY;
		// TODO: cache?
		myLineInfoCache.clear();

		if (page == myCurrentPage) {
			myPreviousPage.reset();
			myNextPage.reset();
		}
	}

	public void clearCaches() {
		rebuildPaintInfo();
		FBReaderApp.Instance().resetWidget();
	}

	protected void rebuildPaintInfo() {
		myPreviousPage.reset();
		myNextPage.reset();
		ZLTextParagraphCursorCache.clear();

		if (myCurrentPage.PaintState != NOTHING_TO_PAINT) {
			myCurrentPage.LineInfos.clear();
			if (!myCurrentPage.StartCursor.isNull()) {
				myCurrentPage.StartCursor.rebuild();
				myCurrentPage.EndCursor.reset();
				myCurrentPage.PaintState = START_IS_KNOWN;
			} else if (!myCurrentPage.EndCursor.isNull()) {
				myCurrentPage.EndCursor.rebuild();
				myCurrentPage.StartCursor.reset();
				myCurrentPage.PaintState = END_IS_KNOWN;
			}
		}

		myLineInfoCache.clear();
	}

	private int infoSize(ZLTextLineInfo info, int unit) {
		return (unit == SizeUnit.PIXEL_UNIT) ? (info.Height + info.Descent + info.VSpaceAfter) : (info.IsVisible ? 1 : 0);
	}

	private int paragraphSize(ZLTextWordCursor cursor, boolean beforeCurrentPosition, int unit) {
		final ZLTextParagraphCursor paragraphCursor = cursor.getParagraphCursor();
		if (paragraphCursor == null) {
			return 0;
		}
		final int endElementIndex =
			beforeCurrentPosition ? cursor.getElementIndex() : paragraphCursor.getParagraphLength();

		resetTextStyle();

		int size = 0;

		int wordIndex = 0;
		int charIndex = 0;
		while (wordIndex != endElementIndex) {
			ZLTextLineInfo info = processTextLine(paragraphCursor, wordIndex, charIndex, endElementIndex);
			wordIndex = info.EndElementIndex;
			charIndex = info.EndCharIndex;
			size += infoSize(info, unit);
		}

		return size;
	}

	private void skip(ZLTextWordCursor cursor, int unit, int size) {
		final ZLTextParagraphCursor paragraphCursor = cursor.getParagraphCursor();
		if (paragraphCursor == null) {
			return;
		}
		final int endElementIndex = paragraphCursor.getParagraphLength();

		resetTextStyle();
		applyControls(paragraphCursor, 0, cursor.getElementIndex());

		while (!cursor.isEndOfParagraph() && (size > 0)) {
			ZLTextLineInfo info = processTextLine(paragraphCursor, cursor.getElementIndex(), cursor.getCharIndex(), endElementIndex);
			cursor.moveTo(info.EndElementIndex, info.EndCharIndex);
			size -= infoSize(info, unit);
		}
	}

	private ZLTextWordCursor findStart(ZLTextWordCursor end, int unit, int size) {
		final ZLTextWordCursor start = new ZLTextWordCursor(end);
		size -= paragraphSize(start, true, unit);
		boolean positionChanged = !start.isStartOfParagraph();
		start.moveToParagraphStart();
		while (size > 0) {
			if (positionChanged && start.getParagraphCursor().isEndOfSection()) {
				break;
			}
			if (!start.previousParagraph()) {
				break;
			}
			if (!start.getParagraphCursor().isEndOfSection()) {
				positionChanged = true;
			}
			size -= paragraphSize(start, false, unit);
		}
		skip(start, unit, -size);

		if (unit == SizeUnit.PIXEL_UNIT) {
			boolean sameStart = start.samePositionAs(end);
			if (!sameStart && start.isEndOfParagraph() && end.isStartOfParagraph()) {
				ZLTextWordCursor startCopy = start;
				startCopy.nextParagraph();
				sameStart = startCopy.samePositionAs(end);
			}
			if (sameStart) {
				start.setCursor(findStart(end, SizeUnit.LINE_UNIT, 1));
			}
		}

		return start;
	}

	protected ZLTextElementArea getElementByCoordinates(int x, int y) {
		return myCurrentPage.TextElementMap.binarySearch(x, y);
	}

	public void hideSelectedRegionBorder() {
		myHighlightSelectedRegion = false;
		FBReaderApp.Instance().resetWidget();
	}

	private ZLTextRegion getSelectedRegion(ZLTextPage page) {
		return page.TextElementMap.getRegion(mySelectedRegionSoul);
	}

	public ZLTextRegion getSelectedRegion() {
		return getSelectedRegion(myCurrentPage);
	}

	protected ZLTextRegion findRegion(int x, int y, ZLTextRegion.Filter filter) {
		return findRegion(x, y, Integer.MAX_VALUE - 1, filter);
	}

	protected ZLTextRegion findRegion(int x, int y, int maxDistance, ZLTextRegion.Filter filter) {
		return myCurrentPage.TextElementMap.findRegion(x, y, maxDistance, filter);
	}

	public void selectRegion(ZLTextRegion region) {
		final ZLTextRegion.Soul soul = region != null ? region.getSoul() : null;
		if (soul == null || !soul.equals(mySelectedRegionSoul)) {
			myHighlightSelectedRegion = true;
		}
		mySelectedRegionSoul = soul;
	}

	protected boolean initSelection(int x, int y) {
		y -= ZLTextSelectionCursor.getHeight() / 2 + ZLTextSelectionCursor.getAccent() / 2;
		if (!mySelection.start(x, y)) {
			return false;
		}
		FBReaderApp.Instance().resetWidget();
		FBReaderApp.Instance().repaintWidget();
		return true;
	}

	public void clearSelection() {
		if (mySelection.clear()) {
			FBReaderApp.Instance().resetWidget();
			FBReaderApp.Instance().repaintWidget();
		}
	}

	public int getSelectionStartY() {
		if (mySelection.isEmpty()) {
			return 0;
		}
		final ZLTextElementArea selectionStartArea = mySelection.getStartArea(myCurrentPage);
		if (selectionStartArea != null) {
			return selectionStartArea.YStart;
		}
		if (mySelection.hasAPartBeforePage(myCurrentPage)) {
			final ZLTextElementArea firstArea = myCurrentPage.TextElementMap.getFirstArea();
			return firstArea != null ? firstArea.YStart : 0;
		} else {
			final ZLTextElementArea lastArea = myCurrentPage.TextElementMap.getLastArea();
			return lastArea != null ? lastArea.YEnd : 0;
		}
	}

	public int getSelectionEndY() {
		if (mySelection.isEmpty()) {
			return 0;
		}
		final ZLTextElementArea selectionEndArea = mySelection.getEndArea(myCurrentPage);
		if (selectionEndArea != null) {
			return selectionEndArea.YEnd;
		}
		if (mySelection.hasAPartAfterPage(myCurrentPage)) {
			final ZLTextElementArea lastArea = myCurrentPage.TextElementMap.getLastArea();
			return lastArea != null ? lastArea.YEnd : 0;
		} else {
			final ZLTextElementArea firstArea = myCurrentPage.TextElementMap.getFirstArea();
			return firstArea != null ? firstArea.YStart : 0;
		}
	}

	public ZLTextPosition getSelectionStartPosition() {
		return mySelection.getStartPosition();
	}

	public ZLTextPosition getSelectionEndPosition() {
		return mySelection.getEndPosition();
	}

	public boolean isSelectionEmpty() {
		return mySelection.isEmpty();
	}

	public void resetRegionPointer() {
		mySelectedRegionSoul = null;
		myHighlightSelectedRegion = true;
	}

	public ZLTextRegion nextRegion(Direction direction, ZLTextRegion.Filter filter) {
		return myCurrentPage.TextElementMap.nextRegion(getSelectedRegion(), direction, filter);
	}

	public boolean canScroll(PageIndex index) {
		switch (index) {
			default:
				return true;
			case next:
			{
				final ZLTextWordCursor cursor = getEndCursor();
				return
					cursor != null &&
					!cursor.isNull() &&
					(!cursor.isEndOfParagraph() || !cursor.getParagraphCursor().isLast());
			}
			case previous:
			{
				final ZLTextWordCursor cursor = getStartCursor();
				return
					cursor != null &&
					!cursor.isNull() &&
					(!cursor.isStartOfParagraph() || !cursor.getParagraphCursor().isFirst());
			}
		}
	}
	
	private ZLTextStyle myTextStyle;
	private int myWordHeight = -1;

	final int getWordHeight() {
		if (myContext == null) {
			return 0;
		}

		if (myWordHeight == -1) {
			final ZLTextStyle textStyle = myTextStyle;
			myWordHeight = (int)(myContext.getStringHeight() * textStyle.getLineSpacePercent() / 100) + textStyle.getVerticalShift();
		}
		return myWordHeight;
	}

	ZLPaintContext.Size getTextAreaSize() {
		return new ZLPaintContext.Size(getTextAreaWidth(), getTextAreaHeight());
	}

	int getTextAreaHeight() {
		if (myContext == null) {
			return 0;
		}
		return myContext.getHeight() - FBReaderApp.Instance().getTopMargin() - FBReaderApp.Instance().getBottomMargin();
	}

	int getTextAreaWidth() {
		if (myContext == null) {
			return 0;
		}
		return myContext.getWidth() - FBReaderApp.Instance().getLeftMargin() - FBReaderApp.Instance().getRightMargin();
	}

	int getBottomLine() {
		if (myContext == null) {
			return 0;
		}
		return myContext.getHeight() - FBReaderApp.Instance().getBottomMargin() - 1;
	}

	int getRightLine() {
		if (myContext == null) {
			return 0;
		}
		return myContext.getWidth() - FBReaderApp.Instance().getRightMargin() - 1;
	}

	final void setTextStyle(ZLTextStyle style) {
		if (myTextStyle != style) {
			myTextStyle = style;
			myWordHeight = -1;
		}
		
		if (myContext == null) {
			return;
		}

		myContext.setFont(style.getFontFamily(), style.getFontSize(), style.isBold(), style.isItalic(), style.isUnderline(), style.isStrikeThrough());
	}

	final void resetTextStyle() {
		if (!myModel.m_supportRichText) {
			return;
		}
		setTextStyle(ZLTextStyleCollection.Instance().getBaseStyle());
	}

	void applyControl(ZLTextControlElement control) {
		if (control.IsStart) {
			final ZLTextStyleDecoration decoration =
				ZLTextStyleCollection.Instance().getDecoration(control.Kind);
			if (control instanceof ZLTextHyperlinkControlElement) {
				setTextStyle(decoration.createDecoratedStyle(myTextStyle, ((ZLTextHyperlinkControlElement)control).Hyperlink));
			} else {
				setTextStyle(decoration.createDecoratedStyle(myTextStyle));
			}
		} else {
			setTextStyle(myTextStyle.Base);
		}
	}

	void applyControls(ZLTextParagraphCursor cursor, int index, int end) {
		for (; index != end; ++index) {
			final ZLTextElement element = cursor.getElement(index);
			if (element instanceof ZLTextControlElement) {
				applyControl((ZLTextControlElement)element);
			}
		}
	}

	final int getElementWidth(ZLTextElement element, int charIndex) {
		if (element instanceof ZLTextWord) {
			return getWordWidth((ZLTextWord)element, charIndex);
		} else if (element instanceof ZLTextImageElement) {
			if (myContext == null) {
				return 0;
			}
			final ZLTextImageElement imageElement = (ZLTextImageElement)element;
			final ZLPaintContext.Size size = myContext.imageSize(
				imageElement.ImageData,
				getTextAreaSize(),
				imageElement.IsCover
					? ZLPaintContext.ScalingType.FitMaximum
					: ZLPaintContext.ScalingType.IntegerCoefficient
			);
			return size != null ? size.Width : 0;
		} else if (element == ZLTextElement.IndentElement) {
			return myTextStyle.getFirstLineIndentDelta();
		} else if (element instanceof ZLTextFixedHSpaceElement) {
			if (myContext == null) {
				return 0;
			}
			return myContext.getSpaceWidth() * ((ZLTextFixedHSpaceElement)element).Length;
		}
		return 0;
	}

	final int getElementHeight(ZLTextElement element) {
		if (element instanceof ZLTextWord) {
			return getWordHeight();
		} else if (element instanceof ZLTextImageElement) {
			if (myContext == null) {
				return 0;
			}
			final ZLTextImageElement imageElement = (ZLTextImageElement)element;
			final ZLPaintContext.Size size = myContext.imageSize(
				imageElement.ImageData,
				getTextAreaSize(),
				imageElement.IsCover
					? ZLPaintContext.ScalingType.FitMaximum
					: ZLPaintContext.ScalingType.IntegerCoefficient
			);
			return (size != null ? size.Height : 0) +
				Math.max(myContext.getStringHeight() * (myTextStyle.getLineSpacePercent() - 100) / 100, 3);
		}
		return 0;
	}

	final int getElementDescent(ZLTextElement element) {
		return element instanceof ZLTextWord ? myContext.getDescent() : 0;
	}

	final int getWordWidth(ZLTextWord word, int start) {
		if (myContext == null) {
			return 0;
		}
		return
			start == 0 ?
				word.getWidth(myContext) :
				myContext.getStringWidth(word.Data, word.Offset + start, word.Length - start);
	}

	final int getWordWidth(ZLTextWord word, int start, int length) {
		if (myContext == null) {
			return 0;
		}
		return myContext.getStringWidth(word.Data, word.Offset + start, length);
	}

	private char[] myWordPartArray = new char[20];

	final int getWordWidth(ZLTextWord word, int start, int length, boolean addHyphenationSign) {
		if (myContext == null) {
			return 0;
		}
		if (length == -1) {
			if (start == 0) {
				return word.getWidth(myContext);
			}
			length = word.Length - start;
		}
		if (!addHyphenationSign) {
			return myContext.getStringWidth(word.Data, word.Offset + start, length);
		}
		char[] part = myWordPartArray;
		if (length + 1 > part.length) {
			part = new char[length + 1];
			myWordPartArray = part;
		}
		System.arraycopy(word.Data, word.Offset + start, part, 0, length);
		part[length] = '-';
		return myContext.getStringWidth(part, 0, length + 1);
	}

	int getAreaLength(ZLTextParagraphCursor paragraph, ZLTextElementArea area, int toCharIndex) {
		setTextStyle(area.Style);
		final ZLTextWord word = (ZLTextWord)paragraph.getElement(area.ElementIndex);
		int length = toCharIndex - area.CharIndex;
		boolean selectHyphenationSign = false;
		if (length >= area.Length) {
			selectHyphenationSign = area.AddHyphenationSign;
			length = area.Length;
		}
		if (length > 0) {
			return getWordWidth(word, area.CharIndex, length, selectHyphenationSign);
		}
		return 0;
	}

	final void drawWord(int x, int y, ZLTextWord word, int start, int length, boolean addHyphenationSign, ZLColor color) {
		if (myContext == null) {
			return;
		}

//		Log.d("drawWord", new String(word.toString()));
		final ZLPaintContext context = myContext;
		context.setTextColor(color);
		if (start == 0 && length == -1) {
			drawString(x, y, word.Data, word.Offset, word.Length, word.getMark(), 0);
		} else {
			if (length == -1) {
				length = word.Length - start;
			}
			if (!addHyphenationSign) {
				drawString(x, y, word.Data, word.Offset + start, length, word.getMark(), start);
			} else {
				char[] part = myWordPartArray;
				if (length + 1 > part.length) {
					part = new char[length + 1];
					myWordPartArray = part;
				}
				System.arraycopy(word.Data, word.Offset + start, part, 0, length);
				part[length] = '-';
				drawString(x, y, part, 0, length + 1, word.getMark(), start);
			}
		}
	}

	private final void drawString(int x, int y, char[] str, int offset, int length, ZLTextWord.Mark mark, int shift) {
		if (myContext == null) {
			return;
		}

		final ZLPaintContext context = myContext;
		if (mark == null) {
			context.drawString(x, y, str, offset, length);
		} else {
			int pos = 0;
			for (; (mark != null) && (pos < length); mark = mark.getNext()) {
				int markStart = mark.Start - shift;
				int markLen = mark.Length;

				if (markStart < pos) {
					markLen += markStart - pos;
					markStart = pos;
				}

				if (markLen <= 0) {
					continue;
				}

				if (markStart > pos) {
					int endPos = Math.min(markStart, length);
					context.drawString(x, y, str, offset + pos, endPos - pos);
					x += context.getStringWidth(str, offset + pos, endPos - pos);
				}

				if (markStart < length) {
					context.setFillColor(getHighlightingColor());
					int endPos = Math.min(markStart + markLen, length);
					final int endX = x + context.getStringWidth(str, offset + markStart, endPos - markStart);
					context.fillRectangle(x, y - context.getStringHeight(), endX - 1, y + context.getDescent());
					context.drawString(x, y, str, offset + markStart, endPos - markStart);
					x = endX;
				}
				pos = markStart + markLen;
			}

			if (pos < length) {
				context.drawString(x, y, str, offset + pos, length - pos);
			}
		}
	}

	public static enum PageIndex {
		previous, current, next;

		public PageIndex getNext() {
			switch (this) {
				case previous:
					return current;
				case current:
					return next;
				default:
					return null;
			}
		}

		public PageIndex getPrevious() {
			switch (this) {
				case next:
					return current;
				case current:
					return previous;
				default:
					return null;
			}
		}
	};
	public static enum Direction {
		leftToRight, rightToLeft, up, down;
	};
	public static enum Animation {
		none, shift, curl, curl3d
	}

	private int myStartY;
	private boolean myIsBrightnessAdjustmentInProgress;
	private int myStartBrightness;

	private String myZoneMapId;
	private TapZoneMap myZoneMap;

	private TapZoneMap getZoneMap() {
		final String id = "up";// 统一使用翻页的操作模式
		if (!id.equals(myZoneMapId)) {
			myZoneMap = new TapZoneMap(id);
			myZoneMapId = id;
		}
		return myZoneMap;
	}

	public boolean onFingerSingleTap(int x, int y) {
		final ZLTextRegion region = findRegion(x, y, MAX_SELECTION_DISTANCE, ZLTextRegion.HyperlinkFilter);
		if (region != null) {
			selectRegion(region);
			FBReaderApp.Instance().resetWidget();
			FBReaderApp.Instance().repaintWidget();
			FBReaderApp.Instance().runAction(ActionCode.PROCESS_HYPERLINK);
			return true;
		}

		FBReaderApp.Instance().runAction(getZoneMap().getActionByCoordinates(
			x, y, myContext.getWidth(), myContext.getHeight(), TapZoneMap.Tap.singleTap), x, y);

		return true;
	}

	public boolean onFingerPress(int x, int y) {
		final ZLTextSelectionCursor cursor = findSelectionCursor(x, y, MAX_SELECTION_DISTANCE);
		if (cursor != ZLTextSelectionCursor.None) {
			FBReaderApp.Instance().runAction(ActionCode.SELECTION_HIDE_PANEL);
			moveSelectionCursorTo(cursor, x, y);
			return true;
		}

		if (FBReaderApp.Instance().AllowScreenBrightnessAdjustmentOption.getValue() && x < myContext.getWidth() / 10) {
			myIsBrightnessAdjustmentInProgress = true;
			myStartY = y;
			myStartBrightness = FBReaderApp.Instance().getScreenBrightness();
			return true;
		}

		startManualScrolling(x, y);
		return true;
	}

	private boolean isFlickScrollingEnabled() {
		final ScrollingPreferences.FingerScrolling fingerScrolling =
			ScrollingPreferences.Instance().FingerScrollingOption.getValue();
		return
			fingerScrolling == ScrollingPreferences.FingerScrolling.byFlick ||
			fingerScrolling == ScrollingPreferences.FingerScrolling.byTapAndFlick;
	}

	private void startManualScrolling(int x, int y) {
		if (!isFlickScrollingEnabled()) {
			return;
		}
		
		if (FBReaderApp.Instance().isUseGLView()) {
			// 3d翻页仅有左右模式
			FBReaderApp.Instance().getWidgetGL().startManualScrolling(x, y);
		} else {
			FBReaderApp.Instance().getWidget().startManualScrolling(x, y);
		}
	}

	public boolean onFingerMove(int x, int y) {
		final ZLTextSelectionCursor cursor = getSelectionCursorInMovement();
		if (cursor != ZLTextSelectionCursor.None) {
			moveSelectionCursorTo(cursor, x, y);
			return true;
		}

		synchronized (this) {
			if (myIsBrightnessAdjustmentInProgress) {
				if (x >= myContext.getWidth() / 5) {
					myIsBrightnessAdjustmentInProgress = false;
					startManualScrolling(x, y);
				} else {
					final int delta = (myStartBrightness + 30) * (myStartY - y) / myContext.getHeight();
					FBReaderApp.Instance().setScreenBrightness(myStartBrightness + delta);
					return true;
				}
			}

			if (isFlickScrollingEnabled()) {
				if (FBReaderApp.Instance().isUseGLView()) {
					FBReaderApp.Instance().getWidgetGL().scrollManuallyTo(x, y);
				} else {
					FBReaderApp.Instance().getWidget().scrollManuallyTo(x, y);
				}
			}
		}
		return true;
	}

	public boolean onFingerRelease(int x, int y) {
		final ZLTextSelectionCursor cursor = getSelectionCursorInMovement();
		if (cursor != ZLTextSelectionCursor.None) {
			releaseSelectionCursor();
			return true;
		}

		if (myIsBrightnessAdjustmentInProgress) {
			myIsBrightnessAdjustmentInProgress = false;
			return true;
		}

		if (isFlickScrollingEnabled()) {
			if (FBReaderApp.Instance().isUseGLView()) {
				FBReaderApp.Instance().getWidgetGL().startAnimatedScrolling(null,
						x, y, null, ScrollingPreferences.Instance().AnimationSpeedOption.getValue()
					);
			} else {
				FBReaderApp.Instance().getWidget().startAnimatedScrolling(
						x, y, ScrollingPreferences.Instance().AnimationSpeedOption.getValue()
					);
			}
			
			return true;
		}

		return true;
	}

	public boolean onFingerLongPress(int x, int y) {
		final ZLTextRegion region = findRegion(x, y, MAX_SELECTION_DISTANCE, ZLTextRegion.AnyRegionFilter);
		if (region != null) {
			final ZLTextRegion.Soul soul = region.getSoul();
			boolean doSelectRegion = false;
			if (soul instanceof ZLTextWordRegionSoul) {
				switch (FBReaderApp.Instance().WordTappingActionOption.getValue()) {
					case startSelecting:
						FBReaderApp.Instance().runAction(ActionCode.SELECTION_HIDE_PANEL);
						initSelection(x, y);
						final ZLTextSelectionCursor cursor = findSelectionCursor(x, y);
						if (cursor != ZLTextSelectionCursor.None) {
							moveSelectionCursorTo(cursor, x, y);
						}
						return true;
					case selectSingleWord:
					case openDictionary:
						doSelectRegion = true;
						break;
				}
			} else if (soul instanceof ZLTextImageRegionSoul) {
				doSelectRegion =
						FBReaderApp.Instance().ImageTappingActionOption.getValue() !=
					FBReaderApp.ImageTappingAction.doNothing;
			} else if (soul instanceof ZLTextHyperlinkRegionSoul) {
				doSelectRegion = true;
			}
        
			if (doSelectRegion) {
				selectRegion(region);
				FBReaderApp.Instance().resetWidget();
				FBReaderApp.Instance().repaintWidget();
				return true;
			}
		}

		return false;
	}

	public boolean onFingerMoveAfterLongPress(int x, int y) {
		final ZLTextSelectionCursor cursor = getSelectionCursorInMovement();
		if (cursor != ZLTextSelectionCursor.None) {
			moveSelectionCursorTo(cursor, x, y);
			return true;
		}

		ZLTextRegion region = getSelectedRegion();
		if (region != null) {
			ZLTextRegion.Soul soul = region.getSoul();
			if (soul instanceof ZLTextHyperlinkRegionSoul ||
				soul instanceof ZLTextWordRegionSoul) {
				if (FBReaderApp.Instance().WordTappingActionOption.getValue() !=
					FBReaderApp.WordTappingAction.doNothing) {
					region = findRegion(x, y, MAX_SELECTION_DISTANCE, ZLTextRegion.AnyRegionFilter);
					if (region != null) {
						soul = region.getSoul();
						if (soul instanceof ZLTextHyperlinkRegionSoul
							 || soul instanceof ZLTextWordRegionSoul) {
							selectRegion(region);
							FBReaderApp.Instance().resetWidget();
							FBReaderApp.Instance().repaintWidget();
						}
					}
				}
			}
		}
		return true;
	}

	public boolean onFingerReleaseAfterLongPress(int x, int y) {
		final ZLTextSelectionCursor cursor = getSelectionCursorInMovement();
		if (cursor != ZLTextSelectionCursor.None) {
			releaseSelectionCursor();
			return true;
		}

		final ZLTextRegion region = getSelectedRegion();
		if (region != null) {
			final ZLTextRegion.Soul soul = region.getSoul();

			boolean doRunAction = false;
			if (soul instanceof ZLTextWordRegionSoul) {
				doRunAction =
						FBReaderApp.Instance().WordTappingActionOption.getValue() ==
					FBReaderApp.WordTappingAction.openDictionary;
			} else if (soul instanceof ZLTextImageRegionSoul) {
				doRunAction =
						FBReaderApp.Instance().ImageTappingActionOption.getValue() ==
					FBReaderApp.ImageTappingAction.openImageView;
			}

			if (doRunAction) {
				FBReaderApp.Instance().runAction(ActionCode.PROCESS_HYPERLINK);
				return true;
			}
		}

		return false;
	}

	public boolean onTrackballRotated(int diffX, int diffY) {
		if (diffX == 0 && diffY == 0) {
			return true;
		}

		final Direction direction = (diffY != 0) ?
			(diffY > 0 ? Direction.down : Direction.up) :
			(diffX > 0 ? Direction.leftToRight : Direction.rightToLeft);

		new MoveCursorAction(FBReaderApp.Instance(), direction).run();
		return true;
	}

	public ZLFile getWallpaperFile() {
		final String filePath = FBReaderApp.Instance().getColorProfile().WallpaperOption.getValue();
		if ("".equals(filePath)) {
			return null;
		}
		
		final ZLFile file = ZLFile.createFileByPath(filePath);
		if (file == null || !file.exists()) {
			return null;
		}
		return file;
	}

	public ZLColor getBackgroundColor() {
		return FBReaderApp.Instance().getColorProfile().BackgroundOption.getValue();
	}

	public ZLColor getSelectedBackgroundColor() {
		return FBReaderApp.Instance().getColorProfile().SelectionBackgroundOption.getValue();
	}

	public ZLColor getSelectedForegroundColor() {
		return FBReaderApp.Instance().getColorProfile().SelectionForegroundOption.getValue();
	}

	public ZLColor getTextColor(ZLTextHyperlink hyperlink) {
		final ColorProfile profile = FBReaderApp.Instance().getColorProfile();
		switch (hyperlink.Type) {
			default:
			case BookModel.NONE:
				return profile.RegularTextOption.getValue();
			case BookModel.INTERNAL:
				return FBReaderApp.Instance().Model.Book.isHyperlinkVisited(hyperlink.Id)
					? profile.VisitedHyperlinkTextOption.getValue()
					: profile.HyperlinkTextOption.getValue();
			case BookModel.EXTERNAL:
				return profile.HyperlinkTextOption.getValue();
		}
	}

	public ZLColor getHighlightingColor() {
		return FBReaderApp.Instance().getColorProfile().HighlightingOption.getValue();
	}

	public class Footer {
		private Runnable UpdateTask = new Runnable() {
			public void run() {
				FBReaderApp.Instance().repaintStatusBar();
			}
		};

		private ArrayList<TOCTree> myTOCMarks;

		public int getHeight() {
			return FBReaderApp.Instance().FooterHeightOption.getValue();
		}

		public synchronized void resetTOCMarks() {
			myTOCMarks = null;
		}

		private final int MAX_TOC_MARKS_NUMBER = 100;
		private synchronized void updateTOCMarks(BookModel model) {
			myTOCMarks = new ArrayList<TOCTree>();
			TOCTree toc = model.TOCTree;
			if (toc == null) {
				return;
			}
			int maxLevel = Integer.MAX_VALUE;
			if (toc.getSize() >= MAX_TOC_MARKS_NUMBER) {
				final int[] sizes = new int[10];
				for (TOCTree tocItem : toc) {
					if (tocItem.Level < 10) {
						++sizes[tocItem.Level];
					}
				}
				for (int i = 1; i < sizes.length; ++i) {
					sizes[i] += sizes[i - 1];
				}
				for (maxLevel = sizes.length - 1; maxLevel >= 0; --maxLevel) {
					if (sizes[maxLevel] < MAX_TOC_MARKS_NUMBER) {
						break;
					}
				}
			}
			for (TOCTree tocItem : toc.allSubTrees(maxLevel)) {
				myTOCMarks.add(tocItem);
			}
		}

		public synchronized void paint(ZLPaintContext context, PageIndex pageIndex, boolean update) {
			final BookModel model = FBReaderApp.Instance().Model;
			if (model == null) {
				return;
			}
			
			if (update) {
				final ZLFile wallpaper = getWallpaperFile();
				if (wallpaper != null) {
					context.clear(wallpaper, wallpaper instanceof ZLResourceFile);
				} else {
					context.clear(getBackgroundColor());
				}
			}

			//final ZLColor bgColor = getBackgroundColor();
			// TODO: separate color option for footer color
			final ZLColor fgColor = getTextColor(ZLTextHyperlink.NO_LINK);
			final ZLColor fillColor = FBReaderApp.Instance().getColorProfile().FooterFillOption.getValue();

			final int left = FBReaderApp.Instance().getLeftMargin();
			final int right = context.getWidth() - FBReaderApp.Instance().getRightMargin();
			final int height = getHeight();
			final int lineWidth = height <= 10 ? 1 : 2;
			final int delta = height <= 10 ? 0 : 1;
			int offsetY = 0;
			
			if (FBReaderApp.Instance().isUseGLView()) {
				final ZLGLWidget widget = FBReaderApp.Instance().getWidgetGL();
				offsetY = widget.getHeight() - getHeight() * 2;
			} else {
				final ZLViewWidget widget = FBReaderApp.Instance().getWidget();
				offsetY = widget.getHeight() - getHeight() * 2;
			}

			context.setFont(
					FBReaderApp.Instance().FooterFontOption.getValue(),
				height <= 10 ? height + 3 : height + 1,
				height > 10, false, false, false
			);

			int pageNumber = 0;//getCurrentCharNumber(pageIndex, true);
			int totalPageNumber = 1;//sizeOfFullText(); TODO impl it
			float percent = (float)pageNumber / totalPageNumber;

			final StringBuilder info = new StringBuilder();
			if (FBReaderApp.Instance().FooterShowProgressOption.getValue()) {
				info.append(String.format("%.2f%%", percent * 100));
			}
			if (FBReaderApp.Instance().FooterShowBatteryOption.getValue()) {
				if (info.length() > 0) {
					info.append(" ");
				}
				info.append(FBReaderApp.Instance().getBatteryLevel());
				info.append("%");
			}
			if (FBReaderApp.Instance().FooterShowClockOption.getValue()) {
				if (info.length() > 0) {
					info.append(" ");
				}
				info.append(FBReaderApp.Instance().getCurrentTimeString());
			}
			final String infoString = info.toString();

			final int infoWidth = context.getStringWidth(infoString);

			// draw info text
			context.setTextColor(fgColor);
			context.drawString(right - infoWidth, offsetY + height - delta, infoString);

			// draw gauge
			final int gaugeRight = right - (infoWidth == 0 ? 0 : infoWidth + 10);
			myGaugeWidth = gaugeRight - left - 2 * lineWidth;

			context.setLineColor(fgColor);
			context.setLineWidth(lineWidth);
			context.drawLine(left, offsetY + lineWidth, left, offsetY + height - lineWidth);
			context.drawLine(left, offsetY + height - lineWidth, gaugeRight, offsetY + height - lineWidth);
			context.drawLine(gaugeRight, offsetY + height - lineWidth, gaugeRight, offsetY + lineWidth);
			context.drawLine(gaugeRight, offsetY + lineWidth, left, offsetY + lineWidth);

			final int gaugeInternalRight =
				left + lineWidth + (int)(1.0 * myGaugeWidth * percent);

			context.setFillColor(fillColor);
			context.fillRectangle(left + 1, offsetY + height - 2 * lineWidth, gaugeInternalRight, offsetY + lineWidth + 1);

			if (FBReaderApp.Instance().FooterShowTOCMarksOption.getValue()) {
				if (myTOCMarks == null) {
					updateTOCMarks(model);
				}
				final int fullLength = 1;//sizeOfFullText(); TODO impl it
				for (TOCTree tocItem : myTOCMarks) {
					TOCTree.Reference reference = tocItem.getReference();
					if (reference != null) {
						final int refCoord = sizeOfTextBeforeParagraph(reference.ParagraphIndex);
						final int xCoord = left + 2 * lineWidth + (int)(1.0 * myGaugeWidth * refCoord / fullLength);
						context.drawLine(xCoord, offsetY + height - lineWidth, xCoord, offsetY + lineWidth);
					}
				}
			}
		}

		// TODO: remove
		int myGaugeWidth = 1;
	}

	private Footer myFooter;
	public Footer getFooterArea() {
		if (FBReaderApp.Instance().ScrollbarTypeOption.getValue() == SCROLLBAR_SHOW_AS_FOOTER) {
			if (myFooter == null) {
				myFooter = new Footer();
				FBReaderApp.Instance().addTimerTask(myFooter.UpdateTask, 30000);
			}
		} else {
			if (myFooter != null) {
				FBReaderApp.Instance().removeTimerTask(myFooter.UpdateTask);
				myFooter = null;
			}
		}
		return myFooter;
	}

	public String getSelectedText() {
		final TextBuildTraverser traverser = new TextBuildTraverser(this);
		if (!isSelectionEmpty()) {
			traverser.traverse(getSelectionStartPosition(), getSelectionEndPosition());
		}
		return traverser.getText();
	}

	public int getCountOfSelectedWords() {
		final WordCountTraverser traverser = new WordCountTraverser(this);
		if (!isSelectionEmpty()) {
			traverser.traverse(getSelectionStartPosition(), getSelectionEndPosition());
		}
		return traverser.getCount();
	}

	public static final int SCROLLBAR_SHOW_AS_FOOTER = 3;

	public int scrollbarType() {
		return FBReaderApp.Instance().ScrollbarTypeOption.getValue();
	}
}
