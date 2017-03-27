package tv.bnpbindonesia.app.object;

import java.io.Serializable;
import java.util.ArrayList;

public class Slider implements Serializable {
    public ArrayList<String> image;

    public Slider(ArrayList<String> image) {
        this.image = image;
    }
}
