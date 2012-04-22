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

package org.geometerplus.zlibrary.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import org.geometerplus.android.fbreader.SCReaderActivity;
import org.geometerplus.fbreader.fbreader.ScrollingPreferences;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.filesystem.ZLResourceFile;
import org.geometerplus.zlibrary.options.ZLBooleanOption;
import org.geometerplus.zlibrary.options.ZLIntegerRangeOption;
import org.geometerplus.zlibrary.options.ZLStringOption;
import org.geometerplus.zlibrary.text.view.ZLTextView;
import org.geometerplus.zlibrary.view.ZLViewWidget;
import org.geometerplus.zlibrary.view.ZLGLWidget;
import org.socool.socoolreader.reader.R;

import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.res.AssetFileDescriptor;
import android.os.Build;
import android.graphics.Point;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;

public class ZLibrary {
	public static ZLibrary Instance() {
		return ourImplementation;
	}
		
	private static ZLibrary ourImplementation;

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

	public ZLibrary(Application application) {
		ourImplementation = this;
		myApplication = application;
	}

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
}
