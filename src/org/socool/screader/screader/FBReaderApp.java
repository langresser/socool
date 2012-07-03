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

import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.UnicodeDetector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


import org.socool.zlibrary.filesystem.ZLFile;
import org.socool.zlibrary.filesystem.ZLPhysicalFile;
import org.socool.zlibrary.filesystem.ZLResource;
import org.socool.zlibrary.filesystem.ZLResourceFile;

import org.socool.zlibrary.options.ZLBooleanOption;
import org.socool.zlibrary.options.ZLColorOption;
import org.socool.zlibrary.options.ZLEnumOption;
import org.socool.zlibrary.options.ZLIntegerOption;
import org.socool.zlibrary.options.ZLIntegerRangeOption;
import org.socool.zlibrary.options.ZLStringOption;
import org.socool.zlibrary.text.ZLTextPosition;
import org.socool.zlibrary.text.ZLTextView;
import org.socool.zlibrary.text.ZLTextWordCursor;
import org.socool.zlibrary.util.ZLBoolean3;
import org.socool.zlibrary.util.ZLColor;
import org.socool.zlibrary.view.ZLGLWidget;
import org.socool.zlibrary.view.ZLViewWidget;

import org.socool.android.BookInfoActivity;
import org.socool.android.ChangeFontSizePopup;
import org.socool.android.ChangeLightPopup;
import org.socool.android.NavigationPopup;
import org.socool.android.PopupPanel;
import org.socool.android.SCReaderActivity;
import org.socool.android.SelectionPopup;
import org.socool.android.TextSearchPopup;
import org.socool.android.preferences.PreferenceActivity;
import org.socool.android.util.UIUtil;
import org.socool.screader.FBTree;
import org.socool.screader.Paths;
import org.socool.screader.bookmodel.BookChapter;
import org.socool.screader.bookmodel.BookModel;
import org.socool.screader.bookmodel.TOCTree;
import org.socool.screader.library.*;
import org.socool.socoolreader.mcnxs.R;

import com.umeng.analytics.MobclickAgent;
//import com.waps.AppConnect;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public final class FBReaderApp {
	public final ZLBooleanOption EnableTipOption = new ZLBooleanOption("Options", "enableTipMcnxs", true);
	public boolean m_hasShowTip = false;
	
	public final ZLStringOption TextSearchPatternOption =
		new ZLStringOption("TextSearch", "Pattern", "");

	public final ZLBooleanOption UseSeparateBindingsOption =
		new ZLBooleanOption("KeysOptions", "UseSeparateBindings", false);

	public final ZLBooleanOption EnableDoubleTapOption =
		new ZLBooleanOption("Options", "EnableDoubleTap", false);
	public final ZLBooleanOption NavigateAllWordsOption =
		new ZLBooleanOption("Options", "NavigateAllWords", false);
	
	public final ZLBooleanOption SoundTurnOption = new ZLBooleanOption("Options", "SoundTurn", true);
	public final ZLStringOption TurnOffMenuLight = new ZLStringOption("Options", "TurnOffMenuLight", "none");

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

	public ZLIntegerRangeOption LeftMarginOption = null;
	public ZLIntegerRangeOption RightMarginOption = null;
	public ZLIntegerRangeOption TopMarginOption = null;
	public ZLIntegerRangeOption BottomMarginOption = null;
	public ZLIntegerRangeOption FooterHeightOption = null;

	public final ZLIntegerRangeOption FirstLineIndentDeltaOption = new ZLIntegerRangeOption("LayoutStyle", "firstLineIndent", 0, 4, 2);	// 段首缩进，字体数目
	public final ZLIntegerRangeOption ParagraphSpaceOption = new ZLIntegerRangeOption("LayoutStyle", "paragraphSpace", 0, 20, 2);	// 段落间隔，除以10再乘以字体高度为实机间隔
	
	public final ZLBooleanOption AutoLineBreakOption = new ZLBooleanOption("Options", "autoLinebreak", true);

	private ZLKeyBindings myBindings = null;
	public ZLTextView BookTextView = null;
	public volatile BookModel Model;
	private static FBReaderApp ourInstance;

	public HashMap<String, BookChapter> m_bookChapterCache = new HashMap<String, BookChapter>();

	public static final String NoAction = "none";

	private final HashMap<String,ZLAction> myIdToActionMap = new HashMap<String,ZLAction>();
	
	private BooksDatabase m_booksDatabase;

	public FBReaderApp(Application application) {
		ourInstance = this;
		myApplication = application;
		
		m_booksDatabase = new BooksDatabase(application);
		
		new FirstLevelTree(myRootTree, ROOT_RECENT);

		initActions();

		BookTextView = new ZLTextView();
	
		// 自动检测编码格式
		CodepageDetectorProxy detector =   CodepageDetectorProxy.getInstance();
		detector.add(UnicodeDetector.getInstance());
		detector.add(ASCIIDetector.getInstance());
		detector.add(JChardetFacade.getInstance());
	}
	
	private void initActions()
	{
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

		addAction(ActionCode.SWITCH_TO_DAY_PROFILE, new SwitchProfileAction(this));
		addAction(ActionCode.SWITCH_TO_NIGHT_PROFILE, new SwitchProfileAction(this));
		addAction(ActionCode.EXIT, new ExitAction(this));
		
		addAction(ActionCode.VOLUME_KEY_SCROLL_FORWARD, new VolumeKeyTurnPageAction(this, true));
		addAction(ActionCode.VOLUME_KEY_SCROLL_BACK, new VolumeKeyTurnPageAction(this, false));
	}
	
	public BooksDatabase getDatabase()
	{
		return m_booksDatabase;
	}

	public void openBook(Book book, final Bookmark bookmark, final Runnable postAction) {
		if (book == null) {
			return;
		}

		if (Model != null) {
			if (bookmark == null && book.m_filePath.equals(Model.Book.m_filePath)) {
				return;
			}
		}
//		final Book bookToOpen = book;
//		runWithMessage("loadingBook", new Runnable() {
//			public void run() {
//				openBookInternal(bookToOpen, bookmark);
//			}
//		}, postAction);
		openBookInternal(book, bookmark);
	}
 
	public void reloadBook() {
		if (Model != null && Model.Book != null) {
			Model.Book.reloadInfoFromDatabase();
			
			if (Model.m_readType == BookModel.READ_TYPE_NORMAL) {
				runWithMessage("loadingBook", new Runnable() {
					public void run() {
						openBookInternal(Model.Book, null);
					}
				}, null);
			} else {
				openBookInternal(Model.Book, null);
			}
		}
	}

	public ZLBooleanOption isNightModeOption = new ZLBooleanOption("Options", "NightMode", false);
	private ColorProfile myColorProfile;
	private ColorProfile m_colorProfileNight;
	
	public String getCurrentTheme()
	{
		if (isNightModeOption.getValue()) {
			return m_colorProfileNight.BaseThemeOption.getValue();
		} else {
			return myColorProfile.BaseThemeOption.getValue();
		}
	}
	
	public ArrayList<TextTheme> m_themes = new ArrayList<TextTheme>();
	public void initTheme()
	{
		if (m_themes.size() > 0) {
			return;
		}

		final List<ZLFile> predefined = WallpapersUtil.predefinedWallpaperFiles();
		
		try {
			for (ZLFile f : predefined) {
				String path = f.getPath();
				// 主题是一个文件夹
				if (path.indexOf('.') != -1) {
					continue;
				}
				String configPath = path + "/config.cfg";
				InputStream cfgInput = getBookFile(configPath);
				if (cfgInput == null) {
					continue;
				}
							
				BufferedReader reader = new BufferedReader(new InputStreamReader(cfgInput, "gbk"));
				
				String line = null;
				TextTheme theme = new TextTheme();
				theme.m_path = path;
				while ((line = reader.readLine()) != null) {
					final int split = line.indexOf('=');
					if (split == -1) {
						continue;
					}

					String key = line.substring(0, split);
					String value = line.substring(split + 1);
					
					if (key.compareToIgnoreCase("title") == 0) {
						theme.m_title = value;
					} else if (key.compareToIgnoreCase("image") == 0) {
						theme.m_imagePath = path + '/' + value;
					} else if (key.compareToIgnoreCase("thumb") == 0) {
						theme.m_thumbPath = path + '/' + value;
					} else if (key.compareToIgnoreCase("fontcolor") == 0) {
						theme.m_textColor = new ZLColor(value);
					} else if (key.compareToIgnoreCase("bgcolor") == 0) {
						theme.m_bgColor = new ZLColor(value);
					} else if (key.compareToIgnoreCase("night") == 0) {
						theme.m_isNightMode = (Integer.parseInt(value) != 0);
					}
				}
				
				if (theme.m_title == null) {
					theme.m_title = "未命名";
				}
				
				if (theme.m_imagePath == null) {
					theme.m_imagePath = path + '/' + "image.jpg";
				}
						
				if (theme.m_bgColor == null) {
					theme.m_bgColor = new ZLColor("0.33,0.3,0.22,1");
				}
				
				if (theme.m_textColor == null) {
					theme.m_textColor = new ZLColor("0,0,0,1");
				}
				
				if (theme.m_selectBgColor == null) {
					if (theme.m_isNightMode) {
						theme.m_selectBgColor = new ZLColor(82, 131, 194);
					} else {
						theme.m_selectBgColor = new ZLColor(82, 131, 194);
					}
				}
				
				if (theme.m_selectTextColor == null) {
					if (theme.m_isNightMode) {
						theme.m_selectTextColor = new ZLColor(255, 255, 220);
					} else {
						theme.m_selectTextColor = new ZLColor(255, 255, 220);
					}
				}
							
				m_themes.add(theme);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public TextTheme getThemeByName(String name) {
		if (m_themes.size() <= 0) {
			initTheme();
		}

		for (TextTheme each : m_themes) {
			if (each.m_path.compareTo(name) == 0) {
				return each;
			}
		}
		
		return null;
	}
	
	public ColorProfile getColorProfile() {
		if (isNightModeOption.getValue() == true) {
			if (m_colorProfileNight == null) {
				m_colorProfileNight = new ColorProfile("night");
			}
			return m_colorProfileNight;
		} else {
			if (myColorProfile == null) {
				myColorProfile = new ColorProfile("day");
			}
			return myColorProfile;
		}
	}

	public void changeTheme(String name) {
		if (name.length() == 0) {
			final ColorProfile color = getColorProfile();
			color.ChangeTheme(null);
			return;
		}

		TextTheme theme = getThemeByName(name);
		if (theme == null) {
			return;
		}

		isNightModeOption.setValue(theme.m_isNightMode);
		final ColorProfile color = getColorProfile();
		color.ChangeTheme(theme);
	}

	public ZLKeyBindings keyBindings() {
		if (myBindings == null) {
			myBindings = new ZLKeyBindings("Keys");
		}
		return myBindings;
	}

	public int getLeftMargin() {
		if (LeftMarginOption == null) {
			LeftMarginOption = new ZLIntegerRangeOption("Options", "LeftMargin", 0, 20, 10);
		}

		return LeftMarginOption.getValue();
	}

	public int getRightMargin() {
		if (RightMarginOption == null) {
			RightMarginOption = new ZLIntegerRangeOption("Options", "RightMargin", 0, 20, 10);
		}
		return RightMarginOption.getValue();
	}
	
	public int getTopMargin() {
		if (TopMarginOption == null) {
			TopMarginOption = new ZLIntegerRangeOption("Options", "TopMargin", 0, 20, 5);
		}
		return TopMarginOption.getValue() + m_adsHeight;
	}

	public int getBottomMargin() {
		if (BottomMarginOption == null) {
			BottomMarginOption = new ZLIntegerRangeOption("Options", "BottomMargin", 0, 20, 5);
		}
		return BottomMarginOption.getValue();
	}
	
	public int getFooterHeight()
	{
		if (FooterHeightOption == null) {
			final int defaultHeight = (int)(12 * getDensity());
			FooterHeightOption = new ZLIntegerRangeOption("Options", "footerheight", 0, 40, defaultHeight);
		}
		
		return FooterHeightOption.getValue();
	}


	public void tryOpenInternalLink(String id) {
		if (Model != null) {
			BookModel.Label label = Model.getLabel(id);
			if (label != null) {
				if (label.ModelId == null) {
					BookTextView.gotoPosition(label.ParagraphIndex, 0, 0);
				}
				resetWidget();
			}
		}
	}

	public void clearTextCaches() {
		BookTextView.clearCaches();
	}

	synchronized void openBookInternal(Book book, Bookmark bookmark) {
		if (book == null) {
			return;
		}

		onViewChanged();

		if (Model != null) {
			Model.Book.storePosition(BookTextView.getStartCursor(),
					BookTextView.m_currentChapterTitle, BookTextView.getCurrentPercent());
		}
		BookTextView.setModel(null);
		clearTextCaches();

		Model = null;
		System.gc();

		Model = BookModel.createModel(book, true);
		BookTextView.setModel(Model);
		
		if (bookmark == null) {
			final BookState state = book.getStoredPosition();
			if (state != null) {
				final ZLTextPosition position = state.m_textPosition;
				BookTextView.gotoPosition(position.getParagraphIndex(), position.getElementIndex(), position.getCharIndex());
			}

			resetWidget();
			repaintWidget(true);
		} else {
			gotoBookmark(bookmark);
		}
		
		addBookToRecentList(book);
		setTitle(book.authors().toString());
	}

	public void gotoBookmark(Bookmark bookmark) {
		final String modelId = bookmark.ModelId;
		final ZLTextPosition position = bookmark.m_posCurrentPage;
		if (modelId == null & position != null) {
			BookTextView.gotoPosition(position.getParagraphIndex(), position.getElementIndex(), position.getCharIndex());
			resetWidget();
			repaintWidget(true);
		}
	}

	private Book createBookForFile(ZLFile file) {
		if (file == null) {
			return null;
		}
		Book book = Book.getByFile(file);
		if (book != null) {
			return book;
		}
		if (file.isArchive()) {
			for (ZLFile child : file.children()) {
				book = Book.getByFile(child);
				if (book != null) {
					return book;
				}
			}
		}
		return null;
	}

	public void openFile(ZLFile file, Runnable postAction) {
		openBook(createBookForFile(file), null, postAction);
	}
	
	public void openFile(ZLFile file, int chapterIndex)
	{
		final Book book = createBookForFile(file);
		if (book == null) {
			return;
		}

		onViewChanged();

		if (Model != null) {
			Model.Book.storePosition(BookTextView.getStartCursor(),
					BookTextView.m_currentChapterTitle, BookTextView.getCurrentPercent());
		}
		BookTextView.setModel(null);
		clearTextCaches();

		Model = null;
		System.gc();

		Model = BookModel.createModel(book, true);
		BookTextView.setModel(Model);
		
		BookTextView.gotoChapter(chapterIndex);
		repaintWidget(true);
		
		addBookToRecentList(book);
		setTitle(book.authors().toString());
	}

	public void onWindowClosing() {
		if (Model != null && BookTextView != null) {
			Model.Book.storePosition(BookTextView.getStartCursor(),
					BookTextView.m_currentChapterTitle, BookTextView.getCurrentPercent());
		}
	}

	public Bookmark addBookmark(int maxLength, boolean visible) {
		final ZLTextView view = getCurrentView();
		final ZLTextWordCursor cursor = view.getStartCursor();

		if (cursor.isNull()) {
			return null;
		}

		return new Bookmark(Model.Book, view.getModel().myId, cursor, maxLength, view.getCurrentPercent());
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

	public final ZLTextView getCurrentView() {
		return BookTextView;
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

	public final HashMap<String, PopupPanel> myPopups = new HashMap<String, PopupPanel>();
	private PopupPanel myActivePopup;
	public final Collection<PopupPanel> popupPanels() {
		return myPopups.values();
	}
	public final PopupPanel getActivePopup() {
		return myActivePopup;
	}
	public final PopupPanel getPopupById(String id) {
		final PopupPanel panel = myPopups.get(id);
		if (panel == null) {
			if (id == TextSearchPopup.ID) {
				return new TextSearchPopup();
			} else if (id == NavigationPopup.ID) {
				return new NavigationPopup();
			} else if (id == SelectionPopup.ID) {
				return new SelectionPopup();
			} else if (id == ChangeFontSizePopup.ID) {
				return new ChangeFontSizePopup();
			} else if (id == ChangeLightPopup.ID) {
				return new ChangeLightPopup();
			}
		}
		
		return panel;
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
	public static final String SCREEN_ORIENTATION_PORTRAIT = "portrait";
	public static final String SCREEN_ORIENTATION_LANDSCAPE = "landscape";

	public final ZLStringOption OrientationOption = new ZLStringOption("LookNFeel", "Orientation", "portrait");

	public String[] allOrientations() {
		return new String[] {
				SCREEN_ORIENTATION_SYSTEM,
				SCREEN_ORIENTATION_PORTRAIT,
				SCREEN_ORIENTATION_LANDSCAPE
			};
	}
	
	public static final int TURN_OFF_TIME_DEFAULT = 0;
	public static final int TURN_OFF_TIME_1 = 1;
	public static final int TURN_OFF_TIME_3 = 2;
	public static final int TURN_OFF_TIME_5 = 3;
	public static final int TURN_OFF_TIME_10 = 4;
	public static final int TURN_OFF_TIME_30 = 5;
	public static final int TURN_OFF_TIME_NEVER = 6;
	
	public final ZLIntegerRangeOption BatteryLevelToTurnScreenOffOption = new ZLIntegerRangeOption("LookNFeel", "BatteryLevelToTurnScreenOff", 0, 100, 50);
	public final ZLStringOption TurnOffTimeOpion = new ZLStringOption("LookNFeel", "TurnOffTime", "default");
	public final ZLIntegerRangeOption ScreenBrightnessLevelOption = new ZLIntegerRangeOption("LookNFeel", "ScreenBrightnessLevel", 0, 100, 10);

	public final ZLBooleanOption ScreenBrightnessAuto = new ZLBooleanOption("LookNFeel", "ScreenBrightnessAuto", true);
	private Boolean myIsKindleFire = null;
	
	// 是否是opengles绘制的3d翻页效果
	public boolean isUseGLView() {
		return false;//return ScrollingPreferences.Instance().AnimationOption.getValue() == ZLTextView.Animation.curl3d;
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

	public void setScreenBrightnessAuto(boolean auto)
	{
		if (myActivity != null) {
			ScreenBrightnessAuto.setValue(auto);
			myActivity.setScreenBrightnessAuto(auto);
		}
	}

	public void setScreenBrightness(int percent) {
		if (myActivity != null) {
			if (percent < 1) {
				percent = 1;
			}

			if (percent > 100) {
				percent = 100;
			}

			ScreenBrightnessLevelOption.setValue(percent);
			myActivity.setScreenBrightness(percent);
		}
	}

	public int getScreenBrightness() {
		return (myActivity != null) ? myActivity.getScreenBrightness() : 0;
	}

	private DisplayMetrics myMetrics;
	private void initDisplayMetrics()
	{
		if (myActivity == null) {
			return;
		}

		myMetrics = new DisplayMetrics();
		myActivity.getWindowManager().getDefaultDisplay().getMetrics(myMetrics);
	}
	public int getDisplayDPI() {
		if (myMetrics == null) {
			initDisplayMetrics();
		}
		return (int)(160 * myMetrics.density);
	}
	
	public float getDensity()
	{
		if (myMetrics == null) {
			initDisplayMetrics();
		}

		return myMetrics.density;
	}

	public int getPixelWidth() {
		if (myMetrics == null) {
			initDisplayMetrics();
		}
		return myMetrics.widthPixels;
	}

	public int getPixelHeight() {
		if (myMetrics == null) {
			initDisplayMetrics();
		}
		return myMetrics.heightPixels;
	}
	
	public InputStream getBookFile(String path)
	{
		try {
			final InputStream stream = myApplication.getAssets().open(path);
			return stream;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
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
				if (myActivity != null) {
					MobclickAgent.onEvent(myActivity, "menu", myMenuItemMap.get(item));
				}
				
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

	public ZLResourceFile getHelpFile() {
		final Locale locale = Locale.getDefault();

		ZLResourceFile file = createResourceFile("book/help/MiniHelp." + locale.getLanguage() + "_" + locale.getCountry() + ".fb2");
		if (file.exists()) {
			return file;
		}

		file = createResourceFile("book/help/MiniHelp." + locale.getLanguage() + ".fb2");
		if (file.exists()) {
			return file;
		}

		return createResourceFile("book/help/MiniHelp.en.fb2");
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

//		final SearchResultsTree found =
//			(SearchResultsTree)getFirstLevelTree(ROOT_FOUND);
//		if (found != null && book.matches(found.getPattern())) {
//			found.getBookSubTree(book, true);
//		}
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
			savedBooksByBookId.put(b.myId, b);
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
			}
			if (book != null) {
				new BookTree(getFirstLevelTree(ROOT_RECENT), book, true);
			}
		}

		for (long id : FBReaderApp.Instance().getDatabase().loadFavoritesIds()) {
			Book book = savedBooksByBookId.get(id);
			if (book == null) {
				book = Book.getById(id);
			}
			if (book != null) {
				getFirstLevelTree(ROOT_FAVORITES).getBookSubTree(book, true);
			}
		}

		fireModelChangedEvent(ChangeListener.Code.BookAdded);

		// Step 4: add help file
		final ZLFile helpFile = getHelpFile();
		Book helpBook = new Book(helpFile.getPath());
		addBookToLibrary(helpBook);
		fireModelChangedEvent(ChangeListener.Code.BookAdded);
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

//		final SearchResultsTree oldSearchResults = (SearchResultsTree)getFirstLevelTree(ROOT_FOUND);
//		if (oldSearchResults != null && pattern.equals(oldSearchResults.getPattern())) {
//			fireModelChangedEvent(ChangeListener.Code.Found);
//			return;
//		}
		
		FirstLevelTree newSearchResults = null;
		final List<Book> booksCopy;
		synchronized (myBooks) {
			booksCopy = new ArrayList<Book>(myBooks);
		}
		if (newSearchResults == null) {
			fireModelChangedEvent(ChangeListener.Code.NotFound);
		}
	}

	public void addBookToRecentList(Book book) {
		final List<Long> ids = m_booksDatabase.loadRecentBookIds();
		final Long bookId = book.myId;
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
			if (book1 == null) {
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
		m_booksDatabase.addToFavorites(book.myId);
	}

	public void removeBookFromFavorites(Book book) {
		if (getFirstLevelTree(ROOT_FAVORITES).removeBook(book, false)) {
			m_booksDatabase.removeFromFavorites(book.myId);
			fireModelChangedEvent(ChangeListener.Code.BookRemoved);
		}
	}

	public boolean canRemoveBookFile(Book book) {
		ZLFile file = ZLFile.createFileByPath(book.m_filePath);
		if (file.getPhysicalFile() == null) {
			return false;
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
			ids.remove(book.myId);
			m_booksDatabase.saveRecentBookIds(ids);
		}
		getFirstLevelTree(ROOT_FAVORITES).removeBook(book, false);
		myRootTree.removeBook(book, true);

		if ((removeMode & REMOVE_FROM_DISK) != 0) {
			ZLFile file = ZLFile.createFileByPath(book.m_filePath);
			file.getPhysicalFile().delete();
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
	
	// 广告模块相关内容
	// 控制是否显示广告，每个电子书对应一个标识
	public final ZLBooleanOption EnableAdsOption = new ZLBooleanOption("Options", "enableAdsMcnxs", false);
	public int m_adsHeight = 0;
	public boolean m_initOfferWall = false;
	public void initOfferWall(Context context)
	{
		// 万普
		try {
//			AppConnect.getInstance(context);
//			AppConnect.getInstance("7281ebff5a5dae5d199636a7b6c8ecc2","appChina", context);
			
			// 有米
		} catch (Exception e) {
			e.printStackTrace();
			MobclickAgent.reportError(context, "initOfferWall error");
		}
		
		
	}
	
	public void releaseOfferWall(Context context)
	{
//		AppConnect.getInstance(context).finalize();
	}
	
	public void showOfferWall(Context context)
	{
		if (!m_initOfferWall) {
			initOfferWall(context);
			m_initOfferWall = true;
		}
		
		try {
			MobclickAgent.onEvent(context, "moreApp");
//			AppConnect.getInstance(context).showOffers(context);
		} catch (Exception e) {
			e.printStackTrace();
			MobclickAgent.reportError(context, "showOfferWall error");
		}	
	}
	
	public void showMore(Context context)
	{
		try {
			MobclickAgent.onEvent(context, "moreBook");
//			AppConnect.getInstance(context).showMore(context);
			UIUtil.showMessageText(context, "敬请期待...");
//			YoumiOffersManager.showOffers(context, YoumiOffersManager.TYPE_REWARDLESS_APPLIST);
		} catch (Exception e) {
			e.printStackTrace();
			MobclickAgent.reportError(context, "showMore error");
		}
	}
	
	public int getOfferPoints(Context context)
	{
//		return YoumiPointsManager.queryPoints(context);
		return 0;
	}
	
	public void costOfferPoints(Context context, int points)
	{
//		YoumiPointsManager.spendPoints(context, points);
	}
	
	public final static int REMOVE_ADS_POINT = 0;
	public final static int IMPORT_BOOK_POINT = 0;
	public void removeAds(final Context context)
	{
		if (EnableAdsOption.getValue() == false) {
			return;
		}

		final int currentPoints = getOfferPoints(context);
		if (currentPoints < REMOVE_ADS_POINT) {
			String text = String.format("移除广告需要 %1d积分，当前积分%2d，您可以通过下载推荐应用的方式免费获取积分", REMOVE_ADS_POINT, currentPoints);
			Dialog dialog = new AlertDialog.Builder(context).setTitle("积分不足").setMessage(text)
					.setPositiveButton("确定",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int whichButton) {
									dialog.cancel();
								}
							}).setNegativeButton("推荐应用",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									showOfferWall(context);
									dialog.cancel();
								}
							}).create();// 创建按钮
			dialog.show();
			
		} else {
			MobclickAgent.onEvent(context, "removeAds", "ok");
			costOfferPoints(context, REMOVE_ADS_POINT);
			UIUtil.showMessageText(context, "广告已移除，感谢您的支持。");
			m_adsHeight = 0;
			EnableAdsOption.setValue(false);
		}
	}
	
	public void showHelpDialog(final Context context)
	{
		final String m_tips = 
				"    点击屏幕中部可以显示系统菜单，通过设置选项可以进行更多的个性化设置。\n\n"+
				"    点击屏幕两侧或者拖动均可实现翻页，长按屏幕文字可以进行文本选择。\n\n";
		Dialog dialog = new AlertDialog.Builder(context).setTitle("帮助信息").setMessage(m_tips)
				.setPositiveButton("确定",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int whichButton) {
								FBReaderApp.Instance().EnableTipOption.setValue(false);
								dialog.cancel();
							}
						}).setNegativeButton("精品推荐",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								showOfferWall(context);
								dialog.cancel();
							}
						}).create();// 创建按钮
		dialog.show();
	}
	
	public BookChapter loadChapter(final String pathBook)
	{
		// 如果已经有缓存过书本章节信息，则无需重新读取
		BookChapter chapterCache = m_bookChapterCache.get(pathBook);
		if (chapterCache != null) {
			return chapterCache;
		}

		final BookChapter chapter = new BookChapter();

		try {
			// 读取书籍信息
			InputStream input = FBReaderApp.Instance().getBookFile(pathBook + "/chapter.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(input, "gbk"));
			
			String line = "";
	
			int paraCount = 0;
			int textSize = 0;
			int currentJuanIndex = -1;
			int currentChapterIndex = -1;
			
			BookChapter.JuanData juanData = null;
	
			while ((line = reader.readLine()) != null) {
				if (line.charAt(0) == 'j') {
					if (juanData != null) {
						chapter.m_juanData.add(juanData);
					}
	
					juanData = new BookChapter.JuanData();
					juanData.m_juanTitle = line.substring(1);
					++currentJuanIndex;
				} else {
					++currentChapterIndex;
	
					// 加1是补上隐藏的段落终结符
					final int firstA = line.indexOf("@@");
					final int count = Integer.parseInt(line.substring(0, firstA)) + 1;
					BookChapter.BookChapterData data = new BookChapter.BookChapterData();
					data.m_startOffset = paraCount;
					data.m_paragraphCount = count;
					final int secondA = line.indexOf("@@", firstA + 2);
					data.m_textSize = Integer.parseInt(line.substring(firstA + 2, secondA));
					data.m_startTxtOffset = textSize;
					data.m_title = line.substring(secondA + 2);
					data.m_juanIndex = currentJuanIndex;
					chapter.addChapterData(data);
					paraCount += count;
					textSize += data.m_textSize;
					
					juanData.m_juanChapter.add(currentChapterIndex);
				}
			}
			input.close();
			
			if (juanData != null) {
				chapter.m_juanData.add(juanData);
			}
			
			chapter.m_allParagraphNumber = paraCount;
			chapter.m_allTextSize = textSize;
			m_bookChapterCache.put(pathBook, chapter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return chapter;
	}
	
	public void importBook(final Context context, String pathBook, String pathTo, String destFile)
	{
		String sdStatus = Environment.getExternalStorageState();
	    if(!sdStatus.equals(Environment.MEDIA_MOUNTED)) {  
	        UIUtil.showMessageText(context, "发生错误，SD卡未找到。");
	        return;  
	    }

	    try {
	    	final BookChapter chapter = loadChapter(pathBook);
			
			// 拷贝书籍内容
	        File path = new File(pathTo);  
	        File file = new File(pathTo + "/" + destFile);  
	        if( !path.exists()) {
	            path.mkdir();  
	        }
	        
	        if( !file.exists()) {  
	            file.createNewFile();  
	        }

	        FileOutputStream outputStream = new FileOutputStream(file);  

	        int currentJuanIndex = -1;
	        final int bufferSize = 1024 * 500;
	        byte[] buffer = new byte[bufferSize];
	        final int chapterCount = chapter.m_chapterData.size();
	        
	        if (chapter.m_juanData.size() <= 0) {
	        	for (int i = 0; i < chapterCount; ++i) {
					InputStream inputTxt = getBookFile(pathBook + "/" + i + ".txt");
					int size = inputTxt.available();
					if (size > bufferSize) {
						buffer = new byte[size];
					}
					inputTxt.read(buffer);
					outputStream.write(buffer, 0, size);
				}
	        } else {
	        	for (int i = 0; i < chapterCount; ++i) {
					final BookChapter.BookChapterData data = chapter.m_chapterData.get(i);
					final int index = data.m_juanIndex;
					if (index != currentJuanIndex) {
						currentJuanIndex = index;
						final String juanTitle = chapter.m_juanData.get(index).m_juanTitle + "\n";
						outputStream.write(juanTitle.getBytes("gbk"));
					}
					
					InputStream inputTxt = getBookFile(pathBook + "/" + i + ".txt");
					int size = inputTxt.available();
					if (size > bufferSize) {
						buffer = new byte[size];
					}
					inputTxt.read(buffer);
					outputStream.write(buffer, 0, size);
				}
	        }

			outputStream.close();
			costOfferPoints(context, IMPORT_BOOK_POINT);
			UIUtil.showMessageText(context, "导出书籍完毕。");
			MobclickAgent.onEvent(context, "import", "ok");
	    } catch(Exception e) {
	    	UIUtil.showMessageText(context, "发生错误，导出书籍失败。");
	    	String error = e.getMessage();
	    	if (error == null) {
	    		error = "import book fail";
	    	}
	    	MobclickAgent.reportError(context, error); 
	    	MobclickAgent.onEvent(context, "import", "fail");
	        e.printStackTrace();  
	    }  
	}
}
