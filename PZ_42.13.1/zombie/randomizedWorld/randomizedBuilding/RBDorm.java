// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.Objects;
import zombie.UsedFromLua;
import zombie.core.random.Rand;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.ItemSpawner;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public final class RBDorm extends RandomizedBuildingBase {
    @Override
    public void randomizeBuilding(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null && this.roomValid(sq) && sq.getObjects().size() <= 3) {
                        for (int i = 0; i < sq.getObjects().size(); i++) {
                            IsoObject obj = sq.getObjects().get(i);
                            if (obj.isTableSurface() && sq.getObjects().size() <= 3 && Rand.NextBool(3)) {
                                IsoDirections facing = this.getFacing(obj.getSprite());
                                if (facing != null) {
                                    if (facing == IsoDirections.E) {
                                        ItemSpawner.spawnItem(
                                            RBBasic.getDormClutterItem(), sq, 0.42F, Rand.Next(0.34F, 0.74F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                    }

                                    if (facing == IsoDirections.W) {
                                        ItemSpawner.spawnItem(
                                            RBBasic.getDormClutterItem(), sq, 0.64F, Rand.Next(0.34F, 0.74F), obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                    }

                                    if (facing == IsoDirections.N) {
                                        ItemSpawner.spawnItem(
                                            RBBasic.getDormClutterItem(), sq, Rand.Next(0.44F, 0.64F), 0.67F, obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                    }

                                    if (facing == IsoDirections.S) {
                                        ItemSpawner.spawnItem(
                                            RBBasic.getDormClutterItem(), sq, Rand.Next(0.44F, 0.64F), 0.42F, obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
                                    }
                                } else {
                                    ItemSpawner.spawnItem(
                                        RBBasic.getDormClutterItem(), sq, Rand.Next(0.4F, 0.8F), Rand.Next(0.4F, 0.8F), obj.getSurfaceOffsetNoTable() / 96.0F
                                    );
                                }

                                if (Rand.NextBool(3)) {
                                    if (facing != null) {
                                        if (facing == IsoDirections.E) {
                                            ItemSpawner.spawnItem(
                                                RBBasic.getDormClutterItem(), sq, 0.42F, Rand.Next(0.34F, 0.74F), obj.getSurfaceOffsetNoTable() / 96.0F
                                            );
                                        }

                                        if (facing == IsoDirections.W) {
                                            ItemSpawner.spawnItem(
                                                RBBasic.getDormClutterItem(), sq, 0.64F, Rand.Next(0.34F, 0.74F), obj.getSurfaceOffsetNoTable() / 96.0F
                                            );
                                        }

                                        if (facing == IsoDirections.N) {
                                            ItemSpawner.spawnItem(
                                                RBBasic.getDormClutterItem(), sq, Rand.Next(0.44F, 0.64F), 0.67F, obj.getSurfaceOffsetNoTable() / 96.0F
                                            );
                                        }

                                        if (facing == IsoDirections.S) {
                                            ItemSpawner.spawnItem(
                                                RBBasic.getDormClutterItem(), sq, Rand.Next(0.44F, 0.64F), 0.42F, obj.getSurfaceOffsetNoTable() / 96.0F
                                            );
                                        }
                                    } else {
                                        ItemSpawner.spawnItem(
                                            RBBasic.getDormClutterItem(),
                                            sq,
                                            Rand.Next(0.4F, 0.8F),
                                            Rand.Next(0.4F, 0.8F),
                                            obj.getSurfaceOffsetNoTable() / 96.0F
                                        );
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
        return sq.getRoom() != null && "livingroom".equals(sq.getRoom().getName());
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        IsoCell cell = IsoWorld.instance.currentCell;
        IsoGridSquare sq = cell.getGridSquare(def.x, def.y, 0);
        return sq == null
            ? false
            : def.getRoom("livingroom") != null
                && ItemPickerJava.getSquareZombiesType(sq) != null
                && Objects.equals(ItemPickerJava.getSquareZombiesType(sq), "University");
    }

    public RBDorm() {
        this.name = "Dorm";
        this.setAlwaysDo(true);
    }

    private IsoDirections getFacing(IsoSprite sprite) {
        if (sprite != null && sprite.getProperties().has("Facing")) {
            String Facing = sprite.getProperties().get("Facing");
            switch (Facing) {
                case "N":
                    return IsoDirections.N;
                case "S":
                    return IsoDirections.S;
                case "W":
                    return IsoDirections.W;
                case "E":
                    return IsoDirections.E;
            }
        }

        return null;
    }
}
