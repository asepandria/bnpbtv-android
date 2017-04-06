package tv.bnpbindonesia.app.object;

import java.io.Serializable;

public class Profile implements Serializable {
    public String title;
    public String video;
    public String youtube;
    public String desc;

    public Profile(String title, String video, String youtube, String desc) {
        this.title = title;
        this.video = video;
        this.youtube = youtube;
        this.desc = desc;
    }
}
