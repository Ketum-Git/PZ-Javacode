// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicles;

import gnu.trove.map.hash.TShortShortHashMap;
import java.util.ArrayList;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.physics.Bullet;
import zombie.core.physics.Transform;
import zombie.core.physics.WorldSimulation;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.debug.DebugLog;
import zombie.iso.IsoChunk;
import zombie.iso.IsoChunkMap;
import zombie.iso.IsoGridSquare;
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
    private final UpdateLimit sendRequestFrequency = new UpdateLimit(500L);
    private final UpdateLimit updateRate = new UpdateLimit(100L);
    private final UpdateLimit updatePassengers = new UpdateLimit(1000L);
    public final UdpConnection[] connected = new UdpConnection[512];
    private final float[] tempFloats = new float[27];
    private final float[] engineSound = new float[2];
    private final VehicleManager.PosUpdateVars posUpdateVars = new VehicleManager.PosUpdateVars();

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
        }
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
            DebugLog.Vehicle.trace("removeFromWorld vehicle id=%d", vehicle.vehicleId);
            if (GameServer.server) {
                for (int j = 0; j < GameServer.udpEngine.connections.size(); j++) {
                    UdpConnection connection = GameServer.udpEngine.connections.get(j);
                    if (vehicle.connectionState[connection.index] != null) {
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
        ArrayList<BaseVehicle> vehicles = IsoWorld.instance.currentCell.getVehicles();

        for (int i = 0; i < this.connected.length; i++) {
            if (this.connected[i] != null && !GameServer.udpEngine.connections.contains(this.connected[i])) {
                DebugLog.Vehicle.trace("vehicles: dropped connection %d", i);

                for (int j = 0; j < vehicles.size(); j++) {
                    vehicles.get(j).connectionState[i] = null;
                }

                this.connected[i] = null;
            } else {
                for (int j = 0; j < vehicles.size(); j++) {
                    if (vehicles.get(j).connectionState[i] != null) {
                        BaseVehicle.ServerVehicleState var10000 = vehicles.get(j).connectionState[i];
                        var10000.flags = (short)(var10000.flags | vehicles.get(j).updateFlags);
                    }
                }
            }
        }

        if (this.updateRate.Check()) {
            if (this.updatePassengers.Check()) {
                for (BaseVehicle vehicle : vehicles) {
                    vehicle.updateFlags = (short)(vehicle.updateFlags | 16384);
                }
            }

            for (int jx = 0; jx < GameServer.udpEngine.connections.size(); jx++) {
                UdpConnection connection = GameServer.udpEngine.connections.get(jx);
                this.sendVehicles(connection);
                this.connected[connection.index] = connection;
            }

            for (int ix = 0; ix < vehicles.size(); ix++) {
                BaseVehicle vehicle = vehicles.get(ix);
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

                if (connection.RelevantTo(vehicle.getX(), vehicle.getY())) {
                    if (vehicle.connectionState[connection.index] == null) {
                        vehicle.connectionState[connection.index] = new BaseVehicle.ServerVehicleState();
                    }

                    BaseVehicle.ServerVehicleState state = vehicle.connectionState[connection.index];
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

            float[] dd = this.tempFloats;
            if (vehicle.interpolation.interpolationDataGet(dd, this.engineSound)) {
                if (!vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.Local)
                    && !vehicle.isNetPlayerAuthorization(BaseVehicle.Authorization.LocalCollide)) {
                    Bullet.setOwnVehiclePhysics(vehicle.vehicleId, dd);
                    float x = dd[0];
                    float y = dd[1];
                    float z = dd[2];
                    IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare((double)x, (double)y, 0.0);
                    this.clientUpdateVehiclePos(vehicle, x, y, z, sq);
                    vehicle.limitPhysicValid.BlockCheck();
                    if (GameClient.client) {
                        this.vehiclePosUpdate(vehicle, dd);
                    }

                    vehicle.engineSpeed = this.engineSound[0];
                    vehicle.throttle = this.engineSound[1];
                }
            } else {
                vehicle.getController().control_NoControl();
                vehicle.throttle = 0.0F;
                vehicle.setSpeedKmHour(0.0F);
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

    public void sendVehicleRequest(short VehicleID, short flag) {
        if (1 == flag || this.sendRequestFrequency.Check()) {
            INetworkPacket.send(PacketTypes.PacketType.VehicleRequest, VehicleID, flag);
        }
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
