// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.iso.BuildingDef;
import zombie.iso.IsoGridSquare;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.network.GameClient;
import zombie.network.GameServer;

/**
 * Shop being looted by bandits + 2 cops and corpses inside the shop
 */
@UsedFromLua
public final class RBShopLooted extends RandomizedBuildingBase {
    private final ArrayList<String> buildingList = new ArrayList<>();

    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        def.setAllExplored(true);
        RoomDef choosenRoom = null;

        for (int i = 0; i < def.rooms.size(); i++) {
            RoomDef room = def.rooms.get(i);
            if (this.buildingList.contains(room.name) || room.isShop()) {
                choosenRoom = room;
                break;
            }
        }

        if (choosenRoom != null) {
            String zombieType = "Bandit_Early";
            if (Rand.NextBool(3)) {
                zombieType = "PrivateMilitia";
            }

            int zedNbr = Rand.Next(3, 8);

            for (int ix = 0; ix < zedNbr; ix++) {
                this.addZombiesOnSquare(1, zombieType, null, choosenRoom.getFreeSquare());
            }

            this.addZombiesOnSquare(2, "Police", null, choosenRoom.getFreeSquare());
            int corpseNbr = Rand.Next(3, 8);

            for (int ix = 0; ix < corpseNbr; ix++) {
                IsoGridSquare freeSQ = getRandomSquareForCorpse(choosenRoom);
                createRandomDeadBody(freeSQ, null, Rand.Next(5, 10), 5, null);
            }
        }
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else if (def.getRooms() != null && def.getRooms().size() >= 2) {
            if (GameClient.client) {
                return false;
            } else if (!this.isTimeValid(force)) {
                return false;
            } else if (def.isAllExplored() && !force) {
                return false;
            } else {
                if (!force) {
                    if (Rand.Next(100) > this.getChance()) {
                        return false;
                    }

                    for (int i = 0; i < GameServer.Players.size(); i++) {
                        IsoPlayer player = GameServer.Players.get(i);
                        if (player.getSquare() != null && player.getSquare().getBuilding() != null && player.getSquare().getBuilding().def == def) {
                            return false;
                        }
                    }
                }

                if (def.isShop()) {
                    return true;
                } else {
                    for (int ix = 0; ix < def.rooms.size(); ix++) {
                        RoomDef room = def.rooms.get(ix);
                        if (this.buildingList.contains(room.name)) {
                            return true;
                        }
                    }

                    if (SandboxOptions.instance.getCurrentLootedChance() < 1 && !force) {
                        return false;
                    } else {
                        int max = SandboxOptions.instance.maximumLootedBuildingRooms.getValue();
                        if (def.getRooms().size() > SandboxOptions.instance.maximumLootedBuildingRooms.getValue()) {
                            this.debugLine = "Building is too large, maximum " + SandboxOptions.instance.maximumLootedBuildingRooms.getValue() + " rooms";
                            return false;
                        } else {
                            this.debugLine = this.debugLine + "Not a shop";
                            return false;
                        }
                    }
                }
            }
        } else {
            this.debugLine = this.debugLine + "Not enough rooms";
            return false;
        }
    }

    public RBShopLooted() {
        this.name = "Looted Shop";
        this.setChance(2);
        this.setAlwaysDo(true);
        this.buildingList.add("conveniencestore");
        this.buildingList.add("warehouse");
        this.buildingList.add("medclinic");
        this.buildingList.add("grocery");
        this.buildingList.add("zippeestore");
        this.buildingList.add("gigamart");
        this.buildingList.add("fossoil");
        this.buildingList.add("spiffo_dining");
        this.buildingList.add("pizzawhirled");
        this.buildingList.add("bookstore");
        this.buildingList.add("grocers");
        this.buildingList.add("library");
        this.buildingList.add("toolstore");
        this.buildingList.add("bar");
        this.buildingList.add("pharmacy");
        this.buildingList.add("gunstore");
        this.buildingList.add("mechanic");
        this.buildingList.add("bakery");
        this.buildingList.add("aesthetic");
        this.buildingList.add("clothesstore");
        this.buildingList.add("restaurant");
        this.buildingList.add("poststorage");
        this.buildingList.add("generalstore");
        this.buildingList.add("furniturestore");
        this.buildingList.add("fishingstorage");
        this.buildingList.add("cornerstore");
        this.buildingList.add("housewarestore");
        this.buildingList.add("shoestore");
        this.buildingList.add("sportstore");
        this.buildingList.add("giftstore");
        this.buildingList.add("candystore");
        this.buildingList.add("toystore");
        this.buildingList.add("electronicsstore");
        this.buildingList.add("sewingstore");
        this.buildingList.add("medical");
        this.buildingList.add("medicaloffice");
        this.buildingList.add("jewelrystore");
        this.buildingList.add("musicstore");
        this.buildingList.add("departmentstore");
        this.buildingList.add("gasstore");
        this.buildingList.add("gardenstore");
        this.buildingList.add("farmstorage");
        this.buildingList.add("hunting");
        this.buildingList.add("camping");
        this.buildingList.add("butcher");
        this.buildingList.add("optometrist");
        this.buildingList.add("knoxbutcher");
    }
}
