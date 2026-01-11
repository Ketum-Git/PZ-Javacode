// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.MovingObject;

@PacketSetting(ordering = 1, priority = 2, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ObjectModDataPacket implements INetworkPacket {
    @JSONField
    protected final MovingObject movingObject = new MovingObject();

    @Override
    public void setData(Object... values) {
        this.movingObject.set((IsoObject)values[0]);
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.movingObject.write(b);
        if (this.movingObject.getObject().getModData().isEmpty()) {
            b.putByte((byte)0);
        } else {
            b.putByte((byte)1);

            try {
                this.movingObject.getObject().getModData().save(b.bb);
            } catch (IOException var3) {
                DebugLog.Multiplayer.printException(var3, "ObjectModDataPacket write error", LogSeverity.Error);
            }
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.movingObject.parse(b, connection);
        boolean hasData = b.get() == 1;
        IsoObject object = this.movingObject.getObject();
        if (object == null) {
            DebugLog.Multiplayer.warn("ObjectModDataPacket.parse: object is null (%s)", this.movingObject.getDescription());
        } else {
            if (hasData) {
                int waterAmount = (int)object.getFluidAmount();

                try {
                    object.getModData().load(b, 240);
                } catch (IOException var8) {
                    DebugLog.Multiplayer.printException(var8, "ObjectModDataPacket parse error", LogSeverity.Error);
                    return;
                }

                if (waterAmount != object.getFluidAmount()) {
                    LuaEventManager.triggerEvent("OnWaterAmountChange", object, waterAmount);
                }
            } else if (object.hasModData()) {
                object.getModData().wipe();
            }

            if (this.movingObject.getObject() instanceof IsoAnimal isoAnimal) {
                if (isoAnimal.isOnHook()) {
                    isoAnimal.getHook().onReceivedNetUpdate();
                }
            } else if (this.movingObject.getObject() instanceof IsoDeadBody isoDeadBody) {
                isoDeadBody.invalidateCorpse();
            }
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.movingObject.isConsistent(connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToRelativeClients(PacketTypes.PacketType.ObjectModData, connection, this.movingObject.getObject().getX(), this.movingObject.getObject().getY());
    }
}
