// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.nio.ByteBuffer;
import zombie.iso.RoomDef;

public final class DummyAmbientStreamManager extends BaseAmbientStreamManager {
    @Override
    public void stop() {
    }

    @Override
    public void doAlarm(RoomDef room) {
    }

    @Override
    public void doGunEvent() {
    }

    @Override
    public void handleThunderEvent(int x, int y) {
    }

    @Override
    public void init() {
    }

    @Override
    public void addBlend(String name, float vol, boolean bIndoors, boolean bRain, boolean bNight, boolean bDay) {
    }

    @Override
    protected void addRandomAmbient() {
    }

    @Override
    public void doOneShotAmbients() {
    }

    @Override
    public void update() {
    }

    @Override
    public void addAmbient(String name, int x, int y, int radius, float volume) {
    }

    @Override
    public void addAmbientEmitter(float x, float y, int z, String name) {
    }

    @Override
    public void addDaytimeAmbientEmitter(float x, float y, int z, String name) {
    }

    @Override
    public void save(ByteBuffer bb) {
    }

    @Override
    public void load(ByteBuffer bb, int worldVersion) {
    }

    @Override
    public void checkHaveElectricity() {
    }

    @Override
    public boolean isParameterInsideTrue() {
        return false;
    }
}
