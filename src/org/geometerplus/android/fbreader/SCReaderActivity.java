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

import java.lang.reflect.Field;
import java.util.*;

import android.app.SearchManager;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import org.geometerplus.zlibrary.application.ZLApplication;
import org.geometerplus.zlibrary.application.ZLApplicationWindow;
import org.geometerplus.zlibrary.application.ZLibrary;
import org.geometerplus.zlibrary.error.UncaughtExceptionHandler;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLAndroidImageManager;

import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.view.ZLGLWidget;
import org.geometerplus.zlibrary.view.ZLViewWidget;

import org.socool.socoolreader.reader.R;

import org.geometerplus.fbreader.fbreader.ActionCode;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.fbreader.ScrollingPreferences;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.android.fbreader.tips.TipsManager;

import org.geometerplus.android.fbreader.action.*;
import org.geometerplus.android.fbreader.library.SQLiteBooksDatabase;
import org.geometerplus.android.fbreader.library.KillerCallback;
import org.geometerplus.android.fbreader.tips.TipsActivity;

import org.geometerplus.android.fbreader.util.UIUtil;

import android.app.Activity;
import org.geometerplus.android.fbreader.FBReaderApplication;

public final class SCReaderActivity extends Activity {
	private static final String REQUESTED_ORIENTATION_KEY = "org.geometerplus.zlibrary.ui.android.library.androidActiviy.RequestedOrientation";
	private static final String ORIENTATION_CHANGE_COUNTER_KEY = "org.geometerplus.zlibrary.ui.android.library.androidActiviy.ChangeCounter";

	public static final String BOOK_PATH_KEY = "BookPath";

	public static final int REQUEST_PREFERENCES = 1;
	public static final int REQUEST_BOOK_INFO = 2;
	public static final int REQUEST_CANCEL_MENU = 3;

	public static final int RESULT_DO_NOTHING = RESULT_FIRST_USER;
	public static final int RESULT_REPAINT = RESULT_FIRST_USER + 1;
	public static final int RESULT_RELOAD_BOOK = RESULT_FIRST_USER + 2;

	private static final String PLUGIN_ACTION_PREFIX = "___";
	private final List<PluginApi.ActionInfo> myPluginActions =
		new LinkedList<PluginApi.ActionInfo>();
	private final BroadcastReceiver myPluginInfoReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final ArrayList<PluginApi.ActionInfo> actions = getResultExtras(true).<PluginApi.ActionInfo>getParcelableArrayList(PluginApi.PluginInfo.KEY);
			if (actions != null) {
				synchronized (myPluginActions) {
					final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
					int index = 0;
					while (index < myPluginActions.size()) {
						fbReader.removeAction(PLUGIN_ACTION_PREFIX + index++);
					}
					myPluginActions.addAll(actions);
					index = 0;
					for (PluginApi.ActionInfo info : myPluginActions) {
						fbReader.addAction(
							PLUGIN_ACTION_PREFIX + index++,
							new RunPluginAction(SCReaderActivity.this, fbReader, info.getId())
						);
					}
				}
			}
		}
	};

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
	public ZLApplicationWindow myMainWindow;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

		ZLibrary.Instance().setActivity(this);
		
		m_mainLayout = new RelativeLayout(this);
		m_mainLayout.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT));
		createBookView();

		setContentView(m_mainLayout);

		if (myMainWindow == null) {
			if (SQLiteBooksDatabase.Instance() == null) {
				new SQLiteBooksDatabase(this, "READER");
			}

			myMainWindow = new ZLApplicationWindow(new FBReaderApp());
		}

		new Thread() {
			public void run() {
				ZLApplication.Instance().openFile(fileFromIntent(getIntent()), getPostponedInitAction());
			}
		}.start();

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 0);

		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		if (fbReader.getPopupById(TextSearchPopup.ID) == null) {
			new TextSearchPopup(fbReader);
		}
		if (fbReader.getPopupById(NavigationPopup.ID) == null) {
			new NavigationPopup(fbReader);
		}
		if (fbReader.getPopupById(SelectionPopup.ID) == null) {
			new SelectionPopup(fbReader);
		}

		fbReader.addAction(ActionCode.SHOW_LIBRARY, new ShowLibraryAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_PREFERENCES, new ShowPreferencesAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOK_INFO, new ShowBookInfoAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_TOC, new ShowTOCAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_BOOKMARKS, new ShowBookmarksAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NETWORK_LIBRARY, new ShowNetworkLibraryAction(this, fbReader));
		
		fbReader.addAction(ActionCode.SHOW_MENU, new ShowMenuAction(this, fbReader));
		fbReader.addAction(ActionCode.SHOW_NAVIGATION, new ShowNavigationAction(this, fbReader));
		fbReader.addAction(ActionCode.SEARCH, new SearchAction(this, fbReader));
		fbReader.addAction(ActionCode.SHARE_BOOK, new ShareBookAction(this, fbReader));

		fbReader.addAction(ActionCode.SELECTION_SHOW_PANEL, new SelectionShowPanelAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_HIDE_PANEL, new SelectionHidePanelAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_COPY_TO_CLIPBOARD, new SelectionCopyAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_SHARE, new SelectionShareAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_TRANSLATE, new SelectionTranslateAction(this, fbReader));
		fbReader.addAction(ActionCode.SELECTION_BOOKMARK, new SelectionBookmarkAction(this, fbReader));

		fbReader.addAction(ActionCode.PROCESS_HYPERLINK, new ProcessHyperlinkAction(this, fbReader));

		fbReader.addAction(ActionCode.SHOW_CANCEL_MENU, new ShowCancelMenuAction(this, fbReader));

		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_SYSTEM, new SetOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_SYSTEM));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_SENSOR, new SetOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_SENSOR));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT, new SetOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_PORTRAIT));
		fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE, new SetOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_LANDSCAPE));
		if (ZLibrary.Instance().supportsAllOrientations()) {
			fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT, new SetOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_REVERSE_PORTRAIT));
			fbReader.addAction(ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE, new SetOrientationAction(this, fbReader, ZLibrary.SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
		}
	}
	
	public void createBookView()
	{
		if (ZLibrary.Instance().isUseGLView()) {
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
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onOptionsMenuClosed(Menu menu) {
		super.onOptionsMenuClosed(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		final Uri data = intent.getData();
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		if ((intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
			super.onNewIntent(intent);
			final String action = intent.getAction();
			if (Intent.ACTION_VIEW.equals(action) || "android.fbreader.action.VIEW".equals(action)) {
				ZLApplication.Instance().openFile(fileFromIntent(intent), null);
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
					if (fbReader.getTextView().search(pattern, true, false, false, false) != 0) {
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
			if (Intent.ACTION_VIEW.equals(action) || "android.fbreader.action.VIEW".equals(action)) {
				ZLApplication.Instance().openFile(fileFromIntent(intent), null);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		initPluginActions();

		final ZLibrary zlibrary = (ZLibrary)ZLibrary.Instance();
		SetOrientationAction.setOrientation(this, zlibrary.OrientationOption.getValue());

		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		((PopupPanel)fbReader.getPopupById(TextSearchPopup.ID)).setPanelInfo(this, m_mainLayout);
		((PopupPanel)fbReader.getPopupById(NavigationPopup.ID)).setPanelInfo(this, m_mainLayout);
		((PopupPanel)fbReader.getPopupById(SelectionPopup.ID)).setPanelInfo(this, m_mainLayout);
	}

	private void initPluginActions() {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		synchronized (myPluginActions) {
			int index = 0;
			while (index < myPluginActions.size()) {
				fbReader.removeAction(PLUGIN_ACTION_PREFIX + index++);
			}
			myPluginActions.clear();
		}

		sendOrderedBroadcast(
			new Intent(PluginApi.ACTION_REGISTER),
			null,
			myPluginInfoReceiver,
			null,
			RESULT_OK,
			null,
			null
		);
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
				case Download:
					manager.startDownloading();
					break;
				case None:
					break;
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		switchWakeLock(
			ZLibrary.Instance().BatteryLevelToTurnScreenOffOption.getValue() <
			ZLApplication.Instance().getBatteryLevel()
		);
		myStartTimer = true;
		final int brightnessLevel = ZLibrary.Instance().ScreenBrightnessLevelOption.getValue();
		if (brightnessLevel != 0) {
			setScreenBrightness(brightnessLevel);
		} else {
			setScreenBrightnessAuto();
		}
		registerReceiver(myBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		try {
			sendBroadcast(new Intent(getApplicationContext(), KillerCallback.class));
		} catch (Throwable t) {
		}
		PopupPanel.restoreVisibilities(FBReaderApp.Instance());
	}

	@Override
	public void onStop() {
		PopupPanel.removeAllWindows(FBReaderApp.Instance(), this);
		super.onStop();
	}

	@Override
	public boolean onSearchRequested() {
		final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
		final FBReaderApp.PopupPanel popup = fbreader.getActivePopup();
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
		final ZLTextView view = fbReader.getTextView();
		((SelectionPopup)fbReader.getPopupById(SelectionPopup.ID))
			.move(view.getSelectionStartY(), view.getSelectionEndY());
		fbReader.showPopup(SelectionPopup.ID);
	}

	public void hideSelectionPanel() {
		final FBReaderApp fbReader = (FBReaderApp)FBReaderApp.Instance();
		final FBReaderApp.PopupPanel popup = fbReader.getActivePopup();
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
						ZLTextHyphenator.Instance().load(book.getLanguage());
					}
				}
				fbReader.clearTextCaches();
				ZLibrary.Instance().repaintWidget();
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
			case REQUEST_CANCEL_MENU:
				((FBReaderApp)FBReaderApp.Instance()).runCancelAction(resultCode - 1);
				break;
		}
	}

	public void navigate() {
		((NavigationPopup)FBReaderApp.Instance().getPopupById(NavigationPopup.ID)).runNavigation();
	}

	private Menu addSubMenu(Menu menu, String id) {
		return myMainWindow.addSubMenu(menu, id);
	}

	private void addMenuItem(Menu menu, String actionId, String name) {
		myMainWindow.addMenuItem(menu, actionId, null, name);
	}

	private void addMenuItem(Menu menu, String actionId, int iconId) {
		myMainWindow.addMenuItem(menu, actionId, iconId, null);
	}

	private void addMenuItem(Menu menu, String actionId) {
		myMainWindow.addMenuItem(menu, actionId, null, null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		addMenuItem(menu, ActionCode.SHOW_LIBRARY, R.drawable.ic_menu_library);
		addMenuItem(menu, ActionCode.SHOW_NETWORK_LIBRARY, R.drawable.ic_menu_networklibrary);
		addMenuItem(menu, ActionCode.SHOW_TOC, R.drawable.ic_menu_toc);
		addMenuItem(menu, ActionCode.SHOW_BOOKMARKS, R.drawable.ic_menu_bookmarks);
		addMenuItem(menu, ActionCode.SWITCH_TO_NIGHT_PROFILE, R.drawable.ic_menu_night);
		addMenuItem(menu, ActionCode.SWITCH_TO_DAY_PROFILE, R.drawable.ic_menu_day);
		addMenuItem(menu, ActionCode.SEARCH, R.drawable.ic_menu_search);
		addMenuItem(menu, ActionCode.SHARE_BOOK, R.drawable.ic_menu_search);
		addMenuItem(menu, ActionCode.SHOW_PREFERENCES);
		addMenuItem(menu, ActionCode.SHOW_BOOK_INFO);
		final Menu subMenu = addSubMenu(menu, "screenOrientation");
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_SYSTEM);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_SENSOR);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_PORTRAIT);
		addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_LANDSCAPE);
		if (ZLibrary.Instance().supportsAllOrientations()) {
			addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_REVERSE_PORTRAIT);
			addMenuItem(subMenu, ActionCode.SET_SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
		}
		addMenuItem(menu, ActionCode.INCREASE_FONT);
		addMenuItem(menu, ActionCode.DECREASE_FONT);
		addMenuItem(menu, ActionCode.SHOW_NAVIGATION);
		synchronized (myPluginActions) {
			int index = 0;
			for (PluginApi.ActionInfo info : myPluginActions) {
				if (info instanceof PluginApi.MenuActionInfo) {
					addMenuItem(
						menu,
						PLUGIN_ACTION_PREFIX + index++,
						((PluginApi.MenuActionInfo)info).MenuItemName
					);
				}
			}
		}

		myMainWindow.refresh();

		return true;
	}
	
	public void setScreenBrightnessAuto() {
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.screenBrightness = -1.0f;
		getWindow().setAttributes(attrs);
	}

	public void setScreenBrightness(int percent) {
		if (percent < 1) {
			percent = 1;
		} else if (percent > 100) {
			percent = 100;
		}
		final WindowManager.LayoutParams attrs = getWindow().getAttributes();
		attrs.screenBrightness = percent / 100.0f;
		getWindow().setAttributes(attrs);
		ZLibrary.Instance().ScreenBrightnessLevelOption.setValue(percent);
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
			ZLApplication.Instance().startTimer();
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
		ZLApplication.Instance().stopTimer();
		switchWakeLock(false);
		ZLApplication.Instance().onWindowClosing();
		super.onPause();
	}

	@Override
	public void onLowMemory() {
		ZLApplication.Instance().onWindowClosing();
		super.onLowMemory();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (ZLibrary.Instance().isUseGLView()) {
			return ((m_bookViewGL != null) && m_bookViewGL.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
		} else {
			return ((m_bookView != null) && m_bookView.onKeyDown(keyCode, event)) || super.onKeyDown(keyCode, event);
		}	
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (ZLibrary.Instance().isUseGLView()) {
			return ((m_bookViewGL != null) && m_bookViewGL.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
		} else {
			return ((m_bookView != null) && m_bookView.onKeyUp(keyCode, event)) || super.onKeyUp(keyCode, event);
		}
	}

	BroadcastReceiver myBatteryInfoReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			final int level = intent.getIntExtra("level", 100);
			myMainWindow.setBatteryLevel(level);
			switchWakeLock(
				ZLibrary.Instance().BatteryLevelToTurnScreenOffOption.getValue() < level
			);
		}
	};
}
