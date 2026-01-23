// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

@UsedFromLua
public final class RBWoodcraft extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null && this.roomValid(sq)) {
                        RBBasic.doWoodcraftStuff(sq);
                    }
                }
            }
        }

        RoomDef room = def.getRoom("woodcraftset");
        if (room != null) {
            ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "Woodcut", 0, room);
            if (!zeds.isEmpty()) {
                IsoZombie dean = zeds.getFirst();
                dean.getHumanVisual().setSkinTextureIndex(1);
                SurvivorDesc desc = dean.getDescriptor();
                if (desc != null) {
                    desc.setForename("Jeremiah");
                    desc.setSurname("H. Wutkrafft");
                    InventoryItem hammer = InventoryItemFactory.CreateItem("Base.Hammer");
                    if (hammer != null) {
                        dean.addItemToSpawnAtDeath(hammer);
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "woodcraftset".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("woodcraftset") != null;
    }

    public RBWoodcraft() {
        this.name = "Woodcraft";
        this.reallyAlwaysForce = true;
        this.setAlwaysDo(true);
    }
}
