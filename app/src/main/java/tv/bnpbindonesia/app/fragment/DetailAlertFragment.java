package tv.bnpbindonesia.app.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import me.relex.circleindicator.CircleIndicator;
import tv.bnpbindonesia.app.R;
import tv.bnpbindonesia.app.adapter.ContentAdapter;
import tv.bnpbindonesia.app.adapter.ImageAdapter;
import tv.bnpbindonesia.app.gson.GsonAlert;
import tv.bnpbindonesia.app.gson.GsonVideo;
import tv.bnpbindonesia.app.object.Alert;
import tv.bnpbindonesia.app.object.ItemObject;
import tv.bnpbindonesia.app.object.Video;
import tv.bnpbindonesia.app.share.Config;
import tv.bnpbindonesia.app.share.Function;
import tv.bnpbindonesia.app.share.ShareSocialMedia;
import tv.bnpbindonesia.app.util.VolleySingleton;
import tv.bnpbindonesia.app.util.VolleyStringRequest;

public class DetailAlertFragment extends Fragment {
    private static final String TAG = DetailAlertFragment.class.getSimpleName();
    private static final String TAG_ALERT = "alert";

    private static final String ARG_ID = "id";
    private static final String ARG_ALERT = "alert";

    private static final String BUNDLE_KEY_MAP = "detail-alert-map";


    private static final int STATE_REQUEST_ALERT = -1;
    private static final int STATE_DONE = 0;

    private static final int LAYOUT_ERROR = -1;
    private static final int LAYOUT_LOADING = 0;
    private static final int LAYOUT_CONTENT = 1;

    private int state = STATE_REQUEST_ALERT;

    private String id;
    private Alert alert;

    private ArrayList<String> images = new ArrayList<>();

    private ImageAdapter adapter;
    private DisplayMetrics displayMetrics;

    private LinearLayout layoutContent;
    private FrameLayout layoutAlert;
    private MapView viewMap;
    private TextView viewAlertType;
    private TextView viewAlertAddress;
    private ViewPager viewImages;
    private CircleIndicator viewIndicator;
    private TextView viewType;
    private TextView viewDetail;
    private TextView viewTitle;
    private TextView viewDescription;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private TextView viewErrorMessage;
    private Button viewRetry;

    public DetailAlertFragment() {
        // Required empty public constructor
    }

    public static DetailAlertFragment newInstance(String id, Alert alert) {
        DetailAlertFragment fragment = new DetailAlertFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putSerializable(ARG_ALERT, alert);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            id = getArguments().getString(ARG_ID);
            alert = (Alert) getArguments().getSerializable(ARG_ALERT);
        }
        state = alert == null ? STATE_REQUEST_ALERT : STATE_DONE;

        adapter = new ImageAdapter(getFragmentManager(), images);

        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail_alert, container, false);

        layoutContent = (LinearLayout) rootView.findViewById(R.id.layout_content);
        layoutAlert = (FrameLayout) rootView.findViewById(R.id.layout_alert);
        viewMap = (MapView) rootView.findViewById(R.id.map);
        viewAlertType = (TextView) rootView.findViewById(R.id.alert_type);
        viewAlertAddress = (TextView) rootView.findViewById(R.id.alert_address);
        viewImages = (ViewPager) rootView.findViewById(R.id.images);
        viewIndicator = (CircleIndicator) rootView.findViewById(R.id.indicator);
        viewType = (TextView) rootView.findViewById(R.id.type);
        viewDetail = (TextView) rootView.findViewById(R.id.detail);
        viewTitle = (TextView) rootView.findViewById(R.id.title);
        viewDescription = (TextView) rootView.findViewById(R.id.description);
        layoutLoading = (LinearLayout) rootView.findViewById(R.id.layout_loading);
        layoutError = (LinearLayout) rootView.findViewById(R.id.layout_error);
        viewErrorMessage = (TextView) rootView.findViewById(R.id.error_message);
        viewRetry = (Button) rootView.findViewById(R.id.retry);

        Bundle bundle = null;
        if (savedInstanceState != null) {
            bundle = savedInstanceState.getBundle(BUNDLE_KEY_MAP);
        }
        viewMap.onCreate(bundle);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        layoutAlert.getLayoutParams().height = displayMetrics.widthPixels * 9 / 16;
        layoutAlert.requestLayout();
        viewImages.getLayoutParams().height = displayMetrics.widthPixels * 9 / 16;
        viewImages.requestLayout();
        viewImages.setAdapter(adapter);
        viewIndicator.setViewPager(viewImages);
        adapter.registerDataSetObserver(viewIndicator.getDataSetObserver());
        viewRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == STATE_REQUEST_ALERT) {
                    startRequestAlert();
                }
            }
        });

        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        if (state == STATE_REQUEST_ALERT) {
            startRequestAlert();
        } else {
            fillContent();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle bundle = outState.getBundle(BUNDLE_KEY_MAP);
        if (bundle == null) {
            bundle = new Bundle();
            outState.putBundle(BUNDLE_KEY_MAP, bundle);
        }

        viewMap.onSaveInstanceState(bundle);
    }

    @Override
    public void onStart() {
        super.onStart();

        viewMap.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        viewMap.onResume();
    }

    @Override
    public void onPause() {
        viewMap.onPause();

        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        viewMap.onStop();

        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_ALERT);
    }

    @Override
    public void onDestroy() {
        viewMap.onDestroy();

        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        viewMap.onLowMemory();
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

    private void fillContent() {
        switchLayout(LAYOUT_CONTENT, null);

        viewMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.clear();
                LatLng position = Function.getLatLng(alert.googlemaps);
                if (position != null) {
                    googleMap.addMarker(
                            new MarkerOptions()
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_place_black_24dp))
                                    .position(position)
                                    .title(alert.title)
                    );
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 10));
                }
            }
        });
        viewAlertType.setText(alert.type);
        viewAlertAddress.setText(alert.address);
        if (alert.slider.image.size() > 0) {
            viewImages.setVisibility(View.VISIBLE);
            images = alert.slider.image;
            adapter = new ImageAdapter(getFragmentManager(), images);
            viewImages.setAdapter(adapter);
            viewIndicator.setViewPager(viewImages);
            adapter.registerDataSetObserver(viewIndicator.getDataSetObserver());
            ShareSocialMedia.initShareLayout(getActivity(), "http://azkaku.com");
        } else {
            viewImages.setVisibility(View.GONE);
        }
        viewType.setText(alert.type);
        String detail = "<b>Earthquake scale :</b> " + alert.scale + "<br/>" +
                "<b>Fatalities :</b> " + alert.fatalities + "<br/>" +
                "<b>Wound :</b> " + alert.wound;
        viewDetail.setText(Function.fromHtml(detail));
        viewTitle.setText(alert.title);
        viewDescription.setText(alert.description);
    }

    private void startRequestAlert() {
        switchLayout(LAYOUT_LOADING, null);

        VolleyStringRequest request = new VolleyStringRequest(
                Request.Method.POST,
                Config.URL_BASE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().create();
                        try {
                            GsonAlert gsonAlert = gson.fromJson(response, GsonAlert.class);
                            alert = gsonAlert.items;
                            fillContent();

                            state = STATE_DONE;
                        } catch (Exception e) {
                            switchLayout(LAYOUT_ERROR, getString(R.string.json_format_error) + "\n" + e.getMessage());
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
                params.put("function", "alert");
                params.put("id", id);
                return params;
            }
        };
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_ALERT);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_ALERT);
    }
}