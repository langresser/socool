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

package org.socool.android;

import java.lang.reflect.Field;
import java.util.*;

import android.app.SearchManager;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import org.socool.zlibrary.filesystem.ZLFile;

import org.socool.zlibrary.text.ZLTextView;
import org.socool.zlibrary.view.ZLGLWidget;
import org.socool.zlibrary.view.ZLViewWidget;

import org.socool.socoolreader.mcnxs.R;

import org.socool.screader.screader.ActionCode;
import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.library.Book;
import org.socool.android.tips.TipsManager;

import org.socool.android.action.*;
import org.socool.android.tips.TipsActivity;

import org.socool.android.util.UIUtil;

import com.guohead.sdk.GHView;
import com.guohead.sdk.GHView.OnAdClosedListener;
import com.guohead.sdk.GHView.OnAdLoadedListener;
import com.umeng.analytics.MobclickAgent;

import android.app.Activity;

public final class SCReaderActivity extends Activity {
	public static final String BOOK_PATH_KEY = "BookPath";

	public static final int REQUEST_PREFERENCES = 1;
	public static final int REQUEST_BOOK_INFO = 2;
	public static final int REQUEST_CANCEL_MENU = 3;

	public static final int RESULT_DO_NOTHING = RESULT_FIRST_USER;
	public static final int RESULT_REPAINT = RESULT_FIRST_USER + 1;
	public static final int RESULT_RELOAD_BOOK = RESULT_FIRST_USER + 2;

	protected ZLFile fileFromIntent(Intent intent) {
		String filePath = intent.getStringExtra(BOOK_PATH_KEY);
		if (filePath == null) {
			final Uri data = intent.getData();
			if (data != null) {
				filePath = data.getPath();
			}
		}
		return filePath != null ? ZLFile.createFileByPath(filePath) : null;
	}

	protected Runnable getPostponedInitAction() {
		return new Runnable() {
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						new TipRunner().start();
						DictionaryUtil.init(SCReaderActivity.this);
					}
				});
			}
		};
	}

	public ZLGLWidget m_bookViewGL;
	public ZLViewWidget m_bookView;
	public RelativeLayout m_mainLayout;
	public GHView m_adsView = null;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		final FBReaderApp fbReader = FBReaderApp.Instance();
		fbReader.setActivity(this);
		
		m_mainLayout = new RelativeLayout(this);
		m_mainLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		createBookView();

		setContentView(m_mainLayout);
		
		createBannerAds();

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

//		fbReader.addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_PREFERENCES, new ShowPreferencesAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOK_INFO, new ShowBookInfoAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOKMARKS, new ShowBookmarksAction(this, fbReader));
		
		fbReader.addAction(ActionCode.SHOW_MENU, new ShowMenuAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NAVIGATION, new ShowNavigationAction(this, fbReader));
//		fbReader.addAction(ActionCode.SEARCH, new SearchAction(this, fbReader));

		fbReader.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(this, fbReader));
//		fbReader.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_TRANSLATE, new SelectionTranslateAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_BOOKMARK, new SelectionBookmarkAction(this, fbReader));
		fbReader.addAction(ActionCode.ADD_BOOKMARK, new AddBookmarkAction(this, fbReader));

		fbReader.addAction(ActionCode.PROCESS_HYPERLINK, new ProcessHyperlinkAction(this, fbReader));

		fbReader.addAction(ActionCode.SHOW_FONT, new ShowFontAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_LIGHT, new ShowLightAction(this, fbReader));

		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, new SetOrientationAction(this, fbReader, FBReaderApp.SCREEN_ORIENTATION_PORTRAIT));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, new SetOrientationAction(this, fbReader, FBReaderApp.SCREEN_ORIENTATION_LANDSCAPE));
	
		FBReaderApp.Instance().openFile(fileFromIntent(getIntent()), null);
	}
	
	private boolean m_hasCloseAds = false;

	// 创建广告条
	final public void createBannerAds()
	{
		if (FBReaderApp.Instance().EnableAdsOption.getValue() == false) {
			return;
		}

		FBReaderApp.Instance().m_adsHeight = 0;
		
		
		m_adsView =new GHView(this); 
		//您可以根据布局需求，对布局参数params设定具体的值 
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT) ;
		params.topMargin = 0;
		params.gravity = Gravity.TOP;
		addContentView(m_adsView, params);
		m_adsView.setAdUnitId("6fc147213f9cd78e216e8e4ecfaf5352");
		m_adsView.startLoadAd();

		m_adsView.setOnAdLoadedListener(new OnAdLoadedListener() {
			
			@Override
			public void OnAdLoaded(GHView arg0) {
				if (m_hasCloseAds || arg0 == null) {
					return;
				}

				final FBReaderApp fbReader = FBReaderApp.Instance();
				
				if (fbReader.m_adsHeight == 0) {
					final int height = (int)(arg0.getAdHeight() * fbReader.getDensity());
					fbReader.m_adsHeight = height;
					fbReader.resetWidget();
					fbReader.repaintWidget(true);
				}
			}
		});
		m_adsView.setOnAdClosedListener(new OnAdClosedListener() {
			
			@Override
			public void OnAdClosed(GHView arg0) {
				if (!m_hasCloseAds) {
					MobclickAgent.onEvent(SCReaderActivity.this, "closeAds");
					FBReaderApp.Instance().m_adsHeight = 0;
					FBReaderApp.Instance().resetWidget();
					FBReaderApp.Instance().repaintWidget(true);
				}
				
				m_hasCloseAds = true;
			}
		});
	}
	
	// 清理广告条资源
	final public void clearAds()
	{
		if (m_adsView != null) {
			m_adsView.destroy();
			m_adsView = null;
		}
	}
	
	// 创建书籍view
	public void createBookView()
	{
		if (FBReaderApp.Instance().isUseGLView()) {
			if (m_bookViewGL == null) {
				m_bookViewGL = new ZLGLWidget(this);
				m_bookViewGL.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
				m_bookViewGL.setFocusable(true);
				m_mainLayout.addView(m_bookViewGL);
			}
			
			if (m_bookView != null) {
				m_mainLayout.removeView(m_bookView);
				m_bookView = null;
			}
		} else {
			if (m_bookView == null) {
				m_bookView = new ZLViewWidget(this);
				m_bookView.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
				m_bookView.setFocusable(true);
				//m_bookView.setScrollBarStyle();
				//android:scrollbars="vertical"
				//android:scrollbarAlwaysDrawVerticalTrack="true"
				m_mainLayout.addView(m_bookView);
			}
			
			if (m_bookViewGL != null) {
				m_mainLayout.removeView(m_bookViewGL);
				m_bookViewGL = null;
			}
		}
	}
	
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		clearAds();
	}

 	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
 		// 弹出菜单时不全屏
		if (!FBReaderApp.Instance().isKindleFire()) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		
		if (FBReaderApp.Instance().getActivePopup() != null) {
			FBReaderApp.Instance().hideActivePopup();
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
		
		if (!FBReaderApp.Instance().isKindleFire()) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (!FBReaderApp.Instance().isKindleFire()) {
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		final Uri data = intent.getData();
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
			super.onNewIntent(intent);
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action) || "android.action.VIEW".equals(action)) {
				FBReaderApp.Instance().openFile(fileFromIntent(intent), null);
			}
		} else if (Intent.ACTION_VIEW.equals(intent.getAction())
					&& data != null && "fbreader-action".equals(data.getScheme())) {
			fbReader.runAction(data.getEncodedSchemeSpecificPart(), data.getFragment());
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			final String pattern = intent.getStringExtra(SearchManager.QUERY);
			final Runnable runnable = new Runnable() {
				public void run() {
					final TextSearchPopup popup = (TextSearchPopup)fbReader.getPopupById(TextSearchPopup.ID);
					popup.initPosition();
					fbReader.TextSearchPatternOption.setValue(pattern);
					if (fbReader.getCurrentView().search(pattern, true, false, false, false) != 0) {
						runOnUiThread(new Runnable() {
							public void run() {
								fbReader.showPopup(popup.getId());
							}
						});
					} else {
						runOnUiThread(new Runnable() {
							public void run() {
								UIUtil.showErrorMessage(SCReaderActivity.this, "textNotFound");
								popup.StartPosition = null;
							}
						});
					}
				}
			};
			UIUtil.wait("search", runnable, this);
		} else {
			super.onNewIntent(intent);
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action) || "android.action.VIEW".equals(action)) {
				FBReaderApp.Instance().openFile(fileFromIntent(intent), null);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		SetOrientationAction.setOrientation(this, FBReaderApp.Instance().OrientationOption.getValue());
		((PopupPanel)FBReaderApp.Instance().getPopupById(TextSearchPopup.ID)).setPanelInfo(this, m_mainLayout);
		((PopupPanel)FBReaderApp.Instance().getPopupById(NavigationPopup.ID)).setPanelInfo(this, m_mainLayout);
		((PopupPanel)FBReaderApp.Instance().getPopupById(SelectionPopup.ID)).setPanelInfo(this, m_mainLayout);
		((PopupPanel)FBReaderApp.Instance().getPopupById(ChangeFontSizePopup.ID)).setPanelInfo(this, m_mainLayout);
		((PopupPanel)FBReaderApp.Instance().getPopupById(ChangeLightPopup.ID)).setPanelInfo(this, m_mainLayout);
	}

	private class TipRunner extends Thread {
		TipRunner() {
			setPriority(MIN_PRIORITY);
		}

		public void run() {
			final TipsManager manager = TipsManager.Instance();
			switch (manager.requiredAction()) {
				case Initialize:
					startActivity(new Intent(
						TipsActivity.INITIALIZE_ACTION, null, SCReaderActivity.this, TipsActivity.class
					));
					break;
				case Show:
					startActivity(new Intent(
						TipsActivity.SHOW_TIP_ACTION, null, SCReaderActivity.this, TipsActivity.class
					));
					break;
				case None:
					break;
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		final FBReaderApp fbReader = FBReaderApp.Instance();
		switchWakeLock(
				fbReader.BatteryLevelToTurnScreenOffOption.getValue() <
				fbReader.getBatteryLevel()
		);
		myStartTimer = true;
		setScreenBrightnessAuto(FBReaderApp.Instance().ScreenBrightnessAuto.getValue());
		
		registerReceiver(myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		PopupPanel.restoreVisibilities();
		
		final String light = FBReaderApp.Instance().TurnOffMenuLight.getValue();
		if (light.compareTo("all") == 0) {
			setButtonLight(false);
		} else if (light.compareTo("night") == 0) {
			final boolean isNight = fbReader.isNightModeOption.getValue();
			if (isNight) {
				setButtonLight(false);
			}
		}
		
		final boolean enableAds = fbReader.EnableAdsOption.getValue();
		if (!enableAds && m_adsView != null) {
			m_adsView.setVisibility(View.GONE);
			clearAds();
		}
		
		MobclickAgent.onResume(this);
	}

	@Override
	public void onStop() {
		PopupPanel.removeAllWindows(this);
		super.onStop();
	}
	
	public boolean m_enableButtonLight = true;
	public void setButtonLight(boolean enabled) {
		if (enabled == m_enableButtonLight) {
			return;
		}

		m_enableButtonLight = enabled;
		try {
			final WindowManager.LayoutParams attrs = getWindow().getAttributes();
			final Class<?> cls = attrs.getClass();
			final Field fld = cls.getField("buttonBrightness");
			if (fld != null && "float".equals(fld.getType().toString())) {
				fld.setFloat(attrs, enabled ? -1.0f : 0.0f);
				getWindow().setAttributes(attrs);
			}
		} catch (NoSuchFieldException e) {
		} catch (IllegalAccessException e) {
		}
	}

	@Override
	public boolean onSearchRequested() {
		final FBReaderApp fbreader = FBReaderApp.Instance();
		final PopupPanel popup = fbreader.getActivePopup();
		fbreader.hideActivePopup();
		final SearchManager manager = (SearchManager)getSystemService(SEARCH_SERVICE);
		manager.setOnCancelListener(new SearchManager.OnCancelListener() {
			public void onCancel() {
				if (popup != null) {
					fbreader.showPopup(popup.getId());
				}
				manager.setOnCancelListener(null);
			}
		});
		startSearch(fbreader.TextSearchPatternOption.getValue(), true, null, false);
		return true;
	}

	public void showSelectionPanel() {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final ZLTextView view = fbReader.getCurrentView();
		((SelectionPopup)fbReader.getPopupById(SelectionPopup.ID))
			.move(view.getSelectionStartY(), view.getSelectionEndY());
		fbReader.showPopup(SelectionPopup.ID);
	}

	public void hideSelectionPanel() {
		final FBReaderApp fbReader = FBReaderApp.Instance();
		final PopupPanel popup = fbReader.getActivePopup();
		if (popup != null && popup.getId() == SelectionPopup.ID) {
			fbReader.hideActivePopup();
		}
	}

	private void onPreferencesUpdate(int resultCode) {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		switch (resultCode) {
			case RESULT_DO_NOTHING:
				break;
			case RESULT_REPAINT:
			{
				final BookModel model = fbReader.Model;
				if (model != null) {
					final Book book = model.Book;
					if (book != null) {
						book.reloadInfoFromDatabase();
					}
				}
				fbReader.clearTextCaches();
				FBReaderApp.Instance().repaintWidget(false);
				break;
			}
			case RESULT_RELOAD_BOOK:
				fbReader.reloadBook();
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_PREFERENCES:
			case REQUEST_BOOK_INFO:
				onPreferencesUpdate(resultCode);
				break;
		}
	}

	private void addMenuItem(Menu menu, String actionId, int iconId) {
		FBReaderApp.Instance().addMenuItem(menu, actionId, iconId, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		addMenuItem(menu, ActionCode.ADD_BOOKMARK, R.drawable.menu_icon_mark);
		addMenuItem(menu, ActionCode.SHOW_BOOKMARKS, R.drawable.menu_icon_catalog);
		addMenuItem(menu, ActionCode.SHOW_FONT, R.drawable.button_text_normal);
		addMenuItem(menu, ActionCode.SHOW_LIGHT, R.drawable.menu_icon_brightness);
//		addMenuItem(menu, ActionCode.SEARCH, R.drawable.ic_menu_search);
	
		addMenuItem(menu, ActionCode.SHOW_NAVIGATION, R.drawable.menu_icon_jump);
		addMenuItem(menu, ActionCode.SHOW_PREFERENCES, R.drawable.menu_icon_setting);

		FBReaderApp.Instance().refresh();

		return true;
	}
	
	public void setScreenBrightnessAuto(boolean auto) {
		if (auto) {
			final WindowManager.LayoutParams attrs = getWindow().getAttributes();
			attrs.screenBrightness = -1.0f;
			getWindow().setAttributes(attrs);
		} else {
			final int brightness = FBReaderApp.Instance().ScreenBrightnessLevelOption.getValue();
			setScreenBrightness(brightness);
		}
	}

	public void setScreenBrightness(int percent) {
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.screenBrightness = percent / 100.0f;
		getWindow().setAttributes(attrs);
	}

	final public int getScreenBrightness() {
		final int level = (int)(100 * getWindow().getAttributes().screenBrightness);
		return (level >= 0) ? level : 50;
	}

	private PowerManager.WakeLock myWakeLock;
	private boolean myWakeLockToCreate;
	private boolean myStartTimer;

	public final void createWakeLock() {
		if (myWakeLockToCreate) {
			synchronized (this) {
				if (myWakeLockToCreate) {
					myWakeLockToCreate = false;
					myWakeLock =
						((PowerManager)getSystemService(POWER_SERVICE)).
							newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "SCReaderActivity");
					myWakeLock.acquire();
				}
			}
		}
		if (myStartTimer) {
			FBReaderApp.Instance().startTimer();
			myStartTimer = false;
		}
	}

	private final void switchWakeLock(boolean on) {
		if (on) {
			if (myWakeLock == null) {
				myWakeLockToCreate = true;
			}
		} else {
			if (myWakeLock != null) {
				synchronized (this) {
					if (myWakeLock != null) {
						myWakeLock.release();
						myWakeLock = null;
					}
				}
			}
		}
	}

	@Override
	public void onPause() {
		unregisterReceiver(myBatteryInfoReceiver);
		FBReaderApp.Instance().stopTimer();
		switchWakeLock(false);
		FBReaderApp.Instance().onWindowClosing();
		
		// 如果按钮灯被禁用，则还原
		if (!m_enableButtonLight) {
			setButtonLight(true);
		}
		
		MobclickAgent.onPause(this);
		super.onPause();
	}

	@Override
	public void onLowMemory() {
		FBReaderApp.Instance().onWindowClosing();
		super.onLowMemory();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (FBReaderApp.Instance().isUseGLView()) {
			return ((m_bookViewGL != null) && m_bookViewGL.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
		} else {
			return ((m_bookView != null) && m_bookView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
		}	
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (FBReaderApp.Instance().isUseGLView()) {
			return ((m_bookViewGL != null) && m_bookViewGL.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
		} else {
			return ((m_bookView != null) && m_bookView.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
		}
	}

	BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			final int level = intent.getIntExtra("level", 100);
			FBReaderApp.Instance().setBatteryLevel(level);
			switchWakeLock(
				FBReaderApp.Instance().BatteryLevelToTurnScreenOffOption.getValue() < level
			);
		}
	};
}
