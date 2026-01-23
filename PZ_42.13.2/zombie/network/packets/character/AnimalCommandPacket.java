// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import zombie.Lua.LuaManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.AnimalGene;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.AnimalInventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.Vector2;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoHutch;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.NetObject;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalInstanceManager;
import zombie.vehicles.BaseVehicle;

@PacketSetting(ordering = 1, priority = 1, reliability = 3, requiredCapability = Capability.AnimalCheats, handlingType = 3)
public class AnimalCommandPacket implements INetworkPacket {
    @JSONField
    public AnimalCommandPacket.Type type = AnimalCommandPacket.Type.None;
    @JSONField
    public byte flags;
    @JSONField
    public AnimalID animalId = new AnimalID();
    @JSONField
    public PlayerID playerId = new PlayerID();
    @JSONField
    public VehicleID vehicleId = new VehicleID();
    @JSONField
    public NetObject objectId = new NetObject();
    @JSONField
    protected final ObjectID bodyId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    @JSONField
    public int x;
    @JSONField
    public int y;
    @JSONField
    public int z;
    public InventoryItem item;

    @Override
    public void setData(Object... values) {
        if (values.length == 2) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1]);
        } else if (values[1] instanceof IsoDeadBody) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoDeadBody)values[1], (InventoryItem)values[2]);
        } else if (values[2] instanceof IsoGridSquare) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1], (IsoGridSquare)values[2]);
        } else if (values[3] instanceof InventoryItem) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1], (IsoPlayer)values[2], (InventoryItem)values[3]);
        } else if (values[3] instanceof IsoHutch && values.length == 4) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1], (IsoPlayer)values[2], (IsoHutch)values[3]);
        } else if (values[3] instanceof IsoHutch && values.length == 5) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1], (IsoPlayer)values[2], (IsoHutch)values[3], (InventoryItem)values[4]);
        } else if (values[3] instanceof BaseVehicle) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1], (IsoPlayer)values[2], (BaseVehicle)values[3], (InventoryItem)values[4]);
        } else if (values[3] instanceof IsoObject && values[4] instanceof InventoryItem) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1], (IsoPlayer)values[2], (IsoObject)values[3], (InventoryItem)values[4]);
        } else if (values[0] == AnimalCommandPacket.Type.AttachAnimalToPlayer) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1], (IsoPlayer)values[2], (IsoObject)values[3], (Boolean)values[4]);
        } else if (values[0] == AnimalCommandPacket.Type.AttachAnimalToTree) {
            this.set((AnimalCommandPacket.Type)values[0], (IsoAnimal)values[1], (IsoObject)values[2], (Boolean)values[3]);
        } else {
            DebugLog.Animal.error("No such packet set: %s", values[0]);
        }
    }

    public void set(AnimalCommandPacket.Type operation, IsoAnimal animal) {
        this.type = operation;
        this.animalId.set(animal);
    }

    public void set(AnimalCommandPacket.Type operation, IsoAnimal animal, IsoGridSquare sq) {
        this.type = operation;
        this.animalId.set(animal);
        this.x = sq.getX();
        this.y = sq.getY();
        this.z = sq.getZ();
    }

    public void set(AnimalCommandPacket.Type operation, IsoAnimal animal, IsoPlayer player, BaseVehicle vehicle, InventoryItem item) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.vehicleId.set(vehicle);
        this.item = item;
    }

    public void set(AnimalCommandPacket.Type operation, IsoAnimal animal, IsoPlayer player, IsoObject object, boolean remove) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.objectId.setObject(object);
        this.flags = (byte)(remove ? 1 : 0);
    }

    public void set(AnimalCommandPacket.Type operation, IsoAnimal animal, IsoObject object, boolean remove) {
        this.type = operation;
        this.animalId.set(animal);
        this.objectId.setObject(object);
        this.flags = (byte)(remove ? 1 : 0);
    }

    public void set(AnimalCommandPacket.Type operation, IsoAnimal animal, IsoPlayer player, InventoryItem item) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.item = item;
    }

    public void set(AnimalCommandPacket.Type operation, IsoDeadBody body, InventoryItem item) {
        this.type = operation;
        this.bodyId.set(body.getObjectID());
        this.item = item;
    }

    public void set(AnimalCommandPacket.Type operation, IsoAnimal animal, IsoPlayer player, IsoObject object, InventoryItem item) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.objectId.setObject(object);
        this.item = item;
    }

    public void set(AnimalCommandPacket.Type operation, IsoAnimal animal, IsoPlayer player, IsoHutch hutch) {
        this.type = operation;
        this.animalId.set(animal);
        this.playerId.set(player);
        this.objectId.setObject(hutch);
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte((byte)this.type.ordinal());
        b.putByte(this.flags);
        this.animalId.write(b);
        if (AnimalCommandPacket.Type.UpdateGenome == this.type) {
            Collection<AnimalGene> genes = this.animalId.getAnimal().getFullGenome().values();
            int sizePos = b.bb.position();
            b.putInt(genes.size());

            try {
                for (AnimalGene gene : genes) {
                    gene.save(b.bb, false);
                }
            } catch (IOException var7) {
                DebugLog.Multiplayer.printException(var7, "Animal genome write failed", LogSeverity.Error);
                b.bb.position(sizePos);
                b.putInt(0);
            }
        } else {
            this.playerId.write(b);
            this.vehicleId.write(b);
            this.objectId.write(b);
            this.bodyId.write(b);
            b.putInt(this.x);
            b.putInt(this.y);
            b.putInt(this.z);
            if (this.item != null) {
                b.putInt(this.item.getID());

                try {
                    this.item.saveWithSize(b.bb, false);
                } catch (Exception var6) {
                    DebugLog.Multiplayer.printException(var6, "AnimalInventoryItem save failed", LogSeverity.Error);
                }
            } else {
                b.putInt(-1);
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        byte operation = b.get();
        if (operation > AnimalCommandPacket.Type.None.ordinal() && operation <= AnimalCommandPacket.Type.UpdateGenome.ordinal()) {
            this.type = AnimalCommandPacket.Type.values()[operation];
        }

        this.flags = b.get();
        this.animalId.parse(b, connection);
        if (AnimalCommandPacket.Type.UpdateGenome == this.type) {
            int size = b.getInt();

            try {
                for (int i = 0; i < size; i++) {
                    AnimalGene gene = new AnimalGene();
                    gene.load(b, IsoWorld.getWorldVersion(), false);
                    this.animalId.getAnimal().getFullGenome().put(gene.name, gene);
                }
            } catch (IOException var8) {
                DebugLog.Multiplayer.printException(var8, "Animal genome read failed", LogSeverity.Error);
                return;
            }
        } else {
            this.playerId.parse(b, connection);
            this.vehicleId.parse(b, connection);
            this.objectId.parse(b, connection);
            this.bodyId.parse(b, connection);
            this.x = b.getInt();
            this.y = b.getInt();
            this.z = b.getInt();
            int itemID = b.getInt();
            if (itemID != -1) {
                try {
                    this.item = zombie.util.Type.tryCastTo(InventoryItem.loadItem(b, IsoWorld.getWorldVersion()), InventoryItem.class);
                } catch (Exception var7) {
                    DebugLog.Multiplayer.printException(var7, "AnimalInventoryItem load failed", LogSeverity.Error);
                }
            }
        }
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection) && AnimalCommandPacket.Type.UpdateGenome == this.type) {
            for (UdpConnection c : GameServer.udpEngine.connections) {
                if (connection.getConnectedGUID() != c.getConnectedGUID()
                    && c.isFullyConnected()
                    && c.RelevantTo(this.animalId.getAnimal().getX(), this.animalId.getAnimal().getY())) {
                    ByteBufferWriter bbw = c.startPacket();
                    packetType.doPacket(bbw);
                    this.write(bbw);
                    packetType.send(c);
                }
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.isConsistent(connection)) {
            switch (this.type) {
                case None:
                default:
                    DebugLog.Animal.warn("AnimalPacket \"%s\" is not supported", this.type);
                    break;
                case AddAnimalFromHandsInTrailer:
                    if (this.animalId.isConsistent(connection) && this.playerId.isConsistent(connection) && this.vehicleId.isConsistent(connection)) {
                        this.vehicleId.getVehicle().animals.add(this.animalId.getAnimal());
                        this.animalId.getAnimal().setVehicle(this.vehicleId.getVehicle());
                        this.vehicleId
                            .getVehicle()
                            .setCurrentTotalAnimalSize(
                                this.vehicleId.getVehicle().getCurrentTotalAnimalSize() + this.animalId.getAnimal().getAnimalTrailerSize()
                            );
                        AnimalInventoryItem item = this.playerId.getPlayer().getInventory().getAnimalInventoryItem(this.animalId.getAnimal());
                        this.animalId.getAnimal().itemId = 0;
                        if (item != null) {
                            this.playerId.getPlayer().getInventory().Remove(item);
                        } else {
                            DebugLog.Animal.error("Animal not found");
                        }
                    }
                    break;
                case AddAnimalInTrailer:
                    if (this.animalId.isConsistent(connection)
                        && this.vehicleId.isConsistent(connection)
                        && !this.vehicleId.getVehicle().animals.contains(this.animalId.getAnimal())) {
                        this.vehicleId.getVehicle().animals.add(this.animalId.getAnimal());
                        if (this.animalId.getAnimal().mother != null) {
                            this.animalId.getAnimal().attachBackToMother = this.animalId.getAnimal().mother.animalId;
                        }

                        this.animalId.getAnimal().setVehicle(this.vehicleId.getVehicle());
                        if (this.animalId.getAnimal().getData().getAttachedPlayer() != null) {
                            this.animalId.getAnimal().getData().getAttachedPlayer().removeAttachedAnimal(this.animalId.getAnimal());
                            this.animalId.getAnimal().getData().setAttachedPlayer(null);
                        }

                        this.animalId.getAnimal().removeFromWorld();
                        this.animalId.getAnimal().removeFromSquare();
                        this.vehicleId
                            .getVehicle()
                            .setCurrentTotalAnimalSize(
                                this.vehicleId.getVehicle().getCurrentTotalAnimalSize() + this.animalId.getAnimal().getAnimalTrailerSize()
                            );
                    }
                    break;
                case RemoveAnimalFromTrailer:
                    if (this.animalId.isConsistent(connection)
                        && this.playerId.isConsistent(connection)
                        && this.vehicleId.isConsistent(connection)
                        && this.vehicleId.getVehicle().animals.remove(this.animalId.getAnimal())) {
                        Vector2 vec = this.vehicleId.getVehicle().getAreaCenter("AnimalEntry");
                        IsoAnimal newAnimal = new IsoAnimal(
                            this.vehicleId.getVehicle().getSquare().getCell(),
                            PZMath.fastfloor(vec.x),
                            PZMath.fastfloor(vec.y),
                            this.vehicleId.getVehicle().getSquare().z,
                            this.animalId.getAnimal().getAnimalType(),
                            this.animalId.getAnimal().getBreed()
                        );
                        newAnimal.copyFrom(this.animalId.getAnimal());
                        newAnimal.attachBackToMotherTimer = 10000.0F;
                        newAnimal.itemId = 0;
                        this.vehicleId
                            .getVehicle()
                            .setCurrentTotalAnimalSize(
                                this.vehicleId.getVehicle().getCurrentTotalAnimalSize() - this.animalId.getAnimal().getAnimalTrailerSize()
                            );
                        AnimalInstanceManager.getInstance().add(newAnimal, this.animalId.getID());
                        newAnimal.addToWorld();
                    }
                    break;
                case RemoveAndGrabAnimalFromTrailer:
                    if (this.animalId.isConsistent(connection)
                        && this.playerId.isConsistent(connection)
                        && this.vehicleId.isConsistent(connection)
                        && this.vehicleId.getVehicle().animals.remove(this.animalId.getAnimal())) {
                        this.vehicleId
                            .getVehicle()
                            .setCurrentTotalAnimalSize(
                                this.vehicleId.getVehicle().getCurrentTotalAnimalSize() - this.animalId.getAnimal().getAnimalTrailerSize()
                            );
                        AnimalInstanceManager.getInstance().remove(this.animalId.getAnimal());
                        if (this.item instanceof AnimalInventoryItem animal) {
                            AnimalInstanceManager.getInstance().add(animal.getAnimal(), this.animalId.getID());
                            this.playerId.getPlayer().getInventory().AddItem(animal);
                            this.playerId.getPlayer().setPrimaryHandItem(animal);
                            this.playerId.getPlayer().setSecondaryHandItem(animal);
                            this.animalId.getAnimal().itemId = this.item.id;
                        } else {
                            DebugLog.Animal.warn("AnimalInventoryItem is null");
                        }
                    }
                    break;
                case AttachAnimalToPlayer:
                    if (this.animalId.isConsistent(connection) && this.playerId.isConsistent(connection)) {
                        if ((this.flags & 1) != 0) {
                            this.playerId.getPlayer().getAttachedAnimals().remove(this.animalId.getAnimal());
                            this.animalId.getAnimal().getData().setAttachedPlayer(null);
                        } else {
                            this.playerId.getPlayer().getAttachedAnimals().add(this.animalId.getAnimal());
                            this.animalId.getAnimal().getData().setAttachedTree(null);
                            this.animalId.getAnimal().getData().setAttachedPlayer(this.playerId.getPlayer());
                        }
                    }
                    break;
                case AttachAnimalToTree:
                    if (this.animalId.isConsistent(connection) && this.objectId.isConsistent(connection)) {
                        if ((this.flags & 1) != 0) {
                            this.animalId.getAnimal().getData().setAttachedTree(null);
                        } else {
                            this.animalId.getAnimal().getData().setAttachedTree(this.objectId.getObject());
                            this.playerId.getPlayer().removeAttachedAnimal(this.animalId.getAnimal());
                            this.animalId.getAnimal().getData().setAttachedPlayer(null);
                        }
                    }
                    break;
                case PickupAnimal:
                    if (this.animalId.isConsistent(connection) && this.playerId.isConsistent(connection)) {
                        if (this.item instanceof AnimalInventoryItem animal) {
                            AnimalInstanceManager.getInstance().add(animal.getAnimal(), this.animalId.getID());
                            this.playerId.getPlayer().getInventory().AddItem(animal);
                            this.playerId.getPlayer().getAttachedAnimals().remove(this.animalId.getAnimal());
                            this.animalId.getAnimal().getData().setAttachedPlayer(null);
                            this.animalId.getAnimal().removeFromWorld();
                            this.animalId.getAnimal().removeFromSquare();
                            this.animalId.getAnimal().itemId = this.item.id;
                            this.animalId.getAnimal().playBreedSound("pick_up");
                        } else {
                            DebugLog.Animal.warn("AnimalInventoryItem is null");
                        }
                    }
                    break;
                case ButcherAnimal:
                    IsoDeadBody deadBody = (IsoDeadBody)this.bodyId.getObject();
                    if (deadBody != null && this.playerId.isConsistent(connection)) {
                        Object functionObj = LuaManager.getFunctionObject("AnimalPartsDefinitions.butcherAnimalFromGround");
                        if (functionObj != null) {
                            LuaManager.caller.protectedCallVoid(LuaManager.thread, functionObj, deadBody, this.playerId.getPlayer());
                        }
                    } else {
                        DebugLog.Animal.warn("IsoDeadBody is null");
                    }
                    break;
                case FeedAnimalFromHand:
                    if (this.animalId.isConsistent(connection) && this.playerId.isConsistent(connection)) {
                        InventoryItem var17 = this.item;
                        if (var17 instanceof InventoryItem) {
                            this.animalId.getAnimal().feedFromHand(this.playerId.getPlayer(), var17);
                        } else {
                            DebugLog.Animal.warn("InventoryItem is null");
                        }
                    }
                    break;
                case HutchGrabAnimal:
                case HutchGrabCorpseAction:
                    if (this.animalId.isConsistent(connection) && this.playerId.isConsistent(connection) && this.objectId.isConsistent(connection)) {
                        InventoryItem var15 = this.item;
                        if (var15 instanceof InventoryItem) {
                            if (this.objectId.getObject() instanceof IsoHutch hutch) {
                                hutch.removeAnimal(this.animalId.getAnimal());
                            } else {
                                DebugLog.Animal.warn("IsoHutch is null");
                            }
                        } else {
                            DebugLog.Animal.warn("InventoryItem is null");
                        }
                    }
                    break;
                case DropAnimal:
                    if (this.animalId.isConsistent(connection)) {
                        IsoAnimal newAnimal = new IsoAnimal(
                            IsoWorld.instance.getCell(),
                            this.x,
                            this.y,
                            this.z,
                            this.animalId.getAnimal().getAnimalType(),
                            this.animalId.getAnimal().getBreed()
                        );
                        newAnimal.copyFrom(this.animalId.getAnimal());
                        newAnimal.itemId = 0;
                        newAnimal.attachBackToMotherTimer = 10000.0F;
                        AnimalInstanceManager.getInstance().add(newAnimal, this.animalId.getID());
                        newAnimal.addToWorld();
                        newAnimal.playBreedSound("put_down");
                    }
                    break;
                case HutchRemoveAnimal:
                    if (this.animalId.isConsistent(connection) && this.playerId.isConsistent(connection) && this.objectId.isConsistent(connection)) {
                        if (this.objectId.getObject() instanceof IsoHutch hutch) {
                            hutch.removeAnimal(this.animalId.getAnimal());
                        } else {
                            DebugLog.Animal.warn("IsoHutch is null");
                        }
                    }
                case UpdateGenome:
            }
        }
    }

    public static enum Type {
        None,
        AddAnimalFromHandsInTrailer,
        AddAnimalInTrailer,
        RemoveAnimalFromTrailer,
        RemoveAndGrabAnimalFromTrailer,
        AttachAnimalToPlayer,
        AttachAnimalToTree,
        PickupAnimal,
        ButcherAnimal,
        FeedAnimalFromHand,
        HutchGrabAnimal,
        HutchGrabCorpseAction,
        DropAnimal,
        HutchRemoveAnimal,
        UpdateGenome;
    }
}
