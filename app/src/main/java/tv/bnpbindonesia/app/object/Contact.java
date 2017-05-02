package tv.bnpbindonesia.app.object;

import java.io.Serializable;

public class Contact implements Serializable {
    public String kontak;

    public Contact(String kontak) {
        this.kontak = kontak;
    }
}
