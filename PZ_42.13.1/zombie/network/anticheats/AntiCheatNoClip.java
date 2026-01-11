// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.anticheats;

import java.util.HashMap;
import zombie.ai.states.ClimbThroughWindowState;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.Vector3;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoDoor;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.network.GameServer;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;

public class AntiCheatNoClip extends AbstractAntiCheat {
    private static final Vector3 sourcePoint = new Vector3();
    private static final Vector3 targetPoint = new Vector3();
    private static final Vector2 direction2 = new Vector2();
    private static final Vector3 direction3 = new Vector3();
    private static final HashMap<Short, Long> teleports = new HashMap<>();
    private static IsoGridSquare sourceSquare;
    private static IsoGridSquare targetSquare;

    public static void teleport(IsoPlayer player) {
        teleports.put(player.getOnlineID(), System.currentTimeMillis());
    }

    @Override
    public void react(UdpConnection connection, INetworkPacket packet) {
        super.react(connection, packet);
        AntiCheatNoClip.IAntiCheat field = (AntiCheatNoClip.IAntiCheat)packet;
        IsoPlayer player = connection.players[field.getPlayerIndex()];
        if ((!connection.role.hasCapability(Capability.ToggleNoclipHimself) || !player.isNoClip())
            && (!connection.role.hasCapability(Capability.UseFastMoveCheat) || !player.isFastMoveCheat())) {
            sourcePoint.set(connection.releventPos[field.getPlayerIndex()]);
            GameServer.sendTeleport(player, sourcePoint.x, sourcePoint.y, sourcePoint.z);
            teleports.remove(player.getOnlineID());
        }
    }

    @Override
    public String validate(UdpConnection connection, INetworkPacket packet) {
        String result = super.validate(connection, packet);
        AntiCheatNoClip.IAntiCheat field = (AntiCheatNoClip.IAntiCheat)packet;
        IsoPlayer player = connection.players[field.getPlayerIndex()];
        if (player.isDead()) {
            return result;
        } else if (!player.isKnockedDown()
            && (
                player.getCurrentState() == null
                    || !"PlayerGetUpState".equals(player.getCurrentState().getName()) && !"PlayerHitReactionState".equals(player.getCurrentState().getName())
            )) {
            if ("ClimbThroughWindowState".equals(player.getNetworkCharacterAI().getState().getEnterStateName())) {
                return result;
            } else {
                long timestamp = teleports.getOrDefault(player.getOnlineID(), 0L);
                if ((!connection.role.hasCapability(Capability.ToggleNoclipHimself) || !player.isNoClip())
                    && (!connection.role.hasCapability(Capability.UseFastMoveCheat) || !player.isFastMoveCheat())) {
                    if (player.getVehicle() != null) {
                        return result;
                    } else if (System.currentTimeMillis() - timestamp <= 500L) {
                        return result;
                    } else {
                        teleports.remove(player.getOnlineID());
                        sourcePoint.set(connection.releventPos[field.getPlayerIndex()]);
                        sourceSquare = ServerMap.getGridSquare(sourcePoint);
                        field.getPosition(targetPoint);
                        targetSquare = ServerMap.getGridSquare(targetPoint);
                        if (sourceSquare != null && targetSquare != null && sourceSquare != targetSquare) {
                            direction2.set(targetSquare.x - sourceSquare.x, targetSquare.y - sourceSquare.y);
                            IsoDirections dir = IsoDirections.fromAngle(direction2);
                            direction3.set(targetSquare.x - sourceSquare.x, targetSquare.y - sourceSquare.y, targetSquare.z - sourceSquare.z);
                            IsoGridSquare outsideBasement = ServerMap.instance.getGridSquare(targetSquare.x, targetSquare.y, sourceSquare.z);
                            if (direction2.getLength() > 2.5F && sourceSquare.x > 100 && sourceSquare.y > 100) {
                                result = String.format(
                                    "Long blocked (%d,%d,%d) => (%d,%d,%d) len=%f",
                                    sourceSquare.x,
                                    sourceSquare.y,
                                    sourceSquare.z,
                                    targetSquare.x,
                                    targetSquare.y,
                                    targetSquare.z,
                                    direction3.getLength()
                                );
                            } else if (1.0F < direction2.getLength() && direction2.getLength() < 2.0F) {
                                IsoGridSquare int1 = ServerMap.instance.getGridSquare(sourceSquare.x, targetSquare.y, targetSquare.z);
                                IsoGridSquare int2 = ServerMap.instance.getGridSquare(targetSquare.x, sourceSquare.y, targetSquare.z);
                                boolean isReachable = checkPathClamp(sourceSquare, int1) && checkPathClamp(int1, targetSquare)
                                    || checkPathClamp(sourceSquare, int2) && checkPathClamp(int2, targetSquare);
                                if (!isReachable) {
                                    result = String.format(
                                        "Diagonal blocked (%d,%d,%d) => (%d,%d,%d) len=%f",
                                        sourceSquare.x,
                                        sourceSquare.y,
                                        sourceSquare.z,
                                        targetSquare.x,
                                        targetSquare.y,
                                        targetSquare.z,
                                        direction3.getLength()
                                    );
                                }
                            } else if (direction3.getLength() > 1.0F
                                && sourceSquare.z < 0
                                && targetSquare.z == 0
                                && !outsideBasement.TreatAsSolidFloor()
                                && !sourceSquare.HasStairs()) {
                                boolean isReachable = checkPathClamp(sourceSquare, outsideBasement);
                                if (!isReachable) {
                                    result = String.format(
                                        "Basement blocked (%d,%d,%d) => (%d,%d,%d) len=%f",
                                        sourceSquare.x,
                                        sourceSquare.y,
                                        sourceSquare.z,
                                        targetSquare.x,
                                        targetSquare.y,
                                        targetSquare.z,
                                        direction3.getLength()
                                    );
                                }
                            } else {
                                boolean isReachable = checkPathClamp(sourceSquare, targetSquare);
                                if (isReachable) {
                                    isReachable = sourceSquare.z > targetSquare.z || checkReachablePath(dir, player);
                                    if (!isReachable) {
                                        result = String.format(
                                            "Reachable blocked (%d,%d,%d) => (%d,%d,%d) len=%f",
                                            sourceSquare.x,
                                            sourceSquare.y,
                                            sourceSquare.z,
                                            targetSquare.x,
                                            targetSquare.y,
                                            targetSquare.z,
                                            direction3.getLength()
                                        );
                                    }
                                } else {
                                    isReachable = checkUnreachablePath(dir, player);
                                    if (!isReachable) {
                                        result = String.format(
                                            "Unreachable blocked (%d,%d,%d) => (%d,%d,%d) len=%f",
                                            sourceSquare.x,
                                            sourceSquare.y,
                                            sourceSquare.z,
                                            targetSquare.x,
                                            targetSquare.y,
                                            targetSquare.z,
                                            direction3.getLength()
                                        );
                                    }
                                }
                            }
                        }

                        return result;
                    }
                } else {
                    return result;
                }
            }
        } else {
            return result;
        }
    }

    private static boolean checkPathClamp(IsoGridSquare source, IsoGridSquare target) {
        boolean isReachableTarget = !source.getPathMatrix(
            PZMath.clamp(target.x - source.x, -1, 1), PZMath.clamp(target.y - source.y, -1, 1), PZMath.clamp(target.z - source.z, -1, 1)
        );
        boolean isReachableSource = !target.getPathMatrix(
            PZMath.clamp(source.x - target.x, -1, 1), PZMath.clamp(source.y - target.y, -1, 1), PZMath.clamp(source.z - target.z, -1, 1)
        );
        return isReachableTarget || isReachableSource && target.z == source.z;
    }

    private static boolean isClose(IsoGridSquare source, IsoGridSquare target) {
        return (-1 <= target.x - source.x || target.x - source.x <= 1) && (-1 <= target.y - source.y || target.y - source.y <= 1);
    }

    private static boolean checkUnreachablePath(IsoDirections direction, IsoPlayer player) {
        IsoObject isoObject = null;
        boolean isHopable = false;
        boolean hasObstacle = false;
        if (IsoDirections.N == direction) {
            isoObject = sourceSquare.getWall(true);
            isHopable = sourceSquare.has(IsoFlagType.HoppableN);
            hasObstacle = ClimbThroughWindowState.isObstacleSquare(sourceSquare);
        } else if (IsoDirections.W == direction) {
            isoObject = sourceSquare.getWall(false);
            isHopable = sourceSquare.has(IsoFlagType.HoppableW);
            hasObstacle = ClimbThroughWindowState.isObstacleSquare(sourceSquare);
        } else if (IsoDirections.S == direction) {
            isoObject = targetSquare.getWall(true);
            isHopable = targetSquare.has(IsoFlagType.HoppableN);
            hasObstacle = ClimbThroughWindowState.isObstacleSquare(targetSquare);
        } else if (IsoDirections.E == direction) {
            isoObject = targetSquare.getWall(false);
            isHopable = targetSquare.has(IsoFlagType.HoppableW);
            hasObstacle = ClimbThroughWindowState.isObstacleSquare(targetSquare);
        }

        boolean isClose = isClose(sourceSquare, targetSquare);
        if (isClose) {
            if (targetSquare.z < sourceSquare.z) {
                return true;
            }

            if (sourceSquare.haveSheetRope || targetSquare.haveSheetRope) {
                return true;
            }

            if (sourceSquare.HasStairs() || targetSquare.HasStairs()) {
                return true;
            }

            if (sourceSquare.getProperties().has(IsoFlagType.burntOut) || targetSquare.getProperties().has(IsoFlagType.burntOut)) {
                return true;
            }
        }

        if (isoObject != null) {
            if (isoObject.isHoppable() || player.canClimbOverWall(direction) || isHopable) {
                return true;
            }

            if (isoObject instanceof IsoThumpable thumpable) {
                return thumpable.canClimbThrough(player) || thumpable.canClimbOver(player) || thumpable.IsOpen() || thumpable.couldBeOpen(player);
            }

            if (isoObject instanceof IsoDoor door) {
                return door.IsOpen() || door.canClimbOver(player) || door.couldBeOpen(player);
            }

            if (isoObject instanceof IsoWindow window) {
                return window.canClimbThrough(player);
            }
        }

        return hasObstacle;
    }

    private static boolean checkReachablePath(IsoDirections direction, IsoPlayer player) {
        IsoObject isoObject = null;
        if (IsoDirections.N == direction) {
            isoObject = sourceSquare.getDoorOrWindow(true);
        } else if (IsoDirections.W == direction) {
            isoObject = sourceSquare.getDoorOrWindow(false);
        } else if (IsoDirections.S == direction) {
            isoObject = targetSquare.getDoorOrWindow(true);
        } else if (IsoDirections.E == direction) {
            isoObject = targetSquare.getDoorOrWindow(false);
        }

        if (isoObject instanceof IsoThumpable thumpable) {
            return thumpable.canClimbThrough(player) || thumpable.canClimbOver(player) || thumpable.IsOpen() || thumpable.couldBeOpen(player);
        } else if (!(isoObject instanceof IsoDoor door)) {
            return isoObject instanceof IsoWindow window ? window.canClimbThrough(player) : true;
        } else {
            return door.IsOpen() || door.canClimbOver(player) || door.couldBeOpen(player);
        }
    }

    public interface IAntiCheat {
        byte getPlayerIndex();

        Vector3 getPosition(Vector3 arg0);
    }
}
