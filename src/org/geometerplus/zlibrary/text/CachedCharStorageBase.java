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

import java.lang.ref.WeakReference;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;

import android.util.Log;

public class CachedCharStorageBase {
	public final ArrayList<WeakReference<char[]>> myArray =
		new ArrayList<WeakReference<char[]>>();

	private final String myDirectoryName;
	private final String myFileExtension;

	public CachedCharStorageBase(int blockSize, String directoryName, String fileExtension) {
		myDirectoryName = directoryName + '/';
		myFileExtension = '.' + fileExtension;

		myBlockSize = blockSize;
		new File(directoryName).mkdirs();
	}

	protected String fileName(int index) {
		return myDirectoryName + index + myFileExtension;
	}

	public char[] block(int index) {
		char[] block = myArray.get(index).get();
		Log.d("fuck", "block: " + index);
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
		Log.d("fuck", "createNewBlock:" + minimumLength);

		int blockSize = myBlockSize;
		if (minimumLength > blockSize) {
			blockSize = minimumLength;
		}
		char[] block = new char[blockSize];
		myArray.add(new WeakReference<char[]>(block));
		return block;
	}

	public void freezeLastBlock() {
		Log.d("fuck", "freezeLastBlock: " + myArray.size());

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
