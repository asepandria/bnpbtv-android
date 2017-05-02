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
import android.util.Log;
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
import java.util.Locale;
import java.util.Map;

import tv.bnpbindonesia.app.MainActivity;
import tv.bnpbindonesia.app.R;
import tv.bnpbindonesia.app.adapter.ContentAdapter;
import tv.bnpbindonesia.app.gson.GsonVideo;
import tv.bnpbindonesia.app.object.ItemObject;
import tv.bnpbindonesia.app.object.Video;
import tv.bnpbindonesia.app.share.Config;
import tv.bnpbindonesia.app.share.Function;
import tv.bnpbindonesia.app.share.ShareSocialMedia;
import tv.bnpbindonesia.app.util.VolleySingleton;
import tv.bnpbindonesia.app.util.VolleyStringRequest;

public class VideoFragment extends Fragment {
    private static final String TAG = VideoFragment.class.getSimpleName();
    private static final String TAG_VIDEO = "video";
    private static final String TAG_VIDEOS = "videos";

    private static final String ARG_ID = "id";
    private static final String ARG_VIDEO = "video";

    private static final int STATE_REQUEST_VIDEO = -2;
    private static final int STATE_REQUEST_VIDEOS = -1;
    private static final int STATE_DONE = 0;

    private static final int RECOVERY_DIALOG_REQUEST = 1;

    private static final int LAYOUT_ERROR = -1;
    private static final int LAYOUT_LOADING = 0;
    private static final int LAYOUT_CONTENT = 1;

    private static final int VIDEO_LAYOUT_ERROR = -1;
    private static final int VIDEO_LAYOUT_LOADING = 0;
    private static final int VIDEO_LAYOUT_READY = 1;

    private static final int HANDLER_VIDEO_PROGRESS = 0;
    private static final int HANDLER_VIDEO_CONTROLLER_HIDE = 1;

    private int state = STATE_REQUEST_VIDEO;
    private int videoLayout = VIDEO_LAYOUT_LOADING;

    private String id;
    private Video video;

    private int currentPage;
    private ArrayList<ItemObject> datas = new ArrayList<>();

    private DisplayMetrics displayMetrics;
    private Handler handlerVideo;
    private YouTubePlayer youTubeplayer;
    private YouTubePlayer.OnInitializedListener onInitializedListener;
    private ContentAdapter adapter;
    private GridLayoutManager layoutManager;

    private LinearLayout layoutContent;
    private FrameLayout layoutYoutube;
    private YouTubePlayerSupportFragment viewYoutubePlayer;
    private FrameLayout layoutVideo;
    private VideoView viewVideo;
    private TextView viewVideoError;
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

    public VideoFragment() {
        // Required empty public constructor
    }

    public static VideoFragment newInstance(String id, Video video) {
        VideoFragment fragment = new VideoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ID, id);
        args.putSerializable(ARG_VIDEO, video);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            id = getArguments().getString(ARG_ID);
            video = (Video) getArguments().getSerializable(ARG_VIDEO);
        }
        state = video == null ? STATE_REQUEST_VIDEO : STATE_REQUEST_VIDEOS;

        displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        handlerVideo = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case HANDLER_VIDEO_PROGRESS:
                        if (viewVideo != null) {
                            viewVideoCurrent.setText(Function.ConvertMSTOHMS(viewVideo.getCurrentPosition()));
                            viewVideoSeekbar.setProgress(viewVideo.getCurrentPosition());
                            sendEmptyMessageDelayed(HANDLER_VIDEO_PROGRESS, 250);
                        }
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
                    public void onPlaying() {
                    }

                    @Override
                    public void onPaused() {
                    }

                    @Override
                    public void onStopped() {
                        if (youTubeplayer != null && youTubeplayer.getCurrentTimeMillis() == youTubeplayer.getDurationMillis()) {
                            youTubeplayer.setFullscreen(false);
                        }
                    }

                    @Override
                    public void onBuffering(boolean b) {
                    }

                    @Override
                    public void onSeekTo(int i) {
                    }
                });

                if (!wasRestored) {
                    youTubeplayer = player;
//                        player.cueVideo(youtube);
                    player.loadVideo(video.youtube);
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
        layoutManager = new GridLayoutManager(getActivity(), 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (datas.get(position).type) {
                    case ContentAdapter.TYPE_STATE_ERROR:
                    case ContentAdapter.TYPE_STATE_LOADING:
                    case ContentAdapter.TYPE_STATE_IDLE:
                    case ContentAdapter.TYPE_LANGUAGE:
                    case ContentAdapter.TYPE_HEADER:
                    case ContentAdapter.TYPE_DESCRIPTION:
                        return 2;
                    case ContentAdapter.TYPE_PREVIEW_IMAGE:
                        return 1;
                    default:
                        return -1;
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_video, container, false);

        layoutContent = (LinearLayout) rootView.findViewById(R.id.layout_content);
        layoutYoutube = (FrameLayout) rootView.findViewById(R.id.layout_youtube);
        layoutVideo = (FrameLayout) rootView.findViewById(R.id.layout_video);
        viewVideo = (VideoView) rootView.findViewById(R.id.video);
        viewVideoError = (TextView) rootView.findViewById(R.id.video_error);
        viewVideoProgress = (ProgressBar) rootView.findViewById(R.id.video_progress);
        viewVideoPlay = (ImageView) rootView.findViewById(R.id.video_play);
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
                if (videoLayout == VIDEO_LAYOUT_READY) {
                    if (layoutVideoController.getVisibility() == View.VISIBLE) {
                        layoutVideoController.setVisibility(View.GONE);
                        handlerVideo.removeMessages(HANDLER_VIDEO_CONTROLLER_HIDE);
                    } else {
                        layoutVideoController.setVisibility(View.VISIBLE);
                        handlerVideo.sendEmptyMessageDelayed(HANDLER_VIDEO_CONTROLLER_HIDE, 3000);
                    }
                } else {
                    layoutVideoController.setVisibility(View.GONE);
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
        viewVideo.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switchVideoLayout(VIDEO_LAYOUT_ERROR);
                return true;
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
                if (state == STATE_REQUEST_VIDEO) {
                    startRequestVideo();
                } else if (state == STATE_REQUEST_VIDEOS) {
                    startRequestVideos(false);
                }
            }
        });

        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        if (state == STATE_REQUEST_VIDEO) {
            startRequestVideo();
        } else if (state == STATE_REQUEST_VIDEOS) {
            startRequestVideos(false);
        } else if (state == STATE_DONE) {
            switchLayout(LAYOUT_CONTENT, null);
            startVideo();
            startYoutube();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (video != null) {
            if (video.video != null && !video.video.isEmpty() && !video.video.contains(Config.YOUTUBE) && viewVideo != null) {
                viewVideo.start();
            }
            if (video.youtube != null && !video.youtube.isEmpty() && youTubeplayer != null) {
                youTubeplayer.play();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (video != null) {
            if (video.video != null && !video.video.isEmpty() && !video.video.contains(Config.YOUTUBE) && viewVideo != null) {
                viewVideo.pause();
                handlerVideo.removeMessages(HANDLER_VIDEO_PROGRESS);
            }
            if (video.youtube != null && !video.youtube.isEmpty() && youTubeplayer != null) {
                youTubeplayer.pause();
            }
        }

        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_VIDEO);
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_VIDEOS);
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
        layoutVideo.getLayoutParams().height = isFullScreen ? displayMetrics.widthPixels : displayMetrics.widthPixels * 9 / 16;
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
        videoLayout = layout;
        if (layout == VIDEO_LAYOUT_ERROR) {
            viewVideoError.setVisibility(View.VISIBLE);
            viewVideoProgress.setVisibility(View.GONE);
            layoutVideoController.setVisibility(View.GONE);
        } else if (layout == VIDEO_LAYOUT_LOADING) {
            viewVideoError.setVisibility(View.GONE);
            viewVideoProgress.setVisibility(View.VISIBLE);
            layoutVideoController.setVisibility(View.GONE);
        } else if (layout == VIDEO_LAYOUT_READY) {
            viewVideoError.setVisibility(View.GONE);
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
        if (video.video != null && !video.video.isEmpty() && !video.video.contains(Config.YOUTUBE)) {
            layoutVideo.setVisibility(View.VISIBLE);
            switchVideoLayout(VIDEO_LAYOUT_LOADING);
            viewVideo.setVideoURI(Uri.parse(video.video));
        }
    }

    private void startYoutube() {
        if (video.youtube != null && !video.youtube.isEmpty()) {
            layoutYoutube.setVisibility(View.VISIBLE);
            viewYoutubePlayer.initialize(Config.YOUTUBE_API_KEY, onInitializedListener);
        }
    }

    public void initLanguage(String lang) {
        if (lang != null) {
            Function.setLang(getContext(), lang);
            ItemObject header = datas.get(1);
            header.object = lang.equals(Config.LANGUANGE_INDONESIA) ? video.judul : video.judul_EN;
            ItemObject description = datas.get(2);
            description.object = lang.equals(Config.LANGUANGE_INDONESIA) ? video.description : video.description_EN;
            adapter.notifyItemRangeChanged(0, 3);
        } else {
            lang = Function.getLang(getContext());
            datas.add(new ItemObject(ContentAdapter.TYPE_LANGUAGE, null));
            datas.add(new ItemObject(ContentAdapter.TYPE_HEADER, lang.equals(Config.LANGUANGE_INDONESIA) ? video.judul : video.judul_EN));
            datas.add(new ItemObject(ContentAdapter.TYPE_DESCRIPTION, lang.equals(Config.LANGUANGE_INDONESIA) ? video.description : video.description_EN));
        }
    }

    private void fillData(boolean isLoadMore, int totalPage, int currentPage, ArrayList<Video> videos) {
        this.currentPage = currentPage;

        if (isLoadMore) {
            datas.remove(datas.size() - 1);
            adapter.notifyItemRemoved(datas.size());
        } else {
            datas.clear();
            adapter.notifyDataSetChanged();

//            String lang = Locale.getDefault().getLanguage();
//            String lang = Function.getLang(getContext());
//            datas.add(new ItemObject(ContentAdapter.TYPE_HEADER, lang.equals(Config.LANGUANGE_INDONESIA) ? video.judul : video.judul_EN));
//            datas.add(new ItemObject(ContentAdapter.TYPE_DESCRIPTION, lang.equals(Config.LANGUANGE_INDONESIA) ? video.description : video.description_EN));
            initLanguage(null);
            datas.add(new ItemObject(ContentAdapter.TYPE_HEADER, "Related Videos"));
        }

        int lastSize = datas.size();

        if (videos != null) {
            for (Video video : videos) {
                datas.add(new ItemObject(ContentAdapter.TYPE_PREVIEW_IMAGE, video));
            }
        }

        if (datas.size() == 0) {
            datas.add(new ItemObject(ContentAdapter.TYPE_HEADER, null));
        } else if (totalPage != currentPage) {
            datas.add(new ItemObject(ContentAdapter.TYPE_STATE_IDLE, null));
        }

        if (!isLoadMore) {
            switchLayout(LAYOUT_CONTENT, null);
            startVideo();
            startYoutube();
            ShareSocialMedia.initShareLayout(getActivity(), video.short_url);
        }

        if (lastSize < datas.size()) {
            adapter.notifyItemRangeInserted(lastSize, datas.size() - lastSize);
        }
    }

    private void startRequestVideo() {
        switchLayout(LAYOUT_LOADING, null);

        VolleyStringRequest request = new VolleyStringRequest(
                Request.Method.POST,
                Config.URL_BASE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().create();
                        try {
                            GsonVideo gsonVideo = gson.fromJson(response, GsonVideo.class);
                            if (gsonVideo.video != null) {
                                for (Video vid : gsonVideo.video) {
                                    video = vid;
                                    break;
                                }
                            }

                            state = STATE_REQUEST_VIDEOS;
                            startRequestVideos(false);
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
                params.put("function", "video");
                params.put("id", id);
                return params;
            }
        };
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_VIDEO);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_VIDEO);
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
                    params.put("category", video.category);
                }
                return params;
            }
        };
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_VIDEOS);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_VIDEOS);
    }
}