package tv.bnpbindonesia.app.share;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.ShareCompat;

public class ShareSocialMedia {
    public static final String TAG = ShareSocialMedia.class.getSimpleName();

    public static final int SHARE_FB = 1;
    public static final int SHARE_GPLUS = 2;
    public static final int SHARE_TWITTER = 3;

    public static void onShare(Context context, String url, int socialMedia) {
        String appSocialMedia = "";
        switch (socialMedia) {
            case SHARE_FB:
                appSocialMedia = "com.facebook.katana";
                break;
            case SHARE_GPLUS:
                appSocialMedia = "com.google.android.apps.plus";
                break;
            case SHARE_TWITTER:
                appSocialMedia = "com.com.twitter.android";
                break;
        }

        Intent intentShareApp = ShareCompat.IntentBuilder.from((Activity) context)
                .setType("text/plain")
                .setSubject("subject")
                .setText(url)
                .getIntent()
                .setPackage(appSocialMedia);

        try {
            context.startActivity(intentShareApp);
        } catch (ActivityNotFoundException e) {
            String urlSocialMedia = "";
            switch (socialMedia) {
                case SHARE_FB:
                    urlSocialMedia = "https://www.facebook.com/sharer/sharer.php?u=" + url;
                    break;
                case SHARE_GPLUS:
                    urlSocialMedia = "https://plus.google.com/share?url=" + url;
                    break;
                case SHARE_TWITTER:
                    urlSocialMedia = "https://twitter.com/intent/tweet?text=" + url;
                    break;
            }
            Intent intentShareWeb = new Intent(Intent.ACTION_VIEW, Uri.parse(urlSocialMedia));
            context.startActivity(intentShareWeb);
        }
    }
}