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

package org.geometerplus.fbreader.fbreader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

import org.geometerplus.zlibrary.error.ErrorKeys;
import org.geometerplus.zlibrary.filesystem.ZLArchiveEntryFile;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.filesystem.ZLPhysicalFile;
import org.geometerplus.zlibrary.filesystem.ZLResourceFile;

import org.geometerplus.zlibrary.options.ZLBooleanOption;
import org.geometerplus.zlibrary.options.ZLColorOption;
import org.geometerplus.zlibrary.options.ZLEnumOption;
import org.geometerplus.zlibrary.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.options.ZLStringOption;
import org.geometerplus.zlibrary.resources.ZLResource;
import org.geometerplus.zlibrary.text.hyphenation.ZLTextHyphenator;
import org.geometerplus.zlibrary.text.view.*;
import org.geometerplus.zlibrary.util.ZLBoolean3;
import org.geometerplus.zlibrary.util.ZLColor;
import org.geometerplus.zlibrary.view.ZLGLWidget;
import org.geometerplus.zlibrary.view.ZLViewWidget;

import org.geometerplus.android.fbreader.SCReaderActivity;
import org.geometerplus.android.fbreader.util.UIUtil;
import org.geometerplus.fbreader.FBTree;
import org.geometerplus.fbreader.Paths;
import org.geometerplus.fbreader.bookmodel.BookModel;
import org.geometerplus.fbreader.bookmodel.BookReadingException;
import org.geometerplus.fbreader.bookmodel.TOCTree;
import org.geometerplus.fbreader.filetype.FileType;
import org.geometerplus.fbreader.filetype.FileTypeCollection;
import org.geometerplus.fbreader.library.*;
import org.socool.socoolreader.reader.R;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;

public final class FBReaderApp {
	public final ZLBooleanOption AllowScreenBrightnessAdjustmentOption =
		new ZLBooleanOption("LookNFeel", "AllowScreenBrightnessAdjustment", true);
	public final ZLStringOption TextSearchPatternOption =
		new ZLStringOption("TextSearch", "Pattern", "");

	public final ZLBooleanOption UseSeparateBindingsOption =
		new ZLBooleanOption("KeysOptions", "UseSeparateBindings", false);

	public final ZLBooleanOption EnableDoubleTapOption =
		new ZLBooleanOption("Options", "EnableDoubleTap", false);
	public final ZLBooleanOption NavigateAllWordsOption =
		new ZLBooleanOption("Options", "NavigateAllWords", false);

	public static enum WordTappingAction {
		doNothing, selectSingleWord, startSelecting, openDictionary
	}
	public final ZLEnumOption<WordTappingAction> WordTappingActionOption =
		new ZLEnumOption<WordTappingAction>("Options", "WordTappingAction", WordTappingAction.startSelecting);

	public final ZLColorOption ImageViewBackgroundOption =
		new ZLColorOption("Colors", "ImageViewBackground", new ZLColor(255, 255, 255));
	public static enum ImageTappingAction {
		doNothing, selectImage, openImageView
	}
	public final ZLEnumOption<ImageTappingAction> ImageTappingActionOption =
		new ZLEnumOption<ImageTappingAction>("Options", "ImageTappingAction", ImageTappingAction.openImageView);

	public final ZLIntegerRangeOption LeftMarginOption;
	public final ZLIntegerRangeOption RightMarginOption;
	public final ZLIntegerRangeOption TopMarginOption;
	public final ZLIntegerRangeOption BottomMarginOption;
	{
		final int dpi = getDisplayDPI();
		final int x = getPixelWidth();
		final int y = getPixelHeight();
		final int horMargin = Math.min(dpi / 5, Math.min(x, y) / 30);
		LeftMarginOption = new ZLIntegerRangeOption("Options", "LeftMargin", 0, 100, horMargin);
		RightMarginOption = new ZLIntegerRangeOption("Options", "RightMargin", 0, 100, horMargin);
		TopMarginOption = new ZLIntegerRangeOption("Options", "TopMargin", 0, 100, 0);
		BottomMarginOption = new ZLIntegerRangeOption("Options", "BottomMargin", 0, 100, 4);
	}

	public final ZLIntegerRangeOption ScrollbarTypeOption =
		new ZLIntegerRangeOption("Options", "ScrollbarType", 0, 3, ZLTextView.SCROLLBAR_SHOW_AS_FOOTER);
	public final ZLIntegerRangeOption FooterHeightOption =
		new ZLIntegerRangeOption("Options", "FooterHeight", 8, 20, 9);
	public final ZLBooleanOption FooterShowTOCMarksOption =
		new ZLBooleanOption("Options", "FooterShowTOCMarks", true);
	public final ZLBooleanOption FooterShowClockOption =
		new ZLBooleanOption("Options", "ShowClockInFooter", true);
	public final ZLBooleanOption FooterShowBatteryOption =
		new ZLBooleanOption("Options", "ShowBatteryInFooter", true);
	public final ZLBooleanOption FooterShowProgressOption =
		new ZLBooleanOption("Options", "ShowProgressInFooter", true);
	public final ZLStringOption FooterFontOption =
		new ZLStringOption("Options", "FooterFont", "Droid Sans");

	final ZLStringOption ColorProfileOption =
		new ZLStringOption("Options", "ColorProfile", ColorProfile.DAY);

	public final ZLBooleanOption ShowLibraryInCancelMenuOption =
			new ZLBooleanOption("CancelMenu", "library", true);
	public final ZLBooleanOption ShowNetworkLibraryInCancelMenuOption =
			new ZLBooleanOption("CancelMenu", "networkLibrary", false);

	public final ZLBooleanOption ShowPreviousBookInCancelMenuOption =
		new ZLBooleanOption("CancelMenu", "previousBook", false);
	public final ZLBooleanOption ShowPositionsInCancelMenuOption =
		new ZLBooleanOption("CancelMenu", "positions", true);

	private ZLKeyBindings myBindings = null;

	public ZLTextView BookTextView = null;

	public volatile BookModel Model;

	private static FBReaderApp ourInstance;

	public static final String NoAction = "none";

	private volatile ZLTextView myView;

	private final HashMap<String,ZLAction> myIdToActionMap = new HashMap<String,ZLAction>();
	
	private BooksDatabase m_booksDatabase;

	public FBReaderApp(Application application) {
		ourInstance = this;
		myApplication = application;
		
		m_booksDatabase = new BooksDatabase(application);
		
		new FavoritesTree(myRootTree, ROOT_FAVORITES);
		new FirstLevelTree(myRootTree, ROOT_RECENT);
		new FirstLevelTree(myRootTree, ROOT_BY_AUTHOR);
		new FirstLevelTree(myRootTree, ROOT_BY_TITLE);
		new FirstLevelTree(myRootTree, ROOT_BY_TAG);
		new FileFirstLevelTree(myRootTree, ROOT_FILE_TREE);

		addAction(ActionCode.INCREASE_FONT, new ChangeFontSizeAction(this, +2));
		addAction(ActionCode.DECREASE_FONT, new ChangeFontSizeAction(this, -2));

		addAction(ActionCode.FIND_NEXT, new FindNextAction(this));
		addAction(ActionCode.FIND_PREVIOUS, new FindPreviousAction(this));
		addAction(ActionCode.CLEAR_FIND_RESULTS, new ClearFindResultsAction(this));

		addAction(ActionCode.SELECTION_CLEAR, new SelectionClearAction(this));

		addAction(ActionCode.TURN_PAGE_FORWARD, new TurnPageAction(this, true));
		addAction(ActionCode.TURN_PAGE_BACK, new TurnPageAction(this, false));

		addAction(ActionCode.MOVE_CURSOR_UP, new MoveCursorAction(this, ZLTextView.Direction.up));
		addAction(ActionCode.MOVE_CURSOR_DOWN, new MoveCursorAction(this, ZLTextView.Direction.down));
		addAction(ActionCode.MOVE_CURSOR_LEFT, new MoveCursorAction(this, ZLTextView.Direction.rightToLeft));
		addAction(ActionCode.MOVE_CURSOR_RIGHT, new MoveCursorAction(this, ZLTextView.Direction.leftToRight));

		addAction(ActionCode.SWITCH_TO_DAY_PROFILE, new SwitchProfileAction(this, ColorProfile.DAY));
		addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, new SwitchProfileAction(this, ColorProfile.NIGHT));

		addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, new VolumeKeyTurnPageAction(this, true));
		addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, new VolumeKeyTurnPageAction(this, false));

		addAction(ActionCode.EXIT, new ExitAction(this));

		BookTextView = new ZLTextView();

		setView(BookTextView);
	}
	
	public BooksDatabase getDatabase()
	{
		return m_booksDatabase;
	}

	public void openBook(Book book, final Bookmark bookmark, final Runnable postAction) {
		if (book == null) {
			if (Model == null) {
				book = getRecentBook();
				if (book == null || !book.File.exists()) {
					book = Book.getByFile(getHelpFile());
				}
			}
			if (book == null) {
				return;
			}
		}
		if (Model != null) {
			if (bookmark == null & book.File.getPath().equals(Model.Book.File.getPath())) {
				return;
			}
		}
		final Book bookToOpen = book;
		runWithMessage("loadingBook", new Runnable() {
			public void run() {
				openBookInternal(bookToOpen, bookmark);
			}
		}, postAction);
	}
 
	public void reloadBook() {
		if (Model != null && Model.Book != null) {
			Model.Book.reloadInfoFromDatabase();
			runWithMessage("loadingBook", new Runnable() {
				public void run() {
					openBookInternal(Model.Book, null);
				}
			}, null);
		}
	}

	private ColorProfile myColorProfile;

	public ColorProfile getColorProfile() {
		if (myColorProfile == null) {
			myColorProfile = ColorProfile.get(getColorProfileName());
		}
		return myColorProfile;
	}

	public String getColorProfileName() {
		return ColorProfileOption.getValue();
	}

	public void setColorProfileName(String name) {
		ColorProfileOption.setValue(name);
		myColorProfile = null;
	}

	public ZLKeyBindings keyBindings() {
		if (myBindings == null) {
			myBindings = new ZLKeyBindings("Keys");
		}
		return myBindings;
	}


	public void tryOpenFootnote(String id) {
		if (Model != null) {
			BookModel.Label label = Model.getLabel(id);
			if (label != null) {
				if (label.ModelId == null) {
					if (getCurrentView() == BookTextView) {
					}
					BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
					setView(BookTextView);
				}
				resetWidget();
			}
		}
	}

	public void clearTextCaches() {
		BookTextView.clearCaches();
	}

	synchronized void openBookInternal(Book book, Bookmark bookmark) {
		if (book != null) {
			onViewChanged();

			if (Model != null) {
				Model.Book.storePosition(BookTextView.getStartCursor());
			}
			BookTextView.setModel(null);
			clearTextCaches();

			Model = null;
			System.gc();
			System.gc();
			try {
				Model = BookModel.createModel(book);
				ZLTextHyphenator.Instance().load(book.getLanguage());
				BookTextView.setModel(Model.getTextModel());
				BookTextView.gotoPosition(book.getStoredPosition());
				if (bookmark == null) {
					setView(BookTextView);
					resetWidget();
					repaintWidget(true);
				} else {
					gotoBookmark(bookmark);
				}
				
				addBookToRecentList(book);
				setTitle(book.authors().toString());
			} catch (BookReadingException e) {
				processException(e);
			}
		}
	}

	public void gotoBookmark(Bookmark bookmark) {
		final String modelId = bookmark.ModelId;
		if (modelId == null) {
			BookTextView.gotoPosition(bookmark.m_posCurrentPage);
			setView(BookTextView);
			resetWidget();
			repaintWidget(true);
		}
	}

	public void showBookTextView() {
		setView(BookTextView);
	}

	private Book createBookForFile(ZLFile file) {
		if (file == null) {
			return null;
		}
		Book book = Book.getByFile(file);
		if (book != null) {
			book.insertIntoBookList();
			return book;
		}
		if (file.isArchive()) {
			for (ZLFile child : file.children()) {
				book = Book.getByFile(child);
				if (book != null) {
					book.insertIntoBookList();
					return book;
				}
			}
		}
		return null;
	}

	public void openFile(ZLFile file, Runnable postAction) {
		openBook(createBookForFile(file), null, postAction);
	}

	public void onWindowClosing() {
		if (Model != null && BookTextView != null) {
			Model.Book.storePosition(BookTextView.getStartCursor());
		}
	}

	public Bookmark addBookmark(int maxLength, boolean visible) {
		final ZLTextView view = getCurrentView();
		final ZLTextWordCursor cursor = view.getStartCursor();

		if (cursor.isNull()) {
			return null;
		}

		return new Bookmark(Model.Book, view.getModel().getId(), cursor, maxLength);
	}

	public TOCTree getCurrentTOCElement() {
		final ZLTextWordCursor cursor = BookTextView.getStartCursor();
		if (Model == null || cursor == null) {
			return null;
		}

		int index = cursor.getParagraphIndex();	
		if (cursor.isEndOfParagraph()) {
			++index;
		}
		TOCTree treeToSelect = null;
		for (TOCTree tree : Model.TOCTree) {
			final TOCTree.Reference reference = tree.getReference();
			if (reference == null) {
				continue;
			}
			if (reference.ParagraphIndex > index) {
				break;
			}
			treeToSelect = tree;
		}
		return treeToSelect;
	}
	
	public static FBReaderApp Instance() {
		return ourInstance;
	}

	protected final void setView(ZLTextView view) {
		if (view != null) {
			myView = view;
			onViewChanged();
		}
	}

	public final ZLTextView getCurrentView() {
		return myView;
	}

	public final void onRepaintFinished() {
		refresh();

		for (PopupPanel popup : popupPanels()) {
			popup.update();
		}
	}

	public final void onViewChanged() {
		hideActivePopup();
	}

	public final void hideActivePopup() {
		if (myActivePopup != null) {
			myActivePopup.hide_();
			myActivePopup = null;
		}
	}

	public final void showPopup(String id) {
		hideActivePopup();
		myActivePopup = myPopups.get(id);
		if (myActivePopup != null) {
			myActivePopup.show_();
		}
	}

	public final void addAction(String actionId, ZLAction action) {
		myIdToActionMap.put(actionId, action);
	}

	public final void removeAction(String actionId) {
		myIdToActionMap.remove(actionId);
	}

	public final boolean isActionVisible(String actionId) {
		final ZLAction action = myIdToActionMap.get(actionId);
		return action != null && action.isVisible();
	}

	public final boolean isActionEnabled(String actionId) {
		final ZLAction action = myIdToActionMap.get(actionId);
		return action != null && action.isEnabled();
	}

	public final ZLBoolean3 isActionChecked(String actionId) {
		final ZLAction action = myIdToActionMap.get(actionId);
		return action != null ? action.isChecked() : ZLBoolean3.B3_UNDEFINED;
	}

	public final void runAction(String actionId, Object ... params) {
		final ZLAction action = myIdToActionMap.get(actionId);
		if (action != null) {
			action.checkAndRun(params);
		}
	}

	public final boolean hasActionForKey(int key, boolean longPress) {
		final String actionId = keyBindings().getBinding(key, longPress);
		return actionId != null && !NoAction.equals(actionId);	
	}

	public final boolean runActionByKey(int key, boolean longPress) {
		final String actionId = keyBindings().getBinding(key, longPress);
		if (actionId != null) {
			final ZLAction action = myIdToActionMap.get(actionId);
			return action != null && action.checkAndRun();
		}
		return false;
	}

	public boolean closeWindow() {
		onWindowClosing();
		if (myActivity != null) {
			myActivity.finish();
		}
		return true;
	}

	//Action
	static abstract public class ZLAction {
		public boolean isVisible() {
			return true;
		}

		public boolean isEnabled() {
			return isVisible();
		}

		public ZLBoolean3 isChecked() {
			return ZLBoolean3.B3_UNDEFINED;
		}

		public final boolean checkAndRun(Object ... params) {
			if (isEnabled()) {
				run(params);
				return true;
			}
			return false;
		}

		abstract protected void run(Object ... params);
	}

	public static abstract class PopupPanel {
		protected final FBReaderApp Application;

		protected PopupPanel(FBReaderApp application) {
			application.myPopups.put(getId(), this);
			Application = application;
		}

		abstract public String getId();
		abstract protected void update();
		abstract protected void hide_();
		abstract protected void show_();
	}

	private final HashMap<String,PopupPanel> myPopups = new HashMap<String,PopupPanel>();
	private PopupPanel myActivePopup;
	public final Collection<PopupPanel> popupPanels() {
		return myPopups.values();
	}
	public final PopupPanel getActivePopup() {
		return myActivePopup;
	}
	public final PopupPanel getPopupById(String id) {
		return myPopups.get(id);
	}

	private volatile Timer myTimer;
	private final HashMap<Runnable,Long> myTimerTaskPeriods = new HashMap<Runnable,Long>();
	private final HashMap<Runnable,TimerTask> myTimerTasks = new HashMap<Runnable,TimerTask>();
	private static class MyTimerTask extends TimerTask {
		private final Runnable myRunnable;

		MyTimerTask(Runnable runnable) {
			myRunnable = runnable;
		}

		public void run() {
			myRunnable.run();
		}
	}

	private void addTimerTaskInternal(Runnable runnable, long periodMilliseconds) {
		final TimerTask task = new MyTimerTask(runnable);
		myTimer.schedule(task, periodMilliseconds / 2, periodMilliseconds);
		myTimerTasks.put(runnable, task);
	}

	private final Object myTimerLock = new Object();
	public final void startTimer() {
		synchronized (myTimerLock) {
			if (myTimer == null) {
				myTimer = new Timer();
				for (Map.Entry<Runnable,Long> entry : myTimerTaskPeriods.entrySet()) {
					addTimerTaskInternal(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	public final void stopTimer() {
		synchronized (myTimerLock) {
			if (myTimer != null) {
				myTimer.cancel();
				myTimer = null;
				myTimerTasks.clear();
			}
		}
	}

	public final void addTimerTask(Runnable runnable, long periodMilliseconds) {
		synchronized (myTimerLock) {
			removeTimerTask(runnable);
			myTimerTaskPeriods.put(runnable, periodMilliseconds);
			if (myTimer != null) {
				addTimerTaskInternal(runnable, periodMilliseconds);
			}
		}
	}	

	public final void removeTimerTask(Runnable runnable) {
		synchronized (myTimerLock) {
			TimerTask task = myTimerTasks.get(runnable);
			if (task != null) {
				task.cancel();
				myTimerTasks.remove(runnable);
			}
			myTimerTaskPeriods.remove(runnable);
		}
	}
	
	
	
	


	public static final String SCREEN_ORIENTATION_SYSTEM = "system";
	public static final String SCREEN_ORIENTATION_SENSOR = "sensor";
	public static final String SCREEN_ORIENTATION_PORTRAIT = "portrait";
	public static final String SCREEN_ORIENTATION_LANDSCAPE = "landscape";
	public static final String SCREEN_ORIENTATION_REVERSE_PORTRAIT = "reversePortrait";
	public static final String SCREEN_ORIENTATION_REVERSE_LANDSCAPE = "reverseLandscape";

	public final ZLStringOption OrientationOption = new ZLStringOption("LookNFeel", "Orientation", "auto");

	public String[] allOrientations() {
		return supportsAllOrientations()
			? new String[] {
				SCREEN_ORIENTATION_SYSTEM,
				SCREEN_ORIENTATION_SENSOR,
				SCREEN_ORIENTATION_PORTRAIT,
				SCREEN_ORIENTATION_LANDSCAPE,
				SCREEN_ORIENTATION_REVERSE_PORTRAIT,
				SCREEN_ORIENTATION_REVERSE_LANDSCAPE
			}
			: new String[] {
				SCREEN_ORIENTATION_SYSTEM,
				SCREEN_ORIENTATION_SENSOR,
				SCREEN_ORIENTATION_PORTRAIT,
				SCREEN_ORIENTATION_LANDSCAPE
			};
	}
	
	public final ZLIntegerRangeOption BatteryLevelToTurnScreenOffOption = new ZLIntegerRangeOption("LookNFeel", "BatteryLevelToTurnScreenOff", 0, 100, 50);
	public final ZLBooleanOption DontTurnScreenOffDuringChargingOption = new ZLBooleanOption("LookNFeel", "DontTurnScreenOffDuringCharging", true);
	public final ZLIntegerRangeOption ScreenBrightnessLevelOption = new ZLIntegerRangeOption("LookNFeel", "ScreenBrightnessLevel", 0, 100, 0);

	private boolean hasNoHardwareMenuButton() {
		return
			// Eken M001
			(Build.DISPLAY != null && Build.DISPLAY.contains("simenxie")) ||
			// PanDigital
			"PD_Novel".equals(Build.MODEL);
	}

	private Boolean myIsKindleFire = null;
	
	// 是否是opengles绘制的3d翻页效果
	public boolean isUseGLView() {
		return ScrollingPreferences.Instance().AnimationOption.getValue() == ZLTextView.Animation.curl3d;
	}
	
	// 卷轴模式 还是翻页模式 无动画效果时是垂直滚动
	public boolean isScrollMode() {
		return ScrollingPreferences.Instance().AnimationOption.getValue() == ZLTextView.Animation.none;
	}

	public boolean isKindleFire() {
		if (myIsKindleFire == null) {
			final String KINDLE_MODEL_REGEXP = ".*kindle(\\s+)fire.*";
			myIsKindleFire =
				Build.MODEL != null &&
				Build.MODEL.toLowerCase().matches(KINDLE_MODEL_REGEXP);
		}
		return myIsKindleFire;
	}

	private SCReaderActivity myActivity;
	private final Application myApplication;

	public void setActivity(SCReaderActivity activity) {
		myActivity = activity;
	}

	public void finish() {
		if ((myActivity != null) && !myActivity.isFinishing()) {
			myActivity.finish();
		}
	}

	public SCReaderActivity getActivity() {
		return myActivity;
	}

	public ZLViewWidget getWidget() {
		if (myActivity.m_bookView == null) {
			myActivity.createBookView();
		}

		return myActivity.m_bookView;
	}
	
	public ZLGLWidget getWidgetGL() {
		if (myActivity.m_bookViewGL == null) {
			myActivity.createBookView();
		}

		return myActivity.m_bookViewGL;
	}
	
	public void resetWidget()
	{
		if (isUseGLView()) {
			getWidgetGL().reset();
		} else {
			getWidget().reset();
		}
	}
	
	public void repaintStatusBar()
	{
		if (isUseGLView()) {
			getWidgetGL().repaintStatusBar();
		} else {
			getWidget().repaint();
		}
	}

	public void repaintWidget()
	{
		repaintWidget(true);
	}

	public void repaintWidget(boolean force)
	{
		if (isUseGLView()) {
			getWidgetGL().repaint(force);
		} else {
			getWidget().repaint();
		}
	}

	public ZLResourceFile createResourceFile(String path) {
		return new AndroidAssetsFile(path);
	}

	public ZLResourceFile createResourceFile(ZLResourceFile parent, String name) {
		return new AndroidAssetsFile((AndroidAssetsFile)parent, name);
	}

	public String getVersionName() {
		try {
			final PackageInfo info =
				myApplication.getPackageManager().getPackageInfo(myApplication.getPackageName(), 0);
			return info.versionName;
		} catch (Exception e) {
			return "";
		}
	}

	public String getFullVersionName() {
		try {
			final PackageInfo info =
				myApplication.getPackageManager().getPackageInfo(myApplication.getPackageName(), 0);
			return info.versionName + " (" + info.versionCode + ")";
		} catch (Exception e) {
			return "";
		}
	}

	public String getCurrentTimeString() {
		return DateFormat.getTimeFormat(myApplication.getApplicationContext()).format(new Date());
	}

	public void setScreenBrightness(int percent) {
		if (myActivity != null) {
			myActivity.setScreenBrightness(percent);
		}
	}

	public int getScreenBrightness() {
		return (myActivity != null) ? myActivity.getScreenBrightness() : 0;
	}

	private DisplayMetrics myMetrics;
	public int getDisplayDPI() {
		if (myMetrics == null) {
			if (myActivity == null) {
				return 0;
			}
			myMetrics = new DisplayMetrics();
			myActivity.getWindowManager().getDefaultDisplay().getMetrics(myMetrics);
		}
		return (int)(160 * myMetrics.density);
	}

	public int getPixelWidth() {
		if (myMetrics == null) {
			if (myActivity == null) {
				return 0;
			}
			myMetrics = new DisplayMetrics();
			myActivity.getWindowManager().getDefaultDisplay().getMetrics(myMetrics);
		}
		return myMetrics.widthPixels;
	}

	public int getPixelHeight() {
		if (myMetrics == null) {
			if (myActivity == null) {
				return 0;
			}
			myMetrics = new DisplayMetrics();
			myActivity.getWindowManager().getDefaultDisplay().getMetrics(myMetrics);
		}
		return myMetrics.heightPixels;
	}

	public Collection<String> defaultLanguageCodes() {
		final TreeSet<String> set = new TreeSet<String>();
		set.add(Locale.getDefault().getLanguage());
		final TelephonyManager manager = (TelephonyManager)myApplication.getSystemService(Context.TELEPHONY_SERVICE);
		if (manager != null) {
			final String country0 = manager.getSimCountryIso().toLowerCase();
			final String country1 = manager.getNetworkCountryIso().toLowerCase();
			for (Locale locale : Locale.getAvailableLocales()) {
				final String country = locale.getCountry().toLowerCase();
				if (country != null && country.length() > 0 &&
					(country.equals(country0) || country.equals(country1))) {
					set.add(locale.getLanguage());
				}
			}
			if ("ru".equals(country0) || "ru".equals(country1)) {
				set.add("ru");
			} else if ("by".equals(country0) || "by".equals(country1)) {
				set.add("ru");
			} else if ("ua".equals(country0) || "ua".equals(country1)) {
				set.add("ru");
			}
		}
		set.add("multi");
		return set;
	}

	public boolean supportsAllOrientations() {
		try {
			return ActivityInfo.class.getField("SCREEN_ORIENTATION_REVERSE_PORTRAIT") != null;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private final class AndroidAssetsFile extends ZLResourceFile {
		private final AndroidAssetsFile myParent;

		AndroidAssetsFile(AndroidAssetsFile parent, String name) {
			super(parent.getPath().length() == 0 ? name : parent.getPath() + '/' + name);
			myParent = parent;
		}

		AndroidAssetsFile(String path) {
			super(path);
			if (path.length() == 0) {
				myParent = null;
			} else {
				final int index = path.lastIndexOf('/');
				myParent = new AndroidAssetsFile(index >= 0 ? path.substring(0, path.lastIndexOf('/')) : "");
			}
		}

		@Override
		protected List<ZLFile> directoryEntries() {
			try {
				String[] names = myApplication.getAssets().list(getPath());
				if (names != null && names.length != 0) {
					ArrayList<ZLFile> files = new ArrayList<ZLFile>(names.length);
					for (String n : names) {
						files.add(new AndroidAssetsFile(this, n));
					}
					return files;
				}
			} catch (IOException e) {
			}
			return Collections.emptyList();
		}

		@Override
		public boolean isDirectory() {
			try {
				InputStream stream = myApplication.getAssets().open(getPath());
				if (stream == null) {
					return true;
				}
				stream.close();
				return false;
			} catch (IOException e) {
				return true;
			}
		}

		@Override
		public boolean exists() {
			try {
				InputStream stream = myApplication.getAssets().open(getPath());
				if (stream != null) {
					stream.close();
					// file exists
					return true;
				}
			} catch (IOException e) {
			}
			try {
				String[] names = myApplication.getAssets().list(getPath());
				if (names != null && names.length != 0) {
					// directory exists
					return true;
				}
			} catch (IOException e) {
			}
			return false;
		}

		private long mySize = -1;
		@Override
		public long size() {
			if (mySize == -1) {
				mySize = sizeInternal();
			}
			return mySize;
		}

		private long sizeInternal() {
			try {
				AssetFileDescriptor descriptor = myApplication.getAssets().openFd(getPath());
				// for some files (archives, crt) descriptor cannot be opened
				if (descriptor == null) {
					return sizeSlow();
				}
				long length = descriptor.getLength();
				descriptor.close();
				return length;
			} catch (IOException e) {
				return sizeSlow();
			} 
		}

		private long sizeSlow() {
			try {
				final InputStream stream = getInputStream();
				if (stream == null) {
					return 0;
				}
				long size = 0;
				final long step = 1024 * 1024;
				while (true) {
					// TODO: does skip work as expected for these files?
					long offset = stream.skip(step);
					size += offset;
					if (offset < step) {
						break;
					}
				}
				return size;
			} catch (IOException e) {
				return 0;
			}
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return myApplication.getAssets().open(getPath());
		}

		@Override
		public ZLFile getParent() {
			return myParent;
		}
	}
	
	
	// window menu and title
	private final HashMap<MenuItem,String> myMenuItemMap = new HashMap<MenuItem,String>();

	private final MenuItem.OnMenuItemClickListener myMenuListener =
		new MenuItem.OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				FBReaderApp.Instance().runAction(myMenuItemMap.get(item));
				return true;
			}
		};

	public Menu addSubMenu(Menu menu, String id) {
		return menu.addSubMenu(ZLResource.resource("menu").getResource(id).getValue());
	}

	public void addMenuItem(Menu menu, String actionId, Integer iconId, String name) {
		if (name == null) {
			name = ZLResource.resource("menu").getResource(actionId).getValue();
		}
		final MenuItem menuItem = menu.add(name);
		if (iconId != null) {
			menuItem.setIcon(iconId);
		}
		menuItem.setOnMenuItemClickListener(myMenuListener);
		myMenuItemMap.put(menuItem, actionId);
	}

	public void refresh() {
		for (Map.Entry<MenuItem,String> entry : myMenuItemMap.entrySet()) {
			final String actionId = entry.getValue();
			final MenuItem menuItem = entry.getKey();
			menuItem.setVisible(FBReaderApp.Instance().isActionVisible(actionId) && FBReaderApp.Instance().isActionEnabled(actionId));
			switch (FBReaderApp.Instance().isActionChecked(actionId)) {
				case B3_TRUE:
					menuItem.setCheckable(true);
					menuItem.setChecked(true);
					break;
				case B3_FALSE:
					menuItem.setCheckable(true);
					menuItem.setChecked(false);
					break;
				case B3_UNDEFINED:
					menuItem.setCheckable(false);
					break;
			}
		}
	}

	public void runWithMessage(String key, Runnable action, Runnable postAction) {
		final Activity activity = FBReaderApp.Instance().getActivity();
		if (activity != null) {
			UIUtil.runWithMessage(activity, key, action, postAction, false);
		} else {
			action.run();
		}
	}

	public void processException(Exception exception) {
		exception.printStackTrace();

		final Activity activity = FBReaderApp.Instance().getActivity();
		final Intent intent = new Intent(
			"android.fbreader.action.ERROR",
			new Uri.Builder().scheme(exception.getClass().getSimpleName()).build()
		);
		intent.putExtra(ErrorKeys.MESSAGE, exception.getMessage());
		final StringWriter stackTrace = new StringWriter();
		exception.printStackTrace(new PrintWriter(stackTrace));
		intent.putExtra(ErrorKeys.STACKTRACE, stackTrace.toString());
		/*
		if (exception instanceof BookReadingException) {
			final ZLFile file = ((BookReadingException)exception).File;
			if (file != null) {
				intent.putExtra("file", file.getPath());
			}
		}
		*/
		try {
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			// ignore
			e.printStackTrace();
		}
	}

	public void setTitle(final String title) {
		final Activity activity = FBReaderApp.Instance().getActivity();
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
				public void run() {
					activity.setTitle(title);
				}
			});
		}
	}

	public void close() {
		FBReaderApp.Instance().finish();
	}

	private int myBatteryLevel;
	public int getBatteryLevel() {
		return myBatteryLevel;
	}
	public void setBatteryLevel(int percent) {
		myBatteryLevel = percent;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	// 书籍信息管理
	public static final String ROOT_FOUND = "found";
	public static final String ROOT_FAVORITES = "favorites";
	public static final String ROOT_RECENT = "recent";
	public static final String ROOT_BY_AUTHOR = "byAuthor";
	public static final String ROOT_BY_TITLE = "byTitle";
	public static final String ROOT_BY_SERIES = "bySeries";
	public static final String ROOT_BY_TAG = "byTag";
	public static final String ROOT_FILE_TREE = "fileTree";
	public static final int REMOVE_DONT_REMOVE = 0x00;
	public static final int REMOVE_FROM_LIBRARY = 0x01;
	public static final int REMOVE_FROM_DISK = 0x02;
	public static final int REMOVE_FROM_LIBRARY_AND_DISK = REMOVE_FROM_LIBRARY | REMOVE_FROM_DISK;
	

	private final List<ChangeListener> myListeners = Collections.synchronizedList(new LinkedList<ChangeListener>());

	public interface ChangeListener {
		public enum Code {
			BookAdded,
			BookRemoved,
			StatusChanged,
			Found,
			NotFound
		}

		void onLibraryChanged(Code code);
	}

	public void addChangeListener(ChangeListener listener) {
		myListeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		myListeners.remove(listener);
	}

	protected void fireModelChangedEvent(ChangeListener.Code code) {
		synchronized (myListeners) {
			for (ChangeListener l : myListeners) {
				l.onLibraryChanged(code);
			}
		}
	}

	private final List<Book> myBooks = Collections.synchronizedList(new LinkedList<Book>());
	private final RootTree myRootTree = new RootTree();
	private boolean myDoGroupTitlesByFirstLetter;

	private final static int STATUS_LOADING = 1;
	private final static int STATUS_SEARCHING = 2;
	private volatile int myStatusMask = 0;

	private synchronized void setStatus(int status) {
		myStatusMask = status;
		fireModelChangedEvent(ChangeListener.Code.StatusChanged);
	}

	public LibraryTree getRootTree() {
		return myRootTree;
	}

	private FirstLevelTree getFirstLevelTree(String key) {
		return (FirstLevelTree)myRootTree.getSubTree(key);
	}

	public LibraryTree getLibraryTree(LibraryTree.Key key) {
		if (key == null) {
			return null;
		}
		if (key.Parent == null) {
			return key.Id.equals(myRootTree.getUniqueKey().Id) ? myRootTree : null;
		}
		final LibraryTree parentTree = getLibraryTree(key.Parent);
		return parentTree != null ? (LibraryTree)parentTree.getSubTree(key.Id) : null;
	}

	public static ZLResourceFile getHelpFile() {
		final Locale locale = Locale.getDefault();

		ZLResourceFile file = ZLResourceFile.createResourceFile(
			"data/help/MiniHelp." + locale.getLanguage() + "_" + locale.getCountry() + ".fb2"
		);
		if (file.exists()) {
			return file;
		}

		file = ZLResourceFile.createResourceFile(
			"data/help/MiniHelp." + locale.getLanguage() + ".fb2"
		);
		if (file.exists()) {
			return file;
		}

		return ZLResourceFile.createResourceFile("data/help/MiniHelp.en.fb2");
	}

	private List<ZLPhysicalFile> collectPhysicalFiles() {
		final Queue<ZLFile> dirQueue = new LinkedList<ZLFile>();
		final HashSet<ZLFile> dirSet = new HashSet<ZLFile>();
		final LinkedList<ZLPhysicalFile> fileList = new LinkedList<ZLPhysicalFile>();

		dirQueue.offer(new ZLPhysicalFile(new File(Paths.BooksDirectoryOption().getValue())));

		while (!dirQueue.isEmpty()) {
			for (ZLFile file : dirQueue.poll().children()) {
				if (file.isDirectory()) {
					if (!dirSet.contains(file)) {
						dirQueue.add(file);
						dirSet.add(file);
					}
				} else {
					file.setCached(true);
					fileList.add((ZLPhysicalFile)file);
				}
			}
		}
		return fileList;
	}

	private synchronized void addBookToLibrary(Book book) {
		myBooks.add(book);

		if (myDoGroupTitlesByFirstLetter) {
			final String letter = TitleTree.firstTitleLetter(book);
			if (letter != null) {
				final TitleTree tree =
					getFirstLevelTree(ROOT_BY_TITLE).getTitleSubTree(letter);
				tree.getBookSubTree(book, true);
			}
		} else {
			getFirstLevelTree(ROOT_BY_TITLE).getBookSubTree(book, true);
		}

		final SearchResultsTree found =
			(SearchResultsTree)getFirstLevelTree(ROOT_FOUND);
		if (found != null && book.matches(found.getPattern())) {
			found.getBookSubTree(book, true);
		}
	}

	private void removeFromTree(String rootId, Book book) {
		final FirstLevelTree tree = getFirstLevelTree(rootId);
		if (tree != null) {
			tree.removeBook(book, false);
		}
	}

	private void refreshInTree(String rootId, Book book) {
		final FirstLevelTree tree = getFirstLevelTree(rootId);
		if (tree != null) {
			int index = tree.indexOf(new BookTree(book, true));
			if (index >= 0) {
				tree.removeBook(book, false);
				new BookTree(tree, book, true, index);
			}
		}
	}

	public synchronized void refreshBookInfo(Book book) {
		if (book == null) {
			return;
		}

		myBooks.remove(book);
		refreshInTree(ROOT_FAVORITES, book);
		refreshInTree(ROOT_RECENT, book);
		removeFromTree(ROOT_FOUND, book);
		removeFromTree(ROOT_BY_TITLE, book);
		removeFromTree(ROOT_BY_SERIES, book);
		removeFromTree(ROOT_BY_AUTHOR, book);
		removeFromTree(ROOT_BY_TAG, book);
		addBookToLibrary(book);
		fireModelChangedEvent(ChangeListener.Code.BookAdded);
	}

	private void build() {
		// Step 0: get database books marked as "existing"
		final Map<Long,Book> savedBooksByFileId = m_booksDatabase.loadBooks();
		final Map<Long,Book> savedBooksByBookId = new HashMap<Long,Book>();
		for (Book b : savedBooksByFileId.values()) {
			savedBooksByBookId.put(b.getId(), b);
		}

		// Step 1: set myDoGroupTitlesByFirstLetter value,
        // add "existing" books into recent and favorites lists
		if (savedBooksByFileId.size() > 10) {
			final HashSet<String> letterSet = new HashSet<String>();
			for (Book book : savedBooksByFileId.values()) {
				final String letter = TitleTree.firstTitleLetter(book);
				if (letter != null) {
					letterSet.add(letter);
				}
			}
			myDoGroupTitlesByFirstLetter = savedBooksByFileId.values().size() > letterSet.size() * 5 / 4;
		}

		for (long id : m_booksDatabase.loadRecentBookIds()) {
			Book book = savedBooksByBookId.get(id);
			if (book == null) {
				book = Book.getById(id);
				if (book != null && !book.File.exists()) {
					book = null;
				}
			}
			if (book != null) {
				new BookTree(getFirstLevelTree(ROOT_RECENT), book, true);
			}
		}

		for (long id : FBReaderApp.Instance().getDatabase().loadFavoritesIds()) {
			Book book = savedBooksByBookId.get(id);
			if (book == null) {
				book = Book.getById(id);
				if (book != null && !book.File.exists()) {
					book = null;
				}
			}
			if (book != null) {
				getFirstLevelTree(ROOT_FAVORITES).getBookSubTree(book, true);
			}
		}

		fireModelChangedEvent(ChangeListener.Code.BookAdded);

		// Step 4: add help file
		try {
			final ZLFile helpFile = getHelpFile();
			Book helpBook = new Book(helpFile);
			addBookToLibrary(helpBook);
			fireModelChangedEvent(ChangeListener.Code.BookAdded);
		} catch (BookReadingException e) {
			// that's impossible
			e.printStackTrace();
		}
	}

	private volatile boolean myBuildStarted = false;

	public synchronized void startBuild() {
		if (myBuildStarted) {
			fireModelChangedEvent(ChangeListener.Code.StatusChanged);
			return;
		}
		myBuildStarted = true;

		setStatus(myStatusMask | STATUS_LOADING);
		final Thread builder = new Thread("Library.build") {
			public void run() {
				try {
					build();
				} finally {
					setStatus(myStatusMask & ~STATUS_LOADING);
				}
			}
		};
		builder.setPriority((Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2);
		builder.start();
	}

	public boolean isUpToDate() {
		return myStatusMask == 0;
	}

	public Book getRecentBook() {
		List<Long> recentIds = m_booksDatabase.loadRecentBookIds();
		return recentIds.size() > 0 ? Book.getById(recentIds.get(0)) : null;
	}

	public Book getPreviousBook() {
		List<Long> recentIds = m_booksDatabase.loadRecentBookIds();
		return recentIds.size() > 1 ? Book.getById(recentIds.get(1)) : null;
	}

	public void startBookSearch(final String pattern) {
		setStatus(myStatusMask | STATUS_SEARCHING);
		final Thread searcher = new Thread("Library.searchBooks") {
			public void run() {
				try {
					searchBooks(pattern);
				} finally {
					setStatus(myStatusMask & ~STATUS_SEARCHING);
				}
			}
		};
		searcher.setPriority((Thread.MIN_PRIORITY + Thread.NORM_PRIORITY) / 2);
		searcher.start();
	}

	private void searchBooks(String pattern) {
		if (pattern == null) {
			fireModelChangedEvent(ChangeListener.Code.NotFound);
			return;
		}

		pattern = pattern.toLowerCase();

		final SearchResultsTree oldSearchResults = (SearchResultsTree)getFirstLevelTree(ROOT_FOUND);
		if (oldSearchResults != null && pattern.equals(oldSearchResults.getPattern())) {
			fireModelChangedEvent(ChangeListener.Code.Found);
			return;
		}
		
		FirstLevelTree newSearchResults = null;
		final List<Book> booksCopy;
		synchronized (myBooks) {
			booksCopy = new ArrayList<Book>(myBooks);
		}
		for (Book book : booksCopy) {
			if (book.matches(pattern)) {
				synchronized (this) {
					if (newSearchResults == null) {
						if (oldSearchResults != null) {
							oldSearchResults.removeSelf();
						}
						newSearchResults = new SearchResultsTree(myRootTree, ROOT_FOUND, pattern);
						fireModelChangedEvent(ChangeListener.Code.Found);
					}
					newSearchResults.getBookSubTree(book, true);
					fireModelChangedEvent(ChangeListener.Code.BookAdded);
				}
			}
		}
		if (newSearchResults == null) {
			fireModelChangedEvent(ChangeListener.Code.NotFound);
		}
	}

	public void addBookToRecentList(Book book) {
		final List<Long> ids = m_booksDatabase.loadRecentBookIds();
		final Long bookId = book.getId();
		ids.remove(bookId);
		ids.add(0, bookId);
		if (ids.size() > 12) {
			ids.remove(12);
		}
		m_booksDatabase.saveRecentBookIds(ids);
		
		// 更新视图
		getFirstLevelTree(ROOT_RECENT).clear();
		for (long id : m_booksDatabase.loadRecentBookIds()) {
			Book book1 = Book.getById(id);
			if (book1 == null || !book1.File.exists()) {
				continue;
			}
			new BookTree(getFirstLevelTree(ROOT_RECENT), book1, true);
		}
	}

	public boolean isBookInFavorites(Book book) {
		if (book == null) {
			return false;
		}
		final LibraryTree rootFavorites = getFirstLevelTree(ROOT_FAVORITES);
		for (FBTree tree : rootFavorites.subTrees()) {
			if (tree instanceof BookTree && book.equals(((BookTree)tree).Book)) {
				return true;
			}
		}
		return false;
	}

	public void addBookToFavorites(Book book) {
		if (isBookInFavorites(book)) {
			return;
		}
		final LibraryTree rootFavorites = getFirstLevelTree(ROOT_FAVORITES);
		rootFavorites.getBookSubTree(book, true);
		m_booksDatabase.addToFavorites(book.getId());
	}

	public void removeBookFromFavorites(Book book) {
		if (getFirstLevelTree(ROOT_FAVORITES).removeBook(book, false)) {
			m_booksDatabase.removeFromFavorites(book.getId());
			fireModelChangedEvent(ChangeListener.Code.BookRemoved);
		}
	}

	public boolean canRemoveBookFile(Book book) {
		ZLFile file = book.File;
		if (file.getPhysicalFile() == null) {
			return false;
		}
		while (file instanceof ZLArchiveEntryFile) {
			file = file.getParent();
			if (file.children().size() != 1) {
				return false;
			}
		}
		return true;
	}

	public void removeBook(Book book, int removeMode) {
		if (removeMode == REMOVE_DONT_REMOVE) {
			return;
		}
		myBooks.remove(book);
		if (getFirstLevelTree(ROOT_RECENT).removeBook(book, false)) {
			final List<Long> ids = m_booksDatabase.loadRecentBookIds();
			ids.remove(book.getId());
			m_booksDatabase.saveRecentBookIds(ids);
		}
		getFirstLevelTree(ROOT_FAVORITES).removeBook(book, false);
		myRootTree.removeBook(book, true);

		m_booksDatabase.deleteFromBookList(book.getId());
		if ((removeMode & REMOVE_FROM_DISK) != 0) {
			book.File.getPhysicalFile().delete();
		}
	}
	
	public boolean hasCustomCover(ZLFile file)
	{
		final String lName = file.getShortName().toLowerCase();
		if (lName.endsWith(".fb2") || lName.endsWith(".fb2.zip")) {
			// fb2
			return true;
		}
		
		if (lName.endsWith(".epub") || lName.endsWith(".oebzip") || lName.endsWith(".opf")) {
			// epub
			return true;
		}
		
		if (lName.endsWith(".html") || lName.endsWith(".htm")) {
			// html
			return false;
		}
		
		if (lName.endsWith(".rtf")) {
			// rtf
			return false;
		}

		if (lName.endsWith(".txt")) {
			// txt
			return false;
		}
		
		return false;
	}

	public int getCoverResourceId(ZLFile file)
	{
		final String lName = file.getShortName().toLowerCase();
		if (lName.endsWith(".fb2") || lName.endsWith(".fb2.zip")) {
			// fb2
		}
		
		if (lName.endsWith(".epub") || lName.endsWith(".oebzip") || lName.endsWith(".opf")) {
			// epub
		}
		
		if (lName.endsWith(".html") || lName.endsWith(".htm")) {
			// html
		}
		
		if (lName.endsWith(".rtf")) {
			// rtf
		}

		if (lName.endsWith(".txt")) {
			// txt
		}
		
		return R.drawable.ic_list_library_book;
	}
}
