// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import fmod.fmod.Audio;
import zombie.SoundManager;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.sprite.IsoSprite;

@UsedFromLua
public class IsoJukebox extends IsoObject {
    private Audio jukeboxTrack;
    private boolean isPlaying;
    private final float musicRadius = 30.0F;
    private boolean activated;
    private final int worldSoundPulseRate = 150;
    private int worldSoundPulseDelay;

    public IsoJukebox(IsoCell cell, IsoGridSquare sq, IsoSprite spr) {
        super(cell, sq, spr);
    }

    @Override
    public String getObjectName() {
        return "Jukebox";
    }

    public IsoJukebox(IsoCell cell) {
        super(cell);
    }

    public IsoJukebox(IsoCell cell, IsoGridSquare sq, String gid) {
        super(cell, sq, gid);
        this.jukeboxTrack = null;
        this.isPlaying = false;
        this.activated = false;
        this.worldSoundPulseDelay = 0;
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.getCell().addToStaticUpdaterObjectList(this);
    }

    public void SetPlaying(boolean shouldPlay) {
        if (this.isPlaying != shouldPlay) {
            this.isPlaying = shouldPlay;
            if (this.isPlaying && this.jukeboxTrack == null) {
                String trackname = null;
                switch (Rand.Next(4)) {
                    case 0:
                        trackname = "paws1";
                        break;
                    case 1:
                        trackname = "paws2";
                        break;
                    case 2:
                        trackname = "paws3";
                        break;
                    case 3:
                        trackname = "paws4";
                }

                this.jukeboxTrack = SoundManager.instance.PlaySound(trackname, false, 0.0F);
            }
        }
    }

    @Override
    public boolean onMouseLeftClick(int x, int y) {
        IsoPlayer player = IsoPlayer.getInstance();
        if (player == null || player.isDead()) {
            return false;
        } else if (IsoPlayer.getInstance().getCurrentSquare() == null) {
            return false;
        } else {
            float distRatioFromSource = 0.0F;
            int dist = Math.abs(this.square.getX() - IsoPlayer.getInstance().getCurrentSquare().getX())
                + Math.abs(
                    this.square.getY()
                        - IsoPlayer.getInstance().getCurrentSquare().getY()
                        + Math.abs(this.square.getZ() - IsoPlayer.getInstance().getCurrentSquare().getZ())
                );
            if (dist < 4) {
                if (!this.activated) {
                    if (Core.numJukeBoxesActive < Core.maxJukeBoxesActive) {
                        this.worldSoundPulseDelay = 0;
                        this.activated = true;
                        this.SetPlaying(true);
                        Core.numJukeBoxesActive++;
                    }
                } else {
                    this.worldSoundPulseDelay = 0;
                    this.SetPlaying(false);
                    this.activated = false;
                    if (this.jukeboxTrack != null) {
                        SoundManager.instance.StopSound(this.jukeboxTrack);
                        this.jukeboxTrack.stop();
                        this.jukeboxTrack = null;
                    }

                    Core.numJukeBoxesActive--;
                }
            }

            return true;
        }
    }

    @Override
    public void update() {
        if (IsoPlayer.getInstance() != null) {
            if (IsoPlayer.getInstance().getCurrentSquare() != null) {
                if (this.activated) {
                    float distRatioFromSource = 0.0F;
                    int dist = Math.abs(this.square.getX() - IsoPlayer.getInstance().getCurrentSquare().getX())
                        + Math.abs(
                            this.square.getY()
                                - IsoPlayer.getInstance().getCurrentSquare().getY()
                                + Math.abs(this.square.getZ() - IsoPlayer.getInstance().getCurrentSquare().getZ())
                        );
                    if (dist < 30.0F) {
                        this.SetPlaying(true);
                        distRatioFromSource = (30.0F - dist) / 30.0F;
                    }

                    if (this.jukeboxTrack != null) {
                        SoundManager.instance.BlendVolume(this.jukeboxTrack, distRatioFromSource);
                        if (this.worldSoundPulseDelay > 0) {
                            this.worldSoundPulseDelay--;
                        }

                        if (this.worldSoundPulseDelay == 0) {
                            WorldSoundManager.instance
                                .addSound(IsoPlayer.getInstance(), this.square.getX(), this.square.getY(), this.square.getZ(), 70, 70, true);
                            this.worldSoundPulseDelay = 150;
                        }

                        if (!this.jukeboxTrack.isPlaying()) {
                            this.worldSoundPulseDelay = 0;
                            this.SetPlaying(false);
                            this.activated = false;
                            if (this.jukeboxTrack != null) {
                                SoundManager.instance.StopSound(this.jukeboxTrack);
                                this.jukeboxTrack.stop();
                                this.jukeboxTrack = null;
                            }

                            Core.numJukeBoxesActive--;
                        }
                    }
                }
            }
        }
    }
}
