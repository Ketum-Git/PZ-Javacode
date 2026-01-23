// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.objects.IsoDeadBody;
import zombie.util.list.PZArrayUtil;

/**
 * Create a dead survivor in the kitchen with empty bleach bottle around him
 */
@UsedFromLua
public final class RDSSpecificProfession extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> specificProfessionDistribution = new ArrayList<>();

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        String profession = PZArrayUtil.pickRandom(this.specificProfessionDistribution);
        ItemPickerJava.ItemPickerRoom prof = ItemPickerJava.rooms.get(profession);
        String outfit = prof.outfit;
        IsoGridSquare sq = def.getFreeSquareInRoom();
        if (sq != null) {
            IsoDeadBody body;
            if (outfit != null && Rand.Next(2) == 0) {
                body = createRandomDeadBody(sq, null, 0, 0, outfit);
            } else {
                body = createRandomDeadBody(sq.getX(), sq.getY(), sq.getZ(), null, 0);
            }

            if (body != null) {
                ItemPickerJava.rollItem(prof.containers.get("body"), body.getContainer(), true, null, null);
            }
        }
    }

    public RDSSpecificProfession() {
        this.specificProfessionDistribution.add("Carpenter");
        this.specificProfessionDistribution.add("Electrician");
        this.specificProfessionDistribution.add("Farmer");
        this.specificProfessionDistribution.add("Nurse");
        this.specificProfessionDistribution.add("Chef");
    }
}
