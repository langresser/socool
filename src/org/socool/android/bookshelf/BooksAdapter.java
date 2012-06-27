package org.socool.android.bookshelf;

import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AbsListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint;

import org.socool.android.covers.CoverManager;
import org.socool.screader.screader.FBReaderApp;
import org.socool.screader.library.Book;
import org.socool.zlibrary.image.ZLImage;
import org.socool.zlibrary.image.ZLImageData;
import org.socool.zlibrary.image.ZLImageManager;
import org.socool.zlibrary.image.ZLLoadableImage;
import org.socool.socoolreader.yhyxcs.R;

class BooksAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final ShelvesActivity mActivity;
    
    public static final int COVER_MAC_COUNT = 1;

    private final FastBitmapDrawable[] mDefaultCoverSet = new FastBitmapDrawable[COVER_MAC_COUNT];
    
    BooksAdapter(ShelvesActivity activity) {
    	mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        
        for (int i = 0; i < COVER_MAC_COUNT; ++i) {
    		Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), 0);
    		final int width = bitmap.getWidth();
        	final int height = bitmap.getHeight();
        	Log.d("Bitmap:" + i, String.format("width:%1d  height:%2d", width, height));

    		Matrix matrix = new Matrix();   
    	    matrix.postScale(120.0f / width, 160.0f / height);   
    	  
    	    // create the new Bitmap object   
    	    Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);   

    		mDefaultCoverSet[i] = new FastBitmapDrawable(resizedBitmap);
    	}
    }

	@Override
	public int getCount() {
		return mActivity.m_bookList.size();
	}

	@Override
	public Object getItem(int position) {
		return mActivity.m_bookList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	class BookViewHolder {
	    BubbleTextView title;
	    String bookId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		BookViewHolder holder = null;
		if (convertView == null) {
			convertView = (BubbleTextView) mInflater.inflate(R.layout.shelf_book, parent, false);
			holder = new BookViewHolder();
			convertView.setTag(holder);
		} else {
			holder = (BookViewHolder)convertView.getTag();
		}
        
        holder.title = (BubbleTextView) convertView;
        String bookId = "";
        holder.bookId = bookId;

        holder.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null, mDefaultCoverSet[position % 4]);
        Book book = mActivity.m_bookList.get(position);
        holder.title.setText(book.myTitle);
		return convertView;
	}
	
	void updateBookButton(ImageView button, int index)
	{
	  	button.setTag(index);
	  	
	  	if (index >= mActivity.m_bookList.size()) {
	  		// 先清空原button书籍
	      	button.setVisibility(View.GONE);
	  		button.setImageDrawable(null);
	  		return;
	  	}
	
	  	Book book = mActivity.m_bookList.get(index);

		final ZLImage image = book.getCover();

		if (image == null) {
			return;
		}

		if (image instanceof ZLLoadableImage) {
			final ZLLoadableImage loadableImage = (ZLLoadableImage)image;
			if (!loadableImage.isSynchronized()) {
				loadableImage.synchronize();
			}
		}
		final ZLImageData data = ZLImageManager.Instance().getImageData(image);
		
		if (data == null) {
			return;
		}

		button.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		int width = button.getWidth();
		int height = button.getHeight();
		final Bitmap coverBitmap = data.getBitmap(width, height);
		if (coverBitmap == null) {
			return;
		}

		button.setVisibility(View.VISIBLE);
		button.setImageBitmap(coverBitmap);
	}
}
