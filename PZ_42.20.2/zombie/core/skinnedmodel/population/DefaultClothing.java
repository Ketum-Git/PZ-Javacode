// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.skinnedmodel.population;

import java.util.ArrayList;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

public final class DefaultClothing {
    public static final DefaultClothing instance = new DefaultClothing();
    public final DefaultClothing.Clothing pants = new DefaultClothing.Clothing();
    public final DefaultClothing.Clothing tShirt = new DefaultClothing.Clothing();
    public final DefaultClothing.Clothing tShirtDecal = new DefaultClothing.Clothing();
    public final DefaultClothing.Clothing vest = new DefaultClothing.Clothing();
    public boolean dirty = true;

    private void checkDirty() {
        if (this.dirty) {
            this.dirty = false;
            this.init();
        }
    }

    private void init() {
        this.pants.clear();
        this.tShirt.clear();
        this.tShirtDecal.clear();
        this.vest.clear();
        if (LuaManager.env.rawget("DefaultClothing") instanceof KahluaTable defaults) {
            this.initClothing(defaults, this.pants, "Pants");
            this.initClothing(defaults, this.tShirt, "TShirt");
            this.initClothing(defaults, this.tShirtDecal, "TShirtDecal");
            this.initClothing(defaults, this.vest, "Vest");
        }
    }

    private void initClothing(KahluaTable defaults, DefaultClothing.Clothing clothing, String key) {
        if (defaults.rawget(key) instanceof KahluaTable table) {
            this.tableToArrayList(table, "hue", clothing.hue);
            this.tableToArrayList(table, "texture", clothing.texture);
            this.tableToArrayList(table, "tint", clothing.tint);
        }
    }

    private void tableToArrayList(KahluaTable table, String key, ArrayList<String> list) {
        KahluaTableImpl table2 = (KahluaTableImpl)table.rawget(key);
        if (table2 != null) {
            int i = 1;

            for (int len = table2.len(); i <= len; i++) {
                Object o = table2.rawget(i);
                if (o != null) {
                    list.add(o.toString());
                }
            }
        }
    }

    public String pickPantsHue() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.pants.hue);
    }

    public String pickPantsTexture() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.pants.texture);
    }

    public String pickPantsTint() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.pants.tint);
    }

    public String pickTShirtTexture() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.tShirt.texture);
    }

    public String pickTShirtTint() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.tShirt.tint);
    }

    public String pickTShirtDecalTexture() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.tShirtDecal.texture);
    }

    public String pickTShirtDecalTint() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.tShirtDecal.tint);
    }

    public String pickVestTexture() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.vest.texture);
    }

    public String pickVestTint() {
        this.checkDirty();
        return OutfitRNG.pickRandom(this.vest.tint);
    }

    private static final class Clothing {
        final ArrayList<String> hue = new ArrayList<>();
        final ArrayList<String> texture = new ArrayList<>();
        final ArrayList<String> tint = new ArrayList<>();

        void clear() {
            this.hue.clear();
            this.texture.clear();
            this.tint.clear();
        }
    }
}
