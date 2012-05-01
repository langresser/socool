package org.geometerplus.android.fbreader;

import org.geometerplus.android.fbreader.util.UIUtil;
import org.geometerplus.fbreader.FBTree;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.BooksDatabase;
import org.geometerplus.fbreader.library.LibraryUtil;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.options.ZLStringOption;
import org.geometerplus.zlibrary.resources.ZLResource;
import org.socool.socoolreader.reader.R;

import android.app.Activity;  
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;  
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;  
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;  
import android.view.ViewGroup;  
import android.view.Window;  
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;  
import android.widget.Button;
import android.widget.ListView;  
import android.widget.Toast;
  
public class BookShelfActivity extends Activity 
							   implements MenuItem.OnMenuItemClickListener, 
							   View.OnCreateContextMenuListener, 
							   FBReaderApp.ChangeListener { 
	final int BOOK_NUM_PER_LINE = 3;
    /** Called when the activity is first created. */  
    private ListView shelf_list;  
    // ��ܵ�����  
    int[] size = new int[5];
    boolean m_realExit = false;
    
    static volatile boolean ourToBeKilled = false;

	public static final String SELECTED_BOOK_PATH_KEY = "SelectedBookPath";
  
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 0);
        
        setContentView(R.layout.book_shelf_layout);
		
        FBReaderApp.Instance().addChangeListener(this);
        FBReaderApp.Instance().startBuild();
  
        shelf_list = (ListView) findViewById(R.id.shelf_list);  
          
        ShelfAdapter adapter = new ShelfAdapter();  
        adapter.m_rootActivity = this;
        shelf_list.setAdapter(adapter);
    }

	@Override
	public void onPause() {
		super.onPause();
		ourToBeKilled = true;
	}

	@Override
	protected void onDestroy() {
		FBReaderApp.Instance().removeChangeListener(this);
		super.onDestroy();
	}

	//
	// show BookInfoActivity
	//
	private static final int BOOK_INFO_REQUEST = 1;

	protected void showBookInfo(Book book) {
		startActivityForResult(
			new Intent(getApplicationContext(), BookInfoActivity.class)
				.putExtra(BookInfoActivity.CURRENT_BOOK_PATH_KEY, book.File.getPath()),
			BOOK_INFO_REQUEST
		);
	}

	@Override
	protected void onActivityResult(int requestCode, int returnCode, Intent intent) {
		if (requestCode == BOOK_INFO_REQUEST && intent != null) {
			final String path = intent.getStringExtra(BookInfoActivity.CURRENT_BOOK_PATH_KEY);
			final Book book = Book.getByFile(ZLFile.createFileByPath(path));
			FBReaderApp.Instance().refreshBookInfo(book);
//			TODO refresh
		} else {
			super.onActivityResult(requestCode, returnCode, intent);
		}
	} 

	//
	// Search
	//
	static final ZLStringOption BookSearchPatternOption =
		new ZLStringOption("BookSearch", "Pattern", "");

	private void openSearchResults() {
		final FBTree tree = FBReaderApp.Instance().getRootTree().getSubTree(FBReaderApp.ROOT_FOUND);
		if (tree != null) {
//			openTree(tree);
		}
	}

	@Override
	public boolean onSearchRequested() {
		startSearch(BookSearchPatternOption.getValue(), true, null, false);
		return true;
	}

	//
	// Context menu
	//
	private static final int OPEN_BOOK_ITEM_ID = 0;
	private static final int SHOW_BOOK_INFO_ITEM_ID = 1;
	private static final int SHARE_BOOK_ITEM_ID = 2;
	private static final int DELETE_BOOK_ITEM_ID = 3;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
		final int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
		final Book book = null;//((LibraryTree)getListAdapter().getItem(position)).getBook();
		if (book != null) {
			final ZLResource resource = LibraryUtil.resource();
			menu.setHeaderTitle(book.getTitle());
			menu.add(0, OPEN_BOOK_ITEM_ID, 0, resource.getResource("openBook").getValue());
			menu.add(0, SHOW_BOOK_INFO_ITEM_ID, 0, resource.getResource("showBookInfo").getValue());
			if (book.File.getPhysicalFile() != null) {
				menu.add(0, SHARE_BOOK_ITEM_ID, 0, resource.getResource("shareBook").getValue());
			}

			if (FBReaderApp.Instance().canRemoveBookFile(book)) {
				menu.add(0, DELETE_BOOK_ITEM_ID, 0, resource.getResource("deleteBook").getValue());
			}
		}
	}

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

	private void openBook(Book book) {
		startActivity(
			new Intent(getApplicationContext(), SCReaderActivity.class)
				.setAction(Intent.ACTION_VIEW)
				.putExtra(SCReaderActivity.BOOK_PATH_KEY, book.File.getPath())
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		);
	}

	//
	// Options menu
	//

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		addMenuItem(menu, 1, "localSearch", R.drawable.ic_menu_search);
		return true;
	}

	private MenuItem addMenuItem(Menu menu, int index, String resourceKey, int iconId) {
		final String label = LibraryUtil.resource().getResource("menu").getResource(resourceKey).getValue();
		final MenuItem item = menu.add(0, index, Menu.NONE, label);
		item.setOnMenuItemClickListener(this);
		item.setIcon(iconId);
		return item;
	}

	public boolean onMenuItemClick(MenuItem item) {
		switch (item.getItemId()) {
			case 1:
				return onSearchRequested();
			default:
				return true;
		}
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
			.setTitle(book.getTitle())
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

	// ���鼮��Ϣ�ı�ʱ����
	public void onLibraryChanged(final Code code) {
		runOnUiThread(new Runnable() {
			public void run() {
				switch (code) {
					case StatusChanged:
						setProgressBarIndeterminateVisibility(!FBReaderApp.Instance().isUpToDate());
						break;
					case Found:
						openSearchResults();
						break;
					case NotFound:
						UIUtil.showErrorMessage(BookShelfActivity.this, "bookNotFound");
						break;
				}
			}
		});
	}
    
    public View.OnClickListener clickListener = new View.OnClickListener()
    {
    	public void onClick(View view)
    	{
		 startActivity(new Intent(getApplicationContext(), SCReaderActivity.class)
    				.setAction(Intent.ACTION_VIEW));
//		 finish();
    	}
    };
    
    @Override
    public void onResume() {
		super.onResume();
		
		m_realExit = false;
    }
      
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (m_realExit == false) {
    			m_realExit = true;
    			
    			Toast.makeText(getApplicationContext(), getResources().getString(R.string.real_exit_tip), Toast.LENGTH_SHORT).show();
    			
//    			Timer timerExit = new Timer();
//    			timerExit.schedule(new TimerTask() {
//    		        @Override  
//    		        public void run() {  
//    		        	m_realExit = false;
//    		        }  
//    		    } , 5000);	// 5���Ӻ�ָ���־
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
 
    public class ShelfAdapter extends BaseAdapter {
    	public BookShelfActivity m_rootActivity;

        @Override  
        public int getCount() {  
            return size.length;  
        }  
  
        @Override  
        public Object getItem(int position) {  
            return size[position];  
        }  
  
        @Override  
        public long getItemId(int position) {  
            return position;  
        }  
  
        @Override  
        public View getView(int position, View convertView, ViewGroup parent) {  
            View layout = LayoutInflater.from(getApplicationContext()).inflate(  
                       R.layout.book_shelf_item, null);  
            
            Button book1 = (Button)layout.findViewById(R.id.button_1);
            Button book2 = (Button)layout.findViewById(R.id.button_2);
            Button book3 = (Button)layout.findViewById(R.id.button_3);
            
            book1.setOnClickListener(m_rootActivity.clickListener);
            book2.setOnClickListener(m_rootActivity.clickListener);
            book3.setOnClickListener(m_rootActivity.clickListener);
            
            return layout;  
        }
    }
}  

