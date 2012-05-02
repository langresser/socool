package org.geometerplus.android.fbreader.bookshelf;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.content.res.TypedArray;
import android.util.Xml;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.xmlpull.v1.XmlPullParserException;
import static org.xmlpull.v1.XmlPullParser.*;

public final class BookStoreFactory {
    private static final String LOG_TAG = "ShelvesParser";
    public static final String DEFAULT_BOOK_STORE = "all";

    private static final String TAG_STORES = "stores";
    private static final String TAG_STORE = "store";

    private static HashMap<String, BooksStore> sStores;

    private BookStoreFactory() {
    }

    public static BooksStore get(Context context) {
        final SharedPreferences preferences = context.getSharedPreferences("fuck", 0);
        final String name = DEFAULT_BOOK_STORE;

        BooksStore store = get(context, name);
        if (store == null) {
            final Iterator<Map.Entry<String, BooksStore>> iterator = sStores.entrySet().iterator();
            if (iterator.hasNext()) {
                store = iterator.next().getValue();

                final SharedPreferences.Editor editor = preferences.edit();
//                editor.putString(Preferences.KEY_BOOKSTORE, store.getName());
                editor.commit();

                return store;
            }
        }

        return store;
    }

    public static BooksStore get(Context context, String store) {
        if (sStores == null) {
//            sStores = inflate(context, R.xml.bookstores);
        }

        BooksStore booksStore = sStores.get(store);
        if (booksStore == null) throw new IllegalStateException("The store " + store +
                " cannot not be found");

        return booksStore;
    }

    public static BooksStore[] getStores(Context context) {
        if (sStores == null) {
//            sStores = inflate(context, R.xml.bookstores);
        }
        return sStores.values().toArray(new BooksStore[sStores.size()]);
    }

    private static HashMap<String, BooksStore> inflate(Context context, int resource) {
        final HashMap<String, BooksStore> stores = new HashMap<String, BooksStore>();
        final XmlResourceParser parser = context.getResources().getXml(resource);

        try {
            inflate(context, parser, stores);
        } catch (IOException e) {
            Log.w(LOG_TAG, "An error occured while loading the bookstores", e);
        } catch (XmlPullParserException e) {
            Log.w(LOG_TAG, "An error occured while loading the bookstores", e);            
        } finally {
            parser.close();
        }

        if (stores.size() >= 1) {
        }

        return stores;
    }

    private static void inflate(Context context, XmlResourceParser parser,
            HashMap<String, BooksStore> stores) throws IOException, XmlPullParserException {

        final AttributeSet attrs = Xml.asAttributeSet(parser);

        int type;
        while ((type = parser.next()) != START_TAG && type != END_DOCUMENT) {
            // Empty
        }

        if (type != START_TAG) {
            throw new InflateException(parser.getPositionDescription() + ": No start tag found!");
        }

        final String name = parser.getName();
        if (TAG_STORES.equals(name)) {
            parseStores(context, parser, attrs, stores);
        } else {
            throw new InflateException(parser.getPositionDescription() +
                    ": The root tag must be " + TAG_STORES);
        }
    }

    private static void parseStores(Context context, XmlResourceParser parser, AttributeSet attrs,
            HashMap<String, BooksStore> stores) throws IOException, XmlPullParserException {

        final int depth = parser.getDepth();

        int type;
        while (((type = parser.next()) != END_TAG || parser.getDepth() > depth) &&
                type != END_DOCUMENT) {

            if (type != START_TAG) {
                continue;
            }

            final String name = parser.getName();
            if (TAG_STORE.equals(name)) {
                addStore(context, parser, attrs, stores);
            }
        }
    }

    private static void addStore(Context context, XmlResourceParser parser, AttributeSet attrs,
            HashMap<String, BooksStore> stores) {
    }
}
