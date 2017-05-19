package tv.bnpbindonesia.app.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tv.bnpbindonesia.app.MainActivity;
import tv.bnpbindonesia.app.R;
import tv.bnpbindonesia.app.adapter.ContentAdapter;
import tv.bnpbindonesia.app.object.Alert;
import tv.bnpbindonesia.app.object.Contact;
import tv.bnpbindonesia.app.object.ItemObject;
import tv.bnpbindonesia.app.object.Profile;
import tv.bnpbindonesia.app.share.Config;
import tv.bnpbindonesia.app.share.Function;
import tv.bnpbindonesia.app.util.VolleySingleton;
import tv.bnpbindonesia.app.util.VolleyStringRequest;

public class ContactFragment extends Fragment {
    private static final String TAG = ContactFragment.class.getSimpleName();
    private static final String TAG_CONTACT = "contact";

    private static final String ARG_HEADER = "header";

    private static final int STATE_REQUEST_CONTACT = -1;
    private static final int STATE_DONE = 0;

    private static final int LAYOUT_ERROR = -1;
    private static final int LAYOUT_LOADING = 0;
    private static final int LAYOUT_CONTENT = 1;

    private static final int VIDEO_LAYOUT_ERROR = -1;
    private static final int VIDEO_LAYOUT_LOADING = 0;
    private static final int VIDEO_LAYOUT_READY = 1;

    private int state = STATE_REQUEST_CONTACT;

    private String header;
    private Contact contact;

    private ArrayList<ItemObject> datas = new ArrayList<>();

    private ContentAdapter adapter;
    private GridLayoutManager layoutManager;

    private LinearLayout layoutContent;
    private RecyclerView viewVideos;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private TextView viewErrorMessage;
    private Button viewRetry;

    public ContactFragment() {
        // Required empty public constructor
    }

    public static ContactFragment newInstance(String header) {
        ContactFragment fragment = new ContactFragment();
        Bundle args = new Bundle();
        args.putString(ARG_HEADER, header);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            header = getArguments().getString(ARG_HEADER);
        }

        adapter = new ContentAdapter(getActivity(), this, datas);
        layoutManager = new GridLayoutManager(getActivity(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (datas.get(position).type) {
                    case ContentAdapter.TYPE_STATE_ERROR:
                    case ContentAdapter.TYPE_STATE_LOADING:
                    case ContentAdapter.TYPE_STATE_IDLE:
                    case ContentAdapter.TYPE_HEADER:
                    case ContentAdapter.TYPE_DESCRIPTION:
                        return 2;
                    default:
                        return -1;
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact, container, false);

        layoutContent = (LinearLayout) rootView.findViewById(R.id.layout_content);
        viewVideos = (RecyclerView) rootView.findViewById(R.id.videos);
        layoutLoading = (LinearLayout) rootView.findViewById(R.id.layout_loading);
        layoutError = (LinearLayout) rootView.findViewById(R.id.layout_error);
        viewErrorMessage = (TextView) rootView.findViewById(R.id.error_message);
        viewRetry = (Button) rootView.findViewById(R.id.retry);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewVideos.setLayoutManager(layoutManager);
        viewVideos.setAdapter(adapter);
        viewRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == STATE_REQUEST_CONTACT) {
                    startRequestContact();
                }
            }
        });

        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        if (state == STATE_REQUEST_CONTACT) {
            startRequestContact();
        } else if (state == STATE_DONE) {
            switchLayout(LAYOUT_CONTENT, null);
        }

        FirebaseAnalytics.getInstance(getActivity()).logEvent("screen_contact", null);
    }

    @Override
    public void onStop() {
        super.onStop();

        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_CONTACT);
    }

    private void switchLayout(int layout, String errorMessage) {
        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        switch (layout) {
            case LAYOUT_CONTENT:
                layoutContent.setVisibility(View.VISIBLE);
                break;
            case LAYOUT_LOADING:
                layoutLoading.setVisibility(View.VISIBLE);
                break;
            case LAYOUT_ERROR:
                viewErrorMessage.setText(errorMessage);
                layoutError.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void fillData() {
        datas.clear();

        datas.add(new ItemObject(ContentAdapter.TYPE_HEADER, header));
        datas.add(new ItemObject(ContentAdapter.TYPE_DESCRIPTION, contact.kontak));
        adapter.notifyDataSetChanged();

        switchLayout(LAYOUT_CONTENT, null);
    }

    private void startRequestContact() {
        switchLayout(LAYOUT_LOADING, null);

        VolleyStringRequest request = new VolleyStringRequest(
                Request.Method.POST,
                Config.URL_BASE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().create();
                        try {
                            contact = gson.fromJson(response, Contact.class);
                            fillData();

                            state = STATE_DONE;
                        } catch (Exception e) {
                            switchLayout(LAYOUT_ERROR, getString(R.string.json_format_error));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        switchLayout(LAYOUT_ERROR, Function.parseVolleyError(getActivity(), error));
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("function", "kontak");
                return params;
            }
        };
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_CONTACT);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_CONTACT);
    }
}