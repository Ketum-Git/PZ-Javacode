// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.characters.IsoPlayer;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.fields.INetworkPacketField;

public class Damage implements INetworkPacketField {
    protected static final float MAX_DAMAGE = 100.0F;
    @JSONField
    protected boolean ignore;
    @JSONField
    protected float damage;

    public void set(boolean ignore, float damage) {
        this.ignore = ignore;
        this.damage = Math.min(damage, 100.0F);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.ignore = b.get() != 0;
        this.damage = b.getFloat();
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putBoolean(this.ignore);
        b.putFloat(this.damage);
    }

    public void processAnimal(IsoAnimal wielder, IsoAnimal target) {
        wielder.atkTarget = target;
        if (GameServer.server) {
            target.HitByAnimal(wielder, this.ignore);
        } else if (GameClient.client) {
            target.setHitReaction("default");
        }
    }

    public void processPlayer(IsoAnimal wielder, IsoPlayer target) {
        wielder.atkTarget = target;
        if (GameServer.server) {
            target.hitConsequences(null, wielder, this.ignore, this.damage, false);
        } else if (GameClient.client) {
            target.setHitReaction("hitreact");
        }
    }

    public float getDamage() {
        return this.damage;
    }
}
