// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.util.list.PZArrayUtil;

/**
 * Create some zombies male zombies with 1 naked female, some alcohol around
 */
@UsedFromLua
public final class RDSStagDo extends RandomizedDeadSurvivorBase {
    private final ArrayList<String> items = new ArrayList<>();
    private final ArrayList<String> otherItems = new ArrayList<>();
    private final ArrayList<String> outfits = new ArrayList<>();

    public RDSStagDo() {
        this.name = "Stag Do";
        this.setChance(2);
        this.setMaximumDays(60);
        this.otherItems.add("Base.CigaretteSingle");
        this.otherItems.add("Base.Whiskey");
        this.otherItems.add("Base.Wine");
        this.otherItems.add("Base.Wine2");
        this.otherItems.add("Base.WineBox");
        this.otherItems.add("Base.Cigar");
        this.otherItems.add("Base.Cigar");
        this.items.add("Base.Crisps");
        this.items.add("Base.Crisps2");
        this.items.add("Base.Crisps3");
        this.items.add("Base.Pop");
        this.items.add("Base.Pop2");
        this.items.add("Base.Pop3");
        this.outfits.add("NakedVeil");
        this.outfits.add("StripperBlack");
        this.outfits.add("StripperNaked");
        this.outfits.add("StripperPink");
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        } else if (def.isAllExplored() && !force) {
            return false;
        } else if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else {
            if (!force) {
                for (int i = 0; i < GameServer.Players.size(); i++) {
                    IsoPlayer player = GameServer.Players.get(i);
                    if (player.getSquare() != null && player.getSquare().getBuilding() != null && player.getSquare().getBuilding().def == def) {
                        return false;
                    }
                }
            }

            if (this.getRoom(def, "livingroom") != null) {
                return true;
            } else {
                this.debugLine = "No living room";
                return false;
            }
        }
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "livingroom");
        this.addZombies(def, Rand.Next(5, 7), null, 0, room);
        this.addZombies(def, 1, PZArrayUtil.pickRandom(this.outfits), 100, room);
        this.addRandomItemsOnGround(room, this.items, Rand.Next(3, 7));
        this.addRandomItemsOnGround(room, this.otherItems, Rand.Next(2, 6));
        def.alarmed = false;
    }
}
