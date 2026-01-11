// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoDirections;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.popman.animal.AnimalSynchronizationManager;

@PacketSetting(ordering = 0, priority = 2, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class AnimalUpdatePacket implements INetworkPacket {
    protected final HashSet<Short> requested = new HashSet<>();
    protected final HashSet<Short> updated = new HashSet<>();
    protected final HashSet<Short> deleted = new HashSet<>();

    public HashSet<Short> getRequested() {
        return this.requested;
    }

    public HashSet<Short> getUpdated() {
        return this.updated;
    }

    public HashSet<Short> getDeleted() {
        return this.deleted;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.deleted.size());

        for (short onlineID : this.deleted) {
            b.putShort(onlineID);
        }

        int count = 0;
        int countPosition = b.bb.position();
        b.putInt(this.requested.size());
        int endPosition = b.bb.position();

        try {
            for (short onlineID : this.requested) {
                if (GameServer.server) {
                    IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
                    if (animal != null) {
                        b.putShort(onlineID);
                        int sizePos = b.bb.position();
                        int dataSize = 0;
                        b.putInt(dataSize);
                        DebugLog.Animal.noise("Send animal response id=%d type=%s", onlineID, animal.getNetworkCharacterAI().getAnimalPacket().type);
                        animal.getNetworkCharacterAI().getAnimalPacket().flags = 6;
                        animal.getNetworkCharacterAI().getAnimalPacket().write(b);
                        int startPosition = b.bb.position();
                        animal.save(b.bb, false, false);
                        endPosition = b.bb.position();
                        dataSize = endPosition - startPosition;
                        b.bb.position(sizePos);
                        b.putInt(dataSize);
                        b.bb.position(endPosition);
                        count++;
                    }
                } else if (GameClient.client) {
                    b.putShort(onlineID);
                    DebugLog.Animal.noise("Send animal request id=%d", onlineID);
                    count++;
                }
            }
        } catch (IOException var11) {
            DebugLog.Animal.printException(var11, "Can't save animals", LogSeverity.Error);
            b.bb.position(endPosition);
            count = 0;
        }

        endPosition = b.bb.position();
        b.bb.position(countPosition);
        b.bb.putInt(count);
        b.bb.position(endPosition);
        count = 0;
        countPosition = b.bb.position();
        b.putInt(this.updated.size());

        for (short onlineIDx : this.updated) {
            IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineIDx);
            if (animal != null) {
                b.putShort(onlineIDx);
                animal.getNetworkCharacterAI().getAnimalPacket().write(b);
                count++;
            }
        }

        endPosition = b.bb.position();
        b.bb.position(countPosition);
        b.bb.putInt(count);
        b.bb.position(endPosition);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.deleted.clear();
        int deletedCount = b.getInt();

        for (int i = 0; i < deletedCount; i++) {
            short onlineID = b.getShort();
            IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
            if (animal != null) {
                if (animal.getHutch() != null) {
                    if (animal.getData().getHutchPosition() != -1) {
                        animal.getHutch().animalInside.remove(animal.getData().getHutchPosition());
                    } else if (animal.getNestBoxIndex() != -1) {
                        animal.getHutch().getNestBox(animal.getNestBoxIndex()).animal = null;
                    }
                }

                if (!animal.networkAi.getAnimalPacket().isDead()) {
                    animal.remove();
                }
            }
        }

        this.requested.clear();
        int requestedCount = b.getInt();

        for (int ix = 0; ix < requestedCount; ix++) {
            short onlineID = b.getShort();
            if (GameClient.client) {
                try {
                    AnimalPacket packet = (AnimalPacket)connection.getPacket(PacketTypes.PacketType.AnimalPacket);
                    int dataSize = b.getInt();
                    packet.parse(b, connection);
                    if (!packet.isConsistent(connection)) {
                        b.position(b.position() + dataSize);
                    } else {
                        IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
                        if (animal == null) {
                            switch (packet.location) {
                                case 0:
                                case 4:
                                    animal = new IsoAnimal(
                                        IsoWorld.instance.getCell(), packet.squareX, packet.squareY, packet.squareZ, packet.type, packet.breed
                                    );
                                    animal.setDir(IsoDirections.fromAngle(packet.prediction.direction));
                                    animal.getForwardDirection().setDirection(packet.prediction.direction);
                                    break;
                                case 1:
                                case 2:
                                case 3:
                                    animal = new IsoAnimal(IsoWorld.instance.getCell(), 0, 0, 0, packet.type, packet.breed);
                                    animal.setX(packet.squareX);
                                    animal.setY(packet.squareY);
                                    animal.setZ(packet.squareZ);
                                    break;
                                default:
                                    DebugLog.Animal.debugln("BAD animal response creation id=%d", onlineID);
                            }

                            AnimalInstanceManager.getInstance().add(animal, onlineID);
                            animal.setVariable("bPathfind", false);
                        }

                        animal.load(b, IsoWorld.getWorldVersion(), false);
                        switch (packet.location) {
                            case 0:
                                animal.addToWorld();
                                break;
                            case 1:
                                IsoHutch hutch = IsoHutch.getHutch(
                                    PZMath.fastfloor(packet.prediction.x), PZMath.fastfloor(packet.prediction.y), PZMath.fastfloor((float)packet.prediction.z)
                                );
                                if (hutch != null) {
                                    if (packet.hutchPosition != -1) {
                                        animal.getData().setPreferredHutchPosition(packet.hutchPosition);
                                        hutch.animalInside.put(Integer.valueOf(packet.hutchPosition), null);
                                        hutch.addAnimalInside(animal, false);
                                    } else {
                                        hutch.getNestBox(Integer.valueOf(packet.hutchNestBox)).animal = animal;
                                        animal.hutch = hutch;
                                        animal.getData().setHutchPosition(-1);
                                        animal.nestBox = packet.hutchNestBox;
                                    }
                                }
                                break;
                            case 2:
                                if (packet.vehicleId.getVehicle() != null && !packet.vehicleId.getVehicle().animals.contains(animal)) {
                                    packet.vehicleId.getVehicle().addAnimalInTrailer(animal);
                                }
                                break;
                            case 3:
                                animal.removeFromWorld();
                                animal.removeFromSquare();
                                break;
                            default:
                                DebugLog.Animal.debugln("BAD animal response id=%d", onlineID);
                        }

                        animal.networkAi.parse(packet);
                        DebugLog.Animal
                            .noise(
                                "Receive animal response id=%d created in %s",
                                onlineID,
                                animal.hutch == null ? "word" : (animal.nestBox == -1 ? "hutch" : "nest box")
                            );
                    }
                } catch (IOException var11) {
                    DebugLog.Animal.printException(var11, String.format("Can't load animal id=%d", onlineID), LogSeverity.Error);
                }
            } else if (GameServer.server) {
                this.requested.add(onlineID);
            }
        }

        deletedCount = b.getInt();

        for (int ixx = 0; ixx < deletedCount; ixx++) {
            short onlineID = b.getShort();
            IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
            if (animal != null) {
                AnimalPacket packet = animal.getNetworkCharacterAI().getAnimalPacket();
                packet.parse(b, connection);
                animal.networkAi.parse(packet);
            } else {
                AnimalPacket packet = (AnimalPacket)connection.getPacket(PacketTypes.PacketType.AnimalPacket);
                packet.parse(b, connection);
                if (GameClient.client) {
                    this.requested.add(onlineID);
                }
            }
        }

        if (GameClient.client) {
            AnimalSynchronizationManager.getInstance().sendRequestToServer(connection);
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        AnimalSynchronizationManager.getInstance().setRequested(connection, this.requested);
        AnimalSynchronizationManager.getInstance().setSendToClients(this.updated);
    }

    @Override
    public void processClient(UdpConnection connection) {
        AnimalSynchronizationManager.getInstance().setRequested(connection, this.requested);
    }
}
