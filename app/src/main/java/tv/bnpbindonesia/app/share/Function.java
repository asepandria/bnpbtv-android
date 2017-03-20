package tv.bnpbindonesia.app.share;

import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import tv.bnpbindonesia.app.R;

public class Function {
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

//    public static void openToolbarSearch(Context context, boolean isOpen) {
//        ImageView toolbarLogo = (ImageView) ((Activity) context).findViewById(R.id.toolbar_logo);
//        EditText toolbarSearch = (EditText) ((Activity) context).findViewById(R.id.toolbar_search);
//
//        toolbarLogo.setVisibility(isOpen ? View.GONE : View.VISIBLE);
//        toolbarSearch.setVisibility(isOpen ? View.VISIBLE : View.GONE);
//        toolbarLogo.startAnimation(AnimationUtils.loadAnimation(context, isOpen ? R.anim.toolbar_exit_up : R.anim.toolbar_enter_down));
//        toolbarSearch.startAnimation(AnimationUtils.loadAnimation(context, isOpen ? R.anim.toolbar_enter_up : R.anim.toolbar_exit_down));
//
//        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (isOpen) {
//            toolbarSearch.requestFocus();
//            inputMethodManager.showSoftInput(toolbarSearch, InputMethodManager.SHOW_IMPLICIT);
//        } else {
//            toolbarSearch.setText("");
//            toolbarSearch.clearFocus();
//            inputMethodManager.hideSoftInputFromWindow(toolbarSearch.getWindowToken(), 0);
//        }
//    }

//    public static boolean isToolbarSearch(Context context) {
//        EditText toolbarSearch = (EditText) ((Activity) context).findViewById(R.id.toolbar_search);
//        return toolbarSearch.getVisibility() == View.VISIBLE;
//    }
}