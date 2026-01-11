// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

public class ISPanelWrapper extends ISUIElementWrapper {
    public ISPanelWrapper(KahluaTable table) {
        super(table);
    }

    public ISPanelWrapper(double x, double y, double width, double height) {
        super(x, y, width, height);
        KahluaTable self = (KahluaTable)LuaManager.env.rawget("ISPanel");
        this.table.setMetatable(self);
        self.rawset("__index", self);
        this.table.rawset("x", x);
        this.table.rawset("y", y);
        this.table.rawset("background", true);
        this.table.rawset("backgroundColor", this.setRGBA(LuaManager.platform.newTable(), 0.0, 0.0, 0.0, 0.5));
        this.table.rawset("borderColor", this.setRGBA(LuaManager.platform.newTable(), 0.4, 0.4, 0.4, 1.0));
        this.table.rawset("width", width);
        this.table.rawset("height", height);
        this.table.rawset("anchorLeft", true);
        this.table.rawset("anchorRight", false);
        this.table.rawset("anchorTop", true);
        this.table.rawset("anchorBottom", false);
        this.table.rawset("moveWithMouse", false);
    }

    public void noBackground() {
        this.table.rawset("background", false);
    }
}
