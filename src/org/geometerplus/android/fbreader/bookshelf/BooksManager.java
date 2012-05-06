package org.geometerplus.android.fbreader.bookshelf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.geometerplus.fbreader.Paths;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;

public class BooksManager {
    static final int BOOK_COVER_WIDTH = 100;
    static final int BOOK_COVER_HEIGHT = 120;

    private static String sBookIdSelection;
    private static String sBookSelection;

    private static String[] sArguments1 = new String[1];
    private static String[] sArguments3 = new String[3];
    private BooksManager() {
    }
    
    public static String findBookId(ContentResolver contentResolver, String id) {
        String internalId = null;
        Cursor c = null;

        try {
            final String[] arguments3 = sArguments3;
            arguments3[0] = arguments3[1] = arguments3[2] = id;
        } finally {
            if (c != null) c.close();
        }

        return internalId;
    }

    public static boolean bookExists(ContentResolver contentResolver, String id) {
        boolean exists = true;
        Cursor c = null;
        return exists;
    }

    public static BooksStore.Book loadAndAddBook(ContentResolver resolver, String id,
            BooksStore booksStore) {

        final BooksStore.Book book = null;//booksStore.findBook(id);
        if (book != null) {
            Bitmap bitmap = book.loadCover(BooksStore.ImageSize.TINY);
            if (bitmap != null) {
                bitmap = ImageUtilities.createBookCover(bitmap, BOOK_COVER_WIDTH, BOOK_COVER_HEIGHT);
                
                // ���������ļ��� TODO ��ʼ����ʱ�򴴽�
                File cacheDirectory = new File(Paths.coverCacheDirectory());
                if (!cacheDirectory.exists()) {
                    cacheDirectory.mkdirs();
                }

                File coverFile = new File(cacheDirectory, book.getInternalId());
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(coverFile);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (FileNotFoundException e) {
                    return null;
                } finally {
                	try {
                		out.close();
                    } catch (IOException e) {
                    }
                }
            }
        }

        return null;
    }

    public static boolean deleteBook(ContentResolver contentResolver, String bookId) {
        final String[] arguments1 = sArguments1;
        arguments1[0] = bookId;
        int count = contentResolver.delete(BooksStore.Book.CONTENT_URI,
                sBookIdSelection, arguments1);
        ImageUtilities.deleteCachedCover(bookId);
        return count > 0;
    }

    public static BooksStore.Book findBook(ContentResolver contentResolver, String id) {
        BooksStore.Book book = null;
        Cursor c = null;

        try {
            sArguments1[0] = id;
            c = contentResolver.query(BooksStore.Book.CONTENT_URI, null, sBookIdSelection,
                    sArguments1, null);
            if (c.getCount() > 0) {
                if (c.moveToFirst()) {
                    book = BooksStore.Book.fromCursor(c);
                }
            }
        } finally {
            if (c != null) c.close();
        }

        return book;
    }

    public static BooksStore.Book findBook(ContentResolver contentResolver, Uri data) {
        BooksStore.Book book = null;
        Cursor c = null;

        try {
            c = contentResolver.query(data, null, null, null, null);
            if (c.getCount() > 0) {
                if (c.moveToFirst()) {
                    book = BooksStore.Book.fromCursor(c);
                }
            }
        } finally {
            if (c != null) c.close();
        }

        return book;
    }
}