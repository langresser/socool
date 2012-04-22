package org.geometerplus.android.fbreader;

import org.socool.socoolreader.reader.R;

import android.app.Activity;  
import android.content.Intent;
import android.os.Bundle;  
import android.view.LayoutInflater;  
import android.view.View;  
import android.view.ViewGroup;  
import android.view.Window;  
import android.widget.BaseAdapter;  
import android.widget.Button;
import android.widget.ListView;  
  
public class BookShelfActivity extends Activity { 
	final int BOOK_NUM_PER_LINE = 3;
    /** Called when the activity is first created. */  
    private ListView shelf_list;  
    // 书架的列数  
    int[] size = new int[5];  
  
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);  
        setContentView(R.layout.book_shelf_layout);  
  
        shelf_list = (ListView) findViewById(R.id.shelf_list);  
          
        ShelfAdapter adapter = new ShelfAdapter();  
        adapter.m_rootActivity = this;
        shelf_list.setAdapter(adapter);
    }
    
    public View.OnClickListener clickListener = new View.OnClickListener()
    {
    	public void onClick(View view)
    	{
		 startActivity(new Intent(getApplicationContext(), SCReaderActivity.class)
    				.setAction(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    	}
    };
    
  
    public class ShelfAdapter extends BaseAdapter {
    	public BookShelfActivity m_rootActivity;

        @Override  
        public int getCount() {  
            return size.length;  
        }  
  
        @Override  
        public Object getItem(int position) {  
            return size[position];  
        }  
  
        @Override  
        public long getItemId(int position) {  
            return position;  
        }  
  
        @Override  
        public View getView(int position, View convertView, ViewGroup parent) {  
            View layout = LayoutInflater.from(getApplicationContext()).inflate(  
                       R.layout.book_shelf_item, null);  
            
            Button book1 = (Button)layout.findViewById(R.id.button_1);
            Button book2 = (Button)layout.findViewById(R.id.button_2);
            Button book3 = (Button)layout.findViewById(R.id.button_3);
            
            book1.setOnClickListener(m_rootActivity.clickListener);
            book2.setOnClickListener(m_rootActivity.clickListener);
            book3.setOnClickListener(m_rootActivity.clickListener);
            
            return layout;  
        }
    }
}  

