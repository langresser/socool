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

package org.socool.android.tips;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.*;

import org.socool.screader.screader.FBReaderApp;
import org.socool.socoolreader.mcnxs.R;

public class TipsActivity extends Activity {
	public static final String INITIALIZE_ACTION = "android.action.tips.INITIALIZE";
	public static final String SHOW_TIP_ACTION = "android.action.tips.SHOW_TIP";

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		setContentView(R.layout.tip);

		setTitle("Banana Studio");
		setTitleColor(0xff000000);
		
		final CheckBox checkBox = (CheckBox)findViewById(R.id.tip_checkbox);

		final Button yesButton = (Button)findViewById(R.id.ok_button);
		yesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FBReaderApp.Instance().EnableTipOption.setValue(!checkBox.isChecked());
				finish();
			}
		});
		
//		final Button nextButton = (Button)findViewById(R.id.next_button);
//		nextButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//			}
//		});

		final Button appButton = (Button)findViewById(R.id.app_button);
		appButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FBReaderApp.Instance().showOfferWall(TipsActivity.this);
				finish();
			}
		});
		
		showText(m_tips);
		FBReaderApp.Instance().m_hasShowTip = true;
	}

	private void showText(CharSequence text) {
		final TextView textView = (TextView)findViewById(R.id.tip_text);
		textView.setText(text);
		textView.setMovementMethod(LinkMovementMethod.getInstance());
	}
	
	private static final String m_tips = 
		"    点击屏幕中部可以显示系统菜单，通过设置选项可以进行更多的个性化设置。\n\n"+
		"    如果您喜欢本应用，可以通过下载或打开精品推荐中您所喜欢的应用的方式来支持我们。有您的支持，我们可以为您提供出更多更好的精品电子书。"; 
}
