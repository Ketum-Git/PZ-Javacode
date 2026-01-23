// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.InventoryContainer;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;

/**
 * Create corpses in a kitchen with one zombie as doctor
 */
@UsedFromLua
public final class RDSCorpsePsycho extends RandomizedDeadSurvivorBase {
    public RDSCorpsePsycho() {
        this.name = "Corpse Psycho";
        this.setChance(1);
        this.setMinimumDays(120);
        this.setUnique(true);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "kitchen");
        if (room == null) {
            room = this.getRoom(def, "livingroom");
        }

        if (room == null) {
            room = this.getRoomNoKids(def, "bedroom");
        }

        if (room != null) {
            int nbrOfZed = Rand.Next(3, 7);

            for (int i = 0; i < nbrOfZed; i++) {
                IsoDeadBody body = createRandomDeadBody(room, Rand.Next(5, 10));
                if (body != null) {
                    this.addBloodSplat(body.getCurrentSquare(), Rand.Next(7, 12));
                }
            }

            ArrayList<IsoZombie> zombies = this.addZombies(def, 1, "MadScientist", null, room);
            if (!zombies.isEmpty()) {
                InventoryContainer bag = InventoryItemFactory.CreateItem("Base.Bag_DoctorBag");
                ItemPickerJava.rollContainerItem(bag, null, ItemPickerJava.getItemPickerContainers().get("Bag_BurglarBag"));
                this.addItemOnGround(def.getFreeSquareInRoom(), bag);

                for (int ix = 0; ix < 8; ix++) {
                    zombies.get(0).addBlood(null, false, true, false);
                }

                def.alarmed = false;
            }
        }
    }
}
