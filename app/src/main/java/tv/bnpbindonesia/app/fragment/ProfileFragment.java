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
import tv.bnpbindonesia.app.object.ItemObject;
import tv.bnpbindonesia.app.object.Profile;
import tv.bnpbindonesia.app.share.Config;
import tv.bnpbindonesia.app.share.Function;
import tv.bnpbindonesia.app.util.VolleySingleton;
import tv.bnpbindonesia.app.util.VolleyStringRequest;

public class ProfileFragment extends Fragment {
    private static final String TAG = ProfileFragment.class.getSimpleName();
    private static final String TAG_PROFILE = "profile";


    private static final int STATE_REQUEST_PROFILE = -1;
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

    private int state = STATE_REQUEST_PROFILE;
    private int videoLayout = VIDEO_LAYOUT_LOADING;

    private Profile profile;

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

    public ProfileFragment() {
        // Required empty public constructor
    }

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                    player.loadVideo(profile.youtube);
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
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

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
        viewRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == STATE_REQUEST_PROFILE) {
                    startRequestProfile();
                }
            }
        });

        layoutContent.setVisibility(View.GONE);
        layoutLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);

        if (state == STATE_REQUEST_PROFILE) {
            startRequestProfile();
        } else if (state == STATE_DONE) {
            switchLayout(LAYOUT_CONTENT, null);
            startVideo();
            startYoutube();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (profile != null) {
            if (profile.video != null && !profile.video.isEmpty() && !profile.video.contains(Config.YOUTUBE) && viewVideo != null) {
                viewVideo.start();
            }
            if (profile.youtube != null && !profile.youtube.isEmpty() && youTubeplayer != null) {
                youTubeplayer.play();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (profile != null) {
            if (profile.video != null && !profile.video.isEmpty() && !profile.video.contains(Config.YOUTUBE) && viewVideo != null) {
                viewVideo.pause();
                handlerVideo.removeMessages(HANDLER_VIDEO_PROGRESS);
            }
            if (profile.youtube != null && !profile.youtube.isEmpty() && youTubeplayer != null) {
                youTubeplayer.pause();
            }
        }

        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_PROFILE);
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
        if (profile.video != null && !profile.video.isEmpty() && !profile.video.contains(Config.YOUTUBE)) {
            layoutVideo.setVisibility(View.VISIBLE);
            switchVideoLayout(VIDEO_LAYOUT_LOADING);
            viewVideo.setVideoURI(Uri.parse(profile.video));
        }
    }

    private void startYoutube() {
        if (profile.youtube != null && !profile.youtube.isEmpty()) {
            layoutYoutube.setVisibility(View.VISIBLE);
            viewYoutubePlayer.initialize(Config.YOUTUBE_API_KEY, onInitializedListener);
        }
    }

    private void fillData() {
        datas.clear();

        datas.add(new ItemObject(ContentAdapter.TYPE_HEADER, profile.title));
        datas.add(new ItemObject(ContentAdapter.TYPE_DESCRIPTION, profile.desc));
        adapter.notifyDataSetChanged();

        switchLayout(LAYOUT_CONTENT, null);
        startVideo();
        startYoutube();
    }

    private void startRequestProfile() {
        switchLayout(LAYOUT_LOADING, null);

        VolleyStringRequest request = new VolleyStringRequest(
                Request.Method.POST,
                Config.URL_BASE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().create();
                        try {
                            profile = gson.fromJson(response, Profile.class);
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
                params.put("function", "profil");
                return params;
            }
        };
        VolleySingleton.getInstance(getActivity()).cancelPendingRequests(TAG_PROFILE);
        VolleySingleton.getInstance(getActivity()).addToRequestQueue(request, TAG_PROFILE);
    }
}