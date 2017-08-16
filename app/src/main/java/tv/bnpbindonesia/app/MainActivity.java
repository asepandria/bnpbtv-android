package tv.bnpbindonesia.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import tv.bnpbindonesia.app.adapter.MenuAdapter;
import tv.bnpbindonesia.app.fragment.AlertFragment;
import tv.bnpbindonesia.app.fragment.AlertHistoryFragment;
import tv.bnpbindonesia.app.fragment.ContactFragment;
import tv.bnpbindonesia.app.fragment.DetailAlertFragment;
import tv.bnpbindonesia.app.fragment.ErrorFragment;
import tv.bnpbindonesia.app.fragment.HomeFragment;
import tv.bnpbindonesia.app.fragment.IndexFragment;
import tv.bnpbindonesia.app.fragment.LoadingFragment;
import tv.bnpbindonesia.app.fragment.ProfileFragment;
import tv.bnpbindonesia.app.fragment.VideoFragment;
import tv.bnpbindonesia.app.gson.GsonMenu;
import tv.bnpbindonesia.app.object.Alert;
import tv.bnpbindonesia.app.object.ItemMenu;
import tv.bnpbindonesia.app.object.Video;
import tv.bnpbindonesia.app.service.MyFirebaseInstanceIdService;
import tv.bnpbindonesia.app.share.Config;
import tv.bnpbindonesia.app.share.Function;
import tv.bnpbindonesia.app.util.VolleySingleton;
import tv.bnpbindonesia.app.util.VolleyStringRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_MENU = "menu";

    private static final String EXTRA_IS_ALERT = "isAlert";
    private static final String EXTRA_ID = "is";

    private static final int STATE_REQUEST_MENU = -1;
    private static final int STATE_DONE = 0;

    private static final String ACTION_LOADING = "loading";
    private static final String ACTION_RETRY_MENU = "retry_menu";

    private boolean isAlert;
    private String id;

    private int state = STATE_REQUEST_MENU;
    private int selectedMenu = 0;
    private ArrayList<Fragment> fragmentStacks = new ArrayList<>();

    private ArrayList<ItemMenu> itemMenus = new ArrayList<>();
    private Map<String, Fragment> fragments = new HashMap<>();

    private DisplayMetrics displayMetrics;
    private MenuAdapter menuAdapter;

    private Toolbar toolbar;
    private ImageView toolbarLogo;
    private DrawerLayout drawer;
    private EditText viewKeyword;
    private ImageView viewSearch;
    private RecyclerView viewMenus;

    public static Intent newInstance(Context context, boolean isAlert, String id) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_IS_ALERT, isAlert);
        intent.putExtra(EXTRA_ID, id);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "token: " + token);

        if (token != null) {
            FirebaseMessaging.getInstance().subscribeToTopic("alert");
            FirebaseMessaging.getInstance().subscribeToTopic("video");
        }

        Intent extra = getIntent();
        if (extra != null) {
            if (extra.hasExtra(EXTRA_IS_ALERT)) isAlert = extra.getBooleanExtra(EXTRA_IS_ALERT, false);
            if (extra.hasExtra(EXTRA_ID)) id = extra.getStringExtra(EXTRA_ID);
        }

        fragments.put(ACTION_LOADING, LoadingFragment.newInstance());

        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        menuAdapter = new MenuAdapter(this, itemMenus);

        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbarLogo = (ImageView) findViewById(R.id.toolbar_logo);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        viewKeyword = (EditText) findViewById(R.id.keyword);
        viewSearch = (ImageView) findViewById(R.id.search);
        viewMenus = (RecyclerView) findViewById(R.id.menus);

        setSupportActionBar(toolbar);
        ViewCompat.setElevation(toolbar, 8 * displayMetrics.density);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.open_menu, R.string.close_menu);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        toolbarLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSelectMenu(0);
            }
        });

        viewKeyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search();
                }
                return false;
            }
        });
        viewSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search();
            }
        });

        viewMenus.setLayoutManager(new LinearLayoutManager(this));
        viewMenus.setAdapter(menuAdapter);

//        startRequestMenu();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (state == STATE_REQUEST_MENU) {
            startRequestMenu();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        VolleySingleton.getInstance(this).cancelPendingRequests(TAG_MENU);
    }

    @Override
    public void onBackPressed() {
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (fragmentStacks.size() > 1) {
            Fragment fragment = fragmentStacks.get(fragmentStacks.size() - 2);
            switchFragment(
                    fragment,
                    R.anim.fragment_fade_in,
                    R.anim.fragment_fade_out
            );
            fragmentStacks.remove(fragmentStacks.size() - 1);
        } else if (selectedMenu != 0) {
            onSelectMenu(0);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawer.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_search:
                drawer.openDrawer(GravityCompat.START);
                viewKeyword.requestFocus();
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.showSoftInput(viewKeyword, InputMethodManager.SHOW_IMPLICIT);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // [ START PUBLIC FUNCTION ]
    public void onRetry(String action) {
        switch (action) {
            case ACTION_RETRY_MENU:
                startRequestMenu();
                break;
        }
    }

    public void onFullScreen(boolean isFullScreen) {
        if (isFullScreen) {
            toolbar.setVisibility(View.GONE);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            toolbar.setVisibility(View.VISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void onSelectMenu(int position) {
        selectedMenu = position;
        fragmentStacks.clear();

        menuAdapter.setSelected(position);
        drawer.closeDrawer(GravityCompat.START);

        String menu = itemMenus.get(position).title;
        if (!fragments.containsKey(menu)) {
            if (position == 0) {
                fragments.put(menu, isAlert ? AlertFragment.newInstance(id) : HomeFragment.newInstance());
            } else  if (position == 1) {
                fragments.put(menu, ProfileFragment.newInstance());
            } else if (position == itemMenus.size() - 3) {
                fragments.put(menu, AlertHistoryFragment.newInstance(itemMenus.get(position).title.toUpperCase()));
            } else if (position == itemMenus.size() - 2) {
                fragments.put(menu, ContactFragment.newInstance(itemMenus.get(position).title.toUpperCase()));
            } else {
                fragments.put(menu,  IndexFragment.newInstance(false, menu));
            }
        }
        fragmentStacks.add(fragments.get(menu));
        switchFragment(
                fragments.get(menu),
                R.anim.fragment_fade_in,
                R.anim.fragment_fade_out
        );
    }

    public void onSelectVideo(Video video) {
        Fragment fragment = VideoFragment.newInstance(video.id, video);
        fragmentStacks.add(fragment);
        switchFragment(
                fragment,
                R.anim.fragment_fade_in,
                R.anim.fragment_fade_out
        );
    }

    public void onSelectAlert(Alert alert) {
        Fragment fragment = DetailAlertFragment.newInstance(alert.id, alert);
        fragmentStacks.add(fragment);
        switchFragment(
                fragment,
                R.anim.fragment_fade_in,
                R.anim.fragment_fade_out
        );
    }
    // [ END PUBLIC FUNCTION ]


    private void initMenu(ArrayList<tv.bnpbindonesia.app.object.Menu> menus) {
        int i = 0;
        for (tv.bnpbindonesia.app.object.Menu menu : menus) {
            String parent = menu.parent.toLowerCase();
            if (parent.equals("main")) {
                itemMenus.add(new ItemMenu(MenuAdapter.TYPE_MENU, menu.menu, ContextCompat.getColor(this, i % 2 == 0 ? R.color.menu_bg1 : R.color.menu_bg2), false, false, new ArrayList<ItemMenu>()));
                i++;
            } else {
                for (ItemMenu itemMenu : itemMenus) {
                    if (itemMenu.title.toLowerCase().equals(parent)) {
                        itemMenu.type = MenuAdapter.TYPE_MENU_PARENT;
                        itemMenu.subMenus.add(new ItemMenu(MenuAdapter.TYPE_SUB_MENU, menu.menu, itemMenu.bg_color, false, false, null));
                    }
                }
            }
        }
        itemMenus.add(new ItemMenu(MenuAdapter.TYPE_SHARE, "", ContextCompat.getColor(this, R.color.menu_bg2), false, false, null));

        menuAdapter.notifyDataSetChanged();
    }

    private void search() {
        drawer.closeDrawer(GravityCompat.START);
        viewKeyword.clearFocus();
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(viewKeyword.getWindowToken(), 0);

        selectedMenu = -1;
        fragmentStacks.clear();

        Fragment fragment = IndexFragment.newInstance(true, viewKeyword.getText().toString());
        fragmentStacks.add(fragment);
        switchFragment(
                fragment,
                R.anim.fragment_fade_in,
                R.anim.fragment_fade_out
        );
    }

    private void switchFragment(Fragment fragment, int animIn, int animOut) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager
                .beginTransaction()
                .setCustomAnimations(animIn, animOut)
                .replace(R.id.layout_main, fragment)
                .commit();
    }

    private void startRequestMenu() {
        switchFragment(fragments.get(ACTION_LOADING), R.anim.fragment_fade_in, R.anim.fragment_fade_out);

        VolleyStringRequest request = new VolleyStringRequest(
                Request.Method.POST,
                Config.URL_BASE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().create();
                        try {
                            GsonMenu gsonMenu = gson.fromJson(response, GsonMenu.class);
                            initMenu(gsonMenu.items);
                            state = STATE_DONE;

                            onSelectMenu(0);
                            if (!isAlert && id != null) {
                                Fragment fragment = VideoFragment.newInstance(id, null);
                                fragmentStacks.add(fragment);
                                switchFragment(
                                        fragment,
                                        R.anim.fragment_fade_in,
                                        R.anim.fragment_fade_out
                                );
                            }
                        } catch (Exception e) {
                            switchFragment(
                                    ErrorFragment.newInstance(ACTION_RETRY_MENU, getString(R.string.json_format_error)),
                                    R.anim.fragment_fade_in,
                                    R.anim.fragment_fade_out
                            );
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        switchFragment(
                                ErrorFragment.newInstance(ACTION_RETRY_MENU, Function.parseVolleyError(getBaseContext(), error)),
                                R.anim.fragment_fade_in,
                                R.anim.fragment_fade_out
                        );
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("function", "menu");
                return params;
            }
        };
        VolleySingleton.getInstance(this).cancelPendingRequests(TAG_MENU);
        VolleySingleton.getInstance(this).addToRequestQueue(request, TAG_MENU);
    }
}