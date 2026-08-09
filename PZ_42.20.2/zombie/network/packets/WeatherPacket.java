// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import zombie.GameTime;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferReader;
import zombie.core.network.ByteBufferWriter;
import zombie.erosion.ErosionMain;
import zombie.iso.IsoWorld;
import zombie.network.IConnection;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class WeatherPacket implements INetworkPacket {
    @Override
    public void parse(ByteBufferReader b, IConnection connection) {
        GameTime gt = GameTime.getInstance();
        gt.setDawn(b.getByte() & 255);
        gt.setDusk(b.getByte() & 255);
        gt.setThunderDay(b.getBoolean());
        gt.setMoon(b.getFloat());
        gt.setAmbientMin(b.getFloat());
        gt.setAmbientMax(b.getFloat());
        gt.setViewDistMin(b.getFloat());
        gt.setViewDistMax(b.getFloat());
        IsoWorld.instance.setWeather(b.getUTF());
        ErosionMain.getInstance().receiveState(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameTime gt = GameTime.getInstance();
        b.putByte(gt.getDawn());
        b.putByte(gt.getDusk());
        b.putBoolean(gt.isThunderDay());
        b.putFloat(gt.moon);
        b.putFloat(gt.getAmbientMin());
        b.putFloat(gt.getAmbientMax());
        b.putFloat(gt.getViewDistMin());
        b.putFloat(gt.getViewDistMax());
        b.putUTF(IsoWorld.instance.getWeather());
        ErosionMain.getInstance().sendState(b);
    }
}
