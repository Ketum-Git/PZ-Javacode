// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoWorld;
import zombie.iso.LightingJNI;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;

@UsedFromLua
public class IsoTelevision extends IsoWaveSignal {
    private IsoTelevision.Screens currentScreen = IsoTelevision.Screens.OFFSCREEN;
    private int spriteIndex;
    private boolean hasSetupScreens;
    private boolean tickIsLightUpdate;
    protected ArrayList<IsoSprite> screenSprites = new ArrayList<>();
    private IsoSprite cacheObjectSprite;
    protected IsoDirections facing = IsoDirections.Max;

    public IsoTelevision(IsoCell cell) {
        super(cell);
    }

    public IsoTelevision(IsoCell cell, IsoGridSquare sq, IsoSprite spr) {
        super(cell, sq, spr);
    }

    @Override
    public String getObjectName() {
        return "Television";
    }

    @Override
    protected void init(boolean objectFromBinary) {
        super.init(objectFromBinary);
    }

    private void setupDefaultScreens() {
        this.hasSetupScreens = true;
        this.cacheObjectSprite = this.sprite;
        if (this.screenSprites.isEmpty()) {
            for (int i = 16; i <= 64; i += 16) {
                IsoSprite sprite = IsoSprite.getSprite(IsoSpriteManager.instance, this.sprite.getName(), i);
                if (sprite != null) {
                    this.addTvScreenSprite(sprite);
                }
            }
        }

        this.facing = IsoDirections.Max;
        if (this.sprite != null && this.sprite.getProperties().has("Facing")) {
            String Facing = this.sprite.getProperties().get("Facing");
            switch (Facing) {
                case "N":
                    this.facing = IsoDirections.N;
                    break;
                case "S":
                    this.facing = IsoDirections.S;
                    break;
                case "W":
                    this.facing = IsoDirections.W;
                    break;
                case "E":
                    this.facing = IsoDirections.E;
            }
        }
    }

    @Override
    public void update() {
        super.update();
        if (this.cacheObjectSprite != null && this.cacheObjectSprite != this.sprite) {
            this.hasSetupScreens = false;
            this.screenSprites.clear();
            this.currentScreen = IsoTelevision.Screens.OFFSCREEN;
            this.nextLightUpdate = 0.0F;
        }

        if (!this.hasSetupScreens) {
            this.setupDefaultScreens();
        }

        this.updateTvScreen();
    }

    @Override
    protected void updateLightSource() {
        this.tickIsLightUpdate = false;
        if (this.lightSource == null) {
            this.lightSource = new IsoLightSource(this.square.getX(), this.square.getY(), this.square.getZ(), 0.0F, 0.0F, 1.0F, this.lightSourceRadius);
            this.lightWasRemoved = true;
        }

        if (this.lightWasRemoved) {
            IsoWorld.instance.currentCell.addLamppost(this.lightSource);
            IsoGridSquare.recalcLightTime = -1.0F;
            Core.dirtyGlobalLightsCount++;
            GameTime.instance.lightSourceUpdate = 100.0F;
            this.lightWasRemoved = false;
        }

        this.lightUpdateCnt = this.lightUpdateCnt + GameTime.getInstance().getMultiplier();
        if (this.lightUpdateCnt >= this.nextLightUpdate) {
            float interval = 300.0F;
            float intensBase = 0.0F;
            if (!this.hasChatToDisplay()) {
                intensBase = 0.6F;
                interval = Rand.Next(200.0F, 400.0F);
            } else {
                interval = Rand.Next(15.0F, 300.0F);
            }

            float intensity = Rand.Next(intensBase, 1.0F);
            this.tickIsLightUpdate = true;
            float r = 0.58F + 0.25F * intensity;
            float gb = Rand.Next(0.65F, 0.85F);
            int radius = 1 + (int)((this.lightSourceRadius - 1) * intensity);
            IsoGridSquare.recalcLightTime = -1.0F;
            GameTime.instance.lightSourceUpdate = 100.0F;
            this.lightSource.setRadius(radius);
            this.lightSource.setR(r);
            this.lightSource.setG(gb);
            this.lightSource.setB(gb);
            if (LightingJNI.init && this.lightSource.id != 0) {
                LightingJNI.setLightColor(this.lightSource.id, this.lightSource.getR(), this.lightSource.getG(), this.lightSource.getB());
            }

            this.lightUpdateCnt = 0.0F;
            this.nextLightUpdate = interval;
        }
    }

    private void setScreen(IsoTelevision.Screens screen) {
        if (screen == IsoTelevision.Screens.OFFSCREEN) {
            this.currentScreen = IsoTelevision.Screens.OFFSCREEN;
            if (this.overlaySprite != null) {
                this.overlaySprite = null;
            }
        } else {
            if (this.currentScreen != screen || screen == IsoTelevision.Screens.ALTERNATESCREEN) {
                this.currentScreen = screen;
                IsoSprite sprite = null;
                switch (screen) {
                    case TESTSCREEN:
                        if (!this.screenSprites.isEmpty()) {
                            sprite = this.screenSprites.get(0);
                        }
                        break;
                    case DEFAULTSCREEN:
                        if (this.screenSprites.size() > 1) {
                            sprite = this.screenSprites.get(1);
                        }
                        break;
                    case ALTERNATESCREEN:
                        if (this.screenSprites.size() >= 2) {
                            if (this.screenSprites.size() == 2) {
                                sprite = this.screenSprites.get(1);
                            } else {
                                this.spriteIndex++;
                                if (this.spriteIndex < 1) {
                                    this.spriteIndex = 1;
                                }

                                if (this.spriteIndex > this.screenSprites.size() - 1) {
                                    this.spriteIndex = 1;
                                }

                                sprite = this.screenSprites.get(this.spriteIndex);
                            }
                        }
                }

                this.overlaySprite = sprite;
            }
        }
    }

    protected void updateTvScreen() {
        IsoSprite overlaySprite1 = this.overlaySprite;
        if (this.deviceData != null && this.deviceData.getIsTurnedOn() && !this.screenSprites.isEmpty()) {
            if (!this.deviceData.isReceivingSignal() && !this.deviceData.isPlayingMedia()) {
                this.setScreen(IsoTelevision.Screens.TESTSCREEN);
            } else if (this.tickIsLightUpdate || this.currentScreen != IsoTelevision.Screens.ALTERNATESCREEN) {
                this.setScreen(IsoTelevision.Screens.ALTERNATESCREEN);
            }
        } else if (this.currentScreen != IsoTelevision.Screens.OFFSCREEN) {
            this.setScreen(IsoTelevision.Screens.OFFSCREEN);
        }

        if (overlaySprite1 != this.overlaySprite) {
            this.invalidateRenderChunkLevel(256L);
        }
    }

    public void addTvScreenSprite(IsoSprite sprite) {
        this.screenSprites.add(sprite);
    }

    public void clearTvScreenSprites() {
        this.screenSprites.clear();
    }

    public void removeTvScreenSprite(IsoSprite sprite) {
        this.screenSprites.remove(sprite);
    }

    @Override
    public void renderlast() {
        super.renderlast();
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.overlaySprite = null;
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
    }

    public boolean isFacing(IsoPlayer player) {
        if (player != null && player.isLocalPlayer()) {
            if (this.getObjectIndex() == -1) {
                return false;
            } else if (!this.square.isCanSee(player.playerIndex)) {
                return false;
            } else if (this.facing == IsoDirections.Max) {
                return false;
            } else {
                switch (this.facing) {
                    case N:
                        if (player.getY() >= this.square.y) {
                            return false;
                        }

                        return player.dir == IsoDirections.SW || player.dir == IsoDirections.S || player.dir == IsoDirections.SE;
                    case S:
                        if (player.getY() < this.square.y + 1) {
                            return false;
                        }

                        return player.dir == IsoDirections.NW || player.dir == IsoDirections.N || player.dir == IsoDirections.NE;
                    case W:
                        if (player.getX() >= this.square.x) {
                            return false;
                        }

                        return player.dir == IsoDirections.SE || player.dir == IsoDirections.E || player.dir == IsoDirections.NE;
                    case E:
                        if (player.getX() < this.square.x + 1) {
                            return false;
                        }

                        return player.dir == IsoDirections.SW || player.dir == IsoDirections.W || player.dir == IsoDirections.NW;
                    default:
                        return false;
                }
            }
        } else {
            return false;
        }
    }

    private static enum Screens {
        OFFSCREEN,
        TESTSCREEN,
        DEFAULTSCREEN,
        ALTERNATESCREEN;
    }
}
