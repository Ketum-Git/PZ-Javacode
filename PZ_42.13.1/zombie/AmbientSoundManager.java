// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.debug.DebugType;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.RoomDef;
import zombie.iso.Vector2;
import zombie.network.GameServer;

public final class AmbientSoundManager extends BaseAmbientStreamManager {
    public final ArrayList<AmbientSoundManager.Ambient> ambient = new ArrayList<>();
    private final Vector2 tempo = new Vector2();
    private int electricityShutOffState = -1;
    private long electricityShutOffTime;
    public boolean initialized;

    @Override
    public void update() {
        if (this.initialized) {
            this.updatePowerSupply();
            this.doOneShotAmbients();
        }
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
    public void doOneShotAmbients() {
        for (int n = 0; n < this.ambient.size(); n++) {
            AmbientSoundManager.Ambient a = this.ambient.get(n);
            if (a.finished()) {
                DebugLog.log(DebugType.Sound, "ambient: removing ambient sound " + a.name);
                this.ambient.remove(n--);
            } else {
                a.update();
            }
        }
    }

    @Override
    public void init() {
        if (!this.initialized) {
            this.initialized = true;
        }
    }

    @Override
    public void addBlend(String name, float vol, boolean bIndoors, boolean bRain, boolean bNight, boolean bDay) {
    }

    @Override
    protected void addRandomAmbient() {
        if (!GameServer.Players.isEmpty()) {
            IsoPlayer player = GameServer.Players.get(Rand.Next(GameServer.Players.size()));
            if (player != null) {
                String sound = null;
                if (GameTime.instance.getHour() > 7 && GameTime.instance.getHour() < 21) {
                    switch (Rand.Next(3)) {
                        case 0:
                            if (Rand.Next(10) < 2) {
                                sound = "MetaDogBark";
                            }
                            break;
                        case 1:
                            if (Rand.Next(10) < 3) {
                                sound = "MetaScream";
                            }
                    }
                } else {
                    switch (Rand.Next(5)) {
                        case 0:
                            if (Rand.Next(10) < 2) {
                                sound = "MetaDogBark";
                            }
                            break;
                        case 1:
                            if (Rand.Next(13) < 3) {
                                sound = "MetaScream";
                            }
                            break;
                        case 2:
                            sound = "MetaOwl";
                            break;
                        case 3:
                            sound = "MetaWolfHowl";
                    }
                }

                if (sound != null) {
                    float x = player.getX();
                    float y = player.getY();
                    double radians = Rand.Next((float) -Math.PI, (float) Math.PI);
                    this.tempo.x = (float)Math.cos(radians);
                    this.tempo.y = (float)Math.sin(radians);
                    this.tempo.setLength(1000.0F);
                    x += this.tempo.x;
                    y += this.tempo.y;
                    AmbientSoundManager.Ambient a = new AmbientSoundManager.Ambient(sound, x, y, 50.0F, Rand.Next(0.2F, 0.5F));
                    this.ambient.add(a);
                    GameServer.sendAmbient(sound, PZMath.fastfloor(x), PZMath.fastfloor(y), 50, Rand.Next(0.2F, 0.5F));
                }
            }
        }
    }

    @Override
    public void doGunEvent() {
        ArrayList<IsoPlayer> players = GameServer.getPlayers();
        if (!players.isEmpty()) {
            IsoPlayer player = players.get(Rand.Next(players.size()));
            String weaponFire = null;
            float x = player.getX();
            float y = player.getY();
            int worldSoundRadius = 600;
            double radians = Rand.Next((float) -Math.PI, (float) Math.PI);
            this.tempo.x = (float)Math.cos(radians);
            this.tempo.y = (float)Math.sin(radians);
            this.tempo.setLength(500.0F);
            x += this.tempo.x;
            y += this.tempo.y;
            WorldSoundManager.instance.addSound(null, PZMath.fastfloor(x) + Rand.Next(-10, 10), PZMath.fastfloor(y) + Rand.Next(-10, 10), 0, 600, 600);
            switch (Rand.Next(6)) {
                case 0:
                    weaponFire = "MetaAssaultRifle1";
                    break;
                case 1:
                    weaponFire = "MetaPistol1";
                    break;
                case 2:
                    weaponFire = "MetaShotgun1";
                    break;
                case 3:
                    weaponFire = "MetaPistol2";
                    break;
                case 4:
                    weaponFire = "MetaPistol3";
                    break;
                case 5:
                    weaponFire = "MetaShotgun1";
            }

            float gain = 1.0F;
            AmbientSoundManager.Ambient a = new AmbientSoundManager.Ambient(weaponFire, x, y, 700.0F, 1.0F);
            this.ambient.add(a);
            GameServer.sendAmbient(weaponFire, PZMath.fastfloor(x), PZMath.fastfloor(y), (int)Math.ceil(a.radius), a.volume);
        }
    }

    @Override
    public void doAlarm(RoomDef room) {
        if (room != null && room.building != null) {
            IsoGridSquare sq = IsoWorld.instance.currentCell.getGridSquare(room.x, room.y, 0);
            if (sq != null && sq.hasGridPower(room.building.alarmDecay)) {
                float gain = 1.0F;
                AmbientSoundManager.Ambient a = new AmbientSoundManager.Ambient("burglar2", room.x + room.getW() / 2, room.y + room.getH() / 2, 700.0F, 1.0F);
                a.duration = 49;
                a.worldSoundDelay = 3;
                this.ambient.add(a);
                GameServer.sendAlarm(room.x + room.getW() / 2, room.y + room.getH() / 2);
            }

            room.building.alarmed = false;
            room.building.setAllExplored(true);
        }
    }

    private int GetDistance(int dx, int dy, int sx, int sy) {
        return (int)Math.sqrt(Math.pow(dx - sx, 2.0) + Math.pow(dy - sy, 2.0));
    }

    @Override
    public void handleThunderEvent(int x, int y) {
        int dist = 9999999;
        ArrayList<IsoPlayer> players = GameServer.getPlayers();
        if (!players.isEmpty()) {
            for (int i = 0; i < players.size(); i++) {
                IsoPlayer player = players.get(i);
                if (player != null && player.isAlive()) {
                    int pdist = this.GetDistance((int)player.getX(), (int)player.getY(), x, y);
                    if (pdist < dist) {
                        dist = pdist;
                    }
                }
            }

            int worldSoundRadius = 5000;
            if (dist <= 5000) {
                WorldSoundManager.instance.addSound(null, PZMath.fastfloor((float)x), PZMath.fastfloor((float)y), 0, 5000, 5000);
                AmbientSoundManager.Ambient a = new AmbientSoundManager.Ambient("", x, y, 5100.0F, 1.0F);
                this.ambient.add(a);
                GameServer.sendAmbient("", PZMath.fastfloor((float)x), PZMath.fastfloor((float)y), (int)Math.ceil(a.radius), a.volume);
            }
        }
    }

    @Override
    public void stop() {
        this.ambient.clear();
        this.initialized = false;
    }

    @Override
    public void save(ByteBuffer bb) {
    }

    @Override
    public void load(ByteBuffer bb, int worldVersion) {
    }

    private void updatePowerSupply() {
        boolean bPowerOn = SandboxOptions.instance.doesPowerGridExist();
        if (this.electricityShutOffState == -1) {
            IsoWorld.instance.setHydroPowerOn(bPowerOn);
        }

        if (this.electricityShutOffState == 0) {
            if (bPowerOn) {
                IsoWorld.instance.setHydroPowerOn(true);
                this.checkHaveElectricity();
                this.electricityShutOffTime = 0L;
            } else if (this.electricityShutOffTime != 0L && System.currentTimeMillis() >= this.electricityShutOffTime) {
                this.electricityShutOffTime = 0L;
                IsoWorld.instance.setHydroPowerOn(false);
                this.checkHaveElectricity();
            }
        }

        if (this.electricityShutOffState == 1 && !bPowerOn) {
            this.electricityShutOffTime = System.currentTimeMillis() + 2650L;
        }

        this.electricityShutOffState = bPowerOn ? 1 : 0;
    }

    @Override
    public void checkHaveElectricity() {
    }

    @Override
    public boolean isParameterInsideTrue() {
        return false;
    }

    public class Ambient {
        public float x;
        public float y;
        public String name;
        public float radius;
        public float volume;
        long startTime;
        public int duration;
        public int worldSoundDelay;

        public Ambient(final String name, final float x, final float y, final float radius, final float volume) {
            Objects.requireNonNull(AmbientSoundManager.this);
            super();
            this.name = name;
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.volume = volume;
            this.startTime = System.currentTimeMillis() / 1000L;
            this.duration = 2;
            this.update();
            LuaEventManager.triggerEvent("OnAmbientSound", name, x, y);
        }

        public boolean finished() {
            long time = System.currentTimeMillis() / 1000L;
            return time - this.startTime >= this.duration;
        }

        public void update() {
            long time = System.currentTimeMillis() / 1000L;
            if (time - this.startTime >= this.worldSoundDelay) {
                WorldSoundManager.instance.addSound(null, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), 0, 600, 600);
            }
        }
    }
}
