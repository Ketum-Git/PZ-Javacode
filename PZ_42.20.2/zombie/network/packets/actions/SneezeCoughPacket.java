// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.Translator;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.IConnection;
import zombie.network.PacketSetting;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 3, reliability = 0, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class SneezeCoughPacket implements INetworkPacket {
    protected final PlayerID wielder = new PlayerID();
    protected byte value;

    @Override
    public void setData(Object... values) {
        this.set((IsoPlayer)values[0], (Integer)values[1], (Byte)values[2]);
    }

    public void set(IsoPlayer wielder, int sneezingCoughing, byte sneezeVar) {
        this.wielder.set(wielder);
        this.value = 0;
        if (sneezingCoughing % 2 == 0) {
            this.value = (byte)(this.value | 1);
        }

        if (sneezingCoughing > 2) {
            this.value = (byte)(this.value | 2);
        }

        if (sneezeVar > 1) {
            this.value = (byte)(this.value | 4);
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.wielder.write(b);
        b.putByte(this.value);
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.wielder.parse(b, connection);
        this.value = b.getByte();
    }

    @Override
    public void processClient(UdpConnection connection) {
        if (this.wielder.getPlayer() != null) {
            boolean isSneeze = (this.value & 1) == 0;
            boolean isMuffled = (this.value & 2) != 0;
            int sneezeVar = (this.value & 4) == 0 ? 1 : 2;
            if (this.wielder.getPlayer().isLocalPlayer()) {
                this.wielder.getPlayer().setVariable("Ext", isSneeze ? "Sneeze" + sneezeVar : "Cough");
                this.wielder.getPlayer().reportEvent("EventDoExt");
            }

            this.wielder.getPlayer().Say(Translator.getText("IGUI_PlayerText_" + (isSneeze ? "Sneeze" : "Cough") + (isMuffled ? "Muffled" : "")));
            if (isSneeze) {
                if (isMuffled) {
                    this.wielder.getPlayer().playerVoiceSound("SneezeLight");
                } else {
                    this.wielder.getPlayer().playerVoiceSound("SneezeHeavy");
                }
            } else if (isMuffled) {
                this.wielder.getPlayer().playerVoiceSound("MuffledCough");
            } else {
                this.wielder.getPlayer().playerVoiceSound("Cough");
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.wielder.isConsistent(connection);
    }
}
