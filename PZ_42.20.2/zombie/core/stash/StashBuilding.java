// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.stash;

import zombie.UsedFromLua;

@UsedFromLua
public final class StashBuilding {
    public int buildingX;
    public int buildingY;
    public String stashName;

    public StashBuilding(String stashName, int buildingX, int buildingY) {
        this.stashName = stashName;
        this.buildingX = buildingX;
        this.buildingY = buildingY;
    }

    public String getName() {
        return this.stashName;
    }
}
