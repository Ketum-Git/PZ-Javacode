// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.vehicleNetworkSound.server;

import gnu.trove.set.hash.TShortHashSet;
import java.util.HashSet;
import java.util.Set;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.network.GameServer;
import zombie.vehicles.BaseVehicle;

public final class Manager {
    private static Manager instance;
    private final Connection[] connections = new Connection[512];
    private final Set<BaseVehicle> tempVehicles = new HashSet<>();
    private final TShortHashSet relevantVehicleIDs = new TShortHashSet();

    public static Manager getInstance() {
        if (instance == null) {
            instance = new Manager();
        }

        return instance;
    }

    public void connect(UdpConnection udpConnection) {
        int index = udpConnection.getIndex();
        this.connections[index] = new Connection(udpConnection);
    }

    public void disconnect(UdpConnection udpConnection) {
        int index = udpConnection.getIndex();
        this.connections[index] = null;
    }

    public void update() {
        for (UdpConnection udpConnection : GameServer.udpEngine.connections) {
            this.updateConnection(udpConnection);
        }
    }

    private void updateConnection(UdpConnection udpConnection) {
        int index = udpConnection.getIndex();
        Connection connection = this.connections[index];
        this.getVehiclesRelevantToConnection(connection, this.tempVehicles);
        this.relevantVehicleIDs.clear();

        for (BaseVehicle vehicle : this.tempVehicles) {
            connection.updateVehicle(vehicle);
            this.relevantVehicleIDs.add(vehicle.getId());
        }

        connection.forgetIrrelevantVehicles(this.relevantVehicleIDs);
    }

    public void removeVehicle(BaseVehicle vehicle) {
        for (UdpConnection udpConnection : GameServer.udpEngine.connections) {
            int index = udpConnection.getIndex();
            if (this.connections[index] != null) {
                this.connections[index].removeVehicle(vehicle);
            }
        }
    }

    private void getVehiclesRelevantToConnection(Connection connection, Set<BaseVehicle> vehicles) {
        vehicles.clear();

        for (BaseVehicle vehicle : IsoWorld.instance.currentCell.getVehicles()) {
            if (vehicle.vehicleId != -1 && connection.isRelevant(vehicle)) {
                vehicles.add(vehicle);
            }
        }
    }
}
