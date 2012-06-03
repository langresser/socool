package org.geometerplus.android.fbreader.bookshelf;

import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.widget.Toast;

import org.geometerplus.android.fbreader.BookInfoActivity;
import org.geometerplus.android.fbreader.SCReaderActivity;
import org.geometerplus.android.fbreader.covers.CoverManager;
import org.geometerplus.android.fbreader.util.UIUtil;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.filesystem.ZLResource;
import org.socool.socoolreader.reader.R;

public class ShelvesActivity extends Activity implements FBReaderApp.ChangeListener {
    private boolean mPendingCoversUpdate;
 
    // 导入和添加数据时的提示进度界面
    private ShelvesView mGrid;
    
    boolean m_realExit = false;
	public ArrayList<Book> m_bookList;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 0);
        
        FBReaderApp.Instance().addChangeListener(this);
        FBReaderApp.Instance().startBuild();
        
        m_bookList = new ArrayList<Book>();
        
        Book book1 = Book.getByPath("book/wxkb");		

        m_bookList.add(book1);

        setContentView(R.layout.screen_shelves);
        getWindow().setBackgroundDrawable(null);

        final BooksAdapter adapter = new BooksAdapter(this);

        mGrid = (ShelvesView) findViewById(R.id.grid_shelves);

        final ShelvesView grid = mGrid;
        grid.setTextFilterEnabled(true);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new BookViewer());

        registerForContextMenu(grid);
    }
    
 // 当书籍信息改变时调用
 	public void onLibraryChanged(final Code code) {
 		runOnUiThread(new Runnable() {
 			public void run() {
 				switch (code) {
 					case StatusChanged:
 						setProgressBarIndeterminateVisibility(!FBReaderApp.Instance().isUpToDate());
 						break;
 					case Found:
// 						openSearchResults();
 						break;
 					case NotFound:
 						UIUtil.showErrorMessage(ShelvesActivity.this, "bookNotFound");
 						break;
 				}
 			}
 		});
 	}
 	
 	//
	// Book deletion
	//
	private class BookDeleter implements DialogInterface.OnClickListener {
		private final Book myBook;
		private final int myMode;

		BookDeleter(Book book, int removeMode) {
			myBook = book;
			myMode = removeMode;
		}

		public void onClick(DialogInterface dialog, int which) {
			deleteBook(myBook, myMode);
		}
	}

	private void tryToDeleteBook(Book book) {
		final ZLResource dialogResource = ZLResource.resource("dialog");
		final ZLResource buttonResource = dialogResource.getResource("button");
		final ZLResource boxResource = dialogResource.getResource("deleteBookBox");
		new AlertDialog.Builder(this)
			.setTitle(book.myTitle)
			.setMessage(boxResource.getResource("message").getValue())
			.setIcon(0)
			.setPositiveButton(buttonResource.getResource("yes").getValue(), new BookDeleter(book, FBReaderApp.REMOVE_FROM_DISK))
			.setNegativeButton(buttonResource.getResource("no").getValue(), null)
			.create().show();
	}

	private void deleteBook(Book book, int mode) {
		FBReaderApp.Instance().removeBook(book, mode);

		// TODO refresh
	}

    boolean isPendingCoversUpdate() {
        return mPendingCoversUpdate;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        m_realExit = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        CoverManager.cleanupCache();
        FBReaderApp.Instance().removeChangeListener(this);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    // 被来电中断应用，保存信息
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	addMenuItem(menu, 1, "localSearch", R.drawable.ic_menu_search);
        return super.onCreateOptionsMenu(menu);
    }
    
    private MenuItem addMenuItem(Menu menu, int index, String resourceKey, int iconId) {
		final String label = ZLResource.resource("library").getResource("menu").getResource(resourceKey).getValue();
		final MenuItem item = menu.add(0, index, Menu.NONE, label);
		item.setIcon(iconId);
		return item;
	}

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
    	if (item.getItemId() == 1) {
    		return onSearchRequested();
    	}
  
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            return onSearchRequested();
        }
        return super.onKeyUp(keyCode, event);
    }
    
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (m_realExit == false) {
    			m_realExit = true;
    			
    			Toast.makeText(getApplicationContext(), getResources().getString(R.string.real_exit_tip), Toast.LENGTH_SHORT).show();
    			return true;
    		} else {
    			if (FBReaderApp.Instance() != null) {
    				FBReaderApp.Instance().closeWindow();
    			}
    			
            	finish();
            	return true;
    		}
    	} else {
    		return super.onKeyDown(keyCode, event);
    	}
	}

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenu.ContextMenuInfo menuInfo) {

    	final int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
		final Book book = null;//((LibraryTree)getListAdapter().getItem(position)).getBook();
		if (book != null) {
			final ZLResource resource = ZLResource.resource("library");
			menu.setHeaderTitle(book.myTitle);
			menu.add(0, OPEN_BOOK_ITEM_ID, 0, resource.getResource("openBook").getValue());
			menu.add(0, SHOW_BOOK_INFO_ITEM_ID, 0, resource.getResource("showBookInfo").getValue());
			menu.add(0, SHARE_BOOK_ITEM_ID, 0, resource.getResource("shareBook").getValue());
			menu.add(0, DELETE_BOOK_ITEM_ID, 0, resource.getResource("deleteBook").getValue());
		}

        super.onCreateContextMenu(menu, v, menuInfo);
    }

    private static final int OPEN_BOOK_ITEM_ID = 0;
	private static final int SHOW_BOOK_INFO_ITEM_ID = 1;
	private static final int SHARE_BOOK_ITEM_ID = 2;
	private static final int DELETE_BOOK_ITEM_ID = 3;

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		final Book book = null;//((LibraryTree)getListAdapter().getItem(position)).getBook();
		if (book != null) {
			switch (item.getItemId()) {
			case OPEN_BOOK_ITEM_ID:
				openBook(book);
				return true;
			case SHOW_BOOK_INFO_ITEM_ID:
				showBookInfo(book);
				return true;
			case SHARE_BOOK_ITEM_ID:
				UIUtil.shareBook(this, book);
				return true;
			case DELETE_BOOK_ITEM_ID:
				tryToDeleteBook(book);
				return true;
			}
		}
        return super.onContextItemSelected(item);
    }
    
    //
	// show BookInfoActivity
	//
	private static final int BOOK_INFO_REQUEST = 1;

	protected void showBookInfo(Book book) {
		startActivityForResult(
			new Intent(getApplicationContext(), BookInfoActivity.class)
				.putExtra(BookInfoActivity.CURRENT_BOOK_PATH_KEY, book.m_filePath),
			BOOK_INFO_REQUEST
		);
	}
    
    private void openBook(Book book) {
		startActivity(
			new Intent(getApplicationContext(), SCReaderActivity.class)
				.setAction(Intent.ACTION_VIEW)
				.putExtra(SCReaderActivity.BOOK_PATH_KEY, book.m_filePath)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		);
	}


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == BOOK_INFO_REQUEST && data != null) {
			final String path = data.getStringExtra(BookInfoActivity.CURRENT_BOOK_PATH_KEY);
			final Book book = Book.getByFile(ZLFile.createFileByPath(path));
			FBReaderApp.Instance().refreshBookInfo(book);
//			TODO refresh
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
    }

    private class BookViewer implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	Book book = m_bookList.get(position);
    		 startActivity(new Intent(getApplicationContext(), SCReaderActivity.class)
    		 			.putExtra(SCReaderActivity.BOOK_PATH_KEY, book.m_filePath)
        				.setAction(Intent.ACTION_VIEW));
        }
    }
}
