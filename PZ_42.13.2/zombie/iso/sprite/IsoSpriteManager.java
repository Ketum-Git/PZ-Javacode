// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.core.Color;
import zombie.debug.DebugLog;
import zombie.iso.SpriteDetails.IsoFlagType;

@UsedFromLua
public final class IsoSpriteManager {
    public static final IsoSpriteManager instance = new IsoSpriteManager();
    public final HashMap<String, IsoSprite> namedMap = new HashMap<>();
    public final TIntObjectHashMap<IsoSprite> intMap = new TIntObjectHashMap<>();
    private final IsoSprite emptySprite = new IsoSprite(this);

    public IsoSpriteManager() {
        IsoSprite sprite = this.emptySprite;
        sprite.name = "";
        sprite.id = -1;
        sprite.properties.set(IsoFlagType.invisible);
        this.namedMap.put(sprite.name, sprite);
    }

    public void Dispose() {
        IsoSprite.DisposeAll();
        IsoAnim.DisposeAll();
        Object[] values = this.intMap.values();

        for (int i = 0; i < values.length; i++) {
            IsoSprite sprite = (IsoSprite)values[i];
            sprite.Dispose();
            sprite.def = null;
            sprite.parentManager = null;
        }

        this.intMap.clear();
        this.namedMap.clear();
        this.namedMap.put(this.emptySprite.name, this.emptySprite);
    }

    public IsoSprite getSprite(int gid) {
        return this.intMap.containsKey(gid) ? this.intMap.get(gid) : null;
    }

    public IsoSprite getSprite(String gid) {
        return this.namedMap.containsKey(gid) ? this.namedMap.get(gid) : this.AddSprite(gid);
    }

    public IsoSprite getOrAddSpriteCache(String tex) {
        if (this.namedMap.containsKey(tex)) {
            return this.namedMap.get(tex);
        } else {
            IsoSprite spr = new IsoSprite(this);
            spr.LoadSingleTexture(tex);
            this.namedMap.put(tex, spr);
            return spr;
        }
    }

    public IsoSprite getOrAddSpriteCache(String tex, Color col) {
        int r = (int)(col.r * 255.0F);
        int g = (int)(col.g * 255.0F);
        int b = (int)(col.b * 255.0F);
        String key = tex + "_" + r + "_" + g + "_" + b;
        if (this.namedMap.containsKey(key)) {
            return this.namedMap.get(key);
        } else {
            IsoSprite spr = new IsoSprite(this);
            spr.LoadSingleTexture(tex);
            this.namedMap.put(key, spr);
            return spr;
        }
    }

    public IsoSprite AddSprite(String tex) {
        IsoSprite spr = new IsoSprite(this);
        spr.LoadSingleTexture(tex);
        this.namedMap.put(tex, spr);
        return spr;
    }

    public IsoSprite AddSprite(String tex, int ID) {
        IsoSprite spr = new IsoSprite(this);
        spr.LoadSingleTexture(tex);
        if (this.namedMap.containsKey(tex)) {
            DebugLog.Sprite.warn("duplicate texture " + tex + " ignore ID=" + ID + ", use ID=" + this.namedMap.get(tex).id);
            ID = this.namedMap.get(tex).id;
        }

        this.namedMap.put(tex, spr);
        spr.id = ID;
        this.intMap.put(ID, spr);
        return spr;
    }
}
