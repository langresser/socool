package org.socool.android;

import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import org.socool.zlibrary.options.ZLIntegerRangeOption;

import org.socool.socoolreader.reader.R;

import org.socool.screader.screader.FBReaderApp;

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
					FBReaderApp.Instance().setScreenBrightness(progress);
					FBReaderApp.Instance().setScreenBrightnessAuto(false);
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
						FBReaderApp.Instance().setScreenBrightnessAuto(false);
					}
				} else if (v == btnDec) {
					if (value > 1) {
						int nextValue = Math.min(Math.max(value - 5, 1), 100);
						option.setValue(nextValue);
						FBReaderApp.Instance().setScreenBrightness(nextValue);
						FBReaderApp.Instance().setScreenBrightnessAuto(false);
					}
				}
				
				setupNavigation(myWindow);
			}
		};

		btnPlus.setOnClickListener(listener);
		btnDec.setOnClickListener(listener);
				
		final ImageButton btnNeight = (ImageButton)layout.findViewById(R.id.night_button);
		final ImageButton btnAuto = (ImageButton)layout.findViewById(R.id.auto_button);
		
		View.OnClickListener listener1 = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (v == btnNeight) {
					final boolean night = !btnNeight.isSelected();
					btnNeight.setSelected(night);
					FBReaderApp.Instance().isNightModeOption.setValue(night);
					FBReaderApp.Instance().resetWidget();
					FBReaderApp.Instance().repaintWidget();
					
					if (FBReaderApp.Instance().TurnOffMenuLight.getValue().compareTo("night") == 0) {
						FBReaderApp.Instance().getActivity().setButtonLight(!night);
					}
				} else if (v == btnAuto) {
					final boolean auto = !btnAuto.isSelected();
					FBReaderApp.Instance().setScreenBrightnessAuto(auto);

					if (auto) {
						setupNavigation(myWindow, 0, true, btnNeight.isSelected());
					} else {
						setupNavigation(myWindow, FBReaderApp.Instance().ScreenBrightnessLevelOption.getValue(), false, btnNeight.isSelected());
					}
				}
			}
		};
		
		btnNeight.setOnClickListener(listener1);
		btnAuto.setOnClickListener(listener1);

		myWindow.addView(layout);
	}

	private void setupNavigation(PopupWindow panel) {
		int value = FBReaderApp.Instance().ScreenBrightnessLevelOption.getValue();
		boolean auto = FBReaderApp.Instance().ScreenBrightnessAuto.getValue();
		boolean night = FBReaderApp.Instance().isNightModeOption.getValue();

		setupNavigation(panel, value, auto, night);
	}
	
	private void setupNavigation(PopupWindow panel, int brightness, boolean auto, boolean night) {
		if (auto) {
			brightness = 0;
		}

		final SeekBar slider = (SeekBar)panel.findViewById(R.id.bar_light);
		slider.setProgress(brightness);
		
		final ImageButton btnPlus = (ImageButton)panel.findViewById(R.id.btn_light_increase);
		final ImageButton btnDec = (ImageButton)panel.findViewById(R.id.btn_light_decreases);
		btnDec.setEnabled(brightness > 1);
		btnPlus.setEnabled(brightness < 100);
		
		final ImageButton btnNeight = (ImageButton)panel.findViewById(R.id.night_button);
		final ImageButton btnAuto = (ImageButton)panel.findViewById(R.id.auto_button);
		btnNeight.setSelected(night);
		btnAuto.setSelected(auto);
	}
}
