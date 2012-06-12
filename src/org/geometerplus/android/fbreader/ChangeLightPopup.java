package org.geometerplus.android.fbreader;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.geometerplus.zlibrary.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.text.ZLTextStyleCollection;

import org.socool.socoolreader.reader.R;

import org.geometerplus.fbreader.fbreader.ColorProfile;
import org.geometerplus.fbreader.fbreader.FBReaderApp;

public class ChangeLightPopup extends PopupPanel {
	public final static String ID = "ChangeLightPopup";

	private volatile boolean myIsInProgress;

	public ChangeLightPopup() {
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

		final View layout = activity.getLayoutInflater().inflate(R.layout.changelight, myWindow, false);

		final SeekBar slider = (SeekBar)layout.findViewById(R.id.bar_light);

		slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				myIsInProgress = false;
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
				myIsInProgress = true;
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					progress = Math.min(Math.max(progress, 1), 100);

					ZLIntegerRangeOption option = FBReaderApp.Instance().ScreenBrightnessLevelOption;
					option.setValue(progress);
					FBReaderApp.Instance().setScreenBrightness(progress);

					setupNavigation(myWindow);
				}
			}
		});
		
		final ImageButton btnPlus = (ImageButton)layout.findViewById(R.id.btn_light_increase);
		final ImageButton btnDec = (ImageButton)layout.findViewById(R.id.btn_light_decreases);
		View.OnClickListener listener = new View.OnClickListener() {
			public void onClick(View v) {
				ZLIntegerRangeOption option = FBReaderApp.Instance().ScreenBrightnessLevelOption;
				final int value = option.getValue();
	
				if (v == btnPlus) {
					if (value < 100) {
						int nextValue = Math.min(Math.max(value + 5, 1), 100);
						option.setValue(nextValue);
						FBReaderApp.Instance().setScreenBrightness(nextValue);
						setupNavigation(myWindow);	
					} else {
						btnDec.setEnabled(value > 1);
						btnPlus.setEnabled(value < 100);
					}
				} else if (v == btnDec) {
					if (value > 1) {
						int nextValue = Math.min(Math.max(value - 5, 1), 100);
						option.setValue(nextValue);
						FBReaderApp.Instance().setScreenBrightness(nextValue);
						
						setupNavigation(myWindow);
					} else {
						btnDec.setEnabled(value > 1);
						btnPlus.setEnabled(value < 100);
					}
				}
			}
		};

		btnPlus.setOnClickListener(listener);
		btnDec.setOnClickListener(listener);
		
		View.OnTouchListener touchListener = new View.OnTouchListener() {
			@Override 
			public boolean onTouch(View v, MotionEvent event) { 
				if(event.getAction() == MotionEvent.ACTION_DOWN){
					if (v instanceof ImageButton) {
						ImageButton btn = (ImageButton)v;
						btn.getDrawable().setAlpha(127);
						btn.invalidate();
					}
				} else if (event.getAction() == MotionEvent.ACTION_UP) {
					if (v instanceof ImageButton) {
						ImageButton btn = (ImageButton)v;
						btn.getDrawable().setAlpha(255);
						btn.invalidate();
					}
				}
				
				return false;
			}
		};
		
		btnPlus.setOnTouchListener(touchListener);
		btnDec.setOnTouchListener(touchListener);
		
		final ImageButton btnNeight = (ImageButton)layout.findViewById(R.id.night_button);
		btnNeight.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final boolean night = !btnNeight.isSelected();
				btnNeight.setSelected(night);
				FBReaderApp.Instance().isNightModeOption.setValue(night);
				FBReaderApp.Instance().resetWidget();
				FBReaderApp.Instance().repaintWidget();
			}
		});

		myWindow.addView(layout);
	}

	private void setupNavigation(PopupWindow panel) {
		final SeekBar slider = (SeekBar)panel.findViewById(R.id.bar_light);

		int value = FBReaderApp.Instance().ScreenBrightnessLevelOption.getValue();
		slider.setProgress(value);
		
		final ImageButton btnPlus = (ImageButton)panel.findViewById(R.id.btn_light_increase);
		final ImageButton btnDec = (ImageButton)panel.findViewById(R.id.btn_light_decreases);
		btnDec.setEnabled(value > 1);
		btnPlus.setEnabled(value < 100);
		
		final ImageButton btnNeight = (ImageButton)panel.findViewById(R.id.night_button);
		
		btnNeight.setSelected(FBReaderApp.Instance().isNightModeOption.getValue() == true);
	}
}
