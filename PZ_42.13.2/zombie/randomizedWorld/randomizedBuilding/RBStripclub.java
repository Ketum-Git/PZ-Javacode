// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;

/**
 * Add money/alcohol on table
 *  Can also generate a rare male venue
 */
@UsedFromLua
public final class RBStripclub extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        def.alarmed = false;
        def.setHasBeenVisited(true);
        def.setAllExplored(true);
        IsoCell cell = IsoWorld.instance.currentCell;
        boolean maleVenue = Rand.NextBool(20);
        ArrayList<Integer> alreadyAddedClothes = new ArrayList<>();

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        for (int i = 0; i < sq.getObjects().size(); i++) {
                            IsoObject obj = sq.getObjects().get(i);
                            if (Rand.NextBool(2) && "location_restaurant_pizzawhirled_01_16".equals(obj.getSprite().getName())) {
                                int money = Rand.Next(1, 4);

                                for (int j = 0; j < money; j++) {
                                    if (Rand.NextBool(8)) {
                                        ItemSpawner.spawnItem("MoneyBundle", sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                                    } else {
                                        ItemSpawner.spawnItem("Money", sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                                    }
                                }

                                int clothing = Rand.Next(1, 4);

                                for (int jx = 0; jx < clothing; jx++) {
                                    int clotheType = Rand.Next(1, 7);

                                    while (alreadyAddedClothes.contains(clotheType)) {
                                        clotheType = Rand.Next(1, 7);
                                    }

                                    switch (clotheType) {
                                        case 1:
                                            ItemSpawner.spawnItem(
                                                maleVenue ? "Trousers" : "TightsFishnet_Ground", sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F
                                            );
                                            alreadyAddedClothes.add(1);
                                            break;
                                        case 2:
                                            ItemSpawner.spawnItem("Vest_DefaultTEXTURE_TINT", sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                                            alreadyAddedClothes.add(2);
                                            break;
                                        case 3:
                                            ItemSpawner.spawnItem(
                                                maleVenue ? "Jacket_Fireman" : "BunnySuitBlack", sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F
                                            );
                                            alreadyAddedClothes.add(3);
                                            break;
                                        case 4:
                                            ItemSpawner.spawnItem(maleVenue ? "Hat_Cowboy" : "Garter", sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                                            alreadyAddedClothes.add(4);
                                            break;
                                        case 5:
                                            if (!maleVenue) {
                                                ItemSpawner.spawnItem("StockingsBlack", sq, Rand.Next(0.0F, 0.5F), Rand.Next(0.0F, 0.5F), 0.0F);
                                            }

                                            alreadyAddedClothes.add(5);
                                    }
                                }
                            }

                            if (obj.hasAdjacentCanStandSquare()
                                && (
                                    "furniture_tables_high_01_16".equals(obj.getSprite().getName())
                                        || "furniture_tables_high_01_17".equals(obj.getSprite().getName())
                                        || "furniture_tables_high_01_18".equals(obj.getSprite().getName())
                                )) {
                                int money = Rand.Next(1, 4);

                                for (int jx = 0; jx < money; jx++) {
                                    ItemSpawner.spawnItem("Money", sq, Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), obj.getSurfaceOffsetNoTable() / 96.0F);
                                }

                                if (Rand.NextBool(3)) {
                                    this.addWorldItem("CigaretteSingle", sq, obj);
                                    if (Rand.NextBool(2)) {
                                        this.addWorldItem("Lighter", sq, obj);
                                    }
                                }

                                int alcohol = Rand.Next(7);
                                switch (alcohol) {
                                    case 0:
                                        ItemSpawner.spawnItem(
                                            "Whiskey", sq, Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                        break;
                                    case 1:
                                        ItemSpawner.spawnItem(
                                            "Champagne", sq, Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                        break;
                                    case 2:
                                        ItemSpawner.spawnItem(
                                            "Champagne", sq, Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                        break;
                                    case 3:
                                        ItemSpawner.spawnItem(
                                            "BeerImported", sq, Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                        break;
                                    case 4:
                                        ItemSpawner.spawnItem(
                                            "BeerBottle", sq, Rand.Next(0.5F, 1.0F), Rand.Next(0.5F, 1.0F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                }
                            }
                        }
                    }
                }
            }
        }

        RoomDef room = def.getRoom("stripclub");
        if (maleVenue) {
            this.addZombies(def, Rand.Next(2, 4), "WaiterStripper", 0, room);
            this.addZombies(def, 1, "PoliceStripper", 0, room);
            this.addZombies(def, 1, "FiremanStripper", 0, room);
            this.addZombies(def, 1, "CowboyStripper", 0, room);
            this.addZombies(def, Rand.Next(9, 15), null, 100, room);
        } else {
            this.addZombies(def, Rand.Next(2, 4), "WaiterStripper", 100, room);
            this.addZombies(def, Rand.Next(2, 5), "StripperNaked", 100, room);
            this.addZombies(def, Rand.Next(2, 5), "StripperBlack", 100, room);
            this.addZombies(def, Rand.Next(2, 5), "StripperWhite", 100, room);
            this.addZombies(def, Rand.Next(9, 15), null, 0, room);
        }
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("stripclub") != null;
    }

    public RBStripclub() {
        this.name = "Stripclub";
        this.setAlwaysDo(true);
    }
}
