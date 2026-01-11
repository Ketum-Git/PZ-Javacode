// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.actions;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.Food;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class EatFoodPacket implements INetworkPacket {
    @JSONField
    PlayerID player = new PlayerID();
    @JSONField
    float percentage;
    @JSONField
    Food food;

    @Override
    public void setData(Object... values) {
        if (values.length == 3 && values[0] instanceof IsoPlayer) {
            this.set((IsoPlayer)values[0], (Food)values[1], (Float)values[2]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    public void set(IsoPlayer player, Food food, float percentage) {
        this.player.set(player);
        this.percentage = percentage;
        this.food = food;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.player.parse(b, connection);
        this.percentage = b.getFloat();
        this.player.getPlayer().getNutrition().load(b);

        try {
            this.food = (Food)InventoryItem.loadItem(b, 240);
        } catch (Exception var4) {
            var4.printStackTrace();
            this.food = null;
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        try {
            this.player.write(b);
            b.putFloat(this.percentage);
            this.player.getPlayer().getNutrition().save(b.bb);
            this.food.saveWithSize(b.bb, false);
        } catch (IOException var3) {
            var3.printStackTrace();
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.player.getPlayer().EatOnClient(this.food, this.percentage);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isConsistent(connection)) {
            this.processClient(connection);
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.player.isConsistent(connection) && this.food != null && this.percentage >= 0.0F && this.percentage < 100.0F;
    }
}
