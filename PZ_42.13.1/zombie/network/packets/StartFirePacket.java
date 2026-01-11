// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoFire;
import zombie.iso.objects.IsoFireManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.anticheats.AntiCheat;
import zombie.network.anticheats.AntiCheatFire;
import zombie.network.anticheats.AntiCheatSmoke;
import zombie.network.fields.Square;

@PacketSetting(
    ordering = 0,
    priority = 1,
    reliability = 2,
    requiredCapability = Capability.LoginOnServer,
    handlingType = 2,
    anticheats = {AntiCheat.Fire, AntiCheat.Smoke}
)
public class StartFirePacket implements INetworkPacket, AntiCheatFire.IAntiCheat, AntiCheatSmoke.IAntiCheat {
    @JSONField
    protected final Square square = new Square();
    @JSONField
    protected int fireEnergy;
    @JSONField
    protected boolean ignite;
    @JSONField
    protected int life;
    @JSONField
    protected boolean smoke;
    @JSONField
    protected int spreadDelay;
    @JSONField
    protected int numParticles;

    @Override
    public void setData(Object... values) {
        this.square.set((IsoGridSquare)values[0]);
        this.ignite = (Boolean)values[1];
        this.fireEnergy = (Integer)values[2];
        this.life = (Integer)values[3];
        this.smoke = (Boolean)values[4];
        this.spreadDelay = 0;
        this.numParticles = 0;
    }

    public void set(IsoGridSquare gridSquare, boolean _ignite, int _fireEnergy, int Life, boolean _smoke) {
        this.square.set(gridSquare);
        this.fireEnergy = _fireEnergy;
        this.ignite = _ignite;
        this.life = Life;
        this.smoke = _smoke;
        this.spreadDelay = 0;
        this.numParticles = 0;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.square.parse(b, connection);
        this.fireEnergy = b.getInt();
        this.ignite = b.get() == 1;
        this.life = b.getInt();
        this.smoke = b.get() == 1;
        if (GameClient.client) {
            this.spreadDelay = b.getInt();
            this.numParticles = b.getInt();
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.square.write(b);
        b.putInt(this.fireEnergy);
        b.putBoolean(this.ignite);
        b.putInt(this.life);
        b.putBoolean(this.smoke);
        if (GameServer.server) {
            b.putInt(this.spreadDelay);
            b.putInt(this.numParticles);
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        IsoFire NewFire = this.smoke
            ? new IsoFire(IsoWorld.instance.currentCell, this.square.getSquare(), this.ignite, this.fireEnergy, this.life, true)
            : new IsoFire(IsoWorld.instance.currentCell, this.square.getSquare(), this.ignite, this.fireEnergy, this.life);
        NewFire.spreadDelay = this.spreadDelay;
        NewFire.numFlameParticles = this.numParticles;
        IsoFireManager.Add(NewFire);
        this.square.getSquare().getObjects().add(NewFire);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        IsoFire NewFire = this.smoke
            ? new IsoFire(this.square.getSquare().getCell(), this.square.getSquare(), this.ignite, this.fireEnergy, this.life, true)
            : new IsoFire(this.square.getSquare().getCell(), this.square.getSquare(), this.ignite, this.fireEnergy, this.life);
        IsoFireManager.Add(NewFire);
        this.spreadDelay = NewFire.getSpreadDelay();
        this.numParticles = NewFire.numFlameParticles;
        this.square.getSquare().getObjects().add(NewFire);
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.square.getSquare() != null && this.life <= 700;
    }

    @Override
    public boolean getSmoke() {
        return this.smoke;
    }

    @Override
    public boolean getIgnition() {
        return this.ignite;
    }

    @Override
    public Square getSquare() {
        return this.square;
    }
}
