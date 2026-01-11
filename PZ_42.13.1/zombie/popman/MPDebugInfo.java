// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.popman;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import zombie.GameTime;
import zombie.WorldSoundManager;
import zombie.core.Color;
import zombie.core.Colors;
import zombie.core.network.ByteBufferWriter;
import zombie.iso.IsoMetaGrid;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.packets.service.ServerDebugInfo;

public final class MPDebugInfo {
    public static final MPDebugInfo instance = new MPDebugInfo();
    private static final ConcurrentHashMap<Long, MPDebugInfo.MPSoundDebugInfo> debugSounds = new ConcurrentHashMap<>();
    public final ArrayList<MPDebugInfo.MPCell> loadedCells = new ArrayList<>();
    public final ObjectPool<MPDebugInfo.MPCell> cellPool = new ObjectPool<>(MPDebugInfo.MPCell::new);
    public final LoadedAreas loadedAreas = new LoadedAreas(false);
    public ArrayList<MPDebugInfo.MPRepopEvent> repopEvents = new ArrayList<>();
    public final ObjectPool<MPDebugInfo.MPRepopEvent> repopEventPool = new ObjectPool<>(MPDebugInfo.MPRepopEvent::new);
    public short repopEpoch;
    public long requestTime;
    private boolean requestFlag;
    public boolean requestPacketReceived;
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
    private static final float RESPAWN_EVERY_HOURS = 1.0F;
    private static final float REPOP_DISPLAY_HOURS = 0.5F;

    private static native boolean n_hasData(boolean var0);

    private static native void n_requestData();

    private static native int n_getLoadedCellsCount();

    private static native int n_getLoadedCellsData(int var0, ByteBuffer var1);

    private static native int n_getLoadedAreasCount();

    private static native int n_getLoadedAreasData(int var0, ByteBuffer var1);

    private static native int n_getRepopEventCount();

    private static native int n_getRepopEventData(int var0, ByteBuffer var1);

    private void requestServerInfo() {
        if (GameClient.client) {
            long currentTimeMS = System.currentTimeMillis();
            if (this.requestTime + 1000L <= currentTimeMS) {
                this.requestTime = currentTimeMS;
                ServerDebugInfo packet = new ServerDebugInfo();
                packet.setRequestServerInfo();
                ByteBufferWriter bbw = GameClient.connection.startPacket();
                PacketTypes.PacketType.ServerDebugInfo.doPacket(bbw);
                packet.write(bbw);
                PacketTypes.PacketType.ServerDebugInfo.send(GameClient.connection);
            }
        }
    }

    public void request() {
        if (GameServer.server) {
            this.requestTime = System.currentTimeMillis();
        }
    }

    private void addRepopEvent(int wx, int wy, float time) {
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();

        while (!this.repopEvents.isEmpty() && this.repopEvents.get(0).worldAge + 0.5F < worldAge) {
            this.repopEventPool.release(this.repopEvents.remove(0));
        }

        this.repopEvents.add(this.repopEventPool.alloc().init(wx, wy, time));
        this.repopEpoch++;
    }

    public void serverUpdate() {
        if (GameServer.server) {
            long currentTimeMS = System.currentTimeMillis();
            if (this.requestTime + 10000L < currentTimeMS) {
                this.requestFlag = false;
                this.requestPacketReceived = false;
            } else {
                if (this.requestFlag) {
                    if (n_hasData(false)) {
                        this.requestFlag = false;
                        this.cellPool.release(this.loadedCells);
                        this.loadedCells.clear();
                        this.loadedAreas.clear();
                        int total = n_getLoadedCellsCount();
                        int offset = 0;

                        while (offset < total) {
                            this.byteBuffer.clear();
                            int count = n_getLoadedCellsData(offset, this.byteBuffer);
                            offset += count;

                            for (int i = 0; i < count; i++) {
                                MPDebugInfo.MPCell cell = this.cellPool.alloc();
                                cell.cx = this.byteBuffer.getShort();
                                cell.cy = this.byteBuffer.getShort();
                                cell.currentPopulation = this.byteBuffer.getShort();
                                cell.desiredPopulation = this.byteBuffer.getShort();
                                cell.lastRepopTime = this.byteBuffer.getFloat();
                                this.loadedCells.add(cell);
                            }
                        }

                        total = n_getLoadedAreasCount();
                        offset = 0;

                        while (offset < total) {
                            this.byteBuffer.clear();
                            int count = n_getLoadedAreasData(offset, this.byteBuffer);
                            offset += count;

                            for (int i = 0; i < count; i++) {
                                boolean serverCell = this.byteBuffer.get() == 0;
                                int x = this.byteBuffer.getShort();
                                int y = this.byteBuffer.getShort();
                                int w = this.byteBuffer.getShort();
                                int h = this.byteBuffer.getShort();
                                this.loadedAreas.add(x, y, w, h);
                            }
                        }
                    }
                } else if (this.requestPacketReceived) {
                    n_requestData();
                    this.requestFlag = true;
                    this.requestPacketReceived = false;
                }

                if (n_hasData(true)) {
                    int total = n_getRepopEventCount();
                    int offset = 0;

                    while (offset < total) {
                        this.byteBuffer.clear();
                        int count = n_getRepopEventData(offset, this.byteBuffer);
                        offset += count;

                        for (int i = 0; i < count; i++) {
                            int wx = this.byteBuffer.getShort();
                            int wy = this.byteBuffer.getShort();
                            float worldAge = this.byteBuffer.getFloat();
                            this.addRepopEvent(wx, wy, worldAge);
                        }
                    }
                }
            }
        }
    }

    boolean isRespawnEnabled() {
        return !IsoWorld.getZombiesDisabled();
    }

    public void render(ZombiePopulationRenderer renderer, float zoom) {
        this.requestServerInfo();
        float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
        IsoMetaGrid metaGrid = IsoWorld.instance.metaGrid;
        renderer.outlineRect(
            metaGrid.minX * 256 * 1.0F,
            metaGrid.minY * 256 * 1.0F,
            (metaGrid.maxX - metaGrid.minX + 1) * 256 * 1.0F,
            (metaGrid.maxY - metaGrid.minY + 1) * 256 * 1.0F,
            1.0F,
            1.0F,
            1.0F,
            0.25F
        );

        for (int i = 0; i < this.loadedCells.size(); i++) {
            MPDebugInfo.MPCell mpCell = this.loadedCells.get(i);
            renderer.outlineRect(mpCell.cx * 256, mpCell.cy * 256, 256.0F, 256.0F, 1.0F, 1.0F, 1.0F, 0.25F);
            if (this.isRespawnEnabled()) {
                float delta = Math.min(worldAge - mpCell.lastRepopTime, 1.0F) / 1.0F;
                if (mpCell.lastRepopTime > worldAge) {
                    delta = 0.0F;
                }

                renderer.outlineRect(mpCell.cx * 256 + 1, mpCell.cy * 256 + 1, 254.0F, 254.0F, 0.0F, 1.0F, 0.0F, delta * delta);
            }
        }

        for (int ix = 0; ix < this.loadedAreas.count; ix++) {
            int n = ix * 4;
            int ax = this.loadedAreas.areas[n++];
            int ay = this.loadedAreas.areas[n++];
            int aw = this.loadedAreas.areas[n++];
            int ah = this.loadedAreas.areas[n++];
            renderer.outlineRect(ax * 8, ay * 8, aw * 8, ah * 8, 0.7F, 0.7F, 0.7F, 1.0F);
        }

        for (int ix = 0; ix < this.repopEvents.size(); ix++) {
            MPDebugInfo.MPRepopEvent evt = this.repopEvents.get(ix);
            if (!(evt.worldAge + 0.5F < worldAge)) {
                float alpha = 1.0F - (worldAge - evt.worldAge) / 0.5F;
                alpha = Math.max(alpha, 0.1F);
                renderer.outlineRect(evt.wx * 8, evt.wy * 8, 40.0F, 40.0F, 0.0F, 0.0F, 1.0F, alpha);
            }
        }

        if (zoom > 0.25F) {
            for (int ixx = 0; ixx < this.loadedCells.size(); ixx++) {
                MPDebugInfo.MPCell cell = this.loadedCells.get(ixx);
                renderer.renderCellInfo(cell.cx, cell.cy, cell.currentPopulation, cell.desiredPopulation, cell.lastRepopTime + 1.0F - worldAge);
            }
        }

        try {
            debugSounds.entrySet().removeIf(sound -> System.currentTimeMillis() > sound.getKey() + 1000L);

            for (Entry<Long, MPDebugInfo.MPSoundDebugInfo> entry : debugSounds.entrySet()) {
                Color c = Colors.LightBlue;
                if (entry.getValue().sourceIsZombie) {
                    c = Colors.GreenYellow;
                } else if (entry.getValue().repeating) {
                    c = Colors.Coral;
                }

                float a = 1.0F - Math.max(0.0F, Math.min(1.0F, (float)(System.currentTimeMillis() - entry.getKey()) / 1000.0F));
                renderer.renderCircle(entry.getValue().x, entry.getValue().y, entry.getValue().radius, c.r, c.g, c.b, a);
            }
        } catch (Exception var11) {
        }
    }

    public static void AddDebugSound(WorldSoundManager.WorldSound sound) {
        try {
            debugSounds.put(System.currentTimeMillis(), new MPDebugInfo.MPSoundDebugInfo(sound));
        } catch (Exception var2) {
        }
    }

    public static final class MPCell {
        public short cx;
        public short cy;
        public short currentPopulation;
        public short desiredPopulation;
        public float lastRepopTime;

        MPDebugInfo.MPCell init(int cx, int cy, int currentPopulation, int desiredPopulation, float lastRepopTime) {
            this.cx = (short)cx;
            this.cy = (short)cy;
            this.currentPopulation = (short)currentPopulation;
            this.desiredPopulation = (short)desiredPopulation;
            this.lastRepopTime = lastRepopTime;
            return this;
        }
    }

    public static final class MPRepopEvent {
        public int wx;
        public int wy;
        public float worldAge;

        public MPDebugInfo.MPRepopEvent init(int wx, int wy, float worldAge) {
            this.wx = wx;
            this.wy = wy;
            this.worldAge = worldAge;
            return this;
        }
    }

    private static class MPSoundDebugInfo {
        int x;
        int y;
        int radius;
        boolean repeating;
        boolean sourceIsZombie;

        MPSoundDebugInfo(WorldSoundManager.WorldSound sound) {
            this.x = sound.x;
            this.y = sound.y;
            this.radius = sound.radius;
            this.repeating = sound.repeating;
            this.sourceIsZombie = sound.sourceIsZombie;
        }
    }
}
