package tv.bnpbindonesia.app.gson;

import java.util.ArrayList;

import tv.bnpbindonesia.app.object.Menu;
import tv.bnpbindonesia.app.object.Video;

public class GsonVideo {
    public int total_page;
    public int current_page;
    public String prev_page;
    public String next_page;
    public ArrayList<Video> video;

    public GsonVideo() {}
}
