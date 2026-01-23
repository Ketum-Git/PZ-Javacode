// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.scripting.objects;

import zombie.UsedFromLua;

@UsedFromLua
public final class ItemRecipe {
    public String name;
    public Integer use = -1;
    public Boolean cooked = false;
    private String module;

    public Integer getUse() {
        return this.use;
    }

    public ItemRecipe(String name, String module, Integer use) {
        this.name = name;
        this.use = use;
        this.setModule(module);
    }

    public String getName() {
        return this.name;
    }

    public String getModule() {
        return this.module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getFullType() {
        return this.module + "." + this.name;
    }
}
