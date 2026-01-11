// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemContainer;
import zombie.inventory.types.HandWeapon;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.objects.IsoDeadBody;
import zombie.scripting.objects.ItemKey;

/**
 * Just a dead survivor in a bathroom with pistol or shotgun on him
 */
@UsedFromLua
public final class RDSGunmanInBathroom extends RandomizedDeadSurvivorBase {
    private static final int PROBABILITY_RIFLE_HUNTING = 50;
    private static final int PROBABILITY_RIFLE_M14 = 85;
    private static final int PROBABILITY_SHOTGUN = 20;
    private static final int PROBABILITY_PISTOL1 = 20;
    private static final int PROBABILITY_PISTOL2 = 40;
    private static final int PROBABILITY_PISTOL3 = 95;
    private static final int PROBABILITY_REVOLVER = 65;
    private static final int PROBABILITY_REVOLVER_LONG = 85;
    private static final int PROBABILITY_SPAWN_RIFLE = 20;
    private static final int PROBABILITY_SPAWN_AMMO = 60;
    private static final int PROBABILITY_SPAWN_CLIP = 60;

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "bathroom");
        IsoDeadBody body = createRandomDeadBody(room, Rand.Next(5, 10));
        if (body != null) {
            HandWeapon gun = gunPicker(Rand.NextBool(20));
            if (gun != null) {
                if (gun.usesExternalMagazine() && !gun.isContainsClip()) {
                    gun.setContainsClip(true);
                }

                String gunType = gun.getType();
                if (!gunType.contains("Shotgun") && !gunType.contains("Hunting") && !gunType.contains("Varmint") && !gunType.contains("Revolver")) {
                    gun.setRoundChambered(true);
                    gun.setCurrentAmmoCount(gun.getMaxAmmo() - 2);
                } else {
                    gun.setSpentRoundChambered(true);
                    gun.setCurrentAmmoCount(gun.getMaxAmmo() - 1);
                }

                gun.setBloodLevel(Rand.Next(0.5F, 1.0F));
                ItemContainer bodyContainer = body.getContainer();
                if (Rand.Next(100) >= 60) {
                    InventoryItem ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
                    bodyContainer.AddItem(ammoBox);
                }

                if (gun.usesExternalMagazine()) {
                    if (Rand.Next(100) >= 60) {
                        int clipCount = Rand.NextInclusive(1, 3);

                        for (int i = 0; i < clipCount; i++) {
                            InventoryItem clip = InventoryItemFactory.CreateItem(gun.getMagazineType());
                            clip.setCurrentAmmoCount(clip.getMaxAmmo());
                            bodyContainer.AddItem(clip);
                        }
                    }
                } else if (Rand.Next(100) >= 60) {
                    InventoryItem ammoBox = InventoryItemFactory.CreateItem(gun.getAmmoBox());
                    bodyContainer.AddItem(ammoBox);
                }

                body.setPrimaryHandItem(gun);
                body.getSquare().splatBlood(4, 1.0F);
            }
        }
    }

    private static HandWeapon gunPicker(boolean isRifle) {
        return isRifle ? InventoryItemFactory.CreateItem(riflePicker(Rand.Next(100))) : InventoryItemFactory.CreateItem(pistolPicker(Rand.Next(100)));
    }

    private static ItemKey riflePicker(int roll) {
        if (roll >= 85) {
            return ItemKey.Weapon.ASSAULT_RIFLE_2;
        } else if (roll >= 50) {
            return ItemKey.Weapon.HUNTING_RIFLE;
        } else {
            return roll >= 20 ? ItemKey.Weapon.SHOTGUN : ItemKey.Weapon.VARMINT_RIFLE;
        }
    }

    private static ItemKey pistolPicker(int roll) {
        if (roll >= 95) {
            return ItemKey.Weapon.PISTOL_3;
        } else if (roll >= 85) {
            return ItemKey.Weapon.REVOLVER_LONG;
        } else if (roll >= 65) {
            return ItemKey.Weapon.REVOLVER;
        } else if (roll >= 40) {
            return ItemKey.Weapon.PISTOL_2;
        } else {
            return roll >= 20 ? ItemKey.Weapon.PISTOL : ItemKey.Weapon.REVOLVER_SHORT;
        }
    }

    public RDSGunmanInBathroom() {
        this.name = "Bathroom Gunman";
        this.setChance(5);
    }
}
