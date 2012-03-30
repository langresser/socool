package org.lancer.android.fbreader;

import org.lancer.zlibrary.ui.android.library.ZLAndroidApplication;

public class FBReaderApplication extends ZLAndroidApplication {
	@Override
	public void onCreate() {
		super.onCreate();
		//bindService(new Intent(this, LibraryService.class), null, LibraryService.BIND_AUTO_CREATE);
	}
}
