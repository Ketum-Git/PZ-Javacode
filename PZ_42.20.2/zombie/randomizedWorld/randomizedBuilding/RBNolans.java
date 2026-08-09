// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;

@UsedFromLua
public final class RBNolans extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null && this.roomValid(sq)) {
                        RBBasic.doNolansOfficeStuff(sq);
                    }
                }
            }
        }

        ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "Nolan", 0, null);
        if (!zeds.isEmpty()) {
            IsoZombie nolan = zeds.get(0);
            nolan.getHumanVisual().setSkinTextureIndex(1);
            SurvivorDesc desc = nolan.getDescriptor();
            if (desc != null) {
                desc.setForename("Nolan");
                desc.setSurname("Nolan");
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "nolansoffice".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("nolansoffice") != null;
    }

    public RBNolans() {
        this.name = "Nolans";
        this.reallyAlwaysForce = true;
        this.setAlwaysDo(true);
    }
}
