package tv.bnpbindonesia.app.service;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {
    private static final String TAG = MyFirebaseInstanceIdService.class.getSimpleName();
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        if (refreshedToken != null) {
            FirebaseMessaging.getInstance().subscribeToTopic("alert");
        }
    }
}