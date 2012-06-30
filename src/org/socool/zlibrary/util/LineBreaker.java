package org.socool.zlibrary.util;

import org.socool.screader.screader.FBReaderApp;

import android.util.Log;

public final class LineBreaker {
	public static final byte MUSTBREAK = 0;
	public static final byte ALLOWBREAK = 1;
	public static final byte NOBREAK = 2;
	public static final byte INSIDEACHAR = 3;

	public static boolean isAsciiLetter(char c)
	{
		if (c >= 'a' && c <= 'z' ||
			c >= 'A' && c <= 'Z' ||
			c >= '0' && c <= '9') {
			return true;
		} else {
			return false;
		}
	}
	public static void setLineBreaks(char[] data, int offset, int length, byte[] breaks) {
		final boolean linebreak = FBReaderApp.Instance().AutoLineBreakOption.getValue();
		if (linebreak) {
			for (int i = offset; i < offset + length - 1; ++i) {
				breaks[i] = ALLOWBREAK;
			}
			return;
		}

		for (int i = offset; i < offset + length - 1; ++i) {
			breaks[i] = ALLOWBREAK;

			final char c = data[i];
			if (isAsciiLetter(c)) {
				final char cn = data[i + 1];
				if (isAsciiLetter(cn)) {
					breaks[i] = NOBREAK;
					continue;
				}
			}
		}
	}
}
