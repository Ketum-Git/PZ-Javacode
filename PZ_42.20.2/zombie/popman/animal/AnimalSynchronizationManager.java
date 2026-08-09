// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman.animal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.raknet.UdpConnection;
import zombie.core.utils.UpdateLimit;
import zombie.iso.IsoUtils;
import zombie.network.GameServer;
import zombie.network.IConnection;
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
    private static final HashMap<UdpConnection, HashSet<Short>> extraUpdate = new HashMap<>();
    private static final UpdateLimit sendAsReliable = new UpdateLimit(2000L);
    private static final short SHORT_DISTANCE_ANIMAL_UPDATE_RATE_MS = 800;
    private static final short LONG_DISTANCE_ANIMAL_UPDATE_RATE_MS = 1000;
    private static final short MAX_ANIMALS_PER_PACKET = 150;

    public static AnimalSynchronizationManager getInstance() {
        return instance;
    }

    private AnimalSynchronizationManager() {
    }

    public HashSet<Short> getDeleted() {
        return deletedToSend;
    }

    public void setExtraUpdate(UdpConnection connection, short onlineId) {
        if (connection != null) {
            Set<Short> extraUpdates = extraUpdate.computeIfAbsent(connection, k -> new HashSet<>());
            extraUpdates.add(onlineId);
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
                this.sendUpdateToClient(connection, isReliable, receivedToSend);
            }
        }

        receivedToSend.clear();
    }

    private void sendUpdateToClient(UdpConnection connection, boolean isReliable, HashSet<Short> toSendList) {
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

        for (short onlineID : toSendList) {
            IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
            if (animal != null && connection.RelevantTo(animal.getX(), animal.getY(), (connection.getRelevantRange() - 2) * 10)) {
                boolean isAnimalOnScreen = isAnimalOnScreen(connection, animal);
                long updateRate = isAnimalOnScreen ? 800L : 1000L;
                connection.timerUpdateAnimal.computeIfAbsent(animal.getOnlineID(), key -> {
                    UpdateLimit updateLimit = new UpdateLimit(updateRate);
                    updateLimit.Reset();
                    return updateLimit;
                });
                Set<Short> extraUpdates = extraUpdate.computeIfAbsent(connection, k -> new HashSet<>());
                if (animal.getNetworkCharacterAI().isAnimalNeedExtraUpdate(connection, isAnimalOnScreen)) {
                    extraUpdates.add(onlineID);
                }

                boolean isExtraUpdateNeeded = extraUpdates.contains(onlineID);
                if (isExtraUpdateNeeded || connection.timerUpdateAnimal.get(animal.getOnlineID()).Check()) {
                    if (animalsCount >= 150) {
                        pending.add(animal.getOnlineID());
                        continue;
                    }

                    connection.timerUpdateAnimal.get(animal.getOnlineID()).Reset(updateRate);
                    animal.getNetworkCharacterAI().setAnimalPacket(connection);
                    updated.add(onlineID);
                    animalsCount++;
                }

                extraUpdates.remove(onlineID);
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
            this.sendUpdateToClient(connection, isReliable, pending);
        }
    }

    public void delete(short onlineID) {
        deletedByServer.add(onlineID);
    }

    public void sendRequestToServer(IConnection connection) {
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

    private static boolean isAnimalOnScreen(UdpConnection connection, IsoAnimal animal) {
        for (int playerIndex = 0; playerIndex < 4; playerIndex++) {
            if (connection.getPlayerAt(playerIndex) != null) {
                IsoPlayer player = connection.getPlayerAt(playerIndex);
                if (player != null) {
                    float distance = IsoUtils.DistanceManhatten(player.getX(), player.getY(), animal.getX(), animal.getY());
                    if (distance <= (connection.getRelevantRange() - 2) * 10 / 2.0F) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
