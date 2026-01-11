// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.network.JSONField;

public class NetObject implements INetworkPacketField {
    private final byte objectTypeNone = 0;
    private final byte objectTypeObject = 1;
    @JSONField
    private byte objectType;
    @JSONField
    private short objectId;
    @JSONField
    private int squareX;
    @JSONField
    private int squareY;
    @JSONField
    private byte squareZ;
    private boolean isProcessed;
    private IsoObject object;

    public NetObject setObject(IsoObject value) {
        if (value instanceof IsoMovingObject) {
            DebugLog.General.printStackTrace("The NetObject can't process IsoMovingObjects");
        }

        this.object = value;
        this.isProcessed = true;
        if (this.object == null) {
            this.objectType = 0;
            this.objectId = 0;
            return this;
        } else {
            IsoGridSquare square = this.object.square;
            this.objectType = 1;
            this.objectId = (short)square.getObjects().indexOf(this.object);
            this.squareX = square.getX();
            this.squareY = square.getY();
            this.squareZ = (byte)square.getZ();
            return this;
        }
    }

    public IsoObject getObject() {
        if (!this.isProcessed) {
            if (this.objectType == 0) {
                this.object = null;
            }

            if (this.objectType == 1) {
                IsoGridSquare square = IsoWorld.instance.currentCell.getGridSquare(this.squareX, this.squareY, this.squareZ);
                if (square == null) {
                    this.object = null;
                } else {
                    this.object = square.getObjects().get(this.objectId);
                }
            }

            this.isProcessed = true;
        }

        return this.object;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.objectType = b.get();
        if (this.objectType == 1) {
            this.objectId = b.getShort();
            this.squareX = b.getInt();
            this.squareY = b.getInt();
            this.squareZ = b.get();
        }

        this.isProcessed = false;
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.put(this.objectType);
        if (this.objectType == 1) {
            b.putShort(this.objectId);
            b.putInt(this.squareX);
            b.putInt(this.squareY);
            b.put(this.squareZ);
        }
    }

    @Override
    public int getPacketSizeBytes() {
        return this.objectType == 1 ? 12 : 1;
    }
}
