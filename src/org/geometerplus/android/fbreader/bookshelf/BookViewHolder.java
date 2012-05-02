package org.geometerplus.android.fbreader.bookshelf;

import android.widget.TextView;
import android.database.CharArrayBuffer;

class BookViewHolder {
    TextView title;
    String bookId;
    CrossFadeDrawable transition;
    final CharArrayBuffer buffer = new CharArrayBuffer(64);
    boolean queryCover;
    String sortTitle;
}
