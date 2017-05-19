package tv.bnpbindonesia.app.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import tv.bnpbindonesia.app.fragment.ImageFragment;

public class ImageAdapter extends FragmentStatePagerAdapter {
    private static final String TAG = ImageAdapter.class.getSimpleName();

    private ArrayList<String> datas = new ArrayList<>();
    private Map<Integer, Fragment> fragments = new HashMap<>();

    public ImageAdapter(FragmentManager fm, ArrayList<String> datas) {
        super(fm);

        this.datas = datas;
    }

    @Override
    public Fragment getItem(int position) {
        if (!fragments.containsKey(position)) {
            fragments.put(position, ImageFragment.newInstance(datas.get(position)));
        }

        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return datas.size();
    }
}