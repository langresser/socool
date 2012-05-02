package org.geometerplus.android.fbreader.bookshelf;

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

class BooksAdapter extends CursorAdapter implements FilterQueryProvider {
    private static final String[] PROJECTION_IDS_AND_TITLE = new String[] {
            BooksStore.Book._ID, BooksStore.Book.INTERNAL_ID, BooksStore.Book.TITLE,
            BooksStore.Book.SORT_TITLE
    };

    private final LayoutInflater mInflater;
    private final int mTitleIndex;
    private final int mSortTitleIndex;
    private final int mInternalIdIndex;
    private final String mSelection;
    private final ShelvesActivity mActivity;
    private final Bitmap mDefaultCoverBitmap;
    private final FastBitmapDrawable mDefaultCover;
    private final String[] mArguments2 = new String[2];

    BooksAdapter(ShelvesActivity activity) {
        super(activity, activity.managedQuery(BooksStore.Book.CONTENT_URI,
                PROJECTION_IDS_AND_TITLE,
                null, null, BooksStore.Book.DEFAULT_SORT_ORDER), true);

        final Cursor c = getCursor();

        mActivity = activity;
        mInflater = LayoutInflater.from(activity);
        mTitleIndex = c.getColumnIndexOrThrow(BooksStore.Book.TITLE);
        mSortTitleIndex = c.getColumnIndexOrThrow(BooksStore.Book.SORT_TITLE);
        mInternalIdIndex = c.getColumnIndexOrThrow(BooksStore.Book.INTERNAL_ID);

        mDefaultCoverBitmap = BitmapFactory.decodeResource(activity.getResources(),
                R.drawable.unknown_cover);
        mDefaultCover = new FastBitmapDrawable(mDefaultCoverBitmap);

        final StringBuilder selection = new StringBuilder();
        selection.append(BooksStore.Book.TITLE);
        selection.append(" LIKE ? OR ");
        selection.append(BooksStore.Book.AUTHORS);
        selection.append(" LIKE ?");
        mSelection = selection.toString();

        setFilterQueryProvider(this);
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
    public void changeCursor(Cursor cursor) {
        final Cursor oldCursor = getCursor();
        if (oldCursor != null) mActivity.stopManagingCursor(oldCursor);
        super.changeCursor(cursor);
    }

    public Cursor runQuery(CharSequence constraint) {
        if (constraint == null || constraint.length() == 0) {
            return mActivity.managedQuery(BooksStore.Book.CONTENT_URI, PROJECTION_IDS_AND_TITLE,
                    null, null, BooksStore.Book.DEFAULT_SORT_ORDER);
        }

        final StringBuilder buffer = new StringBuilder();
        buffer.append('%').append(constraint).append('%');
        final String pattern = buffer.toString();

        final String[] arguments2 = mArguments2;
        arguments2[0] = arguments2[1] = pattern;
        return mActivity.managedQuery(BooksStore.Book.CONTENT_URI, PROJECTION_IDS_AND_TITLE,
                mSelection, arguments2, BooksStore.Book.DEFAULT_SORT_ORDER);
    }
}
