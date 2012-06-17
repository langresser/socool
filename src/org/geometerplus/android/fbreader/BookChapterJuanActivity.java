package org.geometerplus.android.fbreader;

import org.socool.socoolreader.reader.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ExpandableListView;
import android.widget.RelativeLayout;

public class BookChapterJuanActivity extends Activity {

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.reader_chapterjuan_page);
		
		RelativeLayout noneLayout = (RelativeLayout)findViewById(R.id.reader_chapterjuan_cover);

		ExpandableListView listView = (ExpandableListView)findViewById(R.id.reader_chapterjuan_listview);
		BookChapterJuanAdapter adapter = new BookChapterJuanAdapter(this);
		listView.setAdapter(adapter);
		listView.setOnChildClickListener(adapter);
		noneLayout.setVisibility(View.GONE);
		
		listView.expandGroup(adapter.m_currentGroup);
		listView.setSelectedGroup(adapter.m_currentGroup);
	}
}
