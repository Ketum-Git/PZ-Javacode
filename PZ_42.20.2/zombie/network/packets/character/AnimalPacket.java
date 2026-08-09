// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import zombie.characters.Capability;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoHutch;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.NetworkVariables;
import zombie.network.PacketSetting;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.character.AnimalStateVariables;
import zombie.network.fields.character.Prediction;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 7, priority = 2, reliability = 0, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class AnimalPacket implements INetworkPacket {
    public static final int PACKET_SIZE_BYTES = 19;
    @JSONField
    public short flags = 1;
    @JSONField
    public byte variables;
    @JSONField
    public NetworkVariables.ZombieState realState = NetworkVariables.ZombieState.Idle;
    @JSONField
    public byte location;
    @JSONField
    public byte hutchNestBox;
    @JSONField
    public byte hutchPosition;
    @JSONField
    public VehicleID vehicleId = new VehicleID();
    @JSONField
    public String idleAction;
    @JSONField
    public String type;
    @JSONField
    public String breed;
    @JSONField
    public short alertedId;
    @JSONField
    public final Prediction prediction = new Prediction();
    @JSONField
    public int age;
    @JSONField
    public float milkQty;
    @JSONField
    public float woolQty;
    @JSONField
    public float weight;
    @JSONField
    public byte stress;
    @JSONField
    public byte acceptance;
    @JSONField
    public byte health;
    @JSONField
    public byte thirst;
    @JSONField
    public byte hunger;
    @JSONField
    public String customName = "";
    @JSONField
    public int squareX;
    @JSONField
    public int squareY;
    @JSONField
    public byte squareZ;
    @JSONField
    public final AnimalID mother = new AnimalID();
    @JSONField
    public int pregnantTime;
    @JSONField
    public long lastTimeMilked;
    @JSONField
    public float maxMilkActual;
    @JSONField
    public int fertilizedTime;
    @JSONField
    public byte lastImpregnateTime;

    @Override
    public int getPacketSizeBytes() {
        return 19;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.flags);
        b.putByte(this.variables);
        b.putEnum(this.realState);
        b.putByte(this.location);
        if (this.location == 2) {
            this.vehicleId.write(b);
        } else if (this.location == 1) {
            b.putByte(this.hutchNestBox);
            b.putByte(this.hutchPosition);
        }

        this.prediction.write(b);
        if ((this.flags & 2) != 0) {
            b.putUTF(this.idleAction);
        }

        if ((this.flags & 4) != 0) {
            b.putUTF(this.type);
            b.putUTF(this.breed);
        }

        if ((this.flags & 6) != 0) {
            b.putInt(this.squareX);
            b.putInt(this.squareY);
            b.putByte(this.squareZ);
        }

        if (this.location != 4) {
            if ((this.flags & 32) != 0) {
                b.putFloat(this.milkQty);
                b.putLong(this.lastTimeMilked);
            }

            if ((this.flags & 64) != 0) {
                b.putFloat(this.woolQty);
            }

            if ((this.flags & 128) != 0) {
                b.putByte(this.acceptance);
            }

            if ((this.flags & 16) != 0) {
                b.putInt(this.pregnantTime);
            }

            if ((this.flags & 256) != 0) {
                b.putUTF(this.customName);
            }

            if ((this.flags & 512) != 0) {
                this.mother.write(b);
            }

            if ((this.flags & 1024) != 0) {
                b.putFloat(this.maxMilkActual);
            }

            if ((this.flags & 2048) != 0) {
                b.putInt(this.fertilizedTime);
            }

            if ((this.flags & 4096) != 0) {
                b.putByte(this.lastImpregnateTime);
            }

            b.putInt(this.age);
            b.putFloat(this.weight);
            b.putByte(this.stress);
            b.putByte(this.thirst);
            b.putByte(this.hunger);
            b.putByte(this.health);
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.flags = b.getShort();
        this.variables = b.getByte();
        this.realState = b.getEnum(NetworkVariables.ZombieState.class);
        this.location = b.getByte();
        if (this.location == 2) {
            this.vehicleId.parse(b, connection);
        } else if (this.location == 1) {
            this.hutchNestBox = b.getByte();
            this.hutchPosition = b.getByte();
        }

        this.prediction.parse(b, connection);
        if ((this.flags & 2) != 0) {
            this.idleAction = b.getUTF();
        }

        if ((this.flags & 4) != 0) {
            this.type = b.getUTF();
            this.breed = b.getUTF();
        }

        if ((this.flags & 6) != 0) {
            this.squareX = b.getInt();
            this.squareY = b.getInt();
            this.squareZ = b.getByte();
        }

        if (this.location != 4) {
            if ((this.flags & 32) != 0) {
                this.milkQty = b.getFloat();
                this.lastTimeMilked = b.getLong();
            }

            if ((this.flags & 64) != 0) {
                this.woolQty = b.getFloat();
            }

            if ((this.flags & 128) != 0) {
                this.acceptance = b.getByte();
            }

            if ((this.flags & 16) != 0) {
                this.pregnantTime = b.getInt();
            }

            if ((this.flags & 256) != 0) {
                this.customName = b.getUTF();
            }

            if ((this.flags & 512) != 0) {
                this.mother.parse(b, connection);
            }

            if ((this.flags & 1024) != 0) {
                this.maxMilkActual = b.getFloat();
            }

            if ((this.flags & 2048) != 0) {
                this.fertilizedTime = b.getInt();
            }

            if ((this.flags & 4096) != 0) {
                this.lastImpregnateTime = b.getByte();
            }

            this.age = b.getInt();
            this.weight = b.getFloat();
            this.stress = b.getByte();
            this.thirst = b.getByte();
            this.hunger = b.getByte();
            this.health = b.getByte();
        }
    }

    public boolean isDead() {
        return (this.variables & AnimalStateVariables.Flags.isDead) != 0;
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        if (this.location == 2) {
            return this.vehicleId.isConsistent(connection);
        } else if (this.location == 1) {
            return IsoHutch.getHutch(PZMath.fastfloor(this.prediction.x), PZMath.fastfloor(this.prediction.y), this.prediction.z) != null;
        } else {
            return (this.flags & 6) != 0
                ? IsoWorld.instance.getCell().getGridSquare(this.squareX, this.squareY, this.squareZ) != null
                : IsoWorld.instance.getCell().getGridSquare((double)this.prediction.x, (double)this.prediction.y, (double)this.prediction.z) != null;
        }
    }

    public static class Flags {
        public static final short regular = 1;
        public static final short idle = 2;
        public static final short init = 4;
        public static final short alert = 8;
        public static final short pregnant = 16;
        public static final short canhavemilk = 32;
        public static final short canhavewool = 64;
        public static final short acceptance = 128;
        public static final short customname = 256;
        public static final short mother = 512;
        public static final short debuginfo = 1024;
        public static final short fertilized = 2048;
        public static final short impregnatetime = 4096;
        public static final short full = 6;
    }
}
