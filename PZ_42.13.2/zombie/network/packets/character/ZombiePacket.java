// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.NetworkVariables;
import zombie.network.fields.INetworkPacketField;
import zombie.network.fields.character.PFBData;
import zombie.network.fields.character.PlayerID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;

public class ZombiePacket implements INetworkPacketField {
    private static final int PACKET_SIZE_BYTES = 53;
    public short id;
    public byte update;
    public float x;
    public float y;
    public short health;
    public int outfitId;
    public NetworkVariables.ZombieState realState = NetworkVariables.ZombieState.Idle;
    public short target;
    public float targetAngle;
    public short timeSinceSeenFlesh;
    public short smParamTargetAngle;
    public short speedMod;
    public NetworkVariables.WalkType walkType = NetworkVariables.WalkType.WT1;
    public byte predictionType = 0;
    public float realX;
    public float realY;
    public byte realZ;
    public short booleanVariables;
    public final PFBData pfb = new PFBData();
    public final ObjectID reanimatedBodyId = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
    public byte z;
    public PlayerID grappledBy = new PlayerID();
    public String sharedGrappleType;

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.id = b.getShort();
        this.update = b.get();
        this.x = b.getFloat();
        this.y = b.getFloat();
        this.outfitId = b.getInt();
        this.health = b.getShort();
        if ((this.update & 8) != 0) {
            this.booleanVariables = b.getShort();
        } else {
            this.booleanVariables = 0;
        }

        if ((this.update & 64) != 0) {
            this.z = b.get();
        } else {
            this.z = 0;
        }

        if ((this.update & 2) != 0) {
            this.target = b.getShort();
            this.targetAngle = b.getFloat();
            this.timeSinceSeenFlesh = b.getShort();
            this.smParamTargetAngle = b.getShort();
        } else {
            this.target = -1;
            this.targetAngle = 0.0F;
            this.timeSinceSeenFlesh = 0;
            this.smParamTargetAngle = 0;
        }

        if ((this.update & 4) != 0) {
            this.speedMod = b.getShort();
            this.walkType = NetworkVariables.WalkType.fromByte(b.get());
            this.predictionType = b.get();
            this.realX = b.getFloat();
            this.realY = b.getFloat();
            this.realZ = b.get();
        } else {
            this.speedMod = 1000;
            this.walkType = NetworkVariables.WalkType.WT1;
            this.predictionType = 0;
            this.realX = this.x;
            this.realY = this.y;
            this.realZ = this.z;
        }

        if ((this.update & 1) != 0) {
            this.realState = NetworkVariables.ZombieState.fromByte(b.get());
        } else {
            this.realState = NetworkVariables.ZombieState.Idle;
        }

        if ((this.update & 32) != 0) {
            this.reanimatedBodyId.load(b);
        } else {
            this.reanimatedBodyId.reset();
        }

        if ((this.update & 16) != 0) {
            this.pfb.parse(b, connection);
        } else {
            this.pfb.reset();
        }

        if ((this.update & -128) != 0) {
            this.grappledBy.parse(b, connection);
            this.sharedGrappleType = GameWindow.ReadString(b);
        } else {
            this.grappledBy.clear();
            this.sharedGrappleType = "";
        }
    }

    public void write(ByteBuffer bb) {
        bb.putShort(this.id);
        bb.put(this.update);
        bb.putFloat(this.x);
        bb.putFloat(this.y);
        bb.putInt(this.outfitId);
        bb.putShort(this.health);
        if ((this.update & 8) != 0) {
            bb.putShort(this.booleanVariables);
        }

        if ((this.update & 64) != 0) {
            bb.put(this.z);
        }

        if ((this.update & 2) != 0) {
            bb.putShort(this.target);
            bb.putFloat(this.targetAngle);
            bb.putShort(this.timeSinceSeenFlesh);
            bb.putShort(this.smParamTargetAngle);
        }

        if ((this.update & 4) != 0) {
            bb.putShort(this.speedMod);
            bb.put((byte)this.walkType.ordinal());
            bb.put(this.predictionType);
            bb.putFloat(this.realX);
            bb.putFloat(this.realY);
            bb.put(this.realZ);
        }

        if ((this.update & 1) != 0) {
            bb.put((byte)this.realState.ordinal());
        }

        if ((this.update & 32) != 0) {
            this.reanimatedBodyId.save(bb);
        }

        if ((this.update & 16) != 0) {
            this.pfb.write(bb);
        }

        if ((this.update & -128) != 0) {
            this.grappledBy.write(bb);
            GameWindow.WriteString(bb, this.sharedGrappleType);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    @Override
    public int getPacketSizeBytes() {
        return 53;
    }

    public void copy(ZombiePacket packet) {
        this.id = packet.id;
        this.x = packet.x;
        this.y = packet.y;
        this.update = packet.update;
        this.outfitId = packet.outfitId;
        this.health = packet.health;
        if ((this.update & 8) != 0) {
            this.booleanVariables = packet.booleanVariables;
        } else {
            this.booleanVariables = 0;
        }

        if ((this.update & 64) != 0) {
            this.z = packet.z;
        } else {
            this.z = 0;
        }

        if ((this.update & 2) != 0) {
            this.target = packet.target;
            this.targetAngle = packet.targetAngle;
            this.timeSinceSeenFlesh = packet.timeSinceSeenFlesh;
            this.smParamTargetAngle = packet.smParamTargetAngle;
        } else {
            this.target = -1;
            this.targetAngle = 0.0F;
            this.timeSinceSeenFlesh = 0;
            this.smParamTargetAngle = 0;
        }

        if ((this.update & 4) != 0) {
            this.speedMod = packet.speedMod;
            this.walkType = packet.walkType;
            this.predictionType = packet.predictionType;
            this.realX = packet.realX;
            this.realY = packet.realY;
            this.realZ = packet.realZ;
        } else {
            this.speedMod = 1000;
            this.walkType = NetworkVariables.WalkType.WT1;
            this.predictionType = 0;
            this.realX = this.x;
            this.realY = this.y;
            this.realZ = this.z;
        }

        if ((this.update & 1) != 0) {
            this.realState = packet.realState;
        } else {
            this.realState = NetworkVariables.ZombieState.Idle;
        }

        if ((this.update & 32) != 0) {
            this.reanimatedBodyId.set(packet.reanimatedBodyId);
        } else {
            this.reanimatedBodyId.reset();
        }

        if ((this.update & 16) != 0) {
            this.pfb.copy(packet.pfb);
        } else {
            this.pfb.reset();
        }

        if ((this.update & -128) != 0) {
            this.grappledBy.copy(packet.grappledBy);
            this.sharedGrappleType = packet.sharedGrappleType;
        } else {
            this.grappledBy.clear();
            this.sharedGrappleType = "";
        }
    }

    public void set(IsoZombie chr) {
        this.id = chr.onlineId;
        this.update = 0;
        this.outfitId = chr.getPersistentOutfitID();
        chr.networkAi.set(this);
        chr.networkAi.mindSync.set(this);
        chr.thumpSent = true;
        this.grappledBy.clear();
        if (chr.getWrappedGrappleable().getGrappledBy() instanceof IsoPlayer isoPlayer) {
            this.update |= -128;
            this.grappledBy.set(isoPlayer);
            this.sharedGrappleType = chr.getSharedGrappleType();
        }

        if (!this.pfb.isCanceled()) {
            this.update = (byte)(this.update | 16);
        }

        if (this.predictionType != 0) {
            this.update = (byte)(this.update | 4);
        }

        if (this.target != -1) {
            this.update = (byte)(this.update | 2);
        }

        if (this.realState != NetworkVariables.ZombieState.Idle) {
            this.update = (byte)(this.update | 1);
        }

        if (this.reanimatedBodyId.getObjectID() != -1L) {
            this.update = (byte)(this.update | 32);
        }

        if (this.z != 0) {
            this.update = (byte)(this.update | 64);
        }

        if (this.booleanVariables != 0) {
            this.update = (byte)(this.update | 8);
        }
    }

    public static class UpdateFlags {
        public static final byte STATE = 1;
        public static final byte TARGET = 2;
        public static final byte MOVING = 4;
        public static final byte VARIABLES = 8;
        public static final byte PATHFINDING = 16;
        public static final byte REANIMATED = 32;
        public static final byte ZNOTZERO = 64;
        public static final byte GRAPPLED = -128;
    }
}
