// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.characters.SurvivorDesc;
import zombie.core.Translator;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

@UsedFromLua
public final class RBTwiggy extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null && this.roomValid(sq)) {
                        RBBasic.doTwiggyStuff(sq);
                    }
                }
            }
        }

        RoomDef room = def.getRoom("barcountertwiggy");
        ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "Sir_Twiggy", 0, room);
        if (!zeds.isEmpty()) {
            IsoZombie twiggy = zeds.get(0);
            twiggy.getHumanVisual().setSkinTextureIndex(1);
            SurvivorDesc desc = twiggy.getDescriptor();
            if (desc != null) {
                desc.setForename("Ted");
                desc.setSurname("Wigginton");
                InventoryItem license = InventoryItemFactory.CreateItem("Base.OfficialDocument");
                if (license != null) {
                    license.setName(Translator.getText("IGUI_Document_Twiggys"));
                    twiggy.addItemToSpawnAtDeath(license);
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "barcountertwiggy".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("barcountertwiggy") != null;
    }

    public RBTwiggy() {
        this.name = "Twiggy";
        this.reallyAlwaysForce = true;
        this.setAlwaysDo(true);
    }
}
