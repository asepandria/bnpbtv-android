package tv.bnpbindonesia.app.object;

import java.io.Serializable;

public class Video implements Serializable {
    public String id;
    public String idvideo;
    public String category;
    public String judul;
    public String judul_EN;
    public String tanggal;
    public String image;
    public String video;
    public String youtube;
    public String description;
    public String description_EN;
    public String aktivasi;

    public Video (String id, String idvideo, String category, String judul, String judul_EN,
                  String tanggal, String image, String video, String youtube, String description,
                  String description_EN, String aktivasi) {
        this.id = id;
        this.idvideo = idvideo;
        this.category = category;
        this.judul = judul;
        this.judul_EN = judul_EN;
        this.tanggal = tanggal;
        this.image = image;
        this.video = video;
        this.youtube = youtube;
        this.description = description;
        this.description_EN = description_EN;
        this.aktivasi = aktivasi;
    }
}
