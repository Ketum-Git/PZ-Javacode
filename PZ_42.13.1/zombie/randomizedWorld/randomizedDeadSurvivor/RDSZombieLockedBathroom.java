// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.randomizedWorld.randomizedDeadSurvivor;

import zombie.UsedFromLua;
import zombie.VirtualZombieManager;
import zombie.ZombieSpawnRecorder;
import zombie.characters.IsoZombie;
import zombie.core.random.Rand;
import zombie.iso.BuildingDef;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.areas.IsoRoom;
import zombie.iso.objects.IsoBarricade;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoDoor;
import zombie.network.GameServer;

/**
 * Zombie inside the barricaded bathroom and a dead corpse in front of it with a pistol
 */
@UsedFromLua
public final class RDSZombieLockedBathroom extends RandomizedDeadSurvivorBase {
    @Override
    public void randomizeDeadSurvivor(BuildingDef def) {
        IsoDeadBody body = null;

        for (int i = 0; i < def.rooms.size(); i++) {
            RoomDef room = def.rooms.get(i);
            IsoGridSquare sq = null;
            if ("bathroom".equals(room.name)) {
                if (IsoWorld.getZombiesEnabled()) {
                    IsoGridSquare g = IsoWorld.instance.currentCell.getGridSquare(room.getX(), room.getY(), room.getZ());
                    if (g != null && g.getRoom() != null) {
                        IsoRoom isoRoom = g.getRoom();
                        g = isoRoom.getRandomFreeSquare();
                        if (g != null) {
                            VirtualZombieManager.instance.choices.clear();
                            VirtualZombieManager.instance.choices.add(g);
                            IsoZombie zombie = VirtualZombieManager.instance.createRealZombieAlways(IsoDirections.fromIndex(Rand.Next(8)).index(), false);
                            ZombieSpawnRecorder.instance.record(zombie, this.getClass().getSimpleName());
                        }
                    }
                }

                for (int x = room.x - 1; x < room.x2 + 1; x++) {
                    for (int y = room.y - 1; y < room.y2 + 1; y++) {
                        sq = IsoWorld.instance.getCell().getGridSquare(x, y, room.getZ());
                        if (sq != null) {
                            IsoDoor door = sq.getIsoDoor();
                            if (door != null && this.isDoorToRoom(door, room)) {
                                if (door.IsOpen()) {
                                    door.ToggleDoor(null);
                                }

                                IsoBarricade barricade = IsoBarricade.AddBarricadeToObject(door, sq.getRoom().def == room);
                                if (barricade != null) {
                                    barricade.addPlank(null, null);
                                    if (GameServer.server) {
                                        barricade.transmitCompleteItemToClients();
                                    }
                                }

                                body = this.addDeadBodyTheOtherSide(door);
                                break;
                            }
                        }
                    }

                    if (body != null) {
                        break;
                    }
                }

                if (body != null) {
                    body.setPrimaryHandItem(this.addWeapon("Base.Pistol", true));
                }

                return;
            }
        }
    }

    private boolean isDoorToRoom(IsoDoor door, RoomDef roomDef) {
        if (door != null && roomDef != null) {
            IsoGridSquare sqInside = door.getSquare();
            IsoGridSquare sqOpposite = door.getOppositeSquare();
            return sqInside != null && sqOpposite != null ? sqInside.getRoomID() == roomDef.id != (sqOpposite.getRoomID() == roomDef.id) : false;
        } else {
            return false;
        }
    }

    private boolean checkIsBathroom(IsoGridSquare sq) {
        return sq.getRoom() != null && "bathroom".equals(sq.getRoom().getName());
    }

    private IsoDeadBody addDeadBodyTheOtherSide(IsoDoor obj) {
        IsoGridSquare sq = null;
        if (obj.north) {
            sq = IsoWorld.instance.getCell().getGridSquare((double)obj.getX(), (double)obj.getY(), (double)obj.getZ());
            if (this.checkIsBathroom(sq)) {
                sq = IsoWorld.instance.getCell().getGridSquare((double)obj.getX(), (double)(obj.getY() - 1.0F), (double)obj.getZ());
            }
        } else {
            sq = IsoWorld.instance.getCell().getGridSquare((double)obj.getX(), (double)obj.getY(), (double)obj.getZ());
            if (this.checkIsBathroom(sq)) {
                sq = IsoWorld.instance.getCell().getGridSquare((double)(obj.getX() - 1.0F), (double)obj.getY(), (double)obj.getZ());
            }
        }

        return createRandomDeadBody(sq.getX(), sq.getY(), sq.getZ(), null, Rand.Next(5, 10));
    }

    public RDSZombieLockedBathroom() {
        this.name = "Locked in Bathroom";
        this.setChance(5);
    }
}
