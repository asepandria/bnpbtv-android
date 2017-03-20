package tv.bnpbindonesia.app.object;

import java.util.ArrayList;

public class ItemMenu {
    public int type;
    public String title;
    public int bg_color;
    public boolean isExpand;
    public boolean isSelected;
    public ArrayList<ItemMenu> subMenus = new ArrayList<>();

    public ItemMenu(int type, String title, int bg_color, boolean isExpand, boolean isSelected, ArrayList<ItemMenu> subMenus) {
        this.type = type;
        this.title = title;
        this.bg_color = bg_color;
        this.isExpand = isExpand;
        this.isSelected = isSelected;
        this.subMenus = subMenus;
    }
}
