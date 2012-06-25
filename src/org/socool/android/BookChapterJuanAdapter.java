package org.socool.android;

import org.socool.screader.bookmodel.BookChapter;
import org.socool.screader.screader.FBReaderApp;
import org.socool.socoolreader.mcnxs.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

public class BookChapterJuanAdapter extends BaseExpandableListAdapter
		implements ExpandableListView.OnChildClickListener {
	private LayoutInflater mInflater;
	final private BookChapter m_chapter = FBReaderApp.Instance().Model.m_chapter;
	final public int m_currentChapter = FBReaderApp.Instance().BookTextView
			.getCurrentChapter();
	final public int m_currentGroup;
	final Activity m_baseActivity;

	public BookChapterJuanAdapter(Activity activity) {
		// Cache the LayoutInflate to avoid asking for a new one each time.
		mInflater = LayoutInflater.from(activity);
		m_baseActivity = activity;
		m_currentGroup = m_chapter.getChapter(m_currentChapter).m_juanIndex;
	}

	public Object getChild(int groupPosition, int childPosition) {
		final BookChapter.JuanData data = (BookChapter.JuanData) getGroup(groupPosition);
		return data.m_juanChapter.get(childPosition);
	}

	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	public int getChildrenCount(int groupPosition) {
		final BookChapter.JuanData data = (BookChapter.JuanData) getGroup(groupPosition);
		return data.m_juanChapter.size();
	}

	class ViewHolder {
		TextView title;
		TextView percent;
	}

	public View getChildView(int groupPosition, int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.reader_chapterjuan_item,
					null);

			holder = new ViewHolder();
			holder.title = (TextView) convertView
					.findViewById(R.id.reader_chapterjuan_item_content);
			holder.percent = (TextView) convertView
					.findViewById(R.id.reader_chapterjuan_item_percent);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		int chapterIndex = (Integer) getChild(groupPosition, childPosition);

		if (m_currentChapter == chapterIndex) {
			holder.title.setTextColor(0xffffa500);
			holder.percent.setTextColor(0xffffa500);
		} else {
			holder.title.setTextColor(0xff000000);
			holder.percent.setTextColor(0xff000000);
		}

		BookChapter.BookChapterData data = m_chapter.getChapter(chapterIndex);

		holder.title.setText(data.m_title);
		final double percent = (double) data.m_startTxtOffset
				/ m_chapter.m_allTextSize;
		holder.percent.setText(String.format("%1$.2f%%", percent * 100.0));

		return convertView;
	}

	public Object getGroup(int groupPosition) {
		if (groupPosition < 0) {
			groupPosition = 0;
		}

		final int count = getGroupCount();
		if (groupPosition > count - 1) {
			groupPosition = count - 1;
		}

		return m_chapter.m_juanData.get(groupPosition);
	}

	public int getGroupCount() {
		return m_chapter.m_juanData.size();
	}

	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	class GroupViewHolder {
		TextView title;
		ImageView expand;
	}

	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		GroupViewHolder holder;

		if (convertView == null) {
			convertView = mInflater.inflate(
					R.layout.reader_chapterjuan_itemgroup, null);

			holder = new GroupViewHolder();
			holder.title = (TextView) convertView
					.findViewById(R.id.reader_chapterjuan_item_content_group);
			convertView.setTag(holder);
			holder.expand = (ImageView) convertView
					.findViewById(R.id.expand_image);
		} else {
			holder = (GroupViewHolder) convertView.getTag();
		}

		if (m_currentGroup == groupPosition) {
			holder.title.setTextColor(0xffffa500);
		} else {
			holder.title.setTextColor(0xff696969);
		}

		holder.expand.setSelected(isExpanded);

		BookChapter.JuanData data = (BookChapter.JuanData) getGroup(groupPosition);

		holder.title.setText(data.m_juanTitle);
		return convertView;
	}

	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {

		final int chapterIndex = (Integer) getChild(groupPosition,
				childPosition);
		m_baseActivity.finish();
		FBReaderApp.Instance().BookTextView.gotoChapter(chapterIndex);
		return false;
	}
}