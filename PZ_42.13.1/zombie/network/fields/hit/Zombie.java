// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields.hit;

import java.nio.ByteBuffer;
import zombie.GameWindow;
import zombie.PersistentOutfits;
import zombie.characters.IsoZombie;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.skinnedmodel.ModelManager;
import zombie.inventory.types.HandWeapon;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.ServerGUI;
import zombie.network.fields.INetworkPacketField;
import zombie.util.Type;

public class Zombie extends Character implements INetworkPacketField {
    @JSONField
    protected short zombieFlags;
    @JSONField
    protected String attackOutcome;
    @JSONField
    protected String attackPosition;

    public void set(IsoZombie zombie, boolean isHelmetFall) {
        this.set(zombie);
        this.zombieFlags = 0;
        this.zombieFlags = (short)(this.zombieFlags | (zombie.isStaggerBack() ? 1 : 0));
        this.zombieFlags = (short)(this.zombieFlags | (zombie.isFakeDead() ? 2 : 0));
        this.zombieFlags = (short)(this.zombieFlags | (zombie.isBecomeCrawler() ? 4 : 0));
        this.zombieFlags = (short)(this.zombieFlags | (zombie.isCrawling() ? 8 : 0));
        this.zombieFlags = (short)(this.zombieFlags | (zombie.isKnifeDeath() ? 16 : 0));
        this.zombieFlags = (short)(this.zombieFlags | (zombie.isJawStabAttach() ? 32 : 0));
        this.zombieFlags = (short)(this.zombieFlags | (isHelmetFall ? 64 : 0));
        this.zombieFlags = (short)(this.zombieFlags | (zombie.getAttackDidDamage() ? 128 : 0));
        this.attackOutcome = zombie.getAttackOutcome();
        this.attackPosition = zombie.getPlayerAttackPosition();
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.zombieFlags = b.getShort();
        this.attackOutcome = GameWindow.ReadString(b);
        this.attackPosition = GameWindow.ReadString(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putShort(this.zombieFlags);
        b.putUTF(this.attackOutcome);
        b.putUTF(this.attackPosition);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return super.isConsistent(connection);
    }

    @Override
    public void process() {
        super.process();
        this.getZombie().setAttackOutcome(this.attackOutcome);
        this.getZombie().setPlayerAttackPosition(this.attackPosition);
        this.getZombie().setStaggerBack((this.zombieFlags & 1) != 0);
        this.getZombie().setFakeDead((this.zombieFlags & 2) != 0);
        this.getZombie().setBecomeCrawler((this.zombieFlags & 4) != 0);
        this.getZombie().setCrawler((this.zombieFlags & 8) != 0);
        this.getZombie().setKnifeDeath((this.zombieFlags & 16) != 0);
        this.getZombie().setJawStabAttach((this.zombieFlags & 32) != 0);
        this.getZombie().setAttackDidDamage((this.zombieFlags & 128) != 0);
    }

    public void react(HandWeapon weapon) {
        if (this.getZombie().isJawStabAttach()) {
            this.getZombie().setAttachedItem("JawStab", weapon);
        }

        if (GameServer.server && (this.zombieFlags & 64) != 0 && !PersistentOutfits.instance.isHatFallen(this.getZombie())) {
            PersistentOutfits.instance.setFallenHat(this.getZombie(), true);
            if (ServerGUI.isCreated()) {
                PersistentOutfits.instance.removeFallenHat(this.getZombie().getPersistentOutfitID(), this.getZombie());
                ModelManager.instance.ResetNextFrame(this.getZombie());
            }
        }

        this.react();
    }

    public IsoZombie getZombie() {
        return Type.tryCastTo(this.getCharacter(), IsoZombie.class);
    }

    public static class Flags {
        public static final short isStaggerBack = 1;
        public static final short isFakeDead = 2;
        public static final short isBecomeCrawler = 4;
        public static final short isCrawling = 8;
        public static final short isKnifeDeath = 16;
        public static final short isJawStabAttach = 32;
        public static final short isHelmetFall = 64;
        public static final short AttackDidDamage = 128;
    }
}
