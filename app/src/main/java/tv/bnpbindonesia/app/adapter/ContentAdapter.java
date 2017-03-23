package tv.bnpbindonesia.app.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import tv.bnpbindonesia.app.R;
import tv.bnpbindonesia.app.fragment.HomeFragment;
import tv.bnpbindonesia.app.fragment.IndexFragment;
import tv.bnpbindonesia.app.object.ItemObject;
import tv.bnpbindonesia.app.object.Video;
import tv.bnpbindonesia.app.share.Config;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.util.ArrayList;
import java.util.Locale;

public class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ContentAdapter.class.getSimpleName();

    public static final int TYPE_STATE_ERROR = -1;
    public static final int TYPE_STATE_LOADING = 0;
    public static final int TYPE_STATE_IDLE = 1;
    public static final int TYPE_EMPTY = 2;
    public static final int TYPE_PREVIEW_IMAGE = 3;
    public static final int TYPE_PREVIEW_DESCRIPTION = 4;

    private String lang = Locale.getDefault().getLanguage();
    private Context context;
    private Fragment fragment;
    private DisplayMetrics displayMetrics;
    private ArrayList<ItemObject> datas;
    private DisplayImageOptions options;

    public ContentAdapter(Context context, Fragment fragment, ArrayList<ItemObject> datas) {
        this.context = context;
        this.fragment = fragment;
        this.datas = datas;

        displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        options = new DisplayImageOptions.Builder()
                .displayer(new FadeInBitmapDisplayer(500))
                .showImageOnLoading(R.drawable.no_image)
                .showImageForEmptyUri(R.drawable.no_image)
                .showImageOnFail(R.drawable.no_image)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .resetViewBeforeLoading(true)
                .build();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_STATE_ERROR) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_state_error, parent, false);
            return new ViewHolderStateError(view);
        } else if (viewType == TYPE_STATE_LOADING) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_state_loading, parent, false);
            return new ViewHolderNone(view);
        } else if (viewType == TYPE_STATE_IDLE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_state_idle, parent, false);
            return new ViewHolderStateIdle(view);
        } else if (viewType == TYPE_EMPTY) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_empty, parent, false);
            return new ViewHolderNone(view);
        } else if (viewType == TYPE_PREVIEW_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_preview_image, parent, false);
//            view.getLayoutParams().height = displayMetrics.widthPixels * 9 / 16;
            return new ViewHolderPreviewImage(view);
        } else if (viewType == TYPE_PREVIEW_DESCRIPTION) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_preview_description, parent, false);
//            view.getLayoutParams().height = displayMetrics.widthPixels * 9 / 16;
            return new ViewHolderPreviewDescription(view);
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        if (viewType == TYPE_STATE_ERROR) {
            ViewHolderStateError viewHolder = (ViewHolderStateError) holder;
            String message = (String) datas.get(position).object;

            viewHolder.viewMessage.setText(message);
            viewHolder.viewLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragment instanceof HomeFragment) {
                        ((HomeFragment) fragment).startRequestVideo(true);
                    } else if (fragment instanceof IndexFragment) {
                        ((IndexFragment) fragment).startRequestVideo(true);
                    }
                }
            });
        } else if (viewType == TYPE_STATE_LOADING) {

        } else if (viewType == TYPE_STATE_IDLE) {
            ViewHolderStateIdle viewHolder = (ViewHolderStateIdle) holder;
            viewHolder.viewLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (fragment instanceof HomeFragment) {
                        ((HomeFragment) fragment).startRequestVideo(true);
                    } else if (fragment instanceof IndexFragment) {
                        ((IndexFragment) fragment).startRequestVideo(true);
                    }
                }
            });
        } else if (viewType == TYPE_EMPTY) {

        } else if (viewType == TYPE_PREVIEW_IMAGE) {
            ViewHolderPreviewImage viewHolder = (ViewHolderPreviewImage) holder;
            Video video = (Video) datas.get(position).object;

            if (viewHolder.viewImage.getTag() == null || !viewHolder.viewImage.getTag().equals("http://bnpbindonesia.tv/data/upload/" + video.image)) {
                ImageLoader.getInstance().displayImage("http://bnpbindonesia.tv/data/upload/" + video.image, viewHolder.viewImage, options);
                viewHolder.viewImage.setTag("http://bnpbindonesia.tv/data/upload/" + video.image);
            }
            viewHolder.layout.invalidate();
            viewHolder.viewTitle.setText(lang.equals(Config.LANGUANGE_INDONESIA) ? video.judul : video.judul_EN);
            viewHolder.viewDescription.setText(lang.equals(Config.LANGUANGE_INDONESIA) ? video.description : video.description_EN);
            viewHolder.viewLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        } else if (viewType == TYPE_PREVIEW_DESCRIPTION) {
            ViewHolderPreviewDescription viewHolder = (ViewHolderPreviewDescription) holder;
            Video video = (Video) datas.get(position).object;

            viewHolder.layout.invalidate();
            viewHolder.layout.setBackgroundColor(Color.parseColor((position + 1) % 4 == 0 ? "#ec8304" : "#024b98"));
            viewHolder.viewMore.setBackgroundColor(Color.parseColor((position + 1) % 4 == 0 ? "#fe8e04" : "#03438a"));
            viewHolder.viewTitle.setText(lang.equals(Config.LANGUANGE_INDONESIA) ? video.judul : video.judul_EN);
            viewHolder.viewDescription.setText(lang.equals(Config.LANGUANGE_INDONESIA) ? video.description : video.description_EN);
            viewHolder.viewLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return datas.size();
    }

    @Override
    public int getItemViewType(int position) {
        return datas.get(position).type;
    }


    private static class ViewHolderNone extends RecyclerView.ViewHolder {
        public ViewHolderNone(View view) {
            super(view);
        }
    }

    private static class ViewHolderStateError extends RecyclerView.ViewHolder {
        public TextView viewMessage;
        public View viewLayer;

        public ViewHolderStateError(View view) {
            super(view);

            viewMessage = (TextView) view.findViewById(R.id.message);
            viewLayer = view.findViewById(R.id.layer);
        }
    }

    private static class ViewHolderStateIdle extends RecyclerView.ViewHolder {
        public View viewLayer;

        public ViewHolderStateIdle(View view) {
            super(view);

            viewLayer = view.findViewById(R.id.layer);
        }
    }

    private static class ViewHolderPreviewImage extends RecyclerView.ViewHolder {
        public FrameLayout layout;
        public ImageView viewImage;
        public TextView viewTitle;
        public TextView viewDescription;
        public View viewLayer;

        public ViewHolderPreviewImage(View view) {
            super(view);

            layout = (FrameLayout) view.findViewById(R.id.layout);
            viewImage = (ImageView) view.findViewById(R.id.image);
            viewTitle = (TextView) view.findViewById(R.id.title);
            viewDescription = (TextView) view.findViewById(R.id.description);
            viewLayer = view.findViewById(R.id.layer);
        }
    }

    private static class ViewHolderPreviewDescription extends RecyclerView.ViewHolder {
        public FrameLayout layout;
        public TextView viewTitle;
        public TextView viewDescription;
        public TextView viewMore;
        public View viewLayer;

        public ViewHolderPreviewDescription(View view) {
            super(view);

            layout = (FrameLayout) view.findViewById(R.id.layout);
            viewTitle = (TextView) view.findViewById(R.id.title);
            viewDescription = (TextView) view.findViewById(R.id.description);
            viewMore = (TextView) view.findViewById(R.id.more);
            viewLayer = view.findViewById(R.id.layer);
        }
    }
}