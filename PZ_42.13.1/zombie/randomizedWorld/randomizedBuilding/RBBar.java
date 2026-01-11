// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;

/**
 * Add some food on table
 */
@UsedFromLua
public final class RBBar extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null && this.roomValid(sq)) {
                        if (sq.getObjects().size() == 1) {
                            if (Rand.NextBool(160)) {
                                this.addWorldItem("Dart", sq, null);
                            }
                        } else {
                            for (int i = 0; i < sq.getObjects().size(); i++) {
                                IsoObject obj = sq.getObjects().get(i);
                                if (obj.getSprite() != null
                                    && obj.getSprite().getName() != null
                                    && (obj.getSprite().getName().equals("recreational_01_6") || obj.getSprite().getName().equals("recreational_01_7"))) {
                                    if (Rand.NextBool(3)) {
                                        this.addWorldItem("PoolBall", sq, obj);
                                    }

                                    if (Rand.NextBool(3)) {
                                        this.addWorldItem("Poolcue", sq, obj);
                                    }
                                } else if (obj.isTableSurface() && Rand.NextBool(2)) {
                                    if (Rand.NextBool(3)) {
                                        this.addWorldItem("CigaretteSingle", sq, obj, true);
                                        if (Rand.NextBool(2)) {
                                            this.addWorldItem("Matches", sq, obj, true);
                                        } else if (Rand.NextBool(2)) {
                                            this.addWorldItem("LighterDisposable", sq, obj, true);
                                        }
                                    }

                                    int alcohol = Rand.Next(7);
                                    switch (alcohol) {
                                        case 0:
                                            this.addWorldItem("Whiskey", sq, obj, true);
                                            break;
                                        case 1:
                                            this.addWorldItem("Wine", sq, obj, true);
                                            break;
                                        case 2:
                                            this.addWorldItem("Wine2", sq, obj, true);
                                            break;
                                        case 3:
                                            this.addWorldItem("BeerCan", sq, obj, true);
                                            break;
                                        case 4:
                                            this.addWorldItem("BeerBottle", sq, obj, true);
                                    }

                                    if (Rand.NextBool(3)) {
                                        int food = Rand.Next(7);
                                        switch (food) {
                                            case 0:
                                                this.addWorldItem("Crisps", sq, obj, true);
                                                break;
                                            case 1:
                                                this.addWorldItem("Crisps2", sq, obj, true);
                                                break;
                                            case 2:
                                                this.addWorldItem("Crisps3", sq, obj, true);
                                                break;
                                            case 3:
                                                this.addWorldItem("Crisps4", sq, obj, true);
                                                break;
                                            case 4:
                                                this.addWorldItem("Peanuts", sq, obj, true);
                                                break;
                                            case 5:
                                                this.addWorldItem("ScratchTicket_Loser", sq, obj, true);
                                        }
                                    }

                                    if (Rand.NextBool(4)) {
                                        this.addWorldItem("CardDeck", sq, obj, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean roomValid(IsoGridSquare sq) {
        return sq.getRoom() != null && "bar".equals(sq.getRoom().getName());
    }

    /**
     * Description copied from class: RandomizedBuildingBase
     */
    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        return def.getRoom("bar") != null && def.getRoom("stripclub") == null || force;
    }

    public RBBar() {
        this.name = "Bar";
        this.setAlwaysDo(true);
    }
}
