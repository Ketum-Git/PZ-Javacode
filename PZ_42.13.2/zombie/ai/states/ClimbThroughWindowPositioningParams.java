// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ai.states;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoDirections;
import zombie.iso.IsoObject;
import zombie.network.JSONField;
import zombie.network.PZNetKahluaTableImpl;
import zombie.network.fields.INetworkPacketField;
import zombie.util.Pool;
import zombie.util.PooledObject;

public final class ClimbThroughWindowPositioningParams extends PooledObject implements INetworkPacketField {
    private static final byte FLAG_CAN_CLIMB = 1;
    private static final byte FLAG_SCRATCH = 2;
    private static final byte FLAG_COUNTER = 4;
    private static final byte FLAG_FLOOR = 8;
    private static final byte FLAG_SHEET_ROPE = 16;
    @JSONField
    public boolean canClimb;
    @JSONField
    public IsoDirections climbDir;
    @JSONField
    public IsoGameCharacter climbingCharacter;
    @JSONField
    public IsoObject windowObject;
    @JSONField
    public int z;
    @JSONField
    public int startX;
    @JSONField
    public int startY;
    @JSONField
    public int endX;
    @JSONField
    public int endY;
    @JSONField
    public int oppositeX;
    @JSONField
    public int oppositeY;
    @JSONField
    public boolean scratch;
    @JSONField
    public boolean isCounter;
    @JSONField
    public boolean isFloor;
    @JSONField
    public boolean isSheetRope;
    private static final Pool<ClimbThroughWindowPositioningParams> s_pool = new Pool<>(ClimbThroughWindowPositioningParams::new);

    protected ClimbThroughWindowPositioningParams() {
        this.reset();
    }

    private void reset() {
        this.canClimb = false;
        this.climbDir = IsoDirections.N;
        this.climbingCharacter = null;
        this.windowObject = null;
        this.z = 0;
        this.startX = 0;
        this.startY = 0;
        this.endX = 0;
        this.endY = 0;
        this.oppositeX = 0;
        this.oppositeY = 0;
        this.scratch = false;
        this.isCounter = false;
        this.isFloor = false;
        this.isSheetRope = false;
    }

    @Override
    public void onReleased() {
        this.reset();
        super.onReleased();
    }

    public static ClimbThroughWindowPositioningParams alloc() {
        return s_pool.alloc();
    }

    public static void release(ClimbThroughWindowPositioningParams in_params) {
        Pool.tryRelease(in_params);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.write(b.bb);
    }

    public void write(ByteBuffer b) {
        b.putInt(this.startX);
        b.putInt(this.startY);
        b.putInt(this.endX);
        b.putInt(this.endY);
        b.put((byte)this.z);
        b.putInt(this.oppositeX);
        b.putInt(this.oppositeY);
        b.put((byte)this.climbDir.index());
        PZNetKahluaTableImpl.saveIsoGameCharacter(b, this.climbingCharacter);
        PZNetKahluaTableImpl.saveIsoObject(b, this.windowObject);
        byte vars = 0;
        vars = (byte)(vars | (this.canClimb ? 1 : 0));
        vars = (byte)(vars | (this.scratch ? 2 : 0));
        vars = (byte)(vars | (this.isCounter ? 4 : 0));
        vars = (byte)(vars | (this.isFloor ? 8 : 0));
        vars = (byte)(vars | (this.isSheetRope ? 16 : 0));
        b.put(vars);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.startX = b.getInt();
        this.startY = b.getInt();
        this.endX = b.getInt();
        this.endY = b.getInt();
        this.z = b.get();
        this.oppositeX = b.getInt();
        this.oppositeY = b.getInt();
        this.climbDir = IsoDirections.fromIndex(b.get());
        this.climbingCharacter = PZNetKahluaTableImpl.loadIsoGameCharacter(b, null);
        this.windowObject = PZNetKahluaTableImpl.loadIsoObject(b, null);
        byte vars = b.get();
        this.canClimb = (vars & 1) != 0;
        this.scratch = (vars & 2) != 0;
        this.isCounter = (vars & 4) != 0;
        this.isFloor = (vars & 8) != 0;
        this.isSheetRope = (vars & 16) != 0;
    }
}
