package org.socool.android.bookshelf;

import org.socool.socoolreader.reader.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ListView;

public class BookshelfListActivity  extends Activity {
	
	ListView m_allBooks;
	BookshelfListAdapter m_listAdapter;
	
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.bookshelflist);
		m_allBooks = (ListView)findViewById(R.id.bookshelf_list);
		
		m_listAdapter = new BookshelfListAdapter(this);
		m_allBooks.setAdapter(m_listAdapter);
//		m_allBooks.setOnClickListener(this);
	}
}
