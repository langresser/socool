package org.geometerplus.android.fbreader;

import java.util.ArrayList;
import java.util.Map;

import org.geometerplus.android.fbreader.util.UIUtil;
import org.geometerplus.fbreader.FBTree;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.BooksDatabase;
import org.geometerplus.fbreader.library.FileInfoSet;
import org.geometerplus.fbreader.library.LibraryUtil;
import org.geometerplus.zlibrary.filesystem.ZLFile;
import org.geometerplus.zlibrary.image.ZLImage;
import org.geometerplus.zlibrary.image.ZLImageData;
import org.geometerplus.zlibrary.image.ZLImageManager;
import org.geometerplus.zlibrary.image.ZLLoadableImage;
import org.geometerplus.zlibrary.options.ZLStringOption;
import org.geometerplus.zlibrary.resources.ZLResource;
import org.socool.socoolreader.reader.R;

import android.app.Activity;  
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;  
import android.util.DisplayMetrics;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;  
import android.widget.Toast;
  
public class BookShelfActivity extends Activity 
							   implements MenuItem.OnMenuItemClickListener, 
							   View.OnCreateContextMenuListener, 
							   FBReaderApp.ChangeListener { 
	final int BOOK_NUM_PER_LINE = 3;
    /** Called when the activity is first created. */  
    private ListView shelf_list;  
    // 书架的列数  
    int[] size = new int[5];
    boolean m_realExit = false;
    
    static volatile boolean ourToBeKilled = false;

	public static final String SELECTED_BOOK_PATH_KEY = "SelectedBookPath";
	
	private ArrayList<Book> m_bookList;
  
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 0);
        
        setContentView(R.layout.book_shelf_layout);
		
        FBReaderApp.Instance().addChangeListener(this);
        FBReaderApp.Instance().startBuild();
        
        final FileInfoSet fileInfos = new FileInfoSet();
        final Map<Long,Book> books = FBReaderApp.Instance().getDatabase().loadBooks(fileInfos);
        m_bookList = new ArrayList<Book>();
        
        for (Book book : books.values()) {
        	m_bookList.add(book);
        }
  
        shelf_list = (ListView) findViewById(R.id.shelf_list);  
          
        ShelfAdapter adapter = new ShelfAdapter();  
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

	// 当书籍信息改变时调用
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
    	int index = (Integer) view.getTag();
    	Book book = m_bookList.get(index);
		 startActivity(new Intent(getApplicationContext(), SCReaderActivity.class)
		 			.putExtra(SCReaderActivity.BOOK_PATH_KEY, book.File.getPath())
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
//    		    } , 5000);	// 5秒钟后恢复标志
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
        @Override  
        public int getCount() {
        	int shelf = (int)Math.ceil((double)m_bookList.size() / (double)3);
            return Math.max(shelf, 4);  
        }  
  
        @Override
        public Object getItem(int position) {
            return null;  
        }  
  
        @Override  
        public long getItemId(int position) {  
            return position;
        }
  
        @Override  
        public View getView(int position, View convertView, ViewGroup parent) {  
            View layout = LayoutInflater.from(getApplicationContext()).inflate(  
                       R.layout.book_shelf_item, null);
            
            ImageView book1 = (ImageView)layout.findViewById(R.id.button_1);
            updateBookButton(book1, position * 3 + 0);
            ImageView book2 = (ImageView)layout.findViewById(R.id.button_2);
            updateBookButton(book2, position * 3 + 1);
            ImageView book3 = (ImageView)layout.findViewById(R.id.button_3);
            updateBookButton(book3, position * 3 + 2);
             
            return layout;  
        }
        
        void updateBookButton(ImageView button, int index)
        {
        	button.setTag(index);
        	button.setOnClickListener(clickListener);
        	
        	if (index >= m_bookList.size()) {
        		// 先清空原button书籍
            	button.setVisibility(View.GONE);
        		button.setImageDrawable(null);
        		return;
        	}

        	Book book = m_bookList.get(index);
        	
        	if (!FBReaderApp.Instance().hasCustomCover(book.File)) {
        		button.setImageResource(FBReaderApp.Instance().getCoverResourceId(book.File));
        		return;
        	}

    		final ZLImage image = book.getCover();

    		if (image == null) {
    			return;
    		}

    		if (image instanceof ZLLoadableImage) {
    			final ZLLoadableImage loadableImage = (ZLLoadableImage)image;
    			if (!loadableImage.isSynchronized()) {
    				loadableImage.synchronize();
    			}
    		}
    		final ZLImageData data = ZLImageManager.Instance().getImageData(image);
    		
    		if (data == null) {
    			return;
    		}

    		button.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    		int width = button.getWidth();
    		int height = button.getHeight();
    		final Bitmap coverBitmap = data.getBitmap(width, height);
    		if (coverBitmap == null) {
    			return;
    		}

    		button.setVisibility(View.VISIBLE);
    		button.setImageBitmap(coverBitmap);
        }
    }
    
    
}  

