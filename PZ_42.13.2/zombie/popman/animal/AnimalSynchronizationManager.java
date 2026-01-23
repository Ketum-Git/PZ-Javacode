// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman.animal;

import java.util.HashMap;
import java.util.HashSet;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.core.utils.UpdateTimer;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.character.AnimalUpdatePacket;
import zombie.network.packets.character.AnimalUpdateReliablePacket;
import zombie.network.packets.character.AnimalUpdateUnreliablePacket;

public class AnimalSynchronizationManager {
    private static final AnimalSynchronizationManager instance = new AnimalSynchronizationManager();
    private static final HashMap<Long, HashSet<Short>> requests = new HashMap<>();
    private static final HashSet<Short> sendToClients = new HashSet<>();
    private static final HashSet<Short> receivedToSend = new HashSet<>();
    private static final HashSet<Short> deletedByServer = new HashSet<>();
    private static final HashSet<Short> deletedToSend = new HashSet<>();
    private static final HashSet<UdpConnection> extraUpdate = new HashSet<>();
    private static final UpdateLimit sendAsReliable = new UpdateLimit(2000L);
    private static final byte ANIMAL_UPDATE_RATE_MS = 100;
    private static final short MAX_ANIMALS_PER_PACKET = 150;

    public static AnimalSynchronizationManager getInstance() {
        return instance;
    }

    private AnimalSynchronizationManager() {
    }

    public HashSet<Short> getDeleted() {
        return deletedToSend;
    }

    public void setExtraUpdate(UdpConnection connection) {
        if (connection != null) {
            extraUpdate.add(connection);
        }
    }

    public void setSendToClients(HashSet<Short> updated) {
        sendToClients.addAll(updated);
    }

    public void setSendToClients(Short updated) {
        sendToClients.add(updated);
    }

    public void setRequested(UdpConnection connection, HashSet<Short> request) {
        HashSet<Short> r = requests.computeIfAbsent(connection.getConnectedGUID(), k -> new HashSet<>());
        r.clear();
        r.addAll(request);
    }

    public void update() {
        deletedToSend.clear();
        synchronized (deletedByServer) {
            deletedToSend.addAll(deletedByServer);
            deletedByServer.clear();
        }

        synchronized (sendToClients) {
            sendToClients.removeAll(deletedToSend);
            receivedToSend.addAll(sendToClients);
            sendToClients.clear();
        }

        boolean isReliable = sendAsReliable.Check();
        if (isReliable) {
            sendAsReliable.Reset();
        }

        for (UdpConnection connection : GameServer.udpEngine.connections) {
            if (connection != null && connection.isFullyConnected()) {
                this.sendUpdateToClient(connection, isReliable);
            }
        }

        receivedToSend.clear();
    }

    private void sendUpdateToClient(UdpConnection connection, boolean isReliable) {
        PacketTypes.PacketType packetType;
        AnimalUpdatePacket packet;
        if (isReliable) {
            packetType = PacketTypes.PacketType.AnimalUpdateReliable;
            packet = (AnimalUpdateReliablePacket)connection.getPacket(packetType);
        } else {
            packetType = PacketTypes.PacketType.AnimalUpdateUnreliable;
            packet = (AnimalUpdateUnreliablePacket)connection.getPacket(packetType);
        }

        short animalsCount = 0;
        HashSet<Short> requested = packet.getRequested();
        requested.clear();
        requests.computeIfAbsent(connection.getConnectedGUID(), k -> new HashSet<>());

        for (short onlineID : requests.get(connection.getConnectedGUID())) {
            if (animalsCount >= 150) {
                break;
            }

            requested.add(onlineID);
            animalsCount++;
        }

        HashSet<Short> updated = packet.getUpdated();
        HashSet<Short> pending = packet.getPending();
        updated.clear();
        pending.clear();

        for (short onlineID : receivedToSend) {
            IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
            boolean isExtraUpdateNeeded = extraUpdate.contains(connection);
            if (animal != null && connection.RelevantTo(animal.getX(), animal.getY(), (connection.releventRange - 2) * 10)) {
                long updateRate = getUpdateRateForConnection(connection, animal);
                connection.timerUpdateAnimal.computeIfAbsent(animal.getOnlineID(), key -> new UpdateTimer(updateRate));
                if (isExtraUpdateNeeded || connection.timerUpdateAnimal.get(animal.getOnlineID()).check()) {
                    if (animalsCount >= 150) {
                        pending.add(animal.getOnlineID());
                        continue;
                    }

                    connection.timerUpdateAnimal.get(animal.getOnlineID()).reset(updateRate);
                    animal.getNetworkCharacterAI().setAnimalPacket(connection);
                    updated.add(onlineID);
                    animalsCount++;
                }

                extraUpdate.remove(connection);
            }
        }

        HashSet<Short> deleted = packet.getDeleted();
        deleted.clear();
        deleted.addAll(deletedToSend);
        if (!updated.isEmpty() || !requested.isEmpty() || !deleted.isEmpty()) {
            packet.sendToClient(packetType, connection);
            requests.computeIfAbsent(connection.getConnectedGUID(), k -> new HashSet<>()).clear();
        }

        if (!pending.isEmpty()) {
            receivedToSend.clear();
            receivedToSend.addAll(pending);
            this.sendUpdateToClient(connection, isReliable);
        }
    }

    public void delete(short onlineID) {
        deletedByServer.add(onlineID);
    }

    public void sendRequestToServer(UdpConnection connection) {
        PacketTypes.PacketType packetType = PacketTypes.PacketType.AnimalUpdateReliable;
        AnimalUpdatePacket packet = (AnimalUpdateReliablePacket)connection.getPacket(packetType);
        HashSet<Short> requested = packet.getRequested();
        requested.clear();
        requested.addAll(requests.computeIfAbsent(connection.getConnectedGUID(), k -> new HashSet<>()));
        if (!requested.isEmpty()) {
            packet.sendToServer(packetType);
            requests.clear();
        }
    }

    private static long getUpdateRateForConnection(UdpConnection connection, IsoAnimal animal) {
        IsoPlayer player = GameServer.getAnyPlayerFromConnection(connection);
        if (player != null) {
            float distance = IsoUtils.DistanceManhatten(player.getX(), player.getY(), animal.getX(), animal.getY());
            if (distance <= (connection.releventRange - 2) * 10 / 2.0F) {
                return -100L;
            }
        }

        return 100L;
    }
}
