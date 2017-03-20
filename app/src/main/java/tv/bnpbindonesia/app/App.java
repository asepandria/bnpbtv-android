package tv.bnpbindonesia.app;

import android.app.Application;
import android.content.Context;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        ImageLoaderConfiguration config = new ImageLoaderConfiguration
                .Builder(getApplicationContext())
                .threadPoolSize(6)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .imageDownloader(new BaseImageDownloader(getApplicationContext(), 30 * 1000, 30 * 1000))
                .memoryCache(new LruMemoryCache(cacheSize))
                .memoryCacheSize(cacheSize)
                .diskCacheSize(50 * 1024 * 1024)
                .diskCacheFileCount(100)
//                .writeDebugLogs()
                .build();

        ImageLoader.getInstance().init(config);
        ImageLoader.getInstance().handleSlowNetwork(true);
    }
}
