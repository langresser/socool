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

        Book() {
            this("");
        }

        Book(String storePrefix) {
            mStorePrefix = storePrefix;
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

            final ImageUtilities.ExpiringBitmap expiring = ImageUtilities.load(url);
            mLastModified = expiring.lastModified;

            return expiring.bitmap;
        }


        public static Book fromCursor(Cursor c) {
            final Book book = new Book();

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

    BooksStore(String label, String host) {
        mStoreLabel = label;
        mHost = host;
    }

    public String getLabel() {
        return mStoreLabel;
    }
}
