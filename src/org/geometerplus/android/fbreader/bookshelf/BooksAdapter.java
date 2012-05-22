package org.geometerplus.android.fbreader.bookshelf;

import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AbsListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

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
    private final Bitmap mDefaultCoverBitmap;
    private final FastBitmapDrawable mDefaultCover;

//  Bitmap bitmap = book.loadCover(0);
//  if (bitmap != null) {
//      bitmap = ImageUtilities.createBookCover(bitmap, BOOK_COVER_WIDTH, BOOK_COVER_HEIGHT);
//      
//      // 创建缓存文件夹 TODO 初始化的时候创建
//      File cacheDirectory = new File(Paths.coverCacheDirectory());
//      if (!cacheDirectory.exists()) {
//          cacheDirectory.mkdirs();
//      }
//
//      File coverFile = new File(cacheDirectory, book.getInternalId());
//      FileOutputStream out = null;
//      try {
//          out = new FileOutputStream(coverFile);
//          bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//      } catch (FileNotFoundException e) {
//          return null;
//      } finally {
//      	try {
//      		out.close();
//          } catch (IOException e) {
//          }
//      }
//  }
    BooksAdapter(ShelvesActivity activity) {
    	mActivity = activity;
        mInflater = LayoutInflater.from(activity);

        mDefaultCoverBitmap = BitmapFactory.decodeResource(activity.getResources(),
                //R.drawable.shelf_default_cover);
        		R.drawable.unknown_cover);
        mDefaultCover = new FastBitmapDrawable(mDefaultCoverBitmap);
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

//        if (mActivity.mScrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
//        		mActivity.isPendingCoversUpdate()) {
//            holder.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null, mDefaultCover);
//            holder.queryCover = true;
//        } else {
//            holder.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
//                    CoverManager.getCachedCover(bookId, mDefaultCover));
//            holder.queryCover = false;
//        }

//        mDefaultCover.setBounds(0, 0, 100, 100);
        holder.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null, CoverManager.getCachedCover(bookId, mDefaultCover));
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
	  	
	  	if (!FBReaderApp.Instance().hasCustomCover(book.File)) {
	  		button.setImageResource(FBReaderApp.Instance().getCoverResourceId(book.File));
	  		return;
	  	}

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
