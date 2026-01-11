// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoHutch;
import zombie.network.JSONField;
import zombie.network.NetworkVariables;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.character.AnimalStateVariables;
import zombie.network.fields.character.Prediction;
import zombie.network.fields.vehicle.VehicleID;
import zombie.network.packets.INetworkPacket;

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

    @Override
    public int getPacketSizeBytes() {
        return 19;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.putShort(this.flags);
        b.put(this.variables);
        b.put(this.realState.toByte());
        b.put(this.location);
        if (this.location == 2) {
            this.vehicleId.write(b);
        } else if (this.location == 1) {
            b.put(this.hutchNestBox);
            b.put(this.hutchPosition);
        }

        this.prediction.write(b);
        if ((this.flags & 2) != 0) {
            GameWindow.WriteStringUTF(b, this.idleAction);
        }

        if ((this.flags & 4) != 0) {
            GameWindow.WriteStringUTF(b, this.type);
            GameWindow.WriteStringUTF(b, this.breed);
        }

        if ((this.flags & 6) != 0) {
            b.putInt(this.squareX);
            b.putInt(this.squareY);
            b.put(this.squareZ);
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
                b.put(this.acceptance);
            }

            if ((this.flags & 256) != 0) {
                GameWindow.WriteStringUTF(b, this.customName);
            }

            if ((this.flags & 512) != 0) {
                this.mother.write(b);
            }

            if ((this.flags & 1024) != 0) {
                b.putInt(this.pregnantTime);
                b.putFloat(this.maxMilkActual);
            }

            b.putInt(this.age);
            b.putFloat(this.weight);
            b.put(this.stress);
            b.put(this.thirst);
            b.put(this.hunger);
            b.put(this.health);
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.flags = b.getShort();
        this.variables = b.get();
        this.realState = NetworkVariables.ZombieState.fromByte(b.get());
        this.location = b.get();
        if (this.location == 2) {
            this.vehicleId.parse(b, connection);
        } else if (this.location == 1) {
            this.hutchNestBox = b.get();
            this.hutchPosition = b.get();
        }

        this.prediction.parse(b, connection);
        if ((this.flags & 2) != 0) {
            this.idleAction = GameWindow.ReadString(b);
        }

        if ((this.flags & 4) != 0) {
            this.type = GameWindow.ReadString(b);
            this.breed = GameWindow.ReadString(b);
        }

        if ((this.flags & 6) != 0) {
            this.squareX = b.getInt();
            this.squareY = b.getInt();
            this.squareZ = b.get();
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
                this.acceptance = b.get();
            }

            if ((this.flags & 256) != 0) {
                this.customName = GameWindow.ReadString(b);
            }

            if ((this.flags & 512) != 0) {
                this.mother.parse(b, connection);
            }

            if ((this.flags & 1024) != 0) {
                this.pregnantTime = b.getInt();
                this.maxMilkActual = b.getFloat();
            }

            this.age = b.getInt();
            this.weight = b.getFloat();
            this.stress = b.get();
            this.thirst = b.get();
            this.hunger = b.get();
            this.health = b.get();
        }
    }

    public boolean isDead() {
        return (this.variables & AnimalStateVariables.Flags.isDead) != 0;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        if (this.location == 1) {
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
        public static final short full = 6;
    }
}
