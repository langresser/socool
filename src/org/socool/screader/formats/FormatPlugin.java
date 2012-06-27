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

package org.socool.screader.formats;

import org.socool.zlibrary.encodings.EncodingCollection;
import org.socool.zlibrary.filesystem.ZLFile;
import org.socool.zlibrary.image.ZLImage;

import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.library.Book;
import org.socool.socoolreader.yhyxcs.R;

public abstract class FormatPlugin {
	private final String myFileType;

	protected FormatPlugin(String fileType) {
		myFileType = fileType;
	}

	public final String supportedFileType() {
		return myFileType;
	}

	public boolean supportStreamRead()						// �Ƿ�֧���ļ����ֶ�ȡ(��ʱֻ��txt��ȡ֧��)
	{
		return false;
	}

	public void readParagraph(int paragraph)				// ��ȡĳһ���䣨���ֶ�ȡ��
	{	
	}
	public void readPercent(double percent)					// ��ȡ�ļ��ٷֱȣ����ֶ�ȡ��
	{
	}
	
	public void readChapter(int chapter)					// ���½ڶ�ȡ
	{
	}

	public int getDefaultCoverId()
	{
		return 0;
//		return R.drawable.cover_default;
	}

	public abstract void readMetaInfo(Book book);				// ��ȡ������Ϣ
	public abstract void readModel(BookModel model);			// ��ȡ�ı���Ϣ��������ȡ��	
	public abstract void detectLanguageAndEncoding(Book book);	// �����������
	public abstract ZLImage readCover(Book book);						// ��ȡ������Ϣ
	public abstract String readAnnotation(Book book);					// ��ȡ�����Ϣ

	public abstract EncodingCollection supportedEncodings();
}
