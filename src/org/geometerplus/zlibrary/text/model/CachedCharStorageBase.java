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

import java.lang.ref.WeakReference;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

public class CachedCharStorageBase {
	protected final ArrayList<WeakReference<char[]>> myArray =
		new ArrayList<WeakReference<char[]>>();

	private final String myDirectoryName;
	private final String myFileExtension;
	private final boolean m_isReadOnly;

	public CachedCharStorageBase(int blockSize, String directoryName, String fileExtension, boolean readonly) {
		myDirectoryName = directoryName + '/';
		myFileExtension = '.' + fileExtension;
		m_isReadOnly = readonly;

		if (!readonly) {
			myBlockSize = blockSize;
			new File(directoryName).mkdirs();
		} else {
			myBlockSize = blockSize;
			myArray.addAll(Collections.nCopies(blockSize, new WeakReference<char[]>(null)));
		}
	}

	protected String fileName(int index) {
		return myDirectoryName + index + myFileExtension;
	}

	public int size() {
		return myArray.size();
	}

	public char[] block(int index) {
		char[] block = myArray.get(index).get();
		if (block == null) {
			try {
				File file = new File(fileName(index));
				int size = (int)file.length();
				if (size < 0) {
					return null;
				}
				block = new char[size / 2];
				InputStreamReader reader =
					new InputStreamReader(
						new FileInputStream(file),
						"UTF-16LE"
					);
				if (reader.read(block) != block.length) {
					return null;
				}
				reader.close();
			} catch (FileNotFoundException e) {
				return null;
			} catch (IOException e) {
				return null;
			}
			myArray.set(index, new WeakReference<char[]>(block));
		}
		return block;
	}
	
	private final int myBlockSize;

	public char[] createNewBlock(int minimumLength) {
		if (m_isReadOnly) {
			return null;
		}

		int blockSize = myBlockSize;
		if (minimumLength > blockSize) {
			blockSize = minimumLength;
		}
		char[] block = new char[blockSize];
		myArray.add(new WeakReference<char[]>(block));
		return block;
	}

	public void freezeLastBlock() {
		if (m_isReadOnly) {
			return;
		}

		int index = myArray.size() - 1;
		if (index >= 0) {
			char[] block = myArray.get(index).get();
			if (block == null) {
				return;
			}
			try {
				final OutputStreamWriter writer =
					new OutputStreamWriter(
						new FileOutputStream(fileName(index)),
						"UTF-16LE"
					);
				writer.write(block);
				writer.close();
			} catch (IOException e) {
				return;
			}
		}
	}
}
