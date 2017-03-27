package tv.bnpbindonesia.app.object;

import java.io.Serializable;

public class Alert implements Serializable {
    public String id;
    public String title;
    public String type;
    public String date;
    public Slider slider;
    public String address;
    public String googlemaps;
    public String scale;
    public String fatalities;
    public String wound;
    public String description;

    public Alert(String id, String title, String type, String date, Slider slider, String address,
                 String googlemaps, String scale, String fatalities, String wound, String description){
        this.id = id;
        this.title = title;
        this.type = type;
        this.date = date;
        this.slider = slider;
        this.address = address;
        this.googlemaps = googlemaps;
        this.scale = scale;
        this.fatalities = fatalities;
        this.wound = wound;
        this.description = description;
    }
}
