package org.geometerplus.android.fbreader;

import java.text.SimpleDateFormat;
import java.util.List;

import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.fbreader.library.Bookmark;
import org.socool.socoolreader.reader.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BookmarkActivity extends Activity {

	private static final int OPEN_ITEM_ID = 0;
	private static final int DELETE_ITEM_ID = 2;

	List<Bookmark> AllBooksBookmarks;
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.reader_bookmark_page);
		
		AllBooksBookmarks = FBReaderApp.Instance().getDatabase().loadBookmarks(FBReaderApp.Instance().Model.Book.myId);

		RelativeLayout noneLayout = (RelativeLayout)findViewById(R.id.reader_bookmark_cover);

		ListView listView = (ListView)findViewById(R.id.reader_boomark_list);
		BookmarkAdapter adapter = new BookmarkAdapter(this);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(adapter);
		listView.setOnCreateContextMenuListener(adapter);
		noneLayout.setVisibility(View.GONE);
	}
	
	private void gotoBookmark(Bookmark bookmark)
	{
		if (bookmark == null) {
			return;
		}

		final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
		final long bookId = bookmark.getBookId();
		if ((fbreader.Model == null) || (fbreader.Model.Book.myId != bookId)) {
			final Book book = Book.getById(bookId);
			if (book != null) {
				finish();
				fbreader.openBook(book, bookmark, null);
			}
		} else {
			finish();
			fbreader.gotoBookmark(bookmark);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		final ListView view = (ListView)findViewById(R.id.reader_boomark_list);
		final Bookmark bookmark = (Bookmark) ((BookmarkAdapter)view.getAdapter()).getItem(position);
		switch (item.getItemId()) {
			case OPEN_ITEM_ID:
				gotoBookmark(bookmark);
				return true;
			case DELETE_ITEM_ID:
				bookmark.delete();
				AllBooksBookmarks.remove(bookmark);
				view.invalidate();
				view.requestLayout();
				return true;
		}
		return super.onContextItemSelected(item);
	}
	
	private class BookmarkAdapter extends BaseAdapter
				implements AdapterView.OnItemClickListener, View.OnCreateContextMenuListener{
        private LayoutInflater mInflater;

        public BookmarkAdapter(Context context) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);
         }

        public int getCount() {
            return AllBooksBookmarks.size();
        }
        
        public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
			final int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
			final Bookmark bookmark = (Bookmark) getItem(position);
			if (bookmark != null) {
				menu.setHeaderTitle(bookmark.myText);
				menu.add(0, OPEN_ITEM_ID, 0, "Ìø×ª");
				menu.add(0, DELETE_ITEM_ID, 0, "É¾³ý´ËÊéÇ©");
			}
		}
        
        public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			final Bookmark bookmark = (Bookmark) getItem(position);
			gotoBookmark(bookmark);
		}

        public Object getItem(int position) {
        	final int count = AllBooksBookmarks.size();
        	if (position > count - 1) {
        		position = count - 1;
        	}
        	if (position < 0) {
        		position = 0;
        	}

            return AllBooksBookmarks.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.reader_bookmark_item, null);

                holder = new ViewHolder();
                holder.textBookmark = (TextView)convertView.findViewById(R.id.reader_bookmark_item_content);
                holder.textPercent = (TextView)convertView.findViewById(R.id.reader_bookmark_item_percent);
                holder.textTime = (TextView)convertView.findViewById(R.id.reader_bookmark_item_time);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            Bookmark bookmark = AllBooksBookmarks.get(position);
            holder.textBookmark.setText(bookmark.myText);
            final String time = format.format(bookmark.getTime());
            holder.textTime.setText(time);
            holder.textPercent.setText(String.format("%1$.2f%%", bookmark.m_percent / 100.0));

            return convertView;
        }

        class ViewHolder {
            TextView textBookmark;
            TextView textTime;
            TextView textPercent;
        }
    }
}
