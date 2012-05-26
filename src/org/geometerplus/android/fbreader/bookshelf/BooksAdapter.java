package org.geometerplus.android.fbreader.bookshelf;

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

import org.geometerplus.android.fbreader.covers.CoverManager;
import org.geometerplus.fbreader.fbreader.FBReaderApp;
import org.geometerplus.fbreader.library.Book;
import org.geometerplus.zlibrary.image.ZLImage;
import org.geometerplus.zlibrary.image.ZLImageData;
import org.geometerplus.zlibrary.image.ZLImageManager;
import org.geometerplus.zlibrary.image.ZLLoadableImage;
import org.socool.socoolreader.reader.R;

class BooksAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final ShelvesActivity mActivity;
    
    public static final int COVER_MAC_COUNT = 1;
    private final int[] m_coverResId = {R.drawable.wxkb1_cover, R.drawable.wxkb2_cover};

    private final FastBitmapDrawable[] mDefaultCoverSet = new FastBitmapDrawable[COVER_MAC_COUNT];
    
    BooksAdapter(ShelvesActivity activity) {
    	mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        
        for (int i = 0; i < COVER_MAC_COUNT; ++i) {
    		Bitmap bitmap = BitmapFactory.decodeResource(mActivity.getResources(), m_coverResId[i]);
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
