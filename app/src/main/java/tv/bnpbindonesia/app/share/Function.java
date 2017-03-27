package tv.bnpbindonesia.app.share;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.model.LatLng;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tv.bnpbindonesia.app.R;

public class Function {
    private static final String TAG = Function.class.getSimpleName();

    public static String parseVolleyError(Context context, VolleyError error) {
        String result = "";

        if (error instanceof NoConnectionError) {
            result = context.getString(R.string.volley_no_connection_error);
        } else if (error instanceof ServerError) {
            result = context.getString(R.string.volley_server_error);
        } else if (error instanceof AuthFailureError) {
            result = context.getString(R.string.volley_auth_failure_error);
        } else if (error instanceof ParseError) {
            result = context.getString(R.string.volley_parse_error);
        } else if (error instanceof NetworkError) {
            result = context.getString(R.string.volley_network_error);
        } else if (error instanceof TimeoutError) {
            result = context.getString(R.string.volley_timeout_error);
        } else {
            result = context.getString(R.string.volley_unknown_error);
        }

        return result;
    }

    public static String ConvertMSTOHMS(int ms) {
        int hour = ms / (1000 * 60 * 60);
        if (hour > 0) ms = ms - (1000 * 60 * hour);
        int minute = ms / (1000 * 60);
        if (minute > 0) ms = ms - (1000 * 60 * minute);
        int second = ms / 1000;
        String Hour = hour > 0 ? String.valueOf(hour) + ":" : "";
        String Minute = minute > 0 ? (minute < 10 ? "0" : "") + String.valueOf(minute) + ":" : "00:";
        String Second = second > 0 ? (second < 10 ? "0" : "") + String.valueOf(second) : "00";

        return Hour + Minute + Second;
    }

    public static LatLng getLatLng(String url) {
        LatLng position = null;
        Pattern pattern = Pattern.compile(".*/@(.*?)z/.*");
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            try {
                String[] matches = matcher.group(1).split(",");
                Double lat = Double.valueOf(matches[0]);
                Double lng = Double.valueOf(matches[1]);
                position = new LatLng(lat, lng);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return position;
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html){
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }
}