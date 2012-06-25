package org.socool.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.library.Book;
import org.socool.screader.library.Bookmark;
import org.socool.socoolreader.mcnxs.R;

import android.app.Activity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class BookCommentAdapter extends BaseAdapter implements
		AdapterView.OnItemClickListener, View.OnCreateContextMenuListener {
	public static final int OPEN_ITEM_ID = 0;
	public static final int DELETE_ITEM_ID = 2;
	private LayoutInflater mInflater;
	ArrayList<Bookmark> m_bookmarks = new ArrayList<Bookmark>();
	Activity m_baseActivity;
	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public BookCommentAdapter(Activity activity) {
		// Cache the LayoutInflate to avoid asking for a new one each time.
		mInflater = LayoutInflater.from(activity);
		m_baseActivity = activity;
		List<Bookmark> AllBooksBookmarks = FBReaderApp.Instance().getDatabase()
				.loadBookmarks(FBReaderApp.Instance().Model.Book.myId);
		for (Bookmark bookmark : AllBooksBookmarks) {
			if (bookmark.m_bookmarkType == Bookmark.BOOKMARK_TYPE_COMMENT) {
				m_bookmarks.add(bookmark);
			}
		}
		ComparatorBookmark comp = new ComparatorBookmark();
		Collections.sort(m_bookmarks, comp);
	}

	public int getCount() {
		return m_bookmarks.size();
	}

	public void onCreateContextMenu(ContextMenu menu, View view,
			ContextMenu.ContextMenuInfo menuInfo) {
		final int position = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
		final Bookmark bookmark = (Bookmark) getItem(position);
		if (bookmark != null) {
			menu.setHeaderTitle(bookmark.myText);
			menu.add(0, OPEN_ITEM_ID, 0, "Ìø×ª");
			menu.add(0, DELETE_ITEM_ID, 0, "É¾³ý´ËÊéÇ©");
		}
	}
	
	public void gotoBookmark(Bookmark bookmark)
	{
		if (bookmark == null) {
			return;
		}

		final FBReaderApp fbreader = (FBReaderApp)FBReaderApp.Instance();
		final long bookId = bookmark.getBookId();
		if ((fbreader.Model == null) || (fbreader.Model.Book.myId != bookId)) {
			final Book book = Book.getById(bookId);
			if (book != null) {
				m_baseActivity.finish();
				fbreader.openBook(book, bookmark, null);
			}
		} else {
			m_baseActivity.finish();
			fbreader.gotoBookmark(bookmark);
		}
	}

	public final void onItemClick(AdapterView<?> parent, View view,
			int position, long id) {
		final Bookmark bookmark = (Bookmark) getItem(position);
		gotoBookmark(bookmark);
	}

	public Object getItem(int position) {
		final int count = m_bookmarks.size();
		if (position > count - 1) {
			position = count - 1;
		}
		if (position < 0) {
			position = 0;
		}

		return m_bookmarks.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater
					.inflate(R.layout.reader_bookmark_item, null);

			holder = new ViewHolder();
			holder.textBookmark = (TextView) convertView
					.findViewById(R.id.reader_bookmark_item_content);
			holder.textPercent = (TextView) convertView
					.findViewById(R.id.reader_bookmark_item_percent);
			holder.textTime = (TextView) convertView
					.findViewById(R.id.reader_bookmark_item_time);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		Bookmark bookmark = m_bookmarks.get(position);
		holder.textBookmark.setText(bookmark.myText);
		final String time = format.format(bookmark.getTime());
		holder.textTime.setText(time);
		holder.textPercent.setText(String.format("%1$.2f%%",
				bookmark.m_percent / 100.0));

		return convertView;
	}

	class ViewHolder {
		TextView textBookmark;
		TextView textTime;
		TextView textPercent;
	}
}