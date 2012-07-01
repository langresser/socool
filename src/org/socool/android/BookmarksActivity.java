package org.socool.android;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.ListView;

import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.library.Bookmark;
import org.socool.socoolreader.reader.R;

import com.umeng.analytics.MobclickAgent;

public class BookmarksActivity extends Activity {
	Button m_btnChapter;
	Button m_btnBookmark;
	Button m_btnComment;
	ImageButton m_btnApp;
	
	ListView m_bookmark;
	ListView m_bookComment;
	ExpandableListView m_chapter;
	
	private static final int PAGE_CHAPTER = 0;
	private static final int PAGE_BOOKMARK = 1;
	private static final int PAGE_COMMENT = 2;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);
		
		setContentView(R.layout.bookmarks);
		
		m_btnChapter = (Button)findViewById(R.id.book_content);
		m_btnBookmark = (Button)findViewById(R.id.book_mark);
		m_btnComment = (Button)findViewById(R.id.book_comment);
		m_btnApp = (ImageButton)findViewById(R.id.app_button);
		
		m_chapter = (ExpandableListView)findViewById(R.id.chapterjuan_listview);
		m_bookmark = (ListView)findViewById(R.id.boomark_list);
		m_bookComment = (ListView)findViewById(R.id.bookcomment_list);
		
		BookChapterJuanAdapter adapter = new BookChapterJuanAdapter(this, 
				FBReaderApp.Instance().Model.m_chapter, FBReaderApp.Instance().BookTextView.getCurrentChapter(), null);
		m_chapter.setAdapter(adapter);
		m_chapter.setOnChildClickListener(adapter);
		
		m_chapter.expandGroup(adapter.m_currentGroup);
		m_chapter.setSelectedGroup(adapter.m_currentGroup);
		
		BookmarkAdapter adapter2 = new BookmarkAdapter(this);
		m_bookmark.setAdapter(adapter2);
		m_bookmark.setOnItemClickListener(adapter2);
		m_bookmark.setOnCreateContextMenuListener(adapter2);
		
		BookCommentAdapter adapter3 = new BookCommentAdapter(this);
		m_bookComment.setAdapter(adapter3);
		m_bookComment.setOnItemClickListener(adapter3);
		m_bookComment.setOnCreateContextMenuListener(adapter3);
		
		View.OnClickListener listener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (v == m_btnChapter) {
					gotoPage(PAGE_CHAPTER);
				} else if (v == m_btnBookmark){
					gotoPage(PAGE_BOOKMARK);
				} else if (v == m_btnComment) {
					gotoPage(PAGE_COMMENT);
				} else if (v == m_btnApp) {
					FBReaderApp.Instance().showOfferWall(BookmarksActivity.this);
				}
			}
		};
		
		m_btnChapter.setOnClickListener(listener);
		m_btnBookmark.setOnClickListener(listener);
		m_btnComment.setOnClickListener(listener);
		m_btnApp.setOnClickListener(listener);

		gotoPage(PAGE_CHAPTER);
	}
	
	public void gotoPage(int page)
	{
		MobclickAgent.onEvent(this, "chapterList", "" + page);
		if (page == PAGE_CHAPTER) {
			m_btnChapter.setSelected(true);
			m_btnBookmark.setSelected(false);
			m_btnComment.setSelected(false);
			
			m_chapter.setVisibility(View.VISIBLE);
			m_bookmark.setVisibility(View.GONE);
			m_bookComment.setVisibility(View.GONE);
		} else if (page == PAGE_BOOKMARK) {
			m_btnChapter.setSelected(false);
			m_btnBookmark.setSelected(true);
			m_btnComment.setSelected(false);
			
			m_chapter.setVisibility(View.GONE);
			m_bookmark.setVisibility(View.VISIBLE);
			m_bookComment.setVisibility(View.GONE);
		} else if (page == PAGE_COMMENT) {
			m_btnChapter.setSelected(false);
			m_btnBookmark.setSelected(false);
			m_btnComment.setSelected(true);
			
			m_chapter.setVisibility(View.GONE);
			m_bookmark.setVisibility(View.GONE);
			m_bookComment.setVisibility(View.VISIBLE);
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		final int position = ((AdapterView.AdapterContextMenuInfo)item.getMenuInfo()).position;
		
		if (m_bookmark.getVisibility() == View.VISIBLE) {
			final BookmarkAdapter adapter = (BookmarkAdapter)m_bookmark.getAdapter();
			final Bookmark bookmark = (Bookmark)adapter.getItem(position);
			
			switch (item.getItemId()) {
				case BookmarkAdapter.OPEN_ITEM_ID:
					adapter.gotoBookmark(bookmark);
					return true;
				case BookmarkAdapter.DELETE_ITEM_ID:
					bookmark.delete();
					adapter.m_bookmarks.remove(bookmark);
					adapter.notifyDataSetChanged();
					return true;
			}	
		} else if (m_bookComment.getVisibility() == View.VISIBLE) {
			final BookCommentAdapter adapter = (BookCommentAdapter)m_bookComment.getAdapter();
			final Bookmark bookmark = (Bookmark)adapter.getItem(position);
			
			switch (item.getItemId()) {
				case BookmarkAdapter.OPEN_ITEM_ID:
					adapter.gotoBookmark(bookmark);
					return true;
				case BookmarkAdapter.DELETE_ITEM_ID:
					bookmark.delete();
					adapter.m_bookmarks.remove(bookmark);
					adapter.notifyDataSetChanged();
					return true;
			}
		}
		
		return super.onContextItemSelected(item);
	}
	
	public void onResume() {
	    super.onResume();
	    MobclickAgent.onResume(this);
	}
	public void onPause() {
	    super.onPause();
	    MobclickAgent.onPause(this);
	}
}
