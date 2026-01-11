// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public abstract class Hit implements INetworkPacketField {
    private static final float MAX_DAMAGE = 100.0F;
    @JSONField
    protected float damage;
    @JSONField
    protected float hitForce;
    @JSONField
    protected float hitDirectionX;
    @JSONField
    protected float hitDirectionY;

    public void set(float damage, float hitForce, float hitDirectionX, float hitDirectionY) {
        this.damage = Math.min(damage, 100.0F);
        this.hitForce = hitForce;
        this.hitDirectionX = hitDirectionX;
        this.hitDirectionY = hitDirectionY;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.damage = b.getFloat();
        this.hitForce = b.getFloat();
        this.hitDirectionX = b.getFloat();
        this.hitDirectionY = b.getFloat();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putFloat(this.damage);
        b.putFloat(this.hitForce);
        b.putFloat(this.hitDirectionX);
        b.putFloat(this.hitDirectionY);
    }

    void process(IsoGameCharacter wielder, IsoGameCharacter target) {
        target.getHitDir().set(this.hitDirectionX, this.hitDirectionY);
        target.setHitForce(this.hitForce);
        if (GameServer.server && target instanceof IsoZombie isoZombie && wielder instanceof IsoPlayer isoPlayer) {
            isoZombie.addAggro(wielder, this.damage);
            DebugLog.Combat
                .trace("AddAggro zombie=%d player=%d ( \"%s\" ) damage=%f", target.getOnlineID(), wielder.getOnlineID(), isoPlayer.getUsername(), this.damage);
        }

        target.setAttackedBy(wielder);
    }

    public float getDamage() {
        return this.damage;
    }
}
