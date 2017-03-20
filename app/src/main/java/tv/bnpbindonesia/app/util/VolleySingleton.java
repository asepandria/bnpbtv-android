package tv.bnpbindonesia.app.util;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;


public class VolleySingleton {
    public static final String TAG = VolleySingleton.class.getSimpleName();

    private static VolleySingleton instance;
    private RequestQueue requestQueue;
    private ImageLoader imageLoader;

    private VolleySingleton(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }
    
    public static synchronized VolleySingleton getInstance(Context context) {
        if (instance == null) {
            instance = new VolleySingleton(context);
        }
        return instance;
    }
    
    public RequestQueue getRequestQueue() {
        return requestQueue;
    }
 
	public ImageLoader getImageLoader(Context context, boolean horizontal) {
        getRequestQueue();
        if (imageLoader == null) {
            imageLoader = new ImageLoader(requestQueue, new LruBitmapCache(context, horizontal));
        }
        
        return imageLoader;
    }
 
	public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(tag.isEmpty() ? TAG : tag);
        if(requestQueue != null) {
            requestQueue.add(req);
        }
    }
	
	public <T> void addToRequestQueue(Request<T> req) {
        req.setTag(TAG);
        if(requestQueue != null) {
            requestQueue.add(req);
        }
    }
 
    public void cancelPendingRequests(Object tag) {
        if(requestQueue != null) {
            requestQueue.cancelAll(tag);
        }
    }
}