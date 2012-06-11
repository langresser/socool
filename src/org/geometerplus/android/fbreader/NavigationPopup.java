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

package org.geometerplus.android.fbreader;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


import org.geometerplus.zlibrary.filesystem.ZLResource;
import org.geometerplus.zlibrary.text.ZLTextView;
import org.geometerplus.zlibrary.text.ZLTextWordCursor;

import org.socool.socoolreader.reader.R;

import org.geometerplus.fbreader.bookmodel.BookChapter;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class NavigationPopup extends PopupPanel {
	public final static String ID = "NavigationPopup";

	private volatile boolean myIsInProgress;

	public NavigationPopup() {
		super();
	}

	public void run() {
		if (myWindow == null || myWindow.getVisibility() == View.GONE) {
			myIsInProgress = false;
			initPosition();
			FBReaderApp.Instance().showPopup(ID);
		}
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public void show_() {
		super.show_();
		if (myWindow != null) {
			setupNavigation(myWindow);
		}
	}

	@Override
	public void update() {
		if (!myIsInProgress && myWindow != null) {
			setupNavigation(myWindow);
		}
	}

	@Override
	public void createControlPanel(SCReaderActivity activity, RelativeLayout root) {
		if (myWindow != null && activity == myWindow.getActivity()) {
			return;
		}

		myWindow = new PopupWindow(activity, root, PopupWindow.Location.Bottom, true);

		final View layout = activity.getLayoutInflater().inflate(R.layout.navigate, myWindow, false);

		final SeekBar slider = (SeekBar)layout.findViewById(R.id.book_position_slider);

		slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				myIsInProgress = false;
				int progress = seekBar.getProgress();
				if (progress < 0) {
					progress = 0;
				}
				
				if (progress > 10000) {
					progress = 10000;
				}
				
				FBReaderApp.Instance().BookTextView.gotoPercent(progress);
				setupNavigation(myWindow);

				FBReaderApp.Instance().resetWidget();
				FBReaderApp.Instance().repaintWidget();
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				myIsInProgress = true;
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					if (progress < 0) {
						progress = 0;
					}
					
					if (progress > 10000) {
						progress = 10000;
					}
					
					final SeekBar slider = (SeekBar)myWindow.findViewById(R.id.book_position_slider);
					final TextView text = (TextView)myWindow.findViewById(R.id.book_position_text);

					final BookChapter chapter = FBReaderApp.Instance().Model.m_chapter;
					final int percent = progress;
					final int txtOffset = (int)(chapter.m_allTextSize * (percent / 10000.0));
					final int chapterIndex = chapter.getChapterByTxtOffset(txtOffset);
					final String title = FBReaderApp.Instance().Model.m_chapter.getChapterTitle(chapterIndex);
					slider.setProgress(percent);
					text.setText(String.format("%1$.2f%%", percent / 100.0) + "  " + title);
					Log.d("progress change", String.format("%1d %2d %3d %4s", progress, txtOffset, chapterIndex, title));
					
					final ImageButton btnLast = (ImageButton)myWindow.findViewById(R.id.btn_back);
					final ImageButton btnNext = (ImageButton)myWindow.findViewById(R.id.btn_forward);
					
					btnLast.setEnabled(percent > 0);
					btnNext.setEnabled(percent < 10000);
				}
			}
		});

		final Button btnOk = (Button)layout.findViewById(android.R.id.button1);
		final Button btnCancel = (Button)layout.findViewById(android.R.id.button3);
		View.OnClickListener listener = new View.OnClickListener() {
			public void onClick(View v) {
				final ZLTextWordCursor position = StartPosition;
				if (v == btnCancel && position != null) {
					FBReaderApp.Instance().getCurrentView().gotoPosition(position.getParagraphIndex(), position.getElementIndex(), position.getCharIndex());
				} else if (v == btnOk) {
					storePosition();
				}
				StartPosition = null;
				FBReaderApp.Instance().hideActivePopup();
				FBReaderApp.Instance().resetWidget();
				FBReaderApp.Instance().repaintWidget();
			}
		};
		btnOk.setOnClickListener(listener);
		btnCancel.setOnClickListener(listener);
		final ZLResource buttonResource = ZLResource.resource("dialog").getResource("button");
		btnOk.setText(buttonResource.getResource("ok").getValue());
		btnCancel.setText(buttonResource.getResource("cancel").getValue());
		

		final ImageButton btnLastChapter = (ImageButton)layout.findViewById(R.id.btn_back_chapter);
		final ImageButton btnNextChapter = (ImageButton)layout.findViewById(R.id.btn_forward_chapter);
		final ImageButton btnLast = (ImageButton)layout.findViewById(R.id.btn_back);
		final ImageButton btnNext = (ImageButton)layout.findViewById(R.id.btn_forward);
		
		View.OnClickListener listenerJump = new View.OnClickListener() {
			public void onClick(View v) {
				final ZLTextView textView = FBReaderApp.Instance().BookTextView;
				if (v == btnLastChapter) {
					final int currentChapter = textView.getCurrentChapter();
					textView.gotoChapter(currentChapter - 1);
				} else if (v == btnNextChapter) {
					final int currentChapter = textView.getCurrentChapter();
					textView.gotoChapter(currentChapter + 1);
				} else if (v == btnLast) {
					int percent = textView.getCurrentPercent();
					percent -= 1;
					textView.gotoPercent(percent);
				} else if (v == btnNext) {
					int percent = textView.getCurrentPercent();
					percent += 1;
					textView.gotoPercent(percent);
				}

				setupNavigation(myWindow);
				FBReaderApp.Instance().resetWidget();
				FBReaderApp.Instance().repaintWidget();
			}
		};
		
		btnLastChapter.setOnClickListener(listenerJump);
		btnNextChapter.setOnClickListener(listenerJump);
		btnLast.setOnClickListener(listenerJump);
		btnNext.setOnClickListener(listenerJump);
		
		
		myWindow.addView(layout);
	}

	private void setupNavigation(PopupWindow panel) {
		final SeekBar slider = (SeekBar)panel.findViewById(R.id.book_position_slider);
		final TextView text = (TextView)panel.findViewById(R.id.book_position_text);

		final ZLTextView textView = FBReaderApp.Instance().BookTextView;
		final int percent = (int)textView.getCurrentPercent();
		final int chapterIndex = textView.getCurrentChapter();
		final String title = FBReaderApp.Instance().Model.m_chapter.getChapterTitle(chapterIndex);
		slider.setProgress(percent);
		text.setText(String.format("%1$.2f%%", percent / 100.0) + "  " + title);
		
		Log.d("setupNavigation", String.format("%1d %2d %3s", percent, chapterIndex, title));

		final ImageButton btnLast = (ImageButton)panel.findViewById(R.id.btn_back);
		final ImageButton btnNext = (ImageButton)panel.findViewById(R.id.btn_forward);
		
		btnLast.setEnabled(percent > 0);
		btnNext.setEnabled(percent < 10000);
	}
}
