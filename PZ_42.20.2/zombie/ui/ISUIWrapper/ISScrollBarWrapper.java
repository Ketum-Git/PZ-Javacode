// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.vm.KahluaTable;

public class ISScrollBarWrapper extends ISUIElementWrapper {
    public ISScrollBarWrapper(KahluaTable table) {
        super(table);
    }

    public void updatePos() {
        KahluaTable parentTable = (KahluaTable)this.table.rawget("parent");
        if (parentTable != null) {
            ISUIElementWrapper parent = new ISUIElementWrapper(parentTable);
            boolean vertical = LuaHelpers.castBoolean(this.table.rawget("vertical"));
            if (vertical) {
                double sh = parent.getScrollHeight();
                double scrollAreaHeight = parent.getScrollAreaHeight();
                if (sh > scrollAreaHeight) {
                    double yscroll = -parent.getYScroll();
                    double pos = yscroll / (sh - scrollAreaHeight);
                    if (pos < 0.0) {
                        pos = 0.0;
                    }

                    if (pos > 1.0) {
                        pos = 1.0;
                    }

                    this.table.rawset("pos", pos);
                }
            } else {
                double sw = parent.getScrollWidth();
                double scrollAreaWidth = parent.getScrollAreaWidth();
                if (sw > scrollAreaWidth) {
                    double xscroll = -parent.getXScroll();
                    double posx = xscroll / (sw - scrollAreaWidth);
                    if (posx < 0.0) {
                        posx = 0.0;
                    }

                    if (posx > 1.0) {
                        posx = 1.0;
                    }

                    this.table.rawset("pos", posx);
                }
            }
        }
    }
}
