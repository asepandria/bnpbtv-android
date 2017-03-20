package tv.bnpbindonesia.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.DisplayMetrics;

import com.android.volley.toolbox.ImageLoader.ImageCache;

public class LruBitmapCache extends LruCache<String, Bitmap> implements ImageCache {
	private static Context context;
	private static boolean horizontal;
	
	public static int getDefaultLruCacheSize(Context context, boolean horizontal) {
		LruBitmapCache.context = context;
		LruBitmapCache.horizontal = horizontal;
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        return cacheSize;
    }

	public LruBitmapCache(Context context, boolean horizontal) {
        this(getDefaultLruCacheSize(context, horizontal));
    }

    public LruBitmapCache(int sizeInKiloBytes) {
        super(sizeInKiloBytes);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight() / 1024;
    }

    @Override
    public Bitmap getBitmap(String url) {
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        put(url, resizeBitmap(bitmap));
    }
    
    private Bitmap resizeBitmap(Bitmap bitmap) {
		final DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
		final int screenWidth = displayMetrics.widthPixels;
		final int screenHeight = displayMetrics.heightPixels;
		int width = 0;
		int height = 0;
		
		if(horizontal) {
			width = screenWidth;
			height = screenWidth * bitmap.getHeight() / bitmap.getWidth();
			if(height < screenHeight) {
				width = screenHeight * bitmap.getWidth() / bitmap.getHeight();
				height = screenHeight;
			}
		} else {
			width = screenHeight * bitmap.getWidth() / bitmap.getHeight();
			height = screenHeight;
			if(width < screenWidth) {
				width = screenWidth;
				height = screenWidth * bitmap.getHeight() / bitmap.getWidth();
			}
		}

		return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }
}

/*
public class LruBitmapCache extends LruCache<String, Bitmap>
implements ImageCache {

	public LruBitmapCache(int maxSize) {
		super(maxSize);
	}

	public LruBitmapCache(Context ctx) {
		this(getCacheSize(ctx));
	}

	@Override
	protected int sizeOf(String key, Bitmap value) {
		return value.getRowBytes() * value.getHeight();
	}

	@Override
	public Bitmap getBitmap(String url) {
		return get(url);
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		put(url, bitmap);
	}

	// Returns a cache size equal to approximately three screens worth of images.
	public static int getCacheSize(Context ctx) {
		final DisplayMetrics displayMetrics = ctx.getResources().getDisplayMetrics();
		final int screenWidth = displayMetrics.widthPixels;
		final int screenHeight = displayMetrics.heightPixels;
		// 4 bytes per pixel
		final int screenBytes = screenWidth * screenHeight * 4;

		return screenBytes * 3;
	}
}
*/