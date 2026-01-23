// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.streets;

import zombie.UsedFromLua;

@UsedFromLua
public class WorldMapStreetV1 {
    WorldMapStreetsV1 owner;
    WorldMapStreet street;

    protected WorldMapStreetV1 init(WorldMapStreetsV1 owner, WorldMapStreet street) {
        this.owner = owner;
        this.street = street;
        return this;
    }

    public String getTranslatedText() {
        return this.street.getTranslatedText();
    }
}
