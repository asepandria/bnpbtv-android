package tv.bnpbindonesia.app.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
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
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tv.bnpbindonesia.app.MainActivity;
import tv.bnpbindonesia.app.R;
import tv.bnpbindonesia.app.adapter.ContentAdapter;
import tv.bnpbindonesia.app.gson.GsonAlert;
import tv.bnpbindonesia.app.gson.GsonVideo;
import tv.bnpbindonesia.app.object.Alert;
import tv.bnpbindonesia.app.object.ItemObject;
import tv.bnpbindonesia.app.object.Video;
import tv.bnpbindonesia.app.share.Config;
import tv.bnpbindonesia.app.share.Function;
import tv.bnpbindonesia.app.util.VolleySingleton;
import tv.bnpbindonesia.app.util.VolleyStringRequest;

public class AlertFragment extends Fragment {
    private static final String TAG = AlertFragment.class.getSimpleName();
    private static final String TAG_ALERT = "alert";
    private static final String TAG_VIDEOS = "videos";

    private static final String ARG_ID = "id";

    private static final String BUNDLE_KEY_MAP = "alert-map";

    private static final int STATE_REQUEST_ALERT = -2;
    private static final int STATE_REQUEST_VIDEOS = -1;
    private static final int STATE_DONE = 0;

    private static final int LAYOUT_ERROR = -1;
    private static final int LAYOUT_LOADING = 0;
    private static final int LAYOUT_CONTENT = 1;

    private int state = STATE_REQUEST_ALERT;

    private String id;
    private Alert alert;

    private int currentPage;
    private ArrayList<ItemObject> datas = new ArrayList<>();

    private DisplayMetrics displayMetrics;
    private ContentAdapter adapter;
    private GridLayoutManager layoutManager;

    private LinearLayout layoutContent;
    private FrameLayout layoutAlert;
    private MapView viewMap;
    private LinearLayout layoutMore;
    private TextView viewAlertType;
    private TextView viewAlertAddress;
    private RecyclerView viewVideos;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private TextView viewErrorMessage;
    private Button viewRetry;

    public AlertFragment() {
        // Required empty public constructor
    }

    public static AlertFragment newInstance(String id) {
        AlertFragment fragment = new AlertFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            id = getArguments().getString(ARG_ID);
        }

        MapsInitializer.initialize(getContext());

        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        adapter = new ContentAdapter(getActivity(), this, datas);
        layoutManager = new GridLayoutManager(getActivity(), 11);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (datas.get(position).type) {
                    case ContentAdapter.TYPE_STATE_ERROR:
                    case ContentAdapter.TYPE_STATE_LOADING:
                    case ContentAdapter.TYPE_STATE_IDLE:
                    case ContentAdapter.TYPE_HEADER:
                        return 11;
                    case ContentAdapter.TYPE_PREVIEW_IMAGE:
                        return 6;
                    case ContentAdapter.TYPE_PREVIEW_DESCRIPTION:
                        return 5;
                    default:
                        return -1;
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alert, container, false);

        layoutContent = (LinearLayout) rootView.findViewById(R.id.layout_content);
        layoutAlert = (FrameLayout) rootView.findViewById(R.id.layout_alert);
        viewMap = (MapView) rootView.findViewById(R.id.map);
        layoutMore = (LinearLayout) rootView.findViewById(R.id.layout_more);
        viewAlertType = (TextView) rootView.findViewById(R.id.alert_type);
        viewAlertAddress = (TextView) rootView.findViewById(R.id.alert_address);
        viewVideos = (RecyclerView) rootView.findViewById(R.id.videos);
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
        layoutMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).onSelectAlert(alert);
            }
        });
        viewVideos.setLayoutManager(layoutManager);
        viewVideos.setAdapter(adapter);
        viewVideos.getRecycledViewPool().setMaxRecycledViews(ContentAdapter.TYPE_PREVIEW_IMAGE, 0);
        viewVideos.getRecycledViewPool().setMaxRecycledViews(ContentAdapter.TYPE_PREVIEW_DESCRIPTION, 0);
        viewVideos.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int lastView = ((GridLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                if (lastView > -1 && lastView < datas.size()) {
                    if (datas.get(lastView).type == ContentAdapter.TYPE_STATE_ERROR || datas.get(lastView).type == ContentAdapter.TYPE_STATE_IDLE) {
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                startRequestVideos(true);
                            }
                        });
                    }
                }
            }
        });
        viewRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == STATE_REQUEST_ALERT) {
                    startRequestAlert();
                } else if (state == STATE_REQUEST_VIDEOS) {
                    startRequestVideos(false);
                }
            }
        });

        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        if (state == STATE_REQUEST_ALERT) {
            startRequestAlert();
        } else if (state == STATE_REQUEST_VIDEOS) {
            startRequestVideos(false);
        } else if (state == STATE_DONE) {
            fillContent();
        }

        FirebaseAnalytics.getInstance(getActivity()).logEvent("screen_alert", null);
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
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_VIDEOS);
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
                MapsInitializer.initialize(getContext());

                googleMap.clear();
//                    googleMap.getUiSettings().setAllGesturesEnabled(false);
//                LatLng position = Function.getLatLng(alert.googlemaps);
                LatLng position = Function.getLatLng(alert.latlong);
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
    }

    private void fillData(boolean isLoadMore, int totalPage, int currentPage, ArrayList<Video> videos) {
        this.currentPage = currentPage;

        if (isLoadMore) {
            datas.remove(datas.size() - 1);
            adapter.notifyItemRemoved(datas.size());
        } else {
            datas.clear();
            adapter.notifyDataSetChanged();
        }

        int lastSize = datas.size();

        if (videos != null) {
            int i = 0;
            for (Video video : videos) {
                int type = i % 2 == 0 ? ContentAdapter.TYPE_PREVIEW_IMAGE : ContentAdapter.TYPE_PREVIEW_DESCRIPTION;
                datas.add(new ItemObject(type, video));
                i++;
            }
        }

        if (datas.size() == 0) {
            datas.add(new ItemObject(ContentAdapter.TYPE_HEADER, null));
        } else if (totalPage != currentPage) {
            datas.add(new ItemObject(ContentAdapter.TYPE_STATE_IDLE, null));
        }

        if (!isLoadMore) {
            fillContent();
        }

        if (lastSize < datas.size()) {
            adapter.notifyItemRangeInserted(lastSize, datas.size() - lastSize);
        }
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
                            alert = gsonAlert.items.get(0);

                            state = STATE_REQUEST_VIDEOS;
                            startRequestVideos(false);
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

    public void startRequestVideos(final boolean isLoadMore) {
        if (isLoadMore) {
            datas.remove(datas.size() - 1);
            datas.add(new ItemObject(ContentAdapter.TYPE_STATE_LOADING, null));
            adapter.notifyItemChanged(datas.size() - 1);
        } else {
            datas.clear();
            adapter.notifyDataSetChanged();
            switchLayout(LAYOUT_LOADING, null);
        }

        VolleyStringRequest request = new VolleyStringRequest(
                Request.Method.POST,
                Config.URL_BASE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().create();
                        try {
                            GsonVideo gsonVideo = gson.fromJson(response, GsonVideo.class);
                            fillData(isLoadMore, gsonVideo.total_page, gsonVideo.current_page, gsonVideo.video);

                            state = STATE_DONE;
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (isLoadMore) {
                                datas.remove(datas.size() - 1);
                                datas.add(new ItemObject(ContentAdapter.TYPE_STATE_ERROR, getString(R.string.json_format_error)));
                                adapter.notifyItemChanged(datas.size() - 1);
                            } else {
                                switchLayout(LAYOUT_ERROR, getString(R.string.json_format_error));
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isLoadMore) {
                            datas.remove(datas.size() - 1);
                            datas.add(new ItemObject(ContentAdapter.TYPE_STATE_ERROR, Function.parseVolleyError(getActivity(), error)));
                            adapter.notifyItemChanged(datas.size() - 1);
                        } else {
                            switchLayout(LAYOUT_ERROR, Function.parseVolleyError(getActivity(), error));
                        }
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("function", "video");
                if (isLoadMore) {
                    params.put("page", String.valueOf(currentPage + 1));
                }
                return params;
            }
        };
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_VIDEOS);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_VIDEOS);
    }
}