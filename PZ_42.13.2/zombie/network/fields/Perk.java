// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import java.nio.ByteBuffer;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;

public class Perk implements INetworkPacketField {
    protected PerkFactory.Perk perk;
    protected byte perkIndex;

    public void set(PerkFactory.Perk _perk) {
        this.perk = _perk;
        if (this.perk == null) {
            this.perkIndex = -1;
        } else {
            this.perkIndex = (byte)this.perk.index();
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.perkIndex = b.get();
        if (this.perkIndex >= 0 && this.perkIndex <= PerkFactory.Perks.getMaxIndex()) {
            this.perk = PerkFactory.Perks.fromIndex(this.perkIndex);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putByte(this.perkIndex);
    }

    @Override
    public String getDescription() {
        return "\n\t" + this.getClass().getSimpleName() + " [ perk=( " + this.perkIndex + " )" + (this.perk == null ? "null" : this.perk.name) + " ]";
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.perk != null;
    }

    public PerkFactory.Perk getPerk() {
        return this.perk;
    }
}
