// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;

/**
 * Create a dead survivor somewhere with lot of modified guns/ammo on him
 */
@UsedFromLua
public final class RDSGunslinger extends RandomizedDeadSurvivorBase {
    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        IsoGridSquare sq = def.getFreeSquareInRoom();
        if (sq != null && (sq.getRoom() == null || sq.getRoom().getRoomDef() == null || sq.getRoom().getRoomDef().isKidsRoom())) {
            IsoDeadBody body = createRandomDeadBody(sq.getX(), sq.getY(), sq.getZ(), null, 0);
            if (body != null) {
                body.setPrimaryHandItem(this.addRandomRangedWeapon(body.getContainer(), true, false, false));
                int nbOfWeapons = Rand.Next(1, 4);

                for (int i = 0; i < nbOfWeapons; i++) {
                    body.getContainer().AddItem(this.addRandomRangedWeapon(body.getContainer(), true, true, true));
                }
            }
        }
    }

    public RDSGunslinger() {
        this.name = "Gunslinger";
        this.setChance(5);
    }
}
