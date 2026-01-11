// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui.ISUIWrapper;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

public class ISToolTipWrapper extends ISPanelWrapper {
    public ISToolTipWrapper(KahluaTable table) {
        super(table);
    }

    public ISToolTipWrapper() {
        super(0.0, 0.0, 0.0, 0.0);
        KahluaTable self = (KahluaTable)LuaManager.env.rawget("ISToolTip");
        this.table.setMetatable(self);
        self.rawset("__index", self);
        this.noBackground();
        this.table.rawset("name", null);
        this.table.rawset("description", "");
        this.table.rawset("borderColor", this.setRGBA(LuaManager.platform.newTable(), 0.4, 0.4, 0.4, 1.0));
        this.table.rawset("backgroundColor", this.setRGBA(LuaManager.platform.newTable(), 0.0, 0.0, 0.0, 0.0));
        this.table.rawset("width", 0.0);
        this.table.rawset("height", 0.0);
        this.table.rawset("anchorLeft", true);
        this.table.rawset("anchorRight", false);
        this.table.rawset("anchorTop", true);
        this.table.rawset("anchorBottom", false);
        KahluaTable descriptionPanel = (KahluaTable)LuaHelpers.callLuaClass("ISRichTextPanel", "new", null, 0.0, 0.0, 0.0, 0.0);
        ISPanelWrapper descriptionPanelWrapper = new ISPanelWrapper(descriptionPanel);
        this.table.rawset("descriptionPanel", descriptionPanel);
        descriptionPanel.rawset("marginLeft", 0.0);
        descriptionPanel.rawset("marginRight", 0.0);
        descriptionPanelWrapper.initialise();
        descriptionPanelWrapper.instantiate();
        descriptionPanelWrapper.noBackground();
        descriptionPanel.rawset("backgroundColor", this.setRGBA(LuaManager.platform.newTable(), 0.0, 0.0, 0.0, 0.3));
        descriptionPanel.rawset("borderColor", this.setRGBA(LuaManager.platform.newTable(), 1.0, 1.0, 1.0, 0.1));
        this.table.rawset("owner", null);
        this.table.rawset("followMouse", true);
        this.table.rawset("nameMarginX", 50.0);
        this.table.rawset("defaultMyWidth", 220.0);
    }

    public void setName(String name) {
        this.table.rawset("name", name);
    }

    public void reset() {
        this.setVisible(false);
        this.noBackground();
        this.table.rawset("name", null);
        this.table.rawset("description", "");
        this.table.rawset("texture", null);
        this.table.rawset("footNote", null);
        this.setRGBA((KahluaTable)this.table.rawget("borderColor"), 0.4, 0.4, 0.4, 1.0);
        this.setRGBA((KahluaTable)this.table.rawget("backgroundColor"), 0.0, 0.0, 0.0, 0.0);
        this.table.rawset("width", 0.0);
        this.table.rawset("height", 0.0);
        this.table.rawset("maxLineWidth", null);
        this.table.rawset("desiredX", null);
        this.table.rawset("desiredY", null);
        this.table.rawset("anchorLeft", true);
        this.table.rawset("anchorRight", false);
        this.table.rawset("anchorTop", true);
        this.table.rawset("anchorBottom", false);
        KahluaTable descriptionPanel = (KahluaTable)this.table.rawget("descriptionPanel");
        descriptionPanel.rawset("marginLeft", 0.0);
        descriptionPanel.rawset("marginRight", 0.0);
        this.setRGBA((KahluaTable)descriptionPanel.rawget("backgroundColor"), 0.0, 0.0, 0.0, 0.3);
        this.setRGBA((KahluaTable)descriptionPanel.rawget("borderColor"), 1.0, 1.0, 1.0, 0.1);
        this.table.rawset("owner", null);
        this.table.rawset("contextMenu", null);
        this.table.rawset("followMouse", true);
    }
}
