// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.nio.ByteBuffer;
import zombie.iso.RoomDef;

@UsedFromLua
public abstract class BaseAmbientStreamManager {
    public abstract void stop();

    public abstract void doAlarm(RoomDef room);

    public abstract void doGunEvent();

    public abstract void handleThunderEvent(int arg0, int arg1);

    public abstract void init();

    public abstract void addBlend(String name, float vol, boolean bIndoors, boolean bRain, boolean bNight, boolean bDay);

    protected abstract void addRandomAmbient();

    public abstract void doOneShotAmbients();

    public abstract void update();

    public abstract void addAmbient(String name, int x, int y, int radius, float volume);

    public abstract void addAmbientEmitter(float x, float y, int z, String name);

    public abstract void addDaytimeAmbientEmitter(float x, float y, int z, String name);

    public abstract void save(ByteBuffer arg0);

    public abstract void load(ByteBuffer arg0, int arg1);

    public abstract void checkHaveElectricity();

    public abstract boolean isParameterInsideTrue();
}
