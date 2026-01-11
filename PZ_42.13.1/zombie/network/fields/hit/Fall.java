// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.characters.HitReactionNetworkAI;
import zombie.characters.IsoGameCharacter;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public class Fall implements INetworkPacketField {
    @JSONField
    protected float dropPositionX;
    @JSONField
    protected float dropPositionY;
    @JSONField
    protected byte dropPositionZ;
    @JSONField
    public float dropDirection;

    public void set(HitReactionNetworkAI hitReaction) {
        this.dropPositionX = hitReaction.finalPosition.x;
        this.dropPositionY = hitReaction.finalPosition.y;
        this.dropPositionZ = hitReaction.finalPositionZ;
        this.dropDirection = hitReaction.finalDirection.getDirection();
    }

    public void set(float dropPositionX, float dropPositionY, byte dropPositionZ, float dropDirection) {
        this.dropPositionX = dropPositionX;
        this.dropPositionY = dropPositionY;
        this.dropPositionZ = dropPositionZ;
        this.dropDirection = dropDirection;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.dropPositionX = b.getFloat();
        this.dropPositionY = b.getFloat();
        this.dropPositionZ = b.get();
        this.dropDirection = b.getFloat();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putFloat(this.dropPositionX);
        b.putFloat(this.dropPositionY);
        b.putByte(this.dropPositionZ);
        b.putFloat(this.dropDirection);
    }

    public void process(IsoGameCharacter character) {
        if (this.dropPositionX != 0.0F && this.dropPositionY != 0.0F && character.getHitReactionNetworkAI() != null) {
            character.getHitReactionNetworkAI().process(this.dropPositionX, this.dropPositionY, this.dropPositionZ, this.dropDirection);
        }
    }
}
