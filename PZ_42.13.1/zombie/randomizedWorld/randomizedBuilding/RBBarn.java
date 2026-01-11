// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public final class RBBarn extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    boolean barn = sq != null
                        && sq.getRoom() != null
                        && sq.getRoom().getName() != null
                        && ("barn".equals(sq.getRoom().getName()) || "stables".equals(sq.getRoom().getName()));
                    if (sq != null && this.roomValid(sq) && sq.getObjects().size() <= 3) {
                        for (int i = 0; i < sq.getObjects().size(); i++) {
                            IsoObject obj = sq.getObjects().get(i);
                            if (obj.isTableSurface() && sq.getObjects().size() <= 3 && Rand.NextBool(3)) {
                                String item;
                                if (barn) {
                                    item = RBBasic.getBarnClutterItem();
                                } else {
                                    item = RBBasic.getFarmStorageClutterItem();
                                }

                                obj.spawnItemToObjectSurface(item, true);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null
            && ("barn".equals(sq.getRoom().getName()) || "stables".equals(sq.getRoom().getName()) || "farmstorage".equals(sq.getRoom().getName()));
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("barn") != null || def.getRoom("stables") != null || def.getRoom("farmstorage") != null;
    }

    public RBBarn() {
        this.name = "Barn";
        this.setAlwaysDo(true);
    }

    private IsoDirections getFacing(IsoSprite sprite) {
        if (sprite != null && sprite.getProperties().has("Facing")) {
            String Facing = sprite.getProperties().get("Facing");
            switch (Facing) {
                case "N":
                    return IsoDirections.N;
                case "S":
                    return IsoDirections.S;
                case "W":
                    return IsoDirections.W;
                case "E":
                    return IsoDirections.E;
            }
        }

        return null;
    }
}
