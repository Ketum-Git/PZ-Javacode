// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedBuilding;

import java.util.ArrayList;
import java.util.Objects;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.core.stash.StashSystem;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.ItemPickerJava;
import zombie.inventory.types.DrainableComboItem;
import zombie.inventory.types.Moveable;
import zombie.iso.BuildingDef;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.SpawnPoints;
import zombie.iso.areas.IsoBuilding;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameClient;
import zombie.network.GameServer;

@UsedFromLua
public final class RBTrashed extends RandomizedBuildingBase {
    public RBTrashed() {
        this.name = "Trashed Building";
        this.setChance(5);
        this.setAlwaysDo(true);
    }

    @Override
    public void randomizeBuilding(BuildingDef def) {
        this.trashHouse(def);
    }

    @Override
    public boolean isValid(BuildingDef def, boolean force) {
        this.debugLine = "";
        if (GameClient.client) {
            return false;
        } else if (StashSystem.isStashBuilding(def)) {
            this.debugLine = "Stash buildings are invalid";
            return false;
        } else if (SpawnPoints.instance.isSpawnBuilding(def)) {
            this.debugLine = "Spawn houses are invalid";
            return false;
        } else if (def.isAllExplored() && !force) {
            return false;
        } else {
            if (!force) {
                IsoGridSquare sq = IsoCell.getInstance().getGridSquare(def.x, def.y, 0);
                int chance = this.getChance(sq);
                if (Rand.Next(100) > chance) {
                    return false;
                }

                for (int i = 0; i < GameServer.Players.size(); i++) {
                    IsoPlayer player = GameServer.Players.get(i);
                    if (player.getSquare() != null && player.getSquare().getBuilding() != null && player.getSquare().getBuilding().def == def) {
                        return false;
                    }
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
                    return true;
                }
            }
        }
    }

    public IsoGridSquare getFloorSquare(ArrayList<IsoGridSquare> squares, IsoGridSquare square, RoomDef room, IsoBuilding building) {
        IsoGridSquare newSquare = null;
        if (!Rand.NextBool(3)) {
            newSquare = square.getRandomAdjacentFreeSameRoom();
        } else if (!Rand.NextBool(5)) {
            newSquare = Objects.requireNonNull(Objects.requireNonNull(building).getRandomRoom()).getRoomDef().getExtraFreeSquare();
        } else {
            newSquare = room.getExtraFreeSquare();
        }

        return newSquare;
    }

    public void trashHouse(BuildingDef def) {
        IsoCell cell = IsoWorld.instance.currentCell;
        int trashFactor = 40 + SandboxOptions.instance.getCurrentLootedChance();
        if (trashFactor > 90) {
            trashFactor = 90;
        }

        int baseTrashFactor = trashFactor;
        boolean graff = Rand.NextBool(2);
        boolean removedBar = false;

        for (int x = def.x - 1; x < def.x2 + 1; x++) {
            for (int y = def.y - 1; y < def.y2 + 1; y++) {
                for (int z = -32; z < 31; z++) {
                    trashFactor = baseTrashFactor;
                    boolean canTrash = false;
                    if (z < 0) {
                        int depth = z * -1 + 1;
                        trashFactor = baseTrashFactor / depth;
                    }

                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (graff && this.isValidGraffSquare(sq, true, false) && Rand.Next(500) <= trashFactor) {
                        this.graffSquare(sq, true);
                    }

                    if (graff && this.isValidGraffSquare(sq, false, false) && Rand.Next(500) <= trashFactor) {
                        this.graffSquare(sq, false);
                    }

                    if (sq != null && z == 0 && sq.getRoom() == null) {
                        for (int o = 0; o < sq.getObjects().size(); o++) {
                            IsoObject obj = sq.getObjects().get(o);
                            if (obj instanceof IsoDoor door
                                && !door.getProperties().has("DoubleDoor")
                                && !door.getProperties().has("GarageDoor")
                                && !door.isBarricaded()
                                && !door.IsOpen()) {
                                if (z == 0 && door.isLocked()) {
                                    door.destroy();
                                } else if (Rand.Next(200) <= trashFactor) {
                                    door.destroy();
                                } else if (Rand.Next(10) <= trashFactor) {
                                    door.ToggleDoorSilent();
                                    if (door.isLocked()) {
                                        door.setLocked(false);
                                    }
                                } else {
                                    door.setLocked(false);
                                }
                            } else if (obj instanceof IsoDoor doorx && doorx.getProperties().has("GarageDoor") && doorx.isLocked() && !doorx.IsOpen()) {
                                doorx.destroy();
                            }

                            if (sq.getZ() == 0 && obj instanceof IsoWindow window && window.isLocked() && !window.IsOpen()) {
                                window.smashWindow(true, false);
                                window.addBrokenGlass(Rand.NextBool(2));
                            } else if (obj instanceof IsoWindow window && Rand.Next(100) <= trashFactor && !window.IsOpen()) {
                                window.smashWindow(true, false);
                                window.addBrokenGlass(Rand.NextBool(2));
                            }
                        }
                    } else if (sq != null && sq.getRoom() != null && !sq.getRoom().getRoomDef().isKidsRoom()) {
                        IsoBuilding building = sq.getBuilding();
                        RoomDef room = sq.getRoom().getRoomDef();
                        boolean kidsRoom = room != null && room.isKidsRoom();
                        canTrash = !kidsRoom && RandomizedBuildingBase.is1x1AreaClear(sq) && sq.hasFloor() && !sq.isOutside();
                        ArrayList<IsoGridSquare> squares = new ArrayList<>();

                        for (int i = 0; i < 7; i++) {
                            IsoGridSquare testSq = sq.getAdjacentSquare(IsoDirections.fromIndex(i));
                            if (testSq != null && testSq.isExtraFreeSquare() && testSq.getRoom() != null && testSq.getRoom() == sq.getRoom()) {
                                squares.add(testSq);
                            }
                        }

                        if (graff && this.isValidGraffSquare(sq, true, false) && Rand.Next(500) <= trashFactor) {
                            this.graffSquare(sq, true);
                        }

                        if (graff && this.isValidGraffSquare(sq, false, false) && Rand.Next(500) <= trashFactor) {
                            this.graffSquare(sq, false);
                        }

                        for (int o = 0; o < sq.getObjects().size(); o++) {
                            IsoObject objx = sq.getObjects().get(o);
                            if (objx instanceof IsoDoor doorx
                                && !doorx.getProperties().has("DoubleDoor")
                                && !doorx.getProperties().has("GarageDoor")
                                && !doorx.isBarricaded()
                                && !doorx.IsOpen()) {
                                if (z == 0 && doorx.isLocked()) {
                                    doorx.destroy();
                                } else if (Rand.Next(200) <= trashFactor) {
                                    doorx.destroy();
                                } else if (Rand.Next(10) <= trashFactor) {
                                    doorx.ToggleDoorSilent();
                                    if (doorx.isLocked()) {
                                        doorx.setLocked(false);
                                    }
                                } else {
                                    doorx.setLocked(false);
                                }
                            } else if (objx instanceof IsoDoor doorxx && doorxx.getProperties().has("GarageDoor") && doorxx.isLocked() && !doorxx.IsOpen()) {
                                doorxx.destroy();
                            }

                            if (objx instanceof IsoWindow window && Rand.Next(100) <= trashFactor && !window.IsOpen()) {
                                window.smashWindow(true, false);
                                window.addBrokenGlass(Rand.NextBool(2));
                            } else if (sq.getZ() == 0 && objx instanceof IsoWindow window && window.isLocked() && !window.IsOpen()) {
                                window.setIsLocked(false);
                            }

                            if (objx.getContainer() != null && objx.getContainer().getItems() != null && !objx.getSprite().getProperties().has("IsTrashCan")) {
                                for (int k = 0; k < objx.getContainer().getItems().size(); k++) {
                                    InventoryItem item = objx.getContainer().getItems().get(k);
                                    if (Rand.Next(200) < trashFactor && !Objects.equals(item.getType(), "VHS_Home")) {
                                        if (item.getReplaceOnUseFullType() != null && objx.getSquare().getRoom() != null) {
                                            IsoGridSquare square = objx.getSquare().getRandomAdjacentFreeSameRoom();
                                            if (square == null || Rand.NextBool(3)) {
                                                square = objx.getSquare().getRoom().getRoomDef().getExtraFreeSquare();
                                            }

                                            if (square == null || Rand.NextBool(5)) {
                                                square = Objects.requireNonNull(Objects.requireNonNull(objx.getSquare().getBuilding()).getRandomRoom())
                                                    .getRoomDef()
                                                    .getExtraFreeSquare();
                                            }

                                            if (square != null && !square.isOutside() && square.getRoom() != null && square.hasRoomDef()) {
                                                this.addItemOnGround(square, item.getReplaceOnUseFullType());
                                            }
                                        } else if (item instanceof DrainableComboItem drainableComboItem
                                            && drainableComboItem.getReplaceOnDepleteFullType() != null
                                            && objx.getSquare().getRoom() != null) {
                                            IsoGridSquare squarex = this.getFloorSquare(squares, sq, room, building);
                                            if (squarex != null && !squarex.isOutside() && squarex.getRoom() != null && squarex.hasRoomDef()) {
                                                this.addItemOnGround(squarex, drainableComboItem.getReplaceOnDepleteFullType());
                                            }
                                        }

                                        objx.getContainer().getItems().remove(k);
                                        k--;
                                    } else if (Rand.Next(100) < trashFactor && !(item instanceof Moveable)) {
                                        IsoGridSquare squarex = this.getFloorSquare(squares, sq, room, building);
                                        if (squarex != null && !squarex.isOutside() && squarex.getRoom() != null && squarex.hasRoomDef()) {
                                            ItemPickerJava.trashItemLooted(item);
                                            objx.getContainer().getItems().remove(k);
                                            k--;
                                            this.addItemOnGround(squarex, item, false);
                                        }
                                    }
                                }

                                ItemPickerJava.updateOverlaySprite(objx);
                                objx.getContainer().setExplored(true);
                            }

                            if (objx.getContainerByIndex(1) != null && objx.getContainerByIndex(1).getItems() != null) {
                                for (int kx = 0; kx < objx.getContainerByIndex(1).getItems().size(); kx++) {
                                    if (Rand.Next(100) < 80) {
                                        objx.getContainerByIndex(1).getItems().remove(kx);
                                        kx--;
                                    }
                                }

                                ItemPickerJava.updateOverlaySprite(objx);
                                objx.getContainerByIndex(1).setExplored(true);
                            }

                            if (!removedBar
                                && z == 0
                                && objx.getSprite() != null
                                && objx.getSprite().getName() != null
                                && (
                                    Objects.equals(objx.getSprite().getName(), "location_shop_mall_01_18")
                                        || Objects.equals(objx.getSprite().getName(), "location_shop_mall_01_19")
                                )) {
                                sq.RemoveTileObject(objx);
                                sq.RecalcProperties();
                                sq.RecalcAllWithNeighbours(true);
                                if (sq.getWindow() != null) {
                                    sq.getWindow().smashWindow(true, false);
                                }

                                removedBar = true;
                            }
                        }
                    }

                    if (sq != null) {
                        if (canTrash) {
                            if (Rand.Next(500) <= trashFactor) {
                                this.trashSquare(sq);
                            }
                        } else if (z == 0 && sq.isOutside() && RandomizedBuildingBase.is1x1AreaClear(sq) && Rand.Next(2000) <= trashFactor) {
                            this.trashSquare(sq);
                        }

                        if (z == 0 && sq.isOutside() && RandomizedBuildingBase.is2x2AreaClear(sq) && Rand.Next(10000) <= trashFactor) {
                            sq.addCorpse();
                        }
                    }
                }
            }
        }

        for (int ix = 0; ix < def.rooms.size(); ix++) {
            RoomDef room = def.rooms.get(ix);
            IsoGridSquare squarex = room.getExtraFreeSquare();
            int chance = Math.min(baseTrashFactor, room.getIsoRoom().getSquares().size());
            chance = Math.max(chance, baseTrashFactor);
            if (room != null && squarex != null && Rand.Next(1000) <= chance && RandomizedBuildingBase.is2x2AreaClear(squarex)) {
                squarex.addCorpse();
            }
        }

        RoomDef room = def.getRandomRoom(4, true);
        IsoGridSquare freeSQ = getRandomSquareForCorpse(room);
        if (room != null && freeSQ != null && def.getRoomsNumber() > 2 && def.getArea() >= 100 && Rand.NextBool(100)) {
            String zombieType = "Bandit";
            if (!graff && Rand.NextBool(3)) {
                zombieType = "PrivateMilitia";
            } else if (!graff && Rand.NextBool(3)) {
                zombieType = switch (Rand.Next(5)) {
                    case 1 -> "Survivalist02";
                    case 2 -> "Survivalist03";
                    case 3 -> "Survivalist04";
                    case 4 -> "Survivalist05";
                    default -> "Survivalist";
                };
            }

            boolean corpse = Rand.NextBool(2);
            ArrayList<IsoZombie> zombies = this.addZombiesOnSquare(1, zombieType, null, freeSQ);
            if (zombies != null && zombies.get(0) != null) {
                String keyType = "Base.Key1";
                InventoryItem houseKey = InventoryItemFactory.CreateItem("Base.Key1");
                if (houseKey != null) {
                    houseKey.setKeyId(def.getKeyId());
                    zombies.get(0).addItemToSpawnAtDeath(houseKey);
                }

                if (corpse) {
                    freeSQ.createCorpse(zombies.get(0));
                }
            }

            for (int ixx = 0; ixx < def.rooms.size(); ixx++) {
                room = def.rooms.get(ixx);
                IsoGridSquare squarex = room.getExtraFreeSquare();
                if (squarex != null && Rand.NextBool(100) && RandomizedBuildingBase.is2x2AreaClear(squarex)) {
                    if (Rand.NextBool(10)) {
                        corpse = Rand.NextBool(2);
                    }

                    zombies = this.addZombiesOnSquare(1, zombieType, null, squarex);
                    if (corpse) {
                        squarex.createCorpse(zombies.get(0));
                    }
                }
            }
        }

        def.setAllExplored(true);
        def.alarmed = false;
    }
}
