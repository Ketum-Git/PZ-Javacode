// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.fields.character.AnimalID;
import zombie.network.packets.INetworkPacket;

public class AnimalEventPacket implements INetworkPacket {
    @JSONField
    private byte eventId;
    @JSONField
    private final AnimalID animalId = new AnimalID();
    @JSONField
    private short alertedId;
    @JSONField
    public float x;
    @JSONField
    public float y;
    @JSONField
    public float z;
    @JSONField
    private String param;
    @JSONField
    private boolean isAlerted;
    private AnimalEventPacket.EventType event;

    @Override
    public void setData(Object... values) {
        if (values.length == 3 && values[0] instanceof IsoAnimal && values[1] instanceof Boolean) {
            this.eventId = (byte)AnimalEventPacket.EventType.EventAlerted.ordinal();
            this.animalId.set((IsoAnimal)values[0]);
            this.isAlerted = (Boolean)values[1];
            if (values[2] instanceof IsoPlayer) {
                this.alertedId = ((IsoPlayer)values[2]).getOnlineID();
            } else {
                this.alertedId = -1;
            }
        } else if (values.length == 2 && values[0] instanceof IsoAnimal && values[1] instanceof IsoGridSquare) {
            this.eventId = (byte)AnimalEventPacket.EventType.EventEating.ordinal();
            this.animalId.set((IsoAnimal)values[0]);
            this.x = ((IsoGridSquare)values[1]).getX();
            this.y = ((IsoGridSquare)values[1]).getY();
            this.z = ((IsoGridSquare)values[1]).getZ();
        } else if (values.length == 4
            && values[0] instanceof IsoAnimal
            && values[1] instanceof Float
            && values[2] instanceof Float
            && values[3] instanceof Float) {
            this.eventId = (byte)AnimalEventPacket.EventType.EventClimbFence.ordinal();
            this.animalId.set((IsoAnimal)values[0]);
            this.x = (Float)values[1];
            this.y = (Float)values[2];
            this.z = (Float)values[3];
        } else {
            DebugLog.Multiplayer.warn("%s.set get invalid arguments", this.getClass().getSimpleName());
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        boolean result = this.animalId.isConsistent(connection) && this.event != null;
        if (!result) {
            DebugLog.Multiplayer.debugln("[Event] is not consistent %s", this.getDescription());
        }

        return result;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.eventId = b.get();
        this.animalId.parse(b, connection);
        this.alertedId = b.getShort();
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.z = b.getFloat();
        this.param = GameWindow.ReadString(b);
        this.isAlerted = b.get() != 0;
        if (this.eventId >= 0 && this.eventId < AnimalEventPacket.EventType.values().length) {
            this.event = AnimalEventPacket.EventType.values()[this.eventId];
        } else {
            DebugLog.Multiplayer.warn("Unknown event=" + this.eventId);
            this.event = null;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.eventId);
        this.animalId.write(b);
        b.putShort(this.alertedId);
        b.putFloat(this.x);
        b.putFloat(this.y);
        b.putFloat(this.z);
        GameWindow.WriteString(b.bb, this.param);
        b.putBoolean(this.isAlerted);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection)) {
            if (AnimalEventPacket.EventType.EventEating == this.event) {
                IsoGridSquare isoGridSquare = IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
                if (isoGridSquare != null) {
                    isoGridSquare.removeGrass();
                }
            }

            for (UdpConnection c : GameServer.udpEngine.connections) {
                if ((connection.getConnectedGUID() != c.getConnectedGUID() || AnimalEventPacket.EventType.EventEating == this.event)
                    && c.isFullyConnected()
                    && this.animalId.getAnimal().checkCanSeeClient(c)
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
        this.process(connection);
    }

    public void process(UdpConnection connection) {
        if (this.isConsistent(connection)) {
            DebugLog.Multiplayer.debugln(this.getDescription());
            switch (this.event) {
                case EventAlerted:
                    this.animalId.getAnimal().setIsAlerted(this.isAlerted);
                    this.animalId.getAnimal().alertedChr = GameClient.IDToPlayerMap.get(this.alertedId);
                    break;
                case EventClimbFence:
                    IsoDirections assumedDirection = this.checkCurrentIsEventGridSquareFence(this.animalId.getAnimal());
                    if (assumedDirection != IsoDirections.Max) {
                        this.animalId.getAnimal().climbOverFence(assumedDirection);
                    }
                    break;
                case EventEating:
                    IsoGridSquare isoGridSquare = IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
                    if (isoGridSquare != null && isoGridSquare.getFloor() != null && isoGridSquare.checkHaveGrass()) {
                        if (isoGridSquare.getFloor().getAttachedAnimSprite() == null) {
                            isoGridSquare.getFloor().setAttachedAnimSprite(new ArrayList<>());
                        }

                        isoGridSquare.getFloor()
                            .getAttachedAnimSprite()
                            .add(IsoSpriteInstance.get(IsoSprite.getSprite(IsoSpriteManager.instance, "blends_natural_01_87", 0)));
                    }
            }
        }
    }

    private IsoDirections checkCurrentIsEventGridSquareFence(IsoGameCharacter character) {
        IsoGridSquare assumedGridSquare = character.getCell().getGridSquare((double)this.x, (double)this.y, (double)this.z);
        IsoGridSquare assumedGridSquareNextY = character.getCell().getGridSquare((double)this.x, (double)(this.y + 1.0F), (double)this.z);
        IsoGridSquare assumedGridSquareNextX = character.getCell().getGridSquare((double)(this.x + 1.0F), (double)this.y, (double)this.z);
        IsoDirections result;
        if (assumedGridSquare != null && assumedGridSquare.has(IsoFlagType.HoppableN)) {
            result = IsoDirections.N;
        } else if (assumedGridSquare != null && assumedGridSquare.has(IsoFlagType.HoppableW)) {
            result = IsoDirections.W;
        } else if (assumedGridSquareNextY != null && assumedGridSquareNextY.has(IsoFlagType.HoppableN)) {
            result = IsoDirections.S;
        } else if (assumedGridSquareNextX != null && assumedGridSquareNextX.has(IsoFlagType.HoppableW)) {
            result = IsoDirections.E;
        } else {
            result = IsoDirections.Max;
        }

        return result;
    }

    public static enum EventType {
        EventAlerted,
        EventClimbFence,
        EventEating,
        Unknown;
    }
}
