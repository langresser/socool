package org.geometerplus.android.fbreader.bookshelf;

import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Utility class to load books from a books store.
 */
public abstract class BooksStore {
    static final String LOG_TAG = "Shelves";

    private final String mStoreName;
    private final String mStoreLabel;
    private final String mHost;

    public enum ImageSize {
        // SWATCH,
        // SMALL,
        THUMBNAIL,
        TINY,
        // MEDIUM,
        // LARGE
    }

    public static class Book implements Parcelable, BaseColumns {
        public static final Uri CONTENT_URI = Uri.parse("content://shelves/books");

        public static final String DEFAULT_SORT_ORDER = "sort_title ASC";

        public static final String INTERNAL_ID = "internal_id";
        public static final String EAN = "ean";
        public static final String ISBN = "isbn";
        public static final String TITLE = "title";
        public static final String SORT_TITLE = "sort_title";
        public static final String AUTHORS = "authors";
        public static final String PUBLISHER = "publisher";        
        public static final String REVIEWS = "reviews";
        public static final String PAGES = "pages";
        public static final String LAST_MODIFIED = "last_modified";
        public static final String PUBLICATION = "publication";
        public static final String DETAILS_URL = "details_url";
        public static final String TINY_URL = "tiny_url";

        String mIsbn;
        String mEan;
        String mInternalId;
        Map<ImageSize, String> mImages;
        List<String> mAuthors;
        int mPages;
        String mTitle;
        Date mPublicationDate;
        String mDetailsUrl;
        String mPublisher;
        Calendar mLastModified;

        private String mStorePrefix;
        private ImageLoader mLoader;

        Book() {
            this("", null);
        }

        Book(String storePrefix, ImageLoader loader) {
            mStorePrefix = storePrefix;
            mLoader = loader;
            mImages = new HashMap<ImageSize, String>(6);
            mAuthors = new ArrayList<String>(1);
        }

        private Book(Parcel in) {
            mIsbn = in.readString();
            mEan = in.readString();
            mInternalId = in.readString();
            mTitle = in.readString();
            mAuthors = new ArrayList<String>(1);
            in.readStringList(mAuthors);
        }

        public String getIsbn() {
            return mIsbn;
        }

        public String getEan() {
            return mEan;
        }

        public String getInternalId() {
            return mStorePrefix + mInternalId;
        }

        public String getInternalIdNoPrefix() {
            return mInternalId;
        }

        public List<String> getAuthors() {
            return mAuthors;
        }

        public int getPagesCount() {
            return mPages;
        }

        public String getTitle() {
            return mTitle;
        }

        public Date getPublicationDate() {
            return mPublicationDate;
        }

        public String getDetailsUrl() {
            return mDetailsUrl;
        }

        public String getPublisher() {
            return mPublisher;
        }

        public Calendar getLastModified() {
            return mLastModified;
        }

        public String getImageUrl(ImageSize size) {
            return mImages.get(size);
        }

        public Bitmap loadCover(ImageSize size) {
            final String url = mImages.get(size);
            if (url == null) return null;

            final ImageUtilities.ExpiringBitmap expiring;
            if (mLoader == null) {
                expiring = ImageUtilities.load(url);
            } else {
                expiring = mLoader.load(url);
            }
            mLastModified = expiring.lastModified;

            return expiring.bitmap;
        }

        public ContentValues getContentValues() {
            final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
            final ContentValues values = new ContentValues();

            values.put(INTERNAL_ID, mStorePrefix + mInternalId);
            values.put(EAN, mEan);
            values.put(ISBN, mIsbn);
            values.put(TITLE, mTitle);
 //           values.put(AUTHORS, TextUtilities.join(mAuthors, ", "));
            values.put(PUBLISHER, mPublisher);
 //           values.put(REVIEWS,  TextUtilities.join(mDescriptions, "\n\n"));
            values.put(PAGES, mPages);
            if (mLastModified != null) {
                values.put(LAST_MODIFIED, mLastModified.getTimeInMillis());
            }
            values.put(PUBLICATION, mPublicationDate != null ?
                    format.format(mPublicationDate) : "");
            values.put(DETAILS_URL, mDetailsUrl);
            values.put(TINY_URL, mImages.get(ImageSize.TINY));

            return values;
        }

        public static Book fromCursor(Cursor c) {
            final Book book = new Book();

            book.mInternalId = c.getString(c.getColumnIndexOrThrow(INTERNAL_ID));
            book.mEan = c.getString(c.getColumnIndexOrThrow(EAN));
            book.mIsbn = c.getString(c.getColumnIndexOrThrow(ISBN));
            book.mTitle = c.getString(c.getColumnIndexOrThrow(TITLE));
            Collections.addAll(book.mAuthors,
                    c.getString(c.getColumnIndexOrThrow(AUTHORS)).split(", "));
            book.mPublisher = c.getString(c.getColumnIndexOrThrow(PUBLISHER));
            book.mPages = c.getInt(c.getColumnIndexOrThrow(PAGES));

            final Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTimeInMillis(c.getLong(c.getColumnIndexOrThrow(LAST_MODIFIED)));
            book.mLastModified = calendar;

            final SimpleDateFormat format = new SimpleDateFormat("MMMM yyyy");
            try {
                book.mPublicationDate = format.parse(c.getString(
                        c.getColumnIndexOrThrow(PUBLICATION)));
            } catch (ParseException e) {
                // Ignore
            }

            book.mDetailsUrl = c.getString(c.getColumnIndexOrThrow(DETAILS_URL));
            book.mImages.put(ImageSize.TINY, c.getString(c.getColumnIndexOrThrow(TINY_URL)));

            return book;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mIsbn);
            dest.writeString(mEan);
            dest.writeString(mInternalId);
            dest.writeString(mTitle);
            dest.writeStringList(mAuthors);
        }

        public static final Creator<Book> CREATOR = new Creator<Book>() {
            public Book createFromParcel(Parcel in) {
                return new Book(in);
            }

            public Book[] newArray(int size) {
                return new Book[size];
            }
        };
    }

    BooksStore(String name, String label, String host) {
        mStoreName = name;
        mStoreLabel = label;
        mHost = host;
    }

    public String getName() {
        return mStoreName;
    }

    public String getLabel() {
        return mStoreLabel;
    }

    /**
     * Creates an instance of {@link org.curiouscreature.android.shelves.provider.BooksStore.Book}
     * with this book store's name.
     *
     * @return A new instance of Book.
     */
    Book createBook() {
        return new Book(getName(), null);
    }

    /**
     * Interface used to load images with an expiring date. The expiring date is handled by
     * the image cache to check for updated images from time to time.
     */
    static interface ImageLoader {
        /**
         * Load the specified URL as a Bitmap and associates an expiring date to it.
         *
         * @param url The URL of the image to load.
         *
         * @return The Bitmap decoded from the URL and an expiration date.
         */
        public ImageUtilities.ExpiringBitmap load(String url);
    }
}
