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
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoWindow;

@UsedFromLua
public final class RBJackieJaye extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        if (this.roomValid(sq)) {
                            RBBasic.doOfficeStuff(sq);
                        }

                        for (int o = 0; o < sq.getObjects().size(); o++) {
                            IsoObject obj = sq.getObjects().get(o);
                            if (obj instanceof IsoWindow isoWindow
                                && sq.getRoom() != null
                                && isoWindow.getOppositeSquare() != null
                                && isoWindow.getOppositeSquare().isOutside()) {
                                isoWindow.addSheet(null);
                                isoWindow.HasCurtains().ToggleDoor(null);
                            } else if (obj instanceof IsoWindow isoWindow
                                && sq.isOutside()
                                && isoWindow.getOppositeSquare() != null
                                && isoWindow.getOppositeSquare().getRoom() != null) {
                                isoWindow.addSheet(null);
                                isoWindow.HasCurtains().ToggleDoor(null);
                            }
                        }
                    }
                }
            }
        }

        RoomDef room = def.getRoom("jackiejayestudio");
        if (room != null) {
            ArrayList<IsoZombie> zeds = this.addZombies(def, 1, "Jackie_Jaye", 100, room);
            if (zeds != null && !zeds.isEmpty()) {
                IsoZombie jackie = zeds.get(0);
                jackie.getHumanVisual().setSkinTextureIndex(1);
                SurvivorDesc desc = jackie.getDescriptor();
                if (desc != null) {
                    desc.setForename("Jackie");
                    desc.setSurname("Jaye");
                    IsoGridSquare sq = room.getFreeSquare();
                    if (sq != null) {
                        this.addItemOnGround(sq, "Microphone");
                        this.addItemOnGround(sq, "Notepad");
                        this.addItemOnGround(sq, "Pen");
                        InventoryItem pressID = InventoryItemFactory.CreateItem("Base.PressID");
                        if (pressID != null) {
                            pressID.nameAfterDescriptor(desc);
                            jackie.addItemToSpawnAtDeath(pressID);
                            IsoGridSquare sq2 = sq.getCell().getGridSquare(12482, 3910, 0);
                            if (sq2 != null) {
                                this.addSleepingBagWestEast(12482, 3910, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "jackiejayestudio".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("jackiejayestudio") != null;
    }

    public RBJackieJaye() {
        this.name = "JackieJaye";
        this.reallyAlwaysForce = true;
        this.setAlwaysDo(true);
    }
}
