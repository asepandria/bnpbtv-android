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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tv.bnpbindonesia.app.R;
import tv.bnpbindonesia.app.adapter.ContentAdapter;
import tv.bnpbindonesia.app.gson.GsonAlertHistory;
import tv.bnpbindonesia.app.gson.GsonVideo;
import tv.bnpbindonesia.app.object.Alert;
import tv.bnpbindonesia.app.object.ItemObject;
import tv.bnpbindonesia.app.object.Video;
import tv.bnpbindonesia.app.share.Config;
import tv.bnpbindonesia.app.share.Function;
import tv.bnpbindonesia.app.util.VolleySingleton;
import tv.bnpbindonesia.app.util.VolleyStringRequest;

public class AlertHistoryFragment extends Fragment {
    private static final String TAG = AlertHistoryFragment.class.getSimpleName();
    private static final String TAG_ALERTS = "alerts";

    private static final String ARG_TITLE = "title";

    private static final int STATE_REQUEST_ALERTS = -1;
    private static final int STATE_DONE = 0;

    private static final int LAYOUT_ERROR = -2;
    private static final int LAYOUT_LOADING = -1;
    private static final int LAYOUT_EMPTY = 0;
    private static final int LAYOUT_CONTENT = 1;

    private String title;

    private int state = STATE_REQUEST_ALERTS;

    private int currentPage;
    private ArrayList<ItemObject> datas = new ArrayList<>();

    private DisplayMetrics displayMetrics;
    private ContentAdapter adapter;
    private GridLayoutManager layoutManager;

    private TextView viewTitle;
    private LinearLayout layoutContent;
    private RecyclerView viewAlerts;
    private LinearLayout layoutLoading;
    private FrameLayout layoutEmpty;
    private LinearLayout layoutError;
    private TextView viewErrorMessage;
    private Button viewRetry;

    public AlertHistoryFragment() {
        // Required empty public constructor
    }

    public static AlertHistoryFragment newInstance(String title) {
        AlertHistoryFragment fragment = new AlertHistoryFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
        }

        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

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
                        return 2;
                    case ContentAdapter.TYPE_ALERT:
                        return 1;
                    default:
                        return -1;
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_alert_history, container, false);

        viewTitle = (TextView) rootView.findViewById(R.id.title);
        layoutContent = (LinearLayout) rootView.findViewById(R.id.layout_content);
        viewAlerts = (RecyclerView) rootView.findViewById(R.id.alerts);
        layoutLoading = (LinearLayout) rootView.findViewById(R.id.layout_loading);
        layoutEmpty = (FrameLayout) rootView.findViewById(R.id.layout_empty);
        layoutError = (LinearLayout) rootView.findViewById(R.id.layout_error);
        viewErrorMessage = (TextView) rootView.findViewById(R.id.error_message);
        viewRetry = (Button) rootView.findViewById(R.id.retry);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewTitle.setText(title);
        viewAlerts.setLayoutManager(layoutManager);
        viewAlerts.setAdapter(adapter);
        viewAlerts.getRecycledViewPool().setMaxRecycledViews(ContentAdapter.TYPE_ALERT, 0);
        viewAlerts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int lastView = ((GridLayoutManager) recyclerView.getLayoutManager()).findLastVisibleItemPosition();
                if (lastView > -1 && lastView < datas.size()) {
                    if (datas.get(lastView).type == ContentAdapter.TYPE_STATE_ERROR || datas.get(lastView).type == ContentAdapter.TYPE_STATE_IDLE) {
                        recyclerView.post(new Runnable() {
                            @Override
                            public void run() {
                                startRequestAlerts(true);
                            }
                        });
                    }
                }
            }
        });
        viewRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRequestAlerts(false);
            }
        });

        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        if (state == STATE_REQUEST_ALERTS) {
            startRequestAlerts(false);
        } else if (state == STATE_DONE) {
            if (datas.size() == 0) {
                switchLayout(LAYOUT_EMPTY, null);
            } else {
                switchLayout(LAYOUT_CONTENT, null);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_ALERTS);
    }

    private void switchLayout(int layout, String errorMessage) {
        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        switch (layout) {
            case LAYOUT_CONTENT:
                layoutContent.setVisibility(View.VISIBLE);
                break;
            case LAYOUT_LOADING:
                layoutLoading.setVisibility(View.VISIBLE);
                break;
            case LAYOUT_EMPTY:
                layoutEmpty.setVisibility(View.VISIBLE);
                break;
            case LAYOUT_ERROR:
                viewErrorMessage.setText(errorMessage);
                layoutError.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void fillData(boolean isLoadMore, int totalPage, int currentPage, ArrayList<Alert> alerts) {
        this.currentPage = currentPage;

        if (isLoadMore) {
            datas.remove(datas.size() - 1);
            adapter.notifyItemRemoved(datas.size());
        } else {
            datas.clear();
            adapter.notifyDataSetChanged();
        }

        int lastSize = datas.size();

        if (alerts != null) {
            for (Alert alert : alerts) {
                datas.add(new ItemObject(ContentAdapter.TYPE_ALERT, alert));
            }
        }

        if (datas.size() == 0) {
            switchLayout(LAYOUT_EMPTY, null);
        } else {
            if (totalPage != currentPage) {
                datas.add(new ItemObject(ContentAdapter.TYPE_STATE_IDLE, null));
            }
            switchLayout(LAYOUT_CONTENT, null);
        }

        if (lastSize < datas.size()) {
            adapter.notifyItemRangeInserted(lastSize, datas.size() - lastSize);
        }
    }

    public void startRequestAlerts(final boolean isLoadMore) {
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
                            GsonAlertHistory gsonAlertHistory = gson.fromJson(response, GsonAlertHistory.class);
                            fillData(isLoadMore, gsonAlertHistory.total_page, gsonAlertHistory.current_page, gsonAlertHistory.items);

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
                params.put("function", "arsip_bencana");
                if (isLoadMore) {
                    params.put("page", String.valueOf(currentPage + 1));
                }
                return params;
            }
        };
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_ALERTS);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_ALERTS);
    }
}