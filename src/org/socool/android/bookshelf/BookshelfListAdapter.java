package org.socool.android.bookshelf;

import java.util.ArrayList;

import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.AbsListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint;

import org.socool.android.covers.CoverManager;
import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.library.Book;
import org.socool.screader.library.BookState;
import org.socool.zlibrary.image.ZLImage;
import org.socool.zlibrary.image.ZLImageData;
import org.socool.zlibrary.image.ZLImageManager;
import org.socool.zlibrary.image.ZLLoadableImage;
import org.socool.socoolreader.reader.R;

public class BookshelfListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final Activity mActivity;
    public ArrayList<Book> m_bookList = new ArrayList<Book>();
    
    public static final int COVER_MAC_COUNT = 1;

    private final FastBitmapDrawable[] mDefaultCoverSet = new FastBitmapDrawable[COVER_MAC_COUNT];
    
    BookshelfListAdapter(Activity activity) {
    	mActivity = (ShelvesActivity)activity;
        mInflater = LayoutInflater.from(activity);
           
        Book book1 = Book.getByPath("book/wxkb");
        m_bookList.add(book1);
        
        for (int i = 0; i < COVER_MAC_COUNT; ++i) {
    		Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), 0);
    		final int width = bitmap.getWidth();
        	final int height = bitmap.getHeight();

    		Matrix matrix = new Matrix();   
    	    matrix.postScale(120.0f / width, 160.0f / height);   
    	  
    	    // create the new Bitmap object   
    	    Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);   

    		mDefaultCoverSet[i] = new FastBitmapDrawable(resizedBitmap);
    	}
    }

	@Override
	public int getCount() {
		return m_bookList.size();
	}

	@Override
	public Object getItem(int position) {
		return m_bookList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	class BookViewHolder {
	    TextView title;
	    TextView author;
	    ImageView cover;
	    TextView lastRead;
	    TextView newRead;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		BookViewHolder holder = null;
		if (convertView == null) {
			convertView = (LinearLayout) mInflater.inflate(R.layout.bookshelflistitem, parent, false);
			holder = new BookViewHolder();
			holder.title = (TextView)convertView.findViewById(R.id.bookshelf_title);
			holder.author = (TextView)convertView.findViewById(R.id.bookshelf_author);
			holder.cover = (ImageView)convertView.findViewById(R.id.bookshelf_cover);
			holder.lastRead = (TextView)convertView.findViewById(R.id.bookshelf_last_read);
			holder.newRead = (TextView)convertView.findViewById(R.id.bookshelf_new_chapter);
			convertView.setTag(holder);
		} else {
			holder = (BookViewHolder)convertView.getTag();
		}
        
		Book book = (Book)getItem(position);
		
        holder.title.setText(book.myTitle);
        holder.author.setText(book.m_bookAuthor);

        holder.cover.setImageResource(book.m_coverId);
        
        BookState bookstate = book.getStoredPosition();
        if (bookstate == null) {
        	holder.lastRead.setText("未阅读");
        	holder.newRead.setText("阅读进度： 0%");
        } else {
        	holder.lastRead.setText("上次阅读： " + bookstate.m_lastReadChapter);
        	holder.newRead.setText(String.format("阅读进度： %1$.2f%%", bookstate.m_lastReadPercent / 100.0));
        }

		return convertView;
	}
}
