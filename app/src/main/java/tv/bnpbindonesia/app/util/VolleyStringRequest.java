package tv.bnpbindonesia.app.util;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.StringRequest;

public class VolleyStringRequest extends StringRequest {

	public VolleyStringRequest(int method, String url, Listener<String> listener, ErrorListener errorListener) {
		super(method, url, listener, errorListener);
		
		setShouldCache(false);
		setRetryPolicy(new DefaultRetryPolicy(30 * 1000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
	}

}
