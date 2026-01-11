// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameServer;
import zombie.network.fields.INetworkPacketField;

public class Bite implements INetworkPacketField {
    protected short flags;
    protected float hitDirection;

    public void set(IsoZombie wielder) {
        this.flags = 0;
        this.flags = (short)(this.flags | (short)(wielder.getEatBodyTarget() != null ? 1 : 0));
        this.flags = (short)(this.flags | (short)(wielder.getVariableBoolean("AttackDidDamage") ? 2 : 0));
        this.flags = (short)(this.flags | (short)("BiteDefended".equals(wielder.getHitReaction()) ? 4 : 0));
        this.flags = (short)(this.flags | (short)(wielder.scratch ? 8 : 0));
        this.flags = (short)(this.flags | (short)(wielder.laceration ? 16 : 0));
        this.hitDirection = wielder.getHitDir().getDirection();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.flags = b.getShort();
        this.hitDirection = b.getFloat();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putShort(this.flags);
        b.putFloat(this.hitDirection);
    }

    @Override
    public String getDescription() {
        return "\n\tBite [ eatBodyTarget="
            + ((this.flags & 1) != 0)
            + " | attackDidDamage="
            + ((this.flags & 2) != 0)
            + " | biteDefended="
            + ((this.flags & 4) != 0)
            + " | scratch="
            + ((this.flags & 8) != 0)
            + " | laceration="
            + ((this.flags & 16) != 0)
            + " | hitDirection="
            + this.hitDirection
            + " ]";
    }

    public void process(IsoZombie wielder, IsoGameCharacter target) {
        if ((this.flags & 4) == 0) {
            target.setAttackedBy(wielder);
            if ((this.flags & 1) != 0 || target.isDead()) {
                wielder.setEatBodyTarget(target, true);
                wielder.setTarget(null);
            }

            if (target.isAsleep()) {
                if (GameServer.server) {
                    target.sendObjectChange("wakeUp");
                } else {
                    target.forceAwake();
                }
            }

            if ((this.flags & 2) != 0) {
                target.reportEvent("washit");
                target.setVariable("hitpvp", false);
            }

            wielder.scratch = (this.flags & 8) != 0;
            wielder.laceration = (this.flags & 8) != 0;
            if (GameServer.server && target.getBodyDamage() != null) {
                target.getBodyDamage().AddRandomDamageFromZombie(wielder, null);
                if (target.isDeathDragDown()) {
                    target.Kill(wielder);
                }
            }
        }

        wielder.getHitDir().setLengthAndDirection(this.hitDirection, 1.0F);
    }
}
