// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.inventory;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.debug.DebugLog;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.Item;
import zombie.scripting.objects.ItemFilterScript;

public class ItemFilter {
    private ItemFilterScript filterScript;

    public void setFilterScript(String filterScriptName) {
        if (filterScriptName == null) {
            this.filterScript = null;
        } else if (this.filterScript == null || !filterScriptName.equalsIgnoreCase(this.filterScript.getScriptObjectFullType())) {
            ItemFilterScript script = ScriptManager.instance.getItemFilter(filterScriptName);
            if (script == null) {
                DebugLog.General.warn("ItemFilter '" + filterScriptName + "' not found.");
            }

            this.filterScript = script;
        }
    }

    public ItemFilterScript getFilterScript() {
        return this.filterScript;
    }

    public boolean isActive() {
        return this.filterScript != null;
    }

    public boolean allows(String fulltype) {
        Item item = ScriptManager.instance.getItem(fulltype);
        return this.allows(item);
    }

    public boolean allows(InventoryItem item) {
        return !this.isActive() || this.filterScript.allowsItem(item);
    }

    public boolean allows(Item item) {
        return !this.isActive() || this.filterScript.allowsItem(item);
    }

    public void save(ByteBuffer output) throws IOException {
        output.put((byte)(this.filterScript != null ? 1 : 0));
        if (this.filterScript != null) {
            GameWindow.WriteString(output, this.filterScript.getScriptObjectFullType());
        }
    }

    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.filterScript = null;
        if (input.get() == 1) {
            this.setFilterScript(GameWindow.ReadString(input));
        }
    }
}
