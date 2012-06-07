package org.geometerplus.android.fbreader;

import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;


import org.geometerplus.zlibrary.filesystem.ZLResource;
import org.geometerplus.zlibrary.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.text.ZLTextStyleCollection;
import org.geometerplus.zlibrary.text.ZLTextView;
import org.geometerplus.zlibrary.text.ZLTextWordCursor;

import org.socool.socoolreader.reader.R;

import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class ChangeFontSizePopup extends PopupPanel {
	public final static String ID = "ChangeFontSizePopup";

	private volatile boolean myIsInProgress;

	public ChangeFontSizePopup() {
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

		final View layout = activity.getLayoutInflater().inflate(R.layout.changefontsize, myWindow, false);

		final SeekBar slider = (SeekBar)layout.findViewById(R.id.bar_font_size);
		final TextView text = (TextView)layout.findViewById(R.id.font_size_text);

		slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				myIsInProgress = false;
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				myIsInProgress = true;
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					if (progress >= 0 && progress <= 50) {
						ZLIntegerRangeOption option = ZLTextStyleCollection.Instance().getBaseStyle().FontSizeOption;
						option.setValue(progress + 5);
						
						FBReaderApp.Instance().clearTextCaches();
						FBReaderApp.Instance().repaintWidget();
						setupNavigation(myWindow);
					}
				}
			}
		});
		
		final Button btnPlus = (Button)layout.findViewById(R.id.btn_fontsize_increase);
		final Button btnDec = (Button)layout.findViewById(R.id.btn_fontsize_decreases);
		View.OnClickListener listener = new View.OnClickListener() {
			public void onClick(View v) {
				ZLIntegerRangeOption option = ZLTextStyleCollection.Instance().getBaseStyle().FontSizeOption;
				final int value = option.getValue();
	
				if (v == btnPlus) {
					if (value < 55) {
						option.setValue(value + 1);
						FBReaderApp.Instance().clearTextCaches();
						FBReaderApp.Instance().repaintWidget();
						
						setupNavigation(myWindow);	
					} else {
						btnDec.setEnabled(value > 5);
						btnPlus.setEnabled(value < 55);
					}
				} else if (v == btnDec) {
					if (value > 5) {
						option.setValue(value - 1);
						FBReaderApp.Instance().clearTextCaches();
						FBReaderApp.Instance().repaintWidget();
						
						setupNavigation(myWindow);
					} else {
						btnDec.setEnabled(value > 5);
						btnPlus.setEnabled(value < 55);
					}
				}
			}
		};

		btnPlus.setOnClickListener(listener);
		btnDec.setOnClickListener(listener);

		myWindow.addView(layout);
	}

	private void setupNavigation(PopupWindow panel) {
		final SeekBar slider = (SeekBar)panel.findViewById(R.id.bar_font_size);
		final TextView text = (TextView)panel.findViewById(R.id.font_size_text);

		int value = ZLTextStyleCollection.Instance().getBaseStyle().FontSizeOption.getValue();
		slider.setProgress(Math.max(value - 5, 0));

		text.setText(String.valueOf(value) + " P");
		
		final Button btnPlus = (Button)panel.findViewById(R.id.btn_fontsize_increase);
		final Button btnDec = (Button)panel.findViewById(R.id.btn_fontsize_decreases);
		btnDec.setEnabled(value > 5);
		btnPlus.setEnabled(value < 55);
	}
}
