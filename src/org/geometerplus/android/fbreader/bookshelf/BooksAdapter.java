package org.geometerplus.android.fbreader.bookshelf;

import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.TextView;
import android.widget.AbsListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.database.Cursor;
import android.database.CharArrayBuffer;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.socool.socoolreader.reader.R;

class BooksAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final int mTitleIndex;
    private final int mSortTitleIndex;
    private final int mInternalIdIndex;
    private final String mSelection;
    private final ShelvesActivity mActivity;
    private final Bitmap mDefaultCoverBitmap;
    private final FastBitmapDrawable mDefaultCover;
    private final String[] mArguments2 = new String[2];

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
        mTitleIndex = 0;//c.getColumnIndexOrThrow(BooksStore.Book.TITLE);
        mSortTitleIndex = 0;//c.getColumnIndexOrThrow(BooksStore.Book.SORT_TITLE);
        mInternalIdIndex = 0;//c.getColumnIndexOrThrow(BooksStore.Book.INTERNAL_ID);

        mDefaultCoverBitmap = BitmapFactory.decodeResource(activity.getResources(),
                R.drawable.unknown_cover);
        mDefaultCover = new FastBitmapDrawable(mDefaultCoverBitmap);

        final StringBuilder selection = new StringBuilder();
        mSelection = selection.toString();
    }

    FastBitmapDrawable getDefaultCover() {
        return mDefaultCover;
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final TextView view = (TextView) mInflater.inflate(R.layout.shelf_book, parent, false);

        BookViewHolder holder = new BookViewHolder();
        holder.title = view;

        view.setTag(holder);

        final CrossFadeDrawable transition = new CrossFadeDrawable(mDefaultCoverBitmap, null);
        transition.setCallback(view);
        transition.setCrossFadeEnabled(true);
        holder.transition = transition;

        return view;
    }

    public void bindView(View view, Context context, Cursor c) {
        BookViewHolder holder = (BookViewHolder) view.getTag();
        String bookId = c.getString(mInternalIdIndex);
        holder.bookId = bookId;
        holder.sortTitle = c.getString(mSortTitleIndex);

        final ShelvesActivity activity = mActivity;
        if (activity.getScrollState() == AbsListView.OnScrollListener.SCROLL_STATE_FLING ||
                activity.isPendingCoversUpdate()) {
            holder.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null, mDefaultCover);
            holder.queryCover = true;
        } else {
            holder.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null,
                    ImageUtilities.getCachedCover(bookId, mDefaultCover));
            holder.queryCover = false;
        }

        final CharArrayBuffer buffer = holder.buffer;
        c.copyStringToBuffer(mTitleIndex, buffer);
        final int size = buffer.sizeCopied;
        if (size != 0) {
            holder.title.setText(buffer.data, 0, size);
        }
    }

	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		return null;
	}
}
