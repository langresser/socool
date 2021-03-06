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

package org.socool.screader.screader;

import org.socool.zlibrary.text.ZLTextControlElement;
import org.socool.zlibrary.text.ZLTextTraverser;
import org.socool.zlibrary.text.ZLTextView;
import org.socool.zlibrary.text.ZLTextWord;

public class TextBuildTraverser extends ZLTextTraverser {
	protected final StringBuilder myBuffer = new StringBuilder();

	public TextBuildTraverser(ZLTextView view) {
		super(view);
	}

	@Override
	protected void processWord(ZLTextWord word) {
		myBuffer.append(word.Data, word.Offset, word.Length);
	}

	@Override
	protected void processControlElement(ZLTextControlElement control) {
		// does nothing
	}

	@Override
	protected void processSpace() {
		myBuffer.append(" ");
	}

	@Override
	protected void processEndOfParagraph() {
		myBuffer.append("\n");
	}

	public String getText() {
		return myBuffer.toString();
	}
}
