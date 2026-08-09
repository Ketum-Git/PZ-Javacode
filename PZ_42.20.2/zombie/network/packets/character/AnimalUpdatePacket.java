// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.io.IOException;
import java.util.HashSet;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.IConnection;
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
    protected final HashSet<Short> pending = new HashSet<>();

    public HashSet<Short> getRequested() {
        return this.requested;
    }

    public HashSet<Short> getUpdated() {
        return this.updated;
    }

    public HashSet<Short> getDeleted() {
        return this.deleted;
    }

    public HashSet<Short> getPending() {
        return this.pending;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.deleted.size());

        for (short onlineID : this.deleted) {
            b.putShort(onlineID);
        }

        int count = 0;
        int countPosition = b.position();
        b.putInt(this.requested.size());
        int endPosition = b.position();

        try {
            for (short onlineID : this.requested) {
                if (GameServer.server) {
                    IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
                    if (animal != null) {
                        b.putShort(onlineID);
                        int sizePos = b.position();
                        int dataSize = 0;
                        b.putInt(dataSize);
                        DebugType.Animal.noise("Send animal response id=%d type=%s", onlineID, animal.getNetworkCharacterAI().getAnimalPacket().type);
                        animal.getNetworkCharacterAI().getAnimalPacket().flags = 6;
                        animal.getNetworkCharacterAI().getAnimalPacket().write(b);
                        int startPosition = b.position();
                        animal.save(b.bb, false, false);
                        endPosition = b.position();
                        dataSize = endPosition - startPosition;
                        b.position(sizePos);
                        b.putInt(dataSize);
                        b.position(endPosition);
                        count++;
                    }
                } else if (GameClient.client) {
                    b.putShort(onlineID);
                    DebugType.Animal.noise("Send animal request id=%d", onlineID);
                    count++;
                }
            }
        } catch (IOException var11) {
            DebugType.Animal.printException(var11, "Can't save animals", LogSeverity.Error);
            b.position(endPosition);
            count = 0;
        }

        endPosition = b.position();
        b.position(countPosition);
        b.putInt(count);
        b.position(endPosition);
        if (GameServer.server) {
            count = 0;
            countPosition = b.position();
            b.putInt(this.updated.size());

            for (short onlineIDx : this.updated) {
                IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineIDx);
                if (animal != null) {
                    b.putShort(onlineIDx);
                    animal.getNetworkCharacterAI().getAnimalPacket().write(b);
                    count++;
                }
            }

            endPosition = b.position();
            b.position(countPosition);
            b.putInt(count);
            b.position(endPosition);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
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

                animal.remove();
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
                                    animal.setDirectionAngle(packet.prediction.direction * (180.0F / (float)Math.PI));
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
                                    DebugType.Animal.debugln("BAD animal response creation id=%d", onlineID);
                            }

                            AnimalInstanceManager.getInstance().add(animal, onlineID);
                            animal.setVariable("bPathfind", false);
                        }

                        animal.load(b.bb, IsoWorld.getWorldVersion(), false);
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
                                DebugType.Animal.debugln("BAD animal response id=%d", onlineID);
                        }

                        animal.getNetworkCharacterAI().parse(packet);
                        DebugType.Animal
                            .noise(
                                "Receive animal response id=%d created in %s",
                                onlineID,
                                animal.hutch == null ? "word" : (animal.nestBox == -1 ? "hutch" : "nest box")
                            );
                    }
                } catch (IOException var11) {
                    DebugType.Animal.printException(var11, String.format("Can't load animal id=%d", onlineID), LogSeverity.Error);
                }
            } else if (GameServer.server) {
                this.requested.add(onlineID);
            }
        }

        if (GameClient.client) {
            deletedCount = b.getInt();

            for (int ixx = 0; ixx < deletedCount; ixx++) {
                short onlineID = b.getShort();
                IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
                if (animal != null) {
                    AnimalPacket packet = animal.getNetworkCharacterAI().getAnimalPacket();
                    packet.parse(b, connection);
                    animal.getNetworkCharacterAI().parse(packet);
                } else {
                    AnimalPacket packet = (AnimalPacket)connection.getPacket(PacketTypes.PacketType.AnimalPacket);
                    packet.parse(b, connection);
                    this.requested.add(onlineID);
                }
            }

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
