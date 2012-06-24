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
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.reader_bookmark_page);

		ListView listView = (ListView)findViewById(R.id.reader_boomark_list);
		BookmarkAdapter adapter = new BookmarkAdapter(this);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(adapter);
		listView.setOnCreateContextMenuListener(adapter);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		final ListView view = (ListView)findViewById(R.id.reader_boomark_list);
		final Bookmark bookmark = (Bookmark) ((BookmarkAdapter)view.getAdapter()).getItem(position);
		final BookmarkAdapter adapter = (BookmarkAdapter)view.getAdapter();
		switch (item.getItemId()) {
			case BookmarkAdapter.OPEN_ITEM_ID:
				adapter.gotoBookmark(bookmark);
				return true;
			case BookmarkAdapter.DELETE_ITEM_ID:
				bookmark.delete();
				adapter.m_bookmarks.remove(bookmark);
				view.invalidate();
				view.requestLayout();
				return true;
		}
		return super.onContextItemSelected(item);
	}
}
