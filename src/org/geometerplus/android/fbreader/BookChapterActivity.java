package org.geometerplus.android.fbreader;

import java.text.SimpleDateFormat;
import java.util.List;

import org.geometerplus.fbreader.bookmodel.BookChapter;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.socool.socoolreader.reader.R;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class BookChapterActivity extends Activity {

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.reader_chapter_page);
		
		RelativeLayout noneLayout = (RelativeLayout)findViewById(R.id.reader_chapter_cover);

		ListView listView = (ListView)findViewById(R.id.reader_chapter_listview);
		BookChapterAdapter adapter = new BookChapterAdapter(this);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(adapter);
		noneLayout.setVisibility(View.GONE);
		
		int top = adapter.m_currentChapter - 5;
		top = Math.max(top, 0);
		listView.setSelection(top);
	}

	private class BookChapterAdapter extends BaseAdapter
				implements AdapterView.OnItemClickListener {
        private LayoutInflater mInflater;
        final private BookChapter m_chapter = FBReaderApp.Instance().Model.m_chapter;
        final public int m_currentChapter = FBReaderApp.Instance().BookTextView.getCurrentChapter();

        public BookChapterAdapter(Context context) {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);
         }

        public int getCount() {
            return m_chapter.getChapterCount();
        }
               
        public final void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        	finish();
        	FBReaderApp.Instance().BookTextView.gotoChapter(position);
        }

        public Object getItem(int position) {
        	final int count = getCount();
        	if (position > count - 1) {
        		position = count - 1;
        	}
        	if (position < 0) {
        		position = 0;
        	}

            return m_chapter.getChapter(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.reader_chapter_item, null);

                holder = new ViewHolder();
                holder.title = (TextView)convertView.findViewById(R.id.reader_chapter_item_content);
                holder.percent = (TextView)convertView.findViewById(R.id.reader_chapter_item_percent);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            if (m_currentChapter == position) {
            	holder.title.setTextColor(0xffffa500);
            	holder.percent.setTextColor(0xffffa500);
            } else {
            	holder.title.setTextColor(0xff000000);
            	holder.percent.setTextColor(0xff000000);
            }
            
            BookChapter.BookChapterData data = (BookChapter.BookChapterData)getItem(position);
            holder.title.setText(data.m_title);
            final double percent = (double)data.m_startTxtOffset / m_chapter.m_allTextSize;
            holder.percent.setText(String.format("%1$.2f%%", percent * 100.0));

            return convertView;
        }

        class ViewHolder {
            TextView title;
            TextView percent;
        }
    }
}
