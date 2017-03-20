package tv.bnpbindonesia.app.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import tv.bnpbindonesia.app.R;
import tv.bnpbindonesia.app.object.ItemMenu;

import java.util.ArrayList;

public class MenuAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = MenuAdapter.class.getSimpleName();

    public static final int TYPE_MENU = 1;
    public static final int TYPE_MENU_PARENT = 2;
    public static final int TYPE_SUB_MENU = 3;
    public static final int TYPE_SHARE = 4;

    private int selected = 0;
    private Context context;
    private ArrayList<ItemMenu> itemMenus = new ArrayList<>();

    public MenuAdapter(Context context, ArrayList<ItemMenu> itemMenus) {
        this.context = context;
        this.itemMenus = itemMenus;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_MENU | viewType == TYPE_MENU_PARENT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_menu, parent, false);
            return new ViewHolderMenu(view);
        } else if (viewType == TYPE_SUB_MENU) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_sub_menu, parent, false);
            return new ViewHolderSubMenu(view);
        } else if (viewType == TYPE_SHARE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_share, parent, false);
            return new ViewHolderShare(view);
        } else {
            return null;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        int viewType = getItemViewType(position);
        if (viewType == TYPE_MENU || viewType == TYPE_MENU_PARENT) {
            ViewHolderMenu viewHolder = (ViewHolderMenu) holder;
            final ItemMenu itemMenu = itemMenus.get(position);

            viewHolder.layout.setBackgroundColor(itemMenu.bg_color);
            viewHolder.viewTitle.setText(itemMenu.title);
            viewHolder.viewTitle.setTypeface(null, itemMenu.isSelected ? Typeface.BOLD : Typeface.NORMAL);
            if (viewType == TYPE_MENU_PARENT) {
                viewHolder.viewIndicator.setVisibility(View.VISIBLE);
                viewHolder.viewIndicator.setImageResource(itemMenu.isExpand ? R.drawable.ic_expand_less_black_36dp : R.drawable.ic_expand_more_black_36dp);
                viewHolder.viewLayer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        itemMenu.isExpand = !itemMenu.isExpand;
                        notifyItemChanged(position);
                        if (itemMenu.isExpand) {
                            itemMenus.addAll(position + 1, itemMenu.subMenus);
                            notifyItemRangeInserted(position + 1, itemMenu.subMenus.size());
                        } else {
                            itemMenus.removeAll(itemMenu.subMenus);
                            notifyItemRangeRemoved(position + 1, itemMenu.subMenus.size());
                        }
//                        notifyDataSetChanged();
                    }
                });
            } else {
                viewHolder.viewIndicator.setVisibility(View.GONE);
                viewHolder.viewLayer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setSelected(position);
                    }
                });
            }
        } else if (viewType == TYPE_SUB_MENU) {
            ViewHolderSubMenu viewHolder = (ViewHolderSubMenu) holder;
            final ItemMenu itemMenu = itemMenus.get(position);

            viewHolder.layout.setBackgroundColor(itemMenu.bg_color);
            viewHolder.viewTitle.setText(itemMenu.title);
            viewHolder.viewTitle.setTypeface(null, itemMenu.isSelected ? Typeface.BOLD : Typeface.NORMAL);
            viewHolder.viewLayer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setSelected(position);
                }
            });
        } else if (viewType == TYPE_SHARE) {
            ViewHolderShare viewHolder = (ViewHolderShare) holder;
            ItemMenu itemMenu = itemMenus.get(position);

            viewHolder.layout.setBackgroundColor(itemMenu.bg_color);
            viewHolder.viewFb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //
                }
            });
            viewHolder.viewGplus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //
                }
            });
            viewHolder.viewTwitter.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return itemMenus.size();
    }

    @Override
    public int getItemViewType(int position) {
        return itemMenus.get(position).type;
    }

    public void setSelected(int position) {
        int i = 0;
        for (ItemMenu itemMenu : itemMenus) {
            if (itemMenu.isSelected) {
                if (i != position) {
                    itemMenu.isSelected = false;
                    notifyItemChanged(i);
                    break;
                }
            }
            if (itemMenu.type == TYPE_MENU_PARENT && !itemMenu.isExpand) {
                boolean isSelected = false;
                for (ItemMenu subMenu : itemMenu.subMenus) {
                    if (subMenu.isSelected) {
                        subMenu.isSelected = false;
                        isSelected = true;
                        break;
                    }
                }
                if (isSelected) {
                    break;
                }
            }
            i++;
        }
        itemMenus.get(position).isSelected = true;
        notifyItemChanged(position);
    }

    private static class ViewHolderMenu extends RecyclerView.ViewHolder {
        public FrameLayout layout;
        public TextView viewTitle;
        private ImageView viewIndicator;
        public View viewLayer;

        public ViewHolderMenu(View view) {
            super(view);

            layout = (FrameLayout) view.findViewById(R.id.layout);
            viewTitle = (TextView) view.findViewById(R.id.title);
            viewIndicator = (ImageView) view.findViewById(R.id.indicator);
            viewLayer = view.findViewById(R.id.layer);
        }
    }

    private static class ViewHolderSubMenu extends RecyclerView.ViewHolder {
        public FrameLayout layout;
        public TextView viewTitle;
        public View viewLayer;

        public ViewHolderSubMenu(View view) {
            super(view);

            layout = (FrameLayout) view.findViewById(R.id.layout);
            viewTitle = (TextView) view.findViewById(R.id.title);
            viewLayer = view.findViewById(R.id.layer);
        }
    }

    private static class ViewHolderShare extends RecyclerView.ViewHolder {
        private LinearLayout layout;
        private ImageView viewFb;
        private ImageView viewGplus;
        private ImageView viewTwitter;

        public ViewHolderShare(View view) {
            super(view);

            layout = (LinearLayout) view.findViewById(R.id.layout);
            viewFb = (ImageView) view.findViewById(R.id.fb);
            viewGplus = (ImageView) view.findViewById(R.id.gplus);
            viewTwitter = (ImageView) view.findViewById(R.id.twitter);
        }
    }
}