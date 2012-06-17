package org.geometerplus.android.fbreader;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ListView;

import org.geometerplus.fbreader.library.Bookmark;
import org.socool.socoolreader.reader.R;

public class BookmarksActivity extends Activity {
	Button m_btnChapter;
	Button m_btnBookmark;
	
	ListView m_bookmark;
	ExpandableListView m_chapter;
	
	private static final int PAGE_CHAPTER = 0;
	private static final int PAGE_BOOKMARK = 1;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);

		Thread.setDefaultUncaughtExceptionHandler(new org.geometerplus.zlibrary.error.UncaughtExceptionHandler(this));

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		
		setContentView(R.layout.bookmarks);
		
		m_btnChapter = (Button)findViewById(R.id.book_content);
		m_btnBookmark = (Button)findViewById(R.id.book_mark);
		
		m_chapter = (ExpandableListView)findViewById(R.id.chapterjuan_listview);
		m_bookmark = (ListView)findViewById(R.id.boomark_list);
		
		BookChapterJuanAdapter adapter = new BookChapterJuanAdapter(this);
		m_chapter.setAdapter(adapter);
		m_chapter.setOnChildClickListener(adapter);
		
		m_chapter.expandGroup(adapter.m_currentGroup);
		m_chapter.setSelectedGroup(adapter.m_currentGroup);
		
		BookmarkAdapter adapter2 = new BookmarkAdapter(this);
		m_bookmark.setAdapter(adapter2);
		m_bookmark.setOnItemClickListener(adapter2);
		m_bookmark.setOnCreateContextMenuListener(adapter2);
		
		View.OnClickListener listener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Button btn = (Button)v;
				if (btn == m_btnChapter) {
					gotoPage(PAGE_CHAPTER);
				} else if (btn == m_btnBookmark){
					gotoPage(PAGE_BOOKMARK);
				}
			}
		};
		
		m_btnChapter.setOnClickListener(listener);
		m_btnBookmark.setOnClickListener(listener);

		gotoPage(PAGE_CHAPTER);
	}
	
	public void gotoPage(int page)
	{
		if (page == PAGE_CHAPTER) {
			m_btnChapter.setSelected(true);
			m_btnBookmark.setSelected(false);
			
			m_chapter.setVisibility(View.VISIBLE);
			m_bookmark.setVisibility(View.GONE);
		} else if (page == PAGE_BOOKMARK) {
			m_btnChapter.setSelected(false);
			m_btnBookmark.setSelected(true);
			
			m_chapter.setVisibility(View.GONE);
			m_bookmark.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		final ListView view = (ListView)findViewById(R.id.boomark_list);
		final Bookmark bookmark = (Bookmark) ((BookmarkAdapter)view.getAdapter()).getItem(position);
		final BookmarkAdapter adapter = (BookmarkAdapter)view.getAdapter();
		switch (item.getItemId()) {
			case BookmarkAdapter.OPEN_ITEM_ID:
				adapter.gotoBookmark(bookmark);
				return true;
			case BookmarkAdapter.DELETE_ITEM_ID:
				bookmark.delete();
				adapter.AllBooksBookmarks.remove(bookmark);
				view.invalidate();
				view.requestLayout();
				return true;
		}
		return super.onContextItemSelected(item);
	}
}
