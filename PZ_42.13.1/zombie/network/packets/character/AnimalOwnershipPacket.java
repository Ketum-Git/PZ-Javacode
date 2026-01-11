// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.character;

import java.nio.ByteBuffer;
import java.util.HashSet;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.network.PacketSetting;
import zombie.network.packets.INetworkPacket;
import zombie.popman.animal.AnimalInstanceManager;

@PacketSetting(ordering = 7, priority = 0, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class AnimalOwnershipPacket implements INetworkPacket {
    private final HashSet<Short> owned = new HashSet<>();
    private final HashSet<Short> deleted = new HashSet<>();

    public HashSet<Short> getDeleted() {
        return this.deleted;
    }

    public HashSet<Short> getOwned() {
        return this.owned;
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.owned.size());
        this.owned.forEach(b::putShort);
        b.putInt(this.deleted.size());
        this.deleted.forEach(b::putShort);
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.owned.clear();
        int ownedSize = b.getInt();

        for (int i = 0; i < ownedSize; i++) {
            this.owned.add(b.getShort());
        }

        this.deleted.clear();
        int deletedSize = b.getInt();

        for (int i = 0; i < deletedSize; i++) {
            this.deleted.add(b.getShort());
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        for (short onlineID : this.deleted) {
            IsoAnimal animal = AnimalInstanceManager.getInstance().get(onlineID);
            if (animal != null) {
                animal.remove();
            }
        }
    }
}
