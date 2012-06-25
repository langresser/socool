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

import java.util.*;

import org.socool.zlibrary.options.ZLColorOption;
import org.socool.zlibrary.options.ZLIntegerOption;
import org.socool.zlibrary.options.ZLStringOption;
import org.socool.zlibrary.util.ZLColor;

public class ColorProfile {
	public static final String DAY = "day";
	public static final String NIGHT = "night";

	public final ZLStringOption BaseThemeOption;
	public final ZLStringOption WallpaperOption;
	public final ZLColorOption BackgroundOption;
	public final ZLColorOption SelectionBackgroundOption;
	public final ZLColorOption SelectionForegroundOption;
	public final ZLColorOption HighlightingOption;
	public final ZLColorOption RegularTextOption;
	public final ZLColorOption HyperlinkTextOption;
	public final ZLColorOption VisitedHyperlinkTextOption;
	public final ZLColorOption FooterFillOption;

	private static ZLColorOption createOption(String profileName, String optionName, int r, int g, int b) {
		return new ZLColorOption("Colors", profileName + ':' + optionName, new ZLColor(r, g, b));
	}

	public ColorProfile(String name) {
		if (NIGHT.equals(name)) {
			BaseThemeOption = new ZLStringOption("Colors", name + ":Theme", "");
			if (BaseThemeOption.getValue().length() <= 0) {
				TextTheme theme = FBReaderApp.Instance().getThemeByName("wallpapers/2");
				WallpaperOption = new ZLStringOption("Colors", name + ":Wallpaper", "");
				BackgroundOption = createOption(name, "Background", 0, 0, 0);
				SelectionBackgroundOption = createOption(name, "SelectionBackground", 82, 131, 194);
				SelectionForegroundOption = createOption(name, "SelectionForeground", 255, 255, 220);
				RegularTextOption = createOption(name, "Text", 192, 192, 192);
				ChangeTheme(theme);
			} else {
				WallpaperOption = new ZLStringOption("Colors", name + ":Wallpaper", "");
				BackgroundOption = createOption(name, "Background", 0, 0, 0);
				SelectionBackgroundOption = createOption(name, "SelectionBackground", 82, 131, 194);
				SelectionForegroundOption = createOption(name, "SelectionForeground", 255, 255, 220);
				RegularTextOption = createOption(name, "Text", 192, 192, 192);
			}
			
			HighlightingOption = createOption(name, "Highlighting", 96, 96, 128);
			HyperlinkTextOption = createOption(name, "Hyperlink", 60, 142, 224);
			VisitedHyperlinkTextOption = createOption(name, "VisitedHyperlink", 200, 139, 255);
			FooterFillOption = createOption(name, "FooterFillOption", 85, 85, 85);
		} else {
			BaseThemeOption = new ZLStringOption("Colors", name + ":Theme", "");
			if (BaseThemeOption.getValue().length() <= 0) {
				TextTheme theme = FBReaderApp.Instance().getThemeByName("wallpapers/1");
				WallpaperOption = new ZLStringOption("Colors", name + ":Wallpaper", "");
				BackgroundOption = createOption(name, "Background", 255, 255, 255);
				SelectionBackgroundOption = createOption(name, "SelectionBackground", 82, 131, 194);
				SelectionForegroundOption = createOption(name, "SelectionForeground", 255, 255, 220);
				RegularTextOption = createOption(name, "Text", 0, 0, 0);
				ChangeTheme(theme);
			} else {
				WallpaperOption = new ZLStringOption("Colors", name + ":Wallpaper", "wallpapers/1/image.png");
				BackgroundOption = createOption(name, "Background", 255, 255, 255);
				SelectionBackgroundOption = createOption(name, "SelectionBackground", 82, 131, 194);
				SelectionForegroundOption = createOption(name, "SelectionForeground", 255, 255, 220);
				RegularTextOption = createOption(name, "Text", 0, 0, 0);
			}
			
			HighlightingOption = createOption(name, "Highlighting", 255, 192, 128);
			HyperlinkTextOption = createOption(name, "Hyperlink", 60, 139, 255);
			VisitedHyperlinkTextOption = createOption(name, "VisitedHyperlink", 200, 139, 255);
			FooterFillOption = createOption(name, "FooterFillOption", 170, 170, 170);
		}
	}
	
	public void ChangeTheme(TextTheme theme)
	{
		BaseThemeOption.setValue(theme.m_path);
		WallpaperOption.setValue(theme.m_imagePath);
		BackgroundOption.setValue(theme.m_bgColor);
		SelectionBackgroundOption.setValue(theme.m_selectBgColor);
		SelectionForegroundOption.setValue(theme.m_selectTextColor);
		RegularTextOption.setValue(theme.m_textColor);
	}
}
