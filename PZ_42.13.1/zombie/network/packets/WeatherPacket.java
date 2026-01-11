// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.erosion.ErosionMain;
import zombie.iso.IsoWorld;
import zombie.network.PacketSetting;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class WeatherPacket implements INetworkPacket {
    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        GameTime gt = GameTime.getInstance();
        gt.setDawn(b.get() & 255);
        gt.setDusk(b.get() & 255);
        gt.setThunderDay(b.get() == 1);
        gt.setMoon(b.getFloat());
        gt.setAmbientMin(b.getFloat());
        gt.setAmbientMax(b.getFloat());
        gt.setViewDistMin(b.getFloat());
        gt.setViewDistMax(b.getFloat());
        IsoWorld.instance.setWeather(GameWindow.ReadStringUTF(b));
        ErosionMain.getInstance().receiveState(b);
    }

    @Override
    public void write(ByteBufferWriter b) {
        GameTime gt = GameTime.getInstance();
        b.putByte((byte)gt.getDawn());
        b.putByte((byte)gt.getDusk());
        b.putByte((byte)(gt.isThunderDay() ? 1 : 0));
        b.putFloat(gt.moon);
        b.putFloat(gt.getAmbientMin());
        b.putFloat(gt.getAmbientMax());
        b.putFloat(gt.getViewDistMin());
        b.putFloat(gt.getViewDistMax());
        b.putUTF(IsoWorld.instance.getWeather());
        ErosionMain.getInstance().sendState(b.bb);
    }
}
