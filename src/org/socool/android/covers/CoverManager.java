/*
 * Copyright (C) 2010-2012 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.socool.android.covers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.widget.ImageView;

import org.socool.zlibrary.image.ZLImage;
import org.socool.zlibrary.image.ZLImageData;
import org.socool.zlibrary.image.ZLImageManager;
import org.socool.zlibrary.image.ZLLoadableImage;


import org.socool.android.bookshelf.FastBitmapDrawable;
import org.socool.screader.FBTree;
import org.socool.screader.Paths;
import org.socool.socoolreader.yhyxcs.R;

public class CoverManager {
	final CoverCache Cache = new CoverCache();

	private static class MinPriorityThreadFactory implements ThreadFactory {
		private final ThreadFactory myDefaultThreadFactory = Executors.defaultThreadFactory();

		public Thread newThread(Runnable r) {
			final Thread th = myDefaultThreadFactory.newThread(r);
			th.setPriority(Thread.MIN_PRIORITY);
			return th;
		}
	}
	private final ExecutorService myPool = Executors.newFixedThreadPool(1, new MinPriorityThreadFactory());

	private final Activity myActivity;
	private final int myCoverWidth;
	private final int myCoverHeight;

	public CoverManager(Activity activity, int coverWidth, int coverHeight) {
		myActivity = activity;
		myCoverWidth = coverWidth;
		myCoverHeight = coverHeight;
	}

	void runOnUiThread(Runnable runnable) {
		myActivity.runOnUiThread(runnable);
	}

	void setupCoverView(ImageView coverView) {
		coverView.getLayoutParams().width = myCoverWidth;
		coverView.getLayoutParams().height = myCoverHeight;
		coverView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		coverView.requestLayout();
	}

	Bitmap getBitmap(ZLImage image) {
		final ZLImageManager mgr = ZLImageManager.Instance();
		final ZLImageData data = mgr.getImageData(image);
		if (data == null) {
			return null;
		}
		return data.getBitmap(2 * myCoverWidth, 2 * myCoverHeight);
	}

	void setCoverForView(CoverHolder holder, ZLLoadableImage image) {
		synchronized (holder) {
			try {
				final Bitmap coverBitmap = Cache.getBitmap(holder.Key);
				if (coverBitmap != null) {
					holder.CoverView.setImageBitmap(coverBitmap);
				} else if (holder.coverBitmapTask == null) {
					holder.coverBitmapTask = myPool.submit(holder.new CoverBitmapRunnable(image));
				}
			} catch (CoverCache.NullObjectException e) {
			}
		}
	}

	private CoverHolder getHolder(ImageView coverView, FBTree tree) {
		CoverHolder holder = (CoverHolder)coverView.getTag();
		if (holder == null) {
			holder = new CoverHolder(this, coverView, tree.getUniqueKey());
			coverView.setTag(holder);
		} else {
			holder.setKey(tree.getUniqueKey());
		}
		return holder;
	}

	public boolean trySetCoverImage(ImageView coverView, FBTree tree) {
		final CoverHolder holder = getHolder(coverView, tree);

		Bitmap coverBitmap;
		try {
			coverBitmap = Cache.getBitmap(holder.Key);
		} catch (CoverCache.NullObjectException e) {
			return false;
		}

		if (coverBitmap == null) {
			final ZLImage cover = tree.getCover();
			if (cover instanceof ZLLoadableImage) {
				final ZLLoadableImage img = (ZLLoadableImage)cover;
				if (img.isSynchronized()) {
					setCoverForView(holder, img);
				} else {
					img.startSynchronization(holder.new CoverSyncRunnable(img));
				}
			} else if (cover != null) {
				coverBitmap = getBitmap(cover);
			}
		}
		if (coverBitmap != null) {
			holder.CoverView.setImageBitmap(coverBitmap);
			return true;
		}
		return false;
	}
	
	
	static private final int[] m_coverResId = {0, 0};
	static public int getCoverResIdByIndex(int index)
	{
		if (index < 0 || index > m_coverResId.length) {
			return 0;
		}
		
		return m_coverResId[index];
	}
	

    private static final float EDGE_START = 0.0f;
    private static final float EDGE_END = 4.0f;
    private static final int EDGE_COLOR_START = 0x7F000000;
    private static final int EDGE_COLOR_END = 0x00000000;
    private static final Paint EDGE_PAINT = new Paint();

    private static final int END_EDGE_COLOR_START = 0x00000000;
    private static final int END_EDGE_COLOR_END = 0x4F000000;
    private static final Paint END_EDGE_PAINT = new Paint();

    private static final float FOLD_START = 5.0f;
    private static final float FOLD_END = 13.0f;
    private static final int FOLD_COLOR_START = 0x00000000;
    private static final int FOLD_COLOR_END = 0x26000000;
    private static final Paint FOLD_PAINT = new Paint();

    private static final float SHADOW_RADIUS = 12.0f;
    private static final int SHADOW_COLOR = 0x99000000;
    private static final Paint SHADOW_PAINT = new Paint();

    private static final Paint SCALE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG |
            Paint.FILTER_BITMAP_FLAG);

    private static final FastBitmapDrawable NULL_DRAWABLE = new FastBitmapDrawable(null);

    // TODO: Use a concurrent HashMap to support multiple threads
    private static final HashMap<String, SoftReference<FastBitmapDrawable>> sArtCache =
            new HashMap<String, SoftReference<FastBitmapDrawable>>();

    private static volatile Matrix sScaleMatrix;

    static {
        Shader shader = new LinearGradient(EDGE_START, 0.0f, EDGE_END, 0.0f, EDGE_COLOR_START,
                EDGE_COLOR_END, Shader.TileMode.CLAMP);
        EDGE_PAINT.setShader(shader);

        shader = new LinearGradient(EDGE_START, 0.0f, EDGE_END, 0.0f, END_EDGE_COLOR_START,
                END_EDGE_COLOR_END, Shader.TileMode.CLAMP);
        END_EDGE_PAINT.setShader(shader);

        shader = new LinearGradient(FOLD_START, 0.0f, FOLD_END, 0.0f, new int[] {
                FOLD_COLOR_START, FOLD_COLOR_END, FOLD_COLOR_START
        }, new float[] { 0.0f, 0.5f, 1.0f }, Shader.TileMode.CLAMP);
        FOLD_PAINT.setShader(shader);

        SHADOW_PAINT.setShadowLayer(SHADOW_RADIUS / 2.0f, 0.0f, 0.0f, SHADOW_COLOR);
        SHADOW_PAINT.setAntiAlias(true);
        SHADOW_PAINT.setFilterBitmap(true);
        SHADOW_PAINT.setColor(0xFF000000);
        SHADOW_PAINT.setStyle(Paint.Style.FILL);
    }


    /**
     * A Bitmap associated with its last modification date. This can be used to check
     * whether the book covers should be downloaded again.
     */
    public static class ExpiringBitmap {
        public Bitmap bitmap;
        public Calendar lastModified;
    }

    /**
     * Deletes the specified drawable from the cache. Calling this method will remove
     * the drawable from the in-memory cache and delete the corresponding file from the
     * external storage.
     *
     * @param id The id of the drawable to delete from the cache
     */
    public static void deleteCachedCover(String id) {
        new File(Paths.coverCacheDirectory(), id).delete();
        sArtCache.remove(id);
    }

    /**
     * Retrieves a drawable from the book covers cache, identified by the specified id.
     * If the drawable does not exist in the cache, it is loaded and added to the cache.
     * If the drawable cannot be added to the cache, the specified default drwaable is
     * returned.
     *
     * @param id The id of the drawable to retrieve
     * @param defaultCover The default drawable returned if no drawable can be found that
     *         matches the id
     *
     * @return The drawable identified by id or defaultCover
     */
    public static FastBitmapDrawable getCachedCover(String id, FastBitmapDrawable defaultCover) {
        FastBitmapDrawable drawable = null;

        SoftReference<FastBitmapDrawable> reference = sArtCache.get(id);
        if (reference != null) {
            drawable = reference.get();
        }

        if (drawable == null) {
            final Bitmap bitmap = loadCover(id);
            if (bitmap != null) {
                drawable = new FastBitmapDrawable(bitmap);
            } else {
                drawable = NULL_DRAWABLE;
            }

            sArtCache.put(id, new SoftReference<FastBitmapDrawable>(drawable));
        }

        return drawable == NULL_DRAWABLE ? defaultCover : drawable;
    }

    /**
     * Removes all the callbacks from the drawables stored in the memory cache. This
     * method must be called from the onDestroy() method of any activity using the
     * cached drawables. Failure to do so will result in the entire activity being
     * leaked.
     */
    public static void cleanupCache() {
        for (SoftReference<FastBitmapDrawable> reference : sArtCache.values()) {
            final FastBitmapDrawable drawable = reference.get();
            if (drawable != null) drawable.setCallback(null);
        }
    }

    /**
     * Return the same image with a shadow, scaled by the specified amount..
     *
     * @param bitmap The bitmap to decor with a shadow
     * @param width The target width of the decored bitmap
     * @param height The target height of the decored bitmap
     *
     * @return A new Bitmap based on the original bitmap
     */
    public static Bitmap createShadow(Bitmap bitmap, int width, int height) {
        if (bitmap == null) return null;

        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        final float scale = Math.min((float) width / (float) bitmapWidth,
                (float) height / (float) bitmapHeight);

        final int scaledWidth = (int) (bitmapWidth * scale);
        final int scaledHeight = (int) (bitmapHeight * scale);

        return createScaledBitmap(bitmap, scaledWidth, scaledHeight,
                SHADOW_RADIUS, false, SHADOW_PAINT);
    }

    /**
     * Create a book cover with the specified bitmap. This method applies several
     * lighting effects to the original bitmap and returns a new decored bitmap.
     *
     * @param bitmap The bitmap to decor with lighting effects
     * @param width The target width of the decored bitmap
     * @param height The target height of the decored bitmap
     *
     * @return A new Bitmap based on the original bitmap
     */
    public static Bitmap createBookCover(Bitmap bitmap, int width, int height) {
        final int bitmapWidth = bitmap.getWidth();
        final int bitmapHeight = bitmap.getHeight();

        final float scale = Math.min((float) width / (float) bitmapWidth,
                (float) height / (float) bitmapHeight);

        final int scaledWidth = (int) (bitmapWidth * scale);
        final int scaledHeight = (int) (bitmapHeight * scale);

        final Bitmap decored = createScaledBitmap(bitmap, scaledWidth, scaledHeight,
                SHADOW_RADIUS, true, SHADOW_PAINT);
        final Canvas canvas = new Canvas(decored);

        canvas.translate(SHADOW_RADIUS / 2.0f, SHADOW_RADIUS / 2.0f);
        canvas.drawRect(EDGE_START, 0.0f, EDGE_END, scaledHeight, EDGE_PAINT);
        canvas.drawRect(FOLD_START, 0.0f, FOLD_END, scaledHeight, FOLD_PAINT);
        //noinspection PointlessArithmeticExpression
        canvas.translate(scaledWidth - (EDGE_END - EDGE_START), 0.0f);
        canvas.drawRect(EDGE_START, 0.0f, EDGE_END, scaledHeight, END_EDGE_PAINT);

        return decored;
    }

    private static Bitmap loadCover(String id) {
    //  Bitmap bitmap = book.loadCover(0);
    //  if (bitmap != null) {
//          bitmap = ImageUtilities.createBookCover(bitmap, BOOK_COVER_WIDTH, BOOK_COVER_HEIGHT);
//          
//          // 创建缓存文件夹 TODO 初始化的时候创建
//          File cacheDirectory = new File(Paths.coverCacheDirectory());
//          if (!cacheDirectory.exists()) {
//              cacheDirectory.mkdirs();
//          }
    //
//          File coverFile = new File(cacheDirectory, book.getInternalId());
//          FileOutputStream out = null;
//          try {
//              out = new FileOutputStream(coverFile);
//              bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
//          } catch (FileNotFoundException e) {
//              return null;
//          } finally {
//          	try {
//          		out.close();
//              } catch (IOException e) {
//              }
//          }
    //  }
        final File file = new File(Paths.coverCacheDirectory(), id);
        if (file.exists()) {
            InputStream stream = null;
            try {
                stream = new FileInputStream(file);
                return BitmapFactory.decodeStream(stream, null, null);
            } catch (FileNotFoundException e) {
                // Ignore
            } finally {
            	try {
            		stream.close();
            	}  catch (IOException e) {
                }
            }
        }
        return null;
    }

    public static Bitmap createScaledBitmap(Bitmap src, int dstWidth, int dstHeight,
            float offset, boolean clipShadow, Paint paint) {
        
        Matrix m;
        synchronized (Bitmap.class) {
            m = sScaleMatrix;
            sScaleMatrix = null;
        }

        if (m == null) {
            m = new Matrix();
        }

        final int width = src.getWidth();
        final int height = src.getHeight();
        final float sx = dstWidth  / (float) width;
        final float sy = dstHeight / (float) height;
        m.setScale(sx, sy);

        Bitmap b = createBitmap(src, 0, 0, width, height, m, offset, clipShadow, paint);

        synchronized (Bitmap.class) {
            sScaleMatrix = m;
        }

        return b;
    }

    private static Bitmap createBitmap(Bitmap source, int x, int y, int width,
            int height, Matrix m, float offset, boolean clipShadow, Paint paint) {

        int scaledWidth = width;
        int scaledHeight = height;

        final Canvas canvas = new Canvas();
        canvas.translate(offset / 2.0f, offset / 2.0f);

        Bitmap bitmap;

        final Rect from = new Rect(x, y, x + width, y + height);
        final RectF to = new RectF(0, 0, width, height);

        if (m == null || m.isIdentity()) {
            bitmap = Bitmap.createBitmap(scaledWidth + (int) offset,
                    scaledHeight + (int) (clipShadow ? (offset / 2.0f) : offset),
                    Bitmap.Config.ARGB_8888);
            paint = null;
        } else {
            RectF mapped = new RectF();
            m.mapRect(mapped, to);

            scaledWidth = Math.round(mapped.width());
            scaledHeight = Math.round(mapped.height());

            bitmap = Bitmap.createBitmap(scaledWidth + (int) offset,
                    scaledHeight + (int) (clipShadow ? (offset / 2.0f) : offset),
                    Bitmap.Config.ARGB_8888);
            canvas.translate(-mapped.left, -mapped.top);
            canvas.concat(m);
        }

        canvas.setBitmap(bitmap);
        canvas.drawRect(0.0f, 0.0f, width, height, paint);
        canvas.drawBitmap(source, from, to, SCALE_PAINT);

        return bitmap;
    }
}
