// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world;

import zombie.scripting.objects.Item;

public class ItemInfo extends DictionaryInfo<ItemInfo> {
    protected Item scriptItem;

    @Override
    public String getInfoType() {
        return "item";
    }

    public Item getScriptItem() {
        return this.scriptItem;
    }

    public ItemInfo copy() {
        ItemInfo c = new ItemInfo();
        c.copyFrom(this);
        c.scriptItem = this.scriptItem;
        return c;
    }
}
