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

import java.util.HashMap;
import java.util.List;

import org.geometerplus.zlibrary.image.ZLImage;
import org.geometerplus.zlibrary.image.ZLImageMap;
import org.geometerplus.zlibrary.text.model.*;

import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.formats.FormatPlugin;

import android.os.Debug;

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
	
	
	
	
	

	public static BookModel createModel(Book book) throws BookReadingException {
		final FormatPlugin plugin = book.getPlugin();

		System.err.println("using plugin: " + plugin.supportedFileType() + "/" + plugin.type());

		final BookModel model = new BookModel(book);

//		Debug.startMethodTracing("socoolreader.trace");//calc为文件生成名
		plugin.readModel(model);
//		Debug.stopMethodTracing();
		return model;
	}

	public final Book Book;
	public final TOCTree TOCTree = new TOCTree();

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
		BookTextModel = new ZLTextModel(null, book.getLanguage(), 1024, 65536, Paths.cacheDirectory(), "cache", myImageMap);
	}

	public interface LabelResolver {
		List<String> getCandidates(String id);
	}

	private LabelResolver myResolver;

	public void setLabelResolver(LabelResolver resolver) {
		myResolver = resolver;
	}

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
	
	
	
	protected CachedCharStorageBase myInternalHyperlinks;
	protected final ZLImageMap myImageMap = new ZLImageMap();

	protected Label getLabelInternal(String id) {
		final int len = id.length();
		final int size = myInternalHyperlinks.size();

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
	
	public final ZLTextModel BookTextModel;

	public ZLTextModel getTextModel() {
		return BookTextModel;
	}

	private char[] myCurrentLinkBlock;
	private int myCurrentLinkBlockOffset;

	void addHyperlinkLabel(String label, ZLTextModel model, int paragraphNumber) {
		final String modelId = model.getId();
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
}
