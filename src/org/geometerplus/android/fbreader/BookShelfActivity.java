package org.geometerplus.android.fbreader;

import java.util.Timer;
import java.util.TimerTask;

import org.geometerplus.zlibrary.application.ZLApplication;
import org.geometerplus.zlibrary.application.ZLibrary;
import org.socool.socoolreader.reader.R;

import android.app.Activity;  
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;  
import android.view.KeyEvent;
import android.view.LayoutInflater;  
import android.view.View;  
import android.view.ViewGroup;  
import android.view.Window;  
import android.view.WindowManager;
import android.widget.BaseAdapter;  
import android.widget.Button;
import android.widget.ListView;  
import android.widget.Toast;
  
public class BookShelfActivity extends Activity { 
	final int BOOK_NUM_PER_LINE = 3;
    /** Called when the activity is first created. */  
    private ListView shelf_list;  
    // 书架的列数  
    int[] size = new int[5];
    boolean m_realExit = false;
  
    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);  
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 0);
        
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
    				.setAction(Intent.ACTION_VIEW));
//		 finish();
    	}
    };
    
    public void onStart() {
		super.onStart();
		
		m_realExit = false;
    }
      
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (m_realExit == false) {
    			m_realExit = true;
    			
    			Toast.makeText(getApplicationContext(), getResources().getString(R.string.real_exit_tip), Toast.LENGTH_SHORT).show();
    			
//    			Timer timerExit = new Timer();
//    			timerExit.schedule(new TimerTask() {
//    		        @Override  
//    		        public void run() {  
//    		        	m_realExit = false;
//    		        }  
//    		    } , 5000);	// 5秒钟后恢复标志
    			return true;
    		} else {
    			if (ZLApplication.Instance() != null) {
    				ZLApplication.Instance().closeWindow();
    			}
    			
            	finish();
            	return true;
    		}
    	} else {
    		return super.onKeyDown(keyCode, event);
    	}
	}
 
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

