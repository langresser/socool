/*
 * Copyright (C) 2009-2012 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.android.fbreader.preferences;

import android.content.Intent;
import android.view.KeyEvent;


import org.geometerplus.zlibrary.options.ZLIntegerOption;
import org.geometerplus.zlibrary.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.text.ZLTextBaseStyle;
import org.geometerplus.zlibrary.text.ZLTextFullStyleDecoration;
import org.geometerplus.zlibrary.text.ZLTextStyleCollection;
import org.geometerplus.zlibrary.text.ZLTextStyleDecoration;
import org.geometerplus.zlibrary.view.AndroidFontUtil;
import org.geometerplus.zlibrary.view.ZLPaintContext;

import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.fbreader.*;
import org.geometerplus.fbreader.Paths;

import org.geometerplus.android.fbreader.SCReaderActivity;
import org.geometerplus.android.fbreader.DictionaryUtil;

public class PreferenceActivity extends ZLPreferenceActivity {
	public PreferenceActivity() {
		super("Preferences");
	}

	@Override
	protected void init(Intent intent) {
		setResult(SCReaderActivity.RESULT_REPAINT);

		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final ColorProfile profile = fbReader.getColorProfile();

		final Screen directoriesScreen = createPreferenceScreen("directories");
		directoriesScreen.addOption(Paths.BooksDirectoryOption(), "books");
		if (AndroidFontUtil.areExternalFontsSupported()) {
			directoriesScreen.addOption(Paths.FontsDirectoryOption(), "fonts");
		}
		directoriesScreen.addOption(Paths.WallpapersDirectoryOption(), "wallpapers");

		final Screen appearanceScreen = createPreferenceScreen("appearance");
		appearanceScreen.addPreference(new BatteryLevelToTurnScreenOffPreference(
			this,
			fbReader.BatteryLevelToTurnScreenOffOption,
			appearanceScreen.Resource,
			"dontTurnScreenOff"
		));

		final Screen textScreen = createPreferenceScreen("text");

		final ZLTextStyleCollection collection = ZLTextStyleCollection.Instance();
		final ZLTextBaseStyle baseStyle = collection.getBaseStyle();
		textScreen.addPreference(new FontOption(
			this, textScreen.Resource, "font",
			baseStyle.FontFamilyOption, false
		));
		textScreen.addPreference(new ZLIntegerRangePreference(
			this, textScreen.Resource.getResource("fontSize"),
			baseStyle.FontSizeOption
		));
		textScreen.addPreference(new FontStylePreference(
			this, textScreen.Resource, "fontStyle",
			baseStyle.BoldOption, baseStyle.ItalicOption
		));
		final ZLIntegerRangeOption spaceOption = baseStyle.LineSpaceOption;
		final String[] spacings = new String[spaceOption.MaxValue - spaceOption.MinValue + 1];
		for (int i = 0; i < spacings.length; ++i) {
			final int val = spaceOption.MinValue + i;
			spacings[i] = (char)(val / 10 + '0') + "." + (char)(val % 10 + '0');
		}
		textScreen.addPreference(new ZLChoicePreference(
			this, textScreen.Resource, "lineSpacing",
			spaceOption, spacings
		));
		final String[] alignments = { "left", "right", "center", "justify" };
		textScreen.addPreference(new ZLChoicePreference(
			this, textScreen.Resource, "alignment",
			baseStyle.AlignmentOption, alignments
		));
		textScreen.addOption(baseStyle.AutoHyphenationOption, "autoHyphenations");

		final ZLPreferenceSet bgPreferences = new ZLPreferenceSet();

		final Screen colorsScreen = createPreferenceScreen("colors");
		colorsScreen.addPreference(new WallpaperPreference(
			this, profile, colorsScreen.Resource, "background"
		) {
			@Override
			protected void onDialogClosed(boolean result) {
				super.onDialogClosed(result);
				bgPreferences.setEnabled("".equals(getValue()));
			}
		});
		bgPreferences.add(
			colorsScreen.addOption(profile.BackgroundOption, "backgroundColor")
		);
		bgPreferences.setEnabled("".equals(profile.WallpaperOption.getValue()));

		colorsScreen.addOption(profile.HighlightingOption, "highlighting");
		colorsScreen.addOption(profile.RegularTextOption, "text");
		colorsScreen.addOption(profile.HyperlinkTextOption, "hyperlink");
		colorsScreen.addOption(profile.VisitedHyperlinkTextOption, "hyperlinkVisited");
		colorsScreen.addOption(profile.FooterFillOption, "footer");
		colorsScreen.addOption(profile.SelectionBackgroundOption, "selectionBackground");
		colorsScreen.addOption(profile.SelectionForegroundOption, "selectionForeground");

		final Screen marginsScreen = createPreferenceScreen("margins");
		marginsScreen.addPreference(new ZLIntegerRangePreference(
			this, marginsScreen.Resource.getResource("left"),
			fbReader.LeftMarginOption
		));
		marginsScreen.addPreference(new ZLIntegerRangePreference(
			this, marginsScreen.Resource.getResource("right"),
			fbReader.RightMarginOption
		));
		marginsScreen.addPreference(new ZLIntegerRangePreference(
			this, marginsScreen.Resource.getResource("top"),
			fbReader.TopMarginOption
		));
		marginsScreen.addPreference(new ZLIntegerRangePreference(
			this, marginsScreen.Resource.getResource("bottom"),
			fbReader.BottomMarginOption
		));

		final ScrollingPreferences scrollingPreferences = ScrollingPreferences.Instance();

		final Screen scrollingScreen = createPreferenceScreen("scrolling");

		scrollingScreen.addOption(scrollingPreferences.AnimationOption, "animation");
		scrollingScreen.addPreference(new AnimationSpeedPreference(
			this,
			scrollingScreen.Resource,
			"animationSpeed",
			scrollingPreferences.AnimationSpeedOption
		));
	}
	
	void initFormat(Screen textScreen)
	{
		final Screen moreStylesScreen = textScreen.createPreferenceScreen("more");
		final ZLTextStyleCollection collection = ZLTextStyleCollection.Instance();

		byte styles[] = {
			BookModel.REGULAR,
			BookModel.TITLE,
			BookModel.SECTION_TITLE,
			BookModel.SUBTITLE,
			BookModel.H1,
			BookModel.H2,
			BookModel.H3,
			BookModel.H4,
			BookModel.H5,
			BookModel.H6,
			BookModel.ANNOTATION,
			BookModel.EPIGRAPH,
			BookModel.AUTHOR,
			BookModel.POEM_TITLE,
			BookModel.STANZA,
			BookModel.VERSE,
			BookModel.CITE,
			BookModel.INTERNAL_HYPERLINK,
			BookModel.EXTERNAL_HYPERLINK,
			BookModel.FOOTNOTE,
			BookModel.ITALIC,
			BookModel.EMPHASIS,
			BookModel.BOLD,
			BookModel.STRONG,
			BookModel.DEFINITION,
			BookModel.DEFINITION_DESCRIPTION,
			BookModel.PREFORMATTED,
			BookModel.CODE
		};
		for (int i = 0; i < styles.length; ++i) {
			final ZLTextStyleDecoration decoration = collection.getDecoration(styles[i]);
			if (decoration == null) {
				continue;
			}
			ZLTextFullStyleDecoration fullDecoration =
				decoration instanceof ZLTextFullStyleDecoration ?
					(ZLTextFullStyleDecoration)decoration : null;

			final Screen formatScreen = moreStylesScreen.createPreferenceScreen(decoration.getName());
			formatScreen.addPreference(new FontOption(
				this, textScreen.Resource, "font",
				decoration.FontFamilyOption, true
			));
			formatScreen.addPreference(new ZLIntegerRangePreference(
				this, textScreen.Resource.getResource("fontSizeDifference"),
				decoration.FontSizeDeltaOption
			));
			formatScreen.addPreference(new ZLBoolean3Preference(
				this, textScreen.Resource, "bold",
				decoration.BoldOption
			));
			formatScreen.addPreference(new ZLBoolean3Preference(
				this, textScreen.Resource, "italic",
				decoration.ItalicOption
			));
			formatScreen.addPreference(new ZLBoolean3Preference(
				this, textScreen.Resource, "underlined",
				decoration.UnderlineOption
			));
			formatScreen.addPreference(new ZLBoolean3Preference(
				this, textScreen.Resource, "strikedThrough",
				decoration.StrikeThroughOption
			));
			if (fullDecoration != null) {
				final String[] allAlignments = { "unchanged", "left", "right", "center", "justify" };
				formatScreen.addPreference(new ZLChoicePreference(
					this, textScreen.Resource, "alignment",
					fullDecoration.AlignmentOption, allAlignments
				));
			}
			formatScreen.addPreference(new ZLBoolean3Preference(
				this, textScreen.Resource, "allowHyphenations",
				decoration.AllowHyphenationsOption
			));
			if (fullDecoration != null) {
				formatScreen.addPreference(new ZLIntegerRangePreference(
					this, textScreen.Resource.getResource("spaceBefore"),
					fullDecoration.SpaceBeforeOption
				));
				formatScreen.addPreference(new ZLIntegerRangePreference(
					this, textScreen.Resource.getResource("spaceAfter"),
					fullDecoration.SpaceAfterOption
				));
				formatScreen.addPreference(new ZLIntegerRangePreference(
					this, textScreen.Resource.getResource("leftIndent"),
					fullDecoration.LeftIndentOption
				));
				formatScreen.addPreference(new ZLIntegerRangePreference(
					this, textScreen.Resource.getResource("rightIndent"),
					fullDecoration.RightIndentOption
				));
				formatScreen.addPreference(new ZLIntegerRangePreference(
					this, textScreen.Resource.getResource("firstLineIndent"),
					fullDecoration.FirstLineIndentDeltaOption
				));
				final ZLIntegerOption spacePercentOption = fullDecoration.LineSpacePercentOption;
				final int[] spacingValues = new int[17];
				final String[] spacingKeys = new String[17];
				spacingValues[0] = -1;
				spacingKeys[0] = "unchanged";
				for (int j = 1; j < spacingValues.length; ++j) {
					final int val = 4 + j;
					spacingValues[j] = 10 * val;
					spacingKeys[j] = (char)(val / 10 + '0') + "." + (char)(val % 10 + '0');
				}
				formatScreen.addPreference(new ZLIntegerChoicePreference(
					this, textScreen.Resource, "lineSpacing",
					spacePercentOption, spacingValues, spacingKeys
				));
			}
		}

	}
}
