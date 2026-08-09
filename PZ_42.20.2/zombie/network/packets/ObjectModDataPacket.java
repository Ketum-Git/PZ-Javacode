// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.io.IOException;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.animals.IsoAnimal;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.IsoObject;
import zombie.iso.objects.IsoDeadBody;
import zombie.network.IConnection;
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
        if (b.putBoolean(!this.movingObject.getObject().getModData().isEmpty())) {
            try {
                this.movingObject.getObject().getModData().save(b.bb);
            } catch (IOException var3) {
                DebugType.Multiplayer.printException(var3, "ObjectModDataPacket write error", LogSeverity.Error);
            }
        }
    }

    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        this.movingObject.parse(b, connection);
        boolean hasData = b.getBoolean();
        IsoObject object = this.movingObject.getObject();
        if (object == null) {
            DebugType.Multiplayer.warn("ObjectModDataPacket.parse: object is null (%s)", this.movingObject.getDescription());
        } else {
            if (hasData) {
                int waterAmount = (int)object.getFluidAmount();

                try {
                    object.getModData().load(b.bb, 249);
                } catch (IOException var9) {
                    DebugType.Multiplayer.printException(var9, "ObjectModDataPacket parse error", LogSeverity.Error);
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
                if (isoDeadBody.isAnimal() && object.getModData().rawget("animalRotStage") instanceof Double animalRotStage) {
                    isoDeadBody.getAnimalVisual().animalRotStage = PZMath.clamp(animalRotStage.intValue(), -1, 4);
                }

                isoDeadBody.invalidateCorpse();
            }
        }
    }

    @Override
    public boolean isConsistent(IConnection connection) {
        return this.movingObject.isConsistent(connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        this.sendToRelativeClients(PacketTypes.PacketType.ObjectModData, connection, this.movingObject.getObject().getX(), this.movingObject.getObject().getY());
    }
}
