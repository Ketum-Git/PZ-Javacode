// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoGameCharacter;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;

/**
 * Create 2 dead survivor with 1 gunshot, one handle a loaded gun
 */
@UsedFromLua
public final class RDSSuicidePact extends RandomizedDeadSurvivorBase {
    public RDSSuicidePact() {
        this.name = "Suicide Pact";
        this.setChance(7);
        this.setMinimumDays(60);
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getLivingRoomOrKitchen(def);
        IsoGameCharacter zombie = createRandomZombieForCorpse(room);
        if (zombie != null) {
            zombie.addVisualDamage("ZedDmg_HEAD_Bullet");
            IsoDeadBody body = createBodyFromZombie(zombie);
            if (body != null) {
                this.addBloodSplat(body.getSquare(), 4);
                body.setPrimaryHandItem(this.addWeapon("Base.Pistol", true));
                zombie = createRandomZombieForCorpse(room);
                if (zombie != null) {
                    zombie.addVisualDamage("ZedDmg_HEAD_Bullet");
                    body = createBodyFromZombie(zombie);
                    if (body != null) {
                        this.addBloodSplat(body.getSquare(), 4);
                        if (Rand.Next(2) == 0) {
                            InventoryItem note = InventoryItemFactory.CreateItem("Base.Note");
                            if (Rand.Next(2) == 0) {
                                ItemSpawner.spawnItem(note, body.getSquare(), Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), 0.0F);
                            } else {
                                body.getContainer().addItem(note);
                            }
                        }
                    }
                }
            }
        }
    }
}
