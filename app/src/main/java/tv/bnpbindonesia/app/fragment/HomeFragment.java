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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tv.bnpbindonesia.app.MainActivity;
import tv.bnpbindonesia.app.R;
import tv.bnpbindonesia.app.adapter.ContentAdapter;
import tv.bnpbindonesia.app.gson.GsonHeadline;
import tv.bnpbindonesia.app.gson.GsonVideo;
import tv.bnpbindonesia.app.object.Headline;
import tv.bnpbindonesia.app.object.ItemObject;
import tv.bnpbindonesia.app.object.Video;
import tv.bnpbindonesia.app.share.Config;
import tv.bnpbindonesia.app.share.Function;
import tv.bnpbindonesia.app.util.VolleySingleton;
import tv.bnpbindonesia.app.util.VolleyStringRequest;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getSimpleName();
    private static final String TAG_HEADLINE = "headline";
    private static final String TAG_HOME = "home";

    private static final String ARG_IS_ALERT = "is_alert";

    private static final int STATE_REQUEST_HEADLINE = -2;
    private static final int STATE_REQUEST_VIDEO = -1;
    private static final int STATE_DONE = 0;

    private static final int RECOVERY_DIALOG_REQUEST = 1;

    private static final int LAYOUT_ERROR = -1;
    private static final int LAYOUT_LOADING = 0;
    private static final int LAYOUT_CONTENT = 1;

    private static final int VIDEO_LAYOUT_LOADING = 0;
    private static final int VIDEO_LAYOUT_READY = 1;

    private static final int HANDLER_VIDEO_PROGRESS = 0;
    private static final int HANDLER_VIDEO_CONTROLLER_HIDE = 1;

    private boolean isAlert;

    private int state = STATE_REQUEST_HEADLINE;

    private String video = "";
    private String youtube = "";
    private int currentPage;
    private ArrayList<ItemObject> datas = new ArrayList<>();

    private DisplayMetrics displayMetrics;
    private Handler handlerVideo;
    private YouTubePlayer youTubeplayer;
    private YouTubePlayer.OnInitializedListener onInitializedListener;
    private ContentAdapter adapter;
    private GridLayoutManager layoutManager;

    private View rootView;
    private LinearLayout layoutContent;
    private FrameLayout layoutYoutube;
    private YouTubePlayerSupportFragment viewYoutubePlayer;
    private FrameLayout layoutVideo;
    private VideoView viewVideo;
    private ProgressBar viewVideoProgress;
    private ImageView viewVideoPlay;
    private ImageView viewVideoPause;
    private FrameLayout layoutVideoController;
    private TextView viewVideoCurrent;
    private SeekBar viewVideoSeekbar;
    private TextView viewVideoDuration;
    private ImageView viewVideoFullscreen;
    private ImageView viewVideoFullscreenExit;
    private RecyclerView viewVideos;
    private LinearLayout layoutLoading;
    private LinearLayout layoutError;
    private TextView viewErrorMessage;
    private Button viewRetry;

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(boolean isAlert) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_ALERT, isAlert);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isAlert = getArguments().getBoolean(ARG_IS_ALERT);
        }

        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        handlerVideo = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case HANDLER_VIDEO_PROGRESS:
                        viewVideoCurrent.setText(Function.ConvertMSTOHMS(viewVideo.getCurrentPosition()));
                        viewVideoSeekbar.setProgress(viewVideo.getCurrentPosition());
                        sendEmptyMessageDelayed(HANDLER_VIDEO_PROGRESS, 250);
                        break;
                    case HANDLER_VIDEO_CONTROLLER_HIDE:
                        if (layoutVideoController.getVisibility() == View.VISIBLE) {
                            layoutVideoController.setVisibility(View.GONE);
                        }
                }
            }
        };

        onInitializedListener = new YouTubePlayer.OnInitializedListener() {
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean wasRestored) {
                player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE);
                player.setPlaybackEventListener(new YouTubePlayer.PlaybackEventListener() {
                    @Override
                    public void onPlaying() { }

                    @Override
                    public void onPaused() { }

                    @Override
                    public void onStopped() {
                        if (youTubeplayer.getCurrentTimeMillis() == youTubeplayer.getDurationMillis()) {
                            youTubeplayer.setFullscreen(false);
                        }
                    }

                    @Override
                    public void onBuffering(boolean b) { }

                    @Override
                    public void onSeekTo(int i) { }
                });

                if (!wasRestored) {
                    youTubeplayer = player;
//                        player.cueVideo(youtube);
                    player.loadVideo(youtube);
                }
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult errorReason) {
                if (errorReason.isUserRecoverableError()) {
                    errorReason.getErrorDialog(getActivity(), RECOVERY_DIALOG_REQUEST).show();
                } else {
                    String errorMessage = String.format(getString(R.string.error_player), errorReason.toString());
                    Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                }

            }
        };

        adapter = new ContentAdapter(getActivity(), this, datas);
        layoutManager = new GridLayoutManager(getActivity(), 11);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (datas.get(position).type) {
                    case ContentAdapter.TYPE_STATE_ERROR:
                    case ContentAdapter.TYPE_STATE_LOADING:
                    case ContentAdapter.TYPE_STATE_IDLE:
                    case ContentAdapter.TYPE_EMPTY:
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
        if (rootView != null) {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
        } else {
            try {
                rootView = inflater.inflate(R.layout.fragment_home, container, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        layoutContent = (LinearLayout) rootView.findViewById(R.id.layout_content);
        layoutYoutube = (FrameLayout) rootView.findViewById(R.id.layout_youtube);
        layoutVideo = (FrameLayout) rootView.findViewById(R.id.layout_video);
        viewVideo = (VideoView) rootView.findViewById(R.id.video);
        viewVideoProgress = (ProgressBar) rootView.findViewById(R.id.video_progress);
        viewVideoPlay = (ImageView) rootView.findViewById(R.id.video_play);;
        viewVideoPause = (ImageView) rootView.findViewById(R.id.video_pause);
        layoutVideoController = (FrameLayout) rootView.findViewById(R.id.layout_video_controller);
        viewVideoCurrent = (TextView) rootView.findViewById(R.id.video_current);
        viewVideoSeekbar = (SeekBar) rootView.findViewById(R.id.video_seekbar);
        viewVideoDuration = (TextView) rootView.findViewById(R.id.video_duration);
        viewVideoFullscreen = (ImageView) rootView.findViewById(R.id.video_fullscreen);
        viewVideoFullscreenExit = (ImageView) rootView.findViewById(R.id.video_fullscreen_exit);
        viewVideos = (RecyclerView) rootView.findViewById(R.id.videos);
        layoutLoading = (LinearLayout) rootView.findViewById(R.id.layout_loading);
        layoutError = (LinearLayout) rootView.findViewById(R.id.layout_error);
        viewErrorMessage = (TextView) rootView.findViewById(R.id.error_message);
        viewRetry = (Button) rootView.findViewById(R.id.retry);

        viewYoutubePlayer = YouTubePlayerSupportFragment.newInstance();
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.youtube_player, viewYoutubePlayer).commit();

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initLayoutVideo(false);
        layoutVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutVideoController.getVisibility() == View.VISIBLE) {
                    layoutVideoController.setVisibility(View.GONE);
                    handlerVideo.removeMessages(HANDLER_VIDEO_CONTROLLER_HIDE);
                } else {
                    layoutVideoController.setVisibility(View.VISIBLE);
                    handlerVideo.sendEmptyMessageDelayed(HANDLER_VIDEO_CONTROLLER_HIDE, 3000);
                }
            }
        });
        viewVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                switchVideoLayout(VIDEO_LAYOUT_READY);
                viewVideoSeekbar.setMax(mp.getDuration());
                viewVideoDuration.setText(Function.ConvertMSTOHMS(mp.getDuration()));
                handlerVideo.sendEmptyMessage(HANDLER_VIDEO_PROGRESS);
                handlerVideo.sendEmptyMessageDelayed(HANDLER_VIDEO_CONTROLLER_HIDE, 3000);
            }
        });
        viewVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                switchVideoLayout(VIDEO_LAYOUT_READY);
                viewVideoCurrent.setText(Function.ConvertMSTOHMS(mp.getDuration()));
                handlerVideo.removeMessages(HANDLER_VIDEO_PROGRESS);
                handlerVideo.removeMessages(HANDLER_VIDEO_CONTROLLER_HIDE);
            }
        });
        viewVideoPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!viewVideo.isPlaying()) {
                    viewVideo.start();
                    switchVideoLayout(VIDEO_LAYOUT_READY);
                }
                handlerVideo.sendEmptyMessage(HANDLER_VIDEO_PROGRESS);
                handlerVideo.sendEmptyMessageDelayed(HANDLER_VIDEO_CONTROLLER_HIDE, 3000);
            }
        });
        viewVideoPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (viewVideo.isPlaying()) {
                    viewVideo.pause();
                    switchVideoLayout(VIDEO_LAYOUT_READY);
                }
                handlerVideo.removeMessages(HANDLER_VIDEO_PROGRESS);
                handlerVideo.removeMessages(HANDLER_VIDEO_CONTROLLER_HIDE);
            }
        });
        viewVideoSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean isPlaying;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    viewVideo.seekTo(progress);
                    viewVideoCurrent.setText(Function.ConvertMSTOHMS(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isPlaying = viewVideo.isPlaying();
                if (isPlaying) {
                    viewVideo.pause();
                    handlerVideo.removeMessages(HANDLER_VIDEO_PROGRESS);
                    handlerVideo.removeMessages(HANDLER_VIDEO_CONTROLLER_HIDE);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isPlaying) {
                    viewVideo.start();
                    handlerVideo.sendEmptyMessage(HANDLER_VIDEO_PROGRESS);
                    handlerVideo.sendEmptyMessageDelayed(HANDLER_VIDEO_CONTROLLER_HIDE, 3000);
                }
            }
        });
        viewVideoFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        });
        viewVideoFullscreenExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
                if (datas.get(lastView).type == ContentAdapter.TYPE_STATE_ERROR || datas.get(lastView).type == ContentAdapter.TYPE_STATE_IDLE) {
                    recyclerView.post(new Runnable() {
                        @Override
                        public void run() {
                            startRequestVideo(true);
                        }
                    });
                }
            }
        });
        viewRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == STATE_REQUEST_HEADLINE) {
                    startRequestHeadline();
                } else {
                    startRequestVideo(false);
                }
            }
        });

        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        startRequestHeadline();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (!video.isEmpty() && viewVideo != null) {
            viewVideo.start();
        }
        if (!youtube.isEmpty() && youTubeplayer != null) {
            youTubeplayer.play();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (!video.isEmpty() && viewVideo != null) {
            viewVideo.pause();
        }
        if (!youtube.isEmpty() && youTubeplayer != null) {
            youTubeplayer.pause();
        }

        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_HEADLINE);
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_HOME);
    }

    @Override
    public void onDestroy() {
        viewVideo = null;
        youTubeplayer = null;

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_DIALOG_REQUEST) {
            viewYoutubePlayer.initialize(Config.YOUTUBE_API_KEY, onInitializedListener);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        switchVideoLayout(VIDEO_LAYOUT_READY);
        handlerVideo.removeMessages(HANDLER_VIDEO_CONTROLLER_HIDE);
        handlerVideo.sendEmptyMessageDelayed(HANDLER_VIDEO_CONTROLLER_HIDE, 3000);

        Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        boolean isFullScreen = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;

        ((MainActivity) getActivity()).onFullScreen(isFullScreen);
        initLayoutVideo(isFullScreen);
    }

    private void initLayoutVideo(boolean isFullScreen) {
        layoutVideo.getLayoutParams().height = isFullScreen ? displayMetrics.widthPixels : displayMetrics.widthPixels * 9 / 16 ;
        layoutVideo.requestLayout();
    }

    private void switchLayout(int layout, String errorMessage) {
        layoutContent.setVisibility(View.GONE);
        layoutVideo.setVisibility(View.GONE);
        layoutYoutube.setVisibility(View.GONE);
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

    private void switchVideoLayout(int layout) {
        if (layout == VIDEO_LAYOUT_LOADING) {
            viewVideoProgress.setVisibility(View.VISIBLE);
            layoutVideoController.setVisibility(View.GONE);
        } else if (layout == VIDEO_LAYOUT_READY) {
            viewVideoProgress.setVisibility(View.GONE);
            layoutVideoController.setVisibility(View.VISIBLE);

            if (viewVideo.isPlaying()) {
                viewVideoPlay.setVisibility(View.GONE);
                viewVideoPause.setVisibility(View.VISIBLE);
            } else {
                viewVideoPlay.setVisibility(View.VISIBLE);
                viewVideoPause.setVisibility(View.GONE);
            }

            Display display = ((WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int rotation = display.getRotation();
            if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
                viewVideoFullscreen.setVisibility(View.GONE);
                viewVideoFullscreenExit.setVisibility(View.VISIBLE);
            } else {
                viewVideoFullscreen.setVisibility(View.VISIBLE);
                viewVideoFullscreenExit.setVisibility(View.GONE);
            }
        }
    }

    private void startVideo() {
        if (!video.isEmpty()) {
            layoutVideo.setVisibility(View.VISIBLE);
            switchVideoLayout(VIDEO_LAYOUT_LOADING);
            viewVideo.setVideoURI(Uri.parse(video));
        }
    }

    private void startYoutube() {
        if (!youtube.isEmpty()) {
            layoutYoutube.setVisibility(View.VISIBLE);
            viewYoutubePlayer.initialize(Config.YOUTUBE_API_KEY, onInitializedListener);
        }
    }

    private void fillData(boolean isLoadMore, int totalPage, int currentPage, ArrayList<Video> videos) {
        this.currentPage = currentPage;
        if (isLoadMore) {
            datas.remove(datas.size() - 1);
        } else {
            datas.clear();
        }

        if (videos != null) {
            int i = 0;
            for (Video video : videos) {
                int type = i % 2 == 0 ? ContentAdapter.TYPE_PREVIEW_IMAGE : ContentAdapter.TYPE_PREVIEW_DESCRIPTION;
                datas.add(new ItemObject(type, video));
                i++;
            }
        }

        if (datas.size() == 0) {
            datas.add(new ItemObject(ContentAdapter.TYPE_EMPTY, null));
        } else if (totalPage != currentPage) {
            datas.add(new ItemObject(ContentAdapter.TYPE_STATE_IDLE, null));
        }

        switchLayout(LAYOUT_CONTENT, null);
        startVideo();
        startYoutube();
        adapter.notifyDataSetChanged();
    }

    private void startRequestHeadline() {
        switchLayout(LAYOUT_LOADING, null);

        VolleyStringRequest request = new VolleyStringRequest(
                Request.Method.POST,
                Config.URL_BASE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().create();
                        try {
                            GsonHeadline gsonHeadline = gson.fromJson(response, GsonHeadline.class);
                            for (Headline headline : gsonHeadline.headline) {
                                video = headline.video;
                                youtube = headline.youtube;
//                                video = "http://bnpbindonesia.tv/data/upload/db-Tanah-Longsor-Bogor---TV-One_x264-148825786528022017.mp4";
                                break;
                            }

                            state = STATE_REQUEST_VIDEO;
                            startRequestVideo(false);
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
                params.put("function", "headline");
                return params;
            }
        };
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_HEADLINE);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_HEADLINE);
    }

    public void startRequestVideo(final boolean isLoadMore) {
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
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_HOME);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_HOME);
    }
}