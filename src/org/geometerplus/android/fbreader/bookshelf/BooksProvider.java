package org.geometerplus.android.fbreader.bookshelf;

import android.content.ContentProvider;
import android.content.UriMatcher;
import android.content.Context;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.res.Resources;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;
import android.net.Uri;
import android.text.TextUtils;
import android.app.SearchManager;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.socool.socoolreader.reader.R;

public class BooksProvider extends ContentProvider {
    private static final String LOG_TAG = "BooksProvider";

    private static final String DATABASE_NAME = "books.db";
    private static final int DATABASE_VERSION = 1;

    private static final int SEARCH = 1;
    private static final int BOOKS = 2;
    private static final int BOOK_ID = 3;

    private static final String AUTHORITY = "shelves";

    private static final UriMatcher URI_MATCHER;
    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH);
        URI_MATCHER.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH);
        URI_MATCHER.addURI(AUTHORITY, "books", BOOKS);
        URI_MATCHER.addURI(AUTHORITY, "books/#", BOOK_ID);
    }

    private SQLiteOpenHelper mOpenHelper;

    private Pattern[] mKeyPrefixes;
    private Pattern[] mKeySuffixes;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case BOOKS:
                return "vnd.android.cursor.dir/vnd.org.curiouscreature.provider.shelves";
            case BOOK_ID:
                return "vnd.android.cursor.item/vnd.org.curiouscreature.provider.shelves";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    private String keyFor(String name) {
        if (name == null) name = "";

        name = name.trim().toLowerCase();

        if (mKeyPrefixes == null) {
            final Resources resources = getContext().getResources();
            final String[] keyPrefixes = resources.getStringArray(R.array.prefixes);
            final int count = keyPrefixes.length;

            mKeyPrefixes = new Pattern[count];
            for (int i = 0; i < count; i++) {
                mKeyPrefixes[i] = Pattern.compile("^" + keyPrefixes[i] + "\\s+");
            }
        }

        if (mKeySuffixes == null) {
            final Resources resources = getContext().getResources();
            final String[] keySuffixes = resources.getStringArray(R.array.suffixes);
            final int count = keySuffixes.length;

            mKeySuffixes = new Pattern[count];
            for (int i = 0; i < count; i++) {
                mKeySuffixes[i] = Pattern.compile("\\s*" + keySuffixes[i] + "$");
            }
        }

        final Pattern[] prefixes = mKeyPrefixes;
        for (Pattern prefix : prefixes) {
            final Matcher matcher = prefix.matcher(name);
            if (matcher.find()) {
                name = name.substring(matcher.end());
                break;
            }
        }

        final Pattern[] suffixes = mKeySuffixes;
        for (Pattern suffix : suffixes) {
            final Matcher matcher = suffix.matcher(name);
            if (matcher.find()) {
                name = name.substring(0, matcher.start());
                break;
            }
        }

        return name;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int count;
        switch (URI_MATCHER.match(uri)) {
            case BOOKS:
                count = db.delete("books", selection, selectionArgs);
                break;
            case BOOK_ID:
                String segment = uri.getPathSegments().get(1);
                count = db.delete("books", BooksStore.Book._ID + "=" + segment +
                        (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(LOG_TAG, "Upgrading database from version " + oldVersion + " to " +
                    newVersion + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS books");
            onCreate(db);
        }
    }

	@Override
	public Cursor query(Uri arg0, String[] arg1, String arg2, String[] arg3,
			String arg4) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}
}
