// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

/**
 * Create some zombies in varsity outfit + 2 naked zombies in bedroom
 */
@UsedFromLua
public final class RDSPokerNight extends RandomizedDeadSurvivorBase {
    private String money;
    private String card;

    public RDSPokerNight() {
        this.name = "Poker Night";
        this.setChance(4);
        this.setMaximumDays(60);
        this.money = "Base.Money";
        this.card = "Base.CardDeck";
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

            if (this.getRoom(def, "kitchen") != null) {
                return true;
            } else {
                this.debugLine = "No kitchen";
                return false;
            }
        }
    }

    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        RoomDef room = this.getRoom(def, "kitchen");
        this.addZombies(def, Rand.Next(3, 5), null, 10, room);
        this.addZombies(def, 1, "PokerDealer", 0, room);
        this.addRandomItemsOnGround(room, this.getPokerNightClutter(), Rand.Next(3, 7));
        this.addRandomItemsOnGround(room, this.money, Rand.Next(8, 13));
        this.addRandomItemsOnGround(room, this.card, 1);
        def.alarmed = false;
    }
}
