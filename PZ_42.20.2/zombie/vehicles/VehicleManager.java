// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import gnu.trove.map.hash.TShortShortHashMap;
import java.util.ArrayList;
import java.util.Set;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.physics.Bullet;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugType;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;

public final class VehicleManager {
    public static VehicleManager instance;
    private final VehicleIDMap idToVehicle = VehicleIDMap.instance;
    private final ArrayList<BaseVehicle> vehicles = new ArrayList<>();
    private boolean idMapDirty = true;
    public final TShortShortHashMap towedVehicleMap = new TShortShortHashMap();
    private final UpdateLimit updateRate = new UpdateLimit(100L);
    private final UpdateLimit updatePassengers = new UpdateLimit(1000L);
    private final float[] tempFloats = new float[27];
    private final float[] engineSound = new float[2];
    private final VehicleManager.PosUpdateVars posUpdateVars = new VehicleManager.PosUpdateVars();
    static final float OTHER_VEHICLE_IS_CLOSE_DISTANCE = 10.0F;

    public void removeVehicles(IsoPlayer player) {
        int radius = (IsoChunkMap.chunkGridWidth + 2) * 8;
        ArrayList<BaseVehicle> vehiclesToRemove = new ArrayList<>();
        if (player != null) {
            for (BaseVehicle baseVehicle : this.getVehicles()) {
                if (!baseVehicle.hasPassenger() && baseVehicle.isInRange(player, radius)) {
                    vehiclesToRemove.add(baseVehicle);
                }
            }

            for (BaseVehicle baseVehiclex : vehiclesToRemove) {
                baseVehiclex.permanentlyRemove();
            }
        }
    }

    public void registerVehicle(BaseVehicle vehicle) {
        this.idToVehicle.put(vehicle.vehicleId, vehicle);
        this.idMapDirty = true;
    }

    public void unregisterVehicle(BaseVehicle vehicle) {
        if (this.idToVehicle.containsKey(vehicle.vehicleId)) {
            this.idToVehicle.remove(vehicle.vehicleId);
            this.idMapDirty = true;
            if (GameServer.server) {
                for (int i = 0; i < GameServer.udpEngine.connections.size(); i++) {
                    GameServer.udpEngine.connections.get(i).vehicleStates.remove(vehicle.vehicleId);
                }
            }
        }
    }

    public BaseVehicle.ServerVehicleState getVehicleState(UdpConnection connection, BaseVehicle vehicle) {
        BaseVehicle.ServerVehicleState state = connection.vehicleStates.get(vehicle.vehicleId);
        if (state == null) {
            state = new BaseVehicle.ServerVehicleState();
            connection.vehicleStates.put(vehicle.vehicleId, state);
        }

        return state;
    }

    public BaseVehicle getVehicleByID(short id) {
        return this.idToVehicle.get(id);
    }

    public ArrayList<BaseVehicle> getVehicles() {
        if (this.idMapDirty) {
            this.vehicles.clear();
            this.idToVehicle.toArrayList(this.vehicles);
            this.idMapDirty = false;
        }

        return this.vehicles;
    }

    public void removeFromWorld(BaseVehicle vehicle) {
        if (vehicle.vehicleId != -1) {
            DebugType.Vehicle.trace("removeFromWorld vehicle id=%d", vehicle.vehicleId);
            if (GameServer.server) {
                for (int j = 0; j < GameServer.udpEngine.connections.size(); j++) {
                    UdpConnection connection = GameServer.udpEngine.connections.get(j);
                    if (connection.vehicleStates.containsKey(vehicle.vehicleId)) {
                        INetworkPacket.send(connection, PacketTypes.PacketType.VehicleRemove, vehicle);
                    }
                }
            }

            this.unregisterVehicle(vehicle);
            if (GameClient.client) {
                vehicle.serverRemovedFromWorld = false;
                if (vehicle.interpolation != null) {
                    vehicle.interpolation.clear();
                }
            }
        }
    }

    public void serverUpdate() {
        if (this.updateRate.Check()) {
            Set<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();
            if (this.updatePassengers.Check()) {
                for (BaseVehicle vehicle : vehicles) {
                    vehicle.updateFlags = (short)(vehicle.updateFlags | 16384);
                }
            }

            for (int j = 0; j < GameServer.udpEngine.connections.size(); j++) {
                UdpConnection connection = GameServer.udpEngine.connections.get(j);
                if (!GameServer.isDelayedDisconnect(connection)) {
                    this.sendVehicles(connection);
                }
            }

            for (BaseVehicle vehicle : vehicles) {
                if ((vehicle.updateFlags & 3056) != 0) {
                    for (int jx = 0; jx < vehicle.getPartCount(); jx++) {
                        VehiclePart part = vehicle.getPartByIndex(jx);
                        part.updateFlags = 0;
                    }
                }

                vehicle.updateFlags = 0;
            }
        }
    }

    private void sendVehicles(UdpConnection connection) {
        if (connection.isFullyConnected()) {
            for (BaseVehicle vehicle : IsoWorld.instance.currentCell.getVehicles()) {
                if (vehicle.vehicleId == -1) {
                    vehicle.vehicleId = this.idToVehicle.allocateID();
                    this.registerVehicle(vehicle);
                }

                if (connection.isRelevantTo(vehicle.getX(), vehicle.getY())) {
                    BaseVehicle.ServerVehicleState state = this.getVehicleState(connection, vehicle);
                    if (state.shouldSend(vehicle)) {
                        if ((state.flags & 1) != 0) {
                            INetworkPacket.send(connection, PacketTypes.PacketType.VehicleFullUpdate, vehicle);
                            state.flags = (short)(state.flags | 24578);
                        } else {
                            INetworkPacket.send(connection, PacketTypes.PacketType.VehicleUpdate, vehicle, state.flags);
                        }

                        if ((state.flags & 8192) != 0) {
                            state.setAuthorization(vehicle);
                        }

                        if ((state.flags & 2) != 0) {
                            state.x = vehicle.getX();
                            state.y = vehicle.getY();
                            state.z = vehicle.jniTransform.origin.y;
                            state.orient.set(vehicle.savedRot);
                        }

                        state.flags = 0;
                    }
                }
            }
        }
    }

    private void vehiclePosUpdate(BaseVehicle vehicle, float[] ff) {
        int fn = 0;
        Transform tempTransform = this.posUpdateVars.transform;
        Vector3f tempVector3f = this.posUpdateVars.vector3f;
        Quaternionf javaxQuat4f = this.posUpdateVars.quatf;
        float[] wheelSteer = this.posUpdateVars.wheelSteer;
        float[] wheelRotation = this.posUpdateVars.wheelRotation;
        float[] wheelSkidInfo = this.posUpdateVars.wheelSkidInfo;
        float[] wheelSuspensionLength = this.posUpdateVars.wheelSuspensionLength;
        float x = ff[fn++] - WorldSimulation.instance.offsetX;
        float y = ff[fn++] - WorldSimulation.instance.offsetY;
        float z = ff[fn++];
        tempTransform.origin.set(x, z, y);
        float qx = ff[fn++];
        float qy = ff[fn++];
        float qz = ff[fn++];
        float qw = ff[fn++];
        javaxQuat4f.set(qx, qy, qz, qw);
        javaxQuat4f.normalize();
        tempTransform.setRotation(javaxQuat4f);
        float vx = ff[fn++];
        float vy = ff[fn++];
        float vz = ff[fn++];
        tempVector3f.set(vx, vy, vz);
        int countOfWheel = (int)ff[fn++];

        for (int n = 0; n < countOfWheel; n++) {
            wheelSteer[n] = ff[fn++];
            wheelRotation[n] = ff[fn++];
            wheelSkidInfo[n] = ff[fn++];
            wheelSuspensionLength[n] = ff[fn++];
        }

        vehicle.jniTransform.set(tempTransform);
        vehicle.jniLinearVelocity.set(tempVector3f);
        vehicle.jniTransform.basis.getScale(tempVector3f);
        if (tempVector3f.x < 0.99 || tempVector3f.y < 0.99 || tempVector3f.z < 0.99) {
            vehicle.jniTransform.basis.scale(1.0F / tempVector3f.x, 1.0F / tempVector3f.y, 1.0F / tempVector3f.z);
        }

        Vector3f forward = vehicle.getForwardVector(BaseVehicle.allocVector3f());
        vehicle.setSpeedKmHour(vehicle.jniLinearVelocity.length() * 3.6F * PZMath.sign(forward.dot(vehicle.jniLinearVelocity)));
        BaseVehicle.releaseVector3f(forward);

        for (int m = 0; m < 4; m++) {
            vehicle.wheelInfo[m].steering = wheelSteer[m];
            vehicle.wheelInfo[m].rotation = wheelRotation[m];
            vehicle.wheelInfo[m].skidInfo = wheelSkidInfo[m];
            vehicle.wheelInfo[m].suspensionLength = wheelSuspensionLength[m];
        }

        vehicle.polyDirty = true;
    }

    private void updateVehiclePos(BaseVehicle vehicle, VehicleInterpolation towedByInterpolation) {
        float[] dd = this.tempFloats;
        if (vehicle.interpolation.interpolationDataGet(dd, this.engineSound, towedByInterpolation)) {
            if (vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Local) || vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide)) {
                return;
            }

            float x = dd[0];
            float y = dd[1];
            float z = dd[2];
            boolean collide = false;
            if (vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote) || vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide)
                )
             {
                for (int i = 0; i < this.vehicles.size(); i++) {
                    BaseVehicle vehicle2 = this.vehicles.get(i);
                    if (vehicle2 != vehicle
                        && vehicle2 != vehicle.getVehicleTowedBy()
                        && vehicle2 != vehicle.getVehicleTowing()
                        && (
                            vehicle2.isNetPlayerAuthorization(BaseVehicle.Authorization.Remote)
                                || vehicle2.isNetPlayerAuthorization(BaseVehicle.Authorization.RemoteCollide)
                        )) {
                        float distance = IsoUtils.DistanceTo(x, y, z, vehicle2.getX(), vehicle2.getY(), vehicle2.getZ());
                        if (distance < 10.0F) {
                            collide = true;
                            break;
                        }
                    }
                }
            }

            if (collide) {
                Bullet.setOwnVehiclePhysics(vehicle.vehicleId, dd, true);
                Bullet.getOwnVehiclePhysics(vehicle.vehicleId, dd);
                x = dd[0];
                y = dd[1];
                z = dd[2];
            } else {
                Bullet.setOwnVehiclePhysics(vehicle.vehicleId, dd, false);
            }

            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)x, (double)y, 0.0);
            this.clientUpdateVehiclePos(vehicle, x, y, z, sq);
            vehicle.limitPhysicValid.BlockCheck();
            if (GameClient.client) {
                this.vehiclePosUpdate(vehicle, dd);
            }

            vehicle.setEngineSpeed(this.engineSound[0]);
            vehicle.throttle = this.engineSound[1];
        } else {
            vehicle.getController().control_NoControl();
            vehicle.throttle = 0.0F;
            vehicle.setSpeedKmHour(0.0F);
        }
    }

    public void clientUpdate() {
        boolean doPassengersUpdate = this.updatePassengers.Check();
        ArrayList<BaseVehicle> vehicles = this.getVehicles();

        for (int i = 0; i < vehicles.size(); i++) {
            BaseVehicle vehicle = vehicles.get(i);
            if (doPassengersUpdate) {
                instance.sendVehicleRequest(vehicle.getId(), (short)16384);
            }

            if (GameClient.client) {
                if (vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Local)
                    || vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide)) {
                    vehicle.interpolation.clear();
                    continue;
                }
            } else if (vehicle.isKeyboardControlled() || vehicle.getJoypad() != -1) {
                vehicle.interpolation.clear();
                continue;
            }

            if (vehicle.getVehicleTowedBy() == null) {
                this.updateVehiclePos(vehicle, null);
                if (vehicle.getVehicleTowing() != null) {
                    this.updateVehiclePos(vehicle.getVehicleTowing(), vehicle.interpolation);
                }
            }
        }
    }

    public void clientUpdateVehiclePos(BaseVehicle vehicle, float x, float y, float z, IsoGridSquare sq) {
        vehicle.setX(x);
        vehicle.setY(y);
        vehicle.setZ(0.0F);
        vehicle.square = sq;
        vehicle.setCurrent(sq);
        if (sq != null) {
            if (vehicle.chunk != null && vehicle.chunk != sq.chunk) {
                vehicle.chunk.vehicles.remove(vehicle);
            }

            vehicle.chunk = vehicle.square.chunk;
            if (!vehicle.chunk.vehicles.contains(vehicle)) {
                vehicle.chunk.vehicles.add(vehicle);
                IsoChunk.addFromCheckedVehicles(vehicle);
            }

            if (!vehicle.addedToWorld) {
                vehicle.addToWorld();
            }
        } else {
            vehicle.removeFromWorld();
            vehicle.removeFromSquare();
        }

        vehicle.polyDirty = true;
    }

    public void sendVehicleRequest(short vehicleId, short flag) {
        GameClient.connection.vehicleRequests.put(vehicleId, flag);
    }

    public void attachTowing(BaseVehicle vehicleA, BaseVehicle vehicleB, String attachmentA, String attachmentB) {
        if (!this.towedVehicleMap.containsKey(vehicleA.vehicleId)) {
            this.towedVehicleMap.put(vehicleA.vehicleId, vehicleB.vehicleId);
            INetworkPacket.sendToAll(PacketTypes.PacketType.VehicleTowingAttach, vehicleA, vehicleB, attachmentA, attachmentB);
        }
    }

    public void detachTowing(BaseVehicle vehicleTowing, BaseVehicle vehicleTowedBy) {
        if (vehicleTowing != null && this.towedVehicleMap.containsKey(vehicleTowing.vehicleId)) {
            this.towedVehicleMap.remove(vehicleTowing.vehicleId);
        }

        if (vehicleTowedBy != null && this.towedVehicleMap.containsKey(vehicleTowedBy.vehicleId)) {
            this.towedVehicleMap.remove(vehicleTowedBy.vehicleId);
        }

        INetworkPacket.sendToAll(PacketTypes.PacketType.VehicleTowingDetach, vehicleTowing, vehicleTowedBy);
    }

    public short getTowedVehicleID(short towingID) {
        return this.towedVehicleMap.containsKey(towingID) ? this.towedVehicleMap.get(towingID) : -1;
    }

    public static final class PosUpdateVars {
        final Transform transform = new Transform();
        final Vector3f vector3f = new Vector3f();
        final Quaternionf quatf = new Quaternionf();
        final float[] wheelSteer = new float[4];
        final float[] wheelRotation = new float[4];
        final float[] wheelSkidInfo = new float[4];
        final float[] wheelSuspensionLength = new float[4];
    }
}
