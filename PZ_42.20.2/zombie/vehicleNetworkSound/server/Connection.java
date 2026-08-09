// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleNetworkSound.server;

import gnu.trove.map.hash.TShortObjectHashMap;
import gnu.trove.set.hash.TShortHashSet;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.vehicles.BaseVehicle;

final class Connection {
    private final UdpConnection udpConnection;
    private final TShortObjectHashMap<VehicleState> stateMap = new TShortObjectHashMap<>();
    private static final TShortHashSet irrelevantVehicleIDs = new TShortHashSet();

    Connection(UdpConnection udpConnection) {
        this.udpConnection = udpConnection;
    }

    void updateVehicle(BaseVehicle vehicle) {
        VehicleState state = this.getState(vehicle);
        if (state == null) {
            state = this.createState(vehicle);
            state.sendNew(this.udpConnection);
        } else {
            state.update(vehicle, this.udpConnection);
        }
    }

    void forgetIrrelevantVehicles(TShortHashSet relevantVehicleIDs) {
        irrelevantVehicleIDs.clear();
        irrelevantVehicleIDs.addAll(this.stateMap.keySet());
        irrelevantVehicleIDs.removeAll(relevantVehicleIDs);
        irrelevantVehicleIDs.forEach(id -> {
            VehicleState state = this.stateMap.remove(id);
            state.remove(this.udpConnection);
            return true;
        });
    }

    void removeVehicle(BaseVehicle vehicle) {
        VehicleState state = this.getState(vehicle);
        if (state != null) {
            state.remove(this.udpConnection);
            this.stateMap.remove(state.id);
        }
    }

    boolean isRelevant(BaseVehicle vehicle) {
        float radius = 0.0F;
        if (vehicle.isAlarmActive()) {
            radius = PZMath.max(radius, 500.0F);
        }

        if (vehicle.isBackupBeeperSounding()) {
            radius = PZMath.max(radius, 150.0F);
        }

        if (vehicle.isDoorAlarmSounding()) {
            radius = PZMath.max(radius, 50.0F);
        }

        if (vehicle.getEngineState() != BaseVehicle.engineStateTypes.Idle) {
            radius = PZMath.max(radius, 200.0F);
        }

        if (vehicle.isHornSounding()) {
            radius = PZMath.max(radius, 500.0F);
        }

        if (vehicle.isSirenSounding()) {
            radius = PZMath.max(radius, 500.0F);
        }

        return radius > 0.0F && this.udpConnection.RelevantTo(vehicle.getX(), vehicle.getY(), radius);
    }

    VehicleState createState(BaseVehicle vehicle) {
        VehicleState state = new VehicleState();
        state.set(vehicle);
        this.stateMap.put(state.id, state);
        return state;
    }

    VehicleState getState(BaseVehicle vehicle) {
        return this.stateMap.get(vehicle.getId());
    }
}
