// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import fmod.javafmod;
import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.WorldSoundManager;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterTrait;

public class Helicopter {
    private static final float MAX_BOTHER_SECONDS = 60.0F;
    private static final float MAX_UNSEEN_SECONDS = 15.0F;
    private static final int RADIUS_HOVER = 50;
    private static final int RADIUS_SEARCH = 100;
    protected Helicopter.State state;
    public IsoGameCharacter target;
    protected float timeSinceChopperSawPlayer;
    protected float hoverTime;
    protected float searchTime;
    public float x;
    public float y;
    protected float targetX;
    protected float targetY;
    protected Vector2 move = new Vector2();
    protected boolean active;
    protected static long inst;
    protected static FMOD_STUDIO_EVENT_DESCRIPTION event;
    protected boolean soundStarted;
    protected float volume;
    protected float occlusion;

    public void pickRandomTarget() {
        ArrayList<IsoPlayer> players;
        if (GameServer.server) {
            players = GameServer.getPlayers();
        } else {
            if (GameClient.client) {
                throw new IllegalStateException("can't call this on the client");
            }

            players = new ArrayList<>();

            for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player != null && player.isAlive()) {
                    players.add(player);
                }
            }
        }

        if (players.isEmpty()) {
            this.active = false;
            this.target = null;
        } else {
            this.setTarget(players.get(Rand.Next(players.size())));
        }
    }

    public void setTarget(IsoGameCharacter chr) {
        this.target = chr;
        this.x = this.target.getX() + 1000.0F;
        this.y = this.target.getY() + 1000.0F;
        this.targetX = this.target.getX();
        this.targetY = this.target.getY();
        this.move.x = this.targetX - this.x;
        this.move.y = this.targetY - this.y;
        this.move.normalize();
        this.move.setLength(0.5F);
        this.state = Helicopter.State.Arriving;
        this.active = true;
        DebugLog.log("chopper: activated");
    }

    protected void changeState(Helicopter.State newState) {
        DebugLog.log("chopper: state " + this.state + " -> " + newState);
        this.state = newState;
    }

    public void update() {
        if (this.active) {
            if (GameClient.client) {
                this.updateSound();
                this.checkMusicIntensityEvent();
            } else {
                float timeMulti = 1.0F;
                if (GameServer.server) {
                    if (!GameServer.Players.contains(this.target)) {
                        this.target = null;
                    }
                } else {
                    timeMulti = GameTime.getInstance().getTrueMultiplier();
                }

                switch (this.state) {
                    case Arriving:
                        if (this.target != null && !this.target.isDead()) {
                            if (IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 4.0F) {
                                this.changeState(Helicopter.State.Hovering);
                                this.hoverTime = 0.0F;
                                this.searchTime = 0.0F;
                                this.timeSinceChopperSawPlayer = 0.0F;
                            } else {
                                this.targetX = this.target.getX();
                                this.targetY = this.target.getY();
                                this.move.x = this.targetX - this.x;
                                this.move.y = this.targetY - this.y;
                                this.move.normalize();
                                this.move.setLength(0.75F);
                            }
                        } else {
                            this.changeState(Helicopter.State.Leaving);
                        }
                        break;
                    case Hovering:
                        if (this.target != null && !this.target.isDead()) {
                            this.hoverTime = this.hoverTime + GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * timeMulti;
                            if (this.hoverTime + this.searchTime > 60.0F) {
                                this.changeState(Helicopter.State.Leaving);
                            } else {
                                if (!this.isTargetVisible()) {
                                    this.timeSinceChopperSawPlayer = this.timeSinceChopperSawPlayer
                                        + GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * timeMulti;
                                    if (this.timeSinceChopperSawPlayer > 15.0F) {
                                        this.changeState(Helicopter.State.Searching);
                                        break;
                                    }
                                }

                                if (IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 1.0F) {
                                    this.targetX = this.target.getX() + (Rand.Next(100) - 50);
                                    this.targetY = this.target.getY() + (Rand.Next(100) - 50);
                                    this.move.x = this.targetX - this.x;
                                    this.move.y = this.targetY - this.y;
                                    this.move.normalize();
                                    this.move.setLength(0.5F);
                                }
                            }
                        } else {
                            this.changeState(Helicopter.State.Leaving);
                        }
                        break;
                    case Searching:
                        if (this.target != null && !this.target.isDead()) {
                            this.searchTime = this.searchTime + GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * timeMulti;
                            if (this.hoverTime + this.searchTime > 60.0F) {
                                this.changeState(Helicopter.State.Leaving);
                            } else if (this.isTargetVisible()) {
                                this.timeSinceChopperSawPlayer = 0.0F;
                                this.changeState(Helicopter.State.Hovering);
                            } else if (IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 1.0F) {
                                this.targetX = this.target.getX() + (Rand.Next(200) - 100);
                                this.targetY = this.target.getY() + (Rand.Next(200) - 100);
                                this.move.x = this.targetX - this.x;
                                this.move.y = this.targetY - this.y;
                                this.move.normalize();
                                this.move.setLength(0.5F);
                            }
                        } else {
                            this.state = Helicopter.State.Leaving;
                        }
                        break;
                    case Leaving:
                        boolean listener = false;
                        if (GameServer.server) {
                            ArrayList<IsoPlayer> players = GameServer.getPlayers();

                            for (int i = 0; i < players.size(); i++) {
                                IsoPlayer player = players.get(i);
                                if (IsoUtils.DistanceToSquared(this.x, this.y, player.getX(), player.getY()) < 1000000.0F) {
                                    listener = true;
                                    break;
                                }
                            }
                        } else {
                            for (int ix = 0; ix < IsoPlayer.numPlayers; ix++) {
                                IsoPlayer player = IsoPlayer.players[ix];
                                if (player != null && IsoUtils.DistanceToSquared(this.x, this.y, player.getX(), player.getY()) < 1000000.0F) {
                                    listener = true;
                                    break;
                                }
                            }
                        }

                        if (!listener) {
                            this.deactivate();
                            return;
                        }
                }

                if (Rand.Next(Rand.AdjustForFramerate(300)) == 0) {
                    WorldSoundManager.instance.addSound(null, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), 0, 500, 500);
                }

                float dx = this.move.x * GameTime.getInstance().getThirtyFPSMultiplier();
                float dy = this.move.y * GameTime.getInstance().getThirtyFPSMultiplier();
                if (this.state != Helicopter.State.Leaving
                    && IsoUtils.DistanceToSquared(this.x + dx, this.y + dy, this.targetX, this.targetY)
                        > IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY)) {
                    this.x = this.targetX;
                    this.y = this.targetY;
                } else {
                    this.x += dx;
                    this.y += dy;
                }

                if (GameServer.server) {
                    GameServer.sendHelicopter(this.x, this.y, this.active);
                }

                this.updateSound();
                this.checkMusicIntensityEvent();
            }
        }
    }

    protected void updateSound() {
        if (!GameServer.server) {
            if (!Core.soundDisabled) {
                if (FMODManager.instance.getNumListeners() != 0) {
                    GameSound gameSound = GameSounds.getSound("Helicopter");
                    if (gameSound != null && !gameSound.clips.isEmpty()) {
                        if (inst == 0L) {
                            GameSoundClip clip = gameSound.getRandomClip();
                            event = clip.eventDescription;
                            if (event != null) {
                                javafmod.FMOD_Studio_LoadEventSampleData(event.address);
                                inst = javafmod.FMOD_Studio_System_CreateEventInstance(event.address);
                            }
                        }

                        if (inst != 0L) {
                            float volume = SoundManager.instance.getSoundVolume();
                            volume = 1.0F;
                            volume *= gameSound.getUserVolume();
                            if (volume != this.volume) {
                                javafmod.FMOD_Studio_EventInstance_SetVolume(inst, volume);
                                this.volume = volume;
                            }

                            javafmod.FMOD_Studio_EventInstance3D(inst, this.x, this.y, 200.0F);
                            float occlusion = 0.0F;
                            if (IsoPlayer.numPlayers == 1) {
                                IsoGridSquare sqPlayer = IsoPlayer.getInstance().getCurrentSquare();
                                if (sqPlayer != null && !sqPlayer.has(IsoFlagType.exterior)) {
                                    occlusion = 1.0F;
                                }
                            }

                            if (this.occlusion != occlusion) {
                                this.occlusion = occlusion;
                                javafmod.FMOD_Studio_EventInstance_SetParameterByName(inst, "Occlusion", this.occlusion);
                            }

                            if (!this.soundStarted) {
                                javafmod.FMOD_Studio_StartEvent(inst);
                                this.soundStarted = true;
                            }
                        }
                    }
                }
            }
        }
    }

    protected boolean isTargetVisible() {
        if (this.target != null && !this.target.isDead()) {
            IsoGridSquare sq = this.target.getCurrentSquare();
            if (sq == null) {
                return false;
            } else if (!sq.getProperties().has(IsoFlagType.exterior)) {
                return false;
            } else {
                Zone zone = sq.getZone();
                return zone == null ? true : !"Forest".equals(zone.getType()) && !"DeepForest".equals(zone.getType());
            }
        } else {
            return false;
        }
    }

    public void deactivate() {
        if (this.active) {
            this.active = false;
            if (this.soundStarted) {
                javafmod.FMOD_Studio_EventInstance_Stop(inst, false);
                this.soundStarted = false;
            }

            if (GameServer.server) {
                GameServer.sendHelicopter(this.x, this.y, this.active);
            }

            DebugLog.log("chopper: deactivated");
        }
    }

    public boolean isActive() {
        return this.active;
    }

    public void clientSync(float x, float y, boolean active) {
        if (GameClient.client) {
            this.x = x;
            this.y = y;
            if (!active) {
                this.deactivate();
            }

            this.active = active;
        }
    }

    public void save(ByteBuffer bb) {
        bb.put((byte)(this.active ? 1 : 0));
        bb.putInt(this.state == null ? 0 : this.state.ordinal());
        bb.putFloat(this.x);
        bb.putFloat(this.y);
    }

    public void load(ByteBuffer bb, int worldVersion) {
        this.active = bb.get() == 1;
        this.state = Helicopter.State.values()[bb.getInt()];
        this.x = bb.getFloat();
        this.y = bb.getFloat();
        DebugLog.General.debugln("Re-Initializing Chopper %s", this.active);
        if (this.active && !GameServer.server && !GameClient.client) {
            this.target = IsoPlayer.players[0];
            if (this.target == null) {
                this.active = false;
            } else {
                this.targetX = this.target.getX();
                this.targetY = this.target.getY();
                DebugLog.General.debugln("target at %.4f/%.4f", this.targetX, this.targetY);
                this.move.x = this.targetX - this.x;
                this.move.y = this.targetY - this.y;
                this.move.normalize();
                this.move.setLength(0.5F);
            }
        }
    }

    private void checkMusicIntensityEvent() {
        if (!GameServer.server) {
            if (this.active) {
                for (int i = 0; i < IsoPlayer.numPlayers; i++) {
                    IsoPlayer player = IsoPlayer.players[i];
                    if (player != null && !player.hasTrait(CharacterTrait.DEAF) && !player.isDead()) {
                        float distSq = IsoUtils.DistanceToSquared(this.x, this.y, player.getX(), player.getY());
                        if (!(distSq > 2500.0F)) {
                            player.triggerMusicIntensityEvent("HelicopterOverhead");
                            break;
                        }
                    }
                }
            }
        }
    }

    private static enum State {
        Arriving,
        Hovering,
        Searching,
        Leaving;
    }
}
