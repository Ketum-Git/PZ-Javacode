// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoBulletTracerEffects;
import zombie.network.IConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public class TracerInfo implements INetworkPacketField {
    @JSONField
    public boolean hasEndPosition;
    @JSONField
    public boolean hasSquare;
    @JSONField
    public float range;
    @JSONField
    public float endX;
    @JSONField
    public float endY;
    @JSONField
    public float endZ;
    @JSONField
    private int squareX;
    @JSONField
    private int squareY;
    @JSONField
    private byte squareZ;

    public void set(float range) {
        this.hasEndPosition = false;
        this.hasSquare = false;
        this.range = range;
    }

    public void set(float range, float endX, float endY, float endZ, IsoGridSquare square) {
        this.hasEndPosition = true;
        this.hasSquare = false;
        this.range = range;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
        if (square != null) {
            this.hasSquare = true;
            this.squareX = square.getX();
            this.squareY = square.getY();
            this.squareZ = (byte)square.getZ();
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.hasEndPosition = b.getBoolean();
        this.hasSquare = b.getBoolean();
        this.range = b.getFloat();
        if (this.hasEndPosition) {
            this.endX = b.getFloat();
            this.endY = b.getFloat();
            this.endZ = b.getFloat();
        }

        if (this.hasSquare) {
            this.squareX = b.getInt();
            this.squareY = b.getInt();
            this.squareZ = b.getByte();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.hasEndPosition);
        b.putBoolean(this.hasSquare);
        b.putFloat(this.range);
        if (this.hasEndPosition) {
            b.putFloat(this.endX);
            b.putFloat(this.endY);
            b.putFloat(this.endZ);
        }

        if (this.hasSquare) {
            b.putInt(this.squareX);
            b.putInt(this.squareY);
            b.putByte(this.squareZ);
        }
    }

    public void process(IsoPlayer wielder) {
        if (this.hasEndPosition) {
            if (this.hasSquare) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
                IsoBulletTracerEffects.getInstance().addEffect(wielder, this.range, this.endX, this.endY, this.endZ, square);
            } else {
                IsoBulletTracerEffects.getInstance().addEffect(wielder, this.range, this.endX, this.endY, this.endZ);
            }
        } else {
            IsoBulletTracerEffects.getInstance().addEffect(wielder, this.range);
        }
    }
}
