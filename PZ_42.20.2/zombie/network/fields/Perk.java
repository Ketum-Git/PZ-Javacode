// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.fields;

import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.network.IConnection;

public class Perk implements INetworkPacketField {
    protected PerkFactory.Perk perk;
    protected byte perkIndex;

    public void set(PerkFactory.Perk perk) {
        this.perk = perk;
        if (this.perk == null) {
            this.perkIndex = -1;
        } else {
            this.perkIndex = (byte)this.perk.index();
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.perkIndex = b.getByte();
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
    public boolean isConsistent(IConnection connection) {
        return this.perk != null;
    }

    public PerkFactory.Perk getPerk() {
        return this.perk;
    }
}
