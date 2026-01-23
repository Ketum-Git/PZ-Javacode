// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.Lua.LuaEventManager;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoHeatSource;
import zombie.iso.IsoLightSource;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.areas.SafeHouse;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.ServerOptions;
import zombie.ui.TutorialManager;

@UsedFromLua
public class IsoFire extends IsoObject {
    public static final int NUM_FRAMES_FIRE = 30;
    public static final int NUM_FRAMES_SMOKE = 30;
    private static final ColorInfo tempColorInfo = new ColorInfo();
    private static final int DefaultEnergyRequirement = 30;
    private static final int VegetationEnergyBonus = -15;
    private static final int InteriorEnergyRequirement = 40;
    private static final int TutorialEnergyDivisor = 4;
    public static final int MaxLife = 3000;
    public static final int MinLife = 800;
    public int age;
    public int energy;
    public int life;
    public int lifeStage;
    public int lifeStageDuration;
    public int lifeStageTimer;
    public int spreadDelay;
    public int spreadTimer;
    public int numFlameParticles;
    public boolean perm;
    public boolean smoke;
    public IsoLightSource lightSource;
    public int lightRadius = 1;
    public float lightOscillator;
    private IsoHeatSource heatSource;
    private float accum;
    private short[] soffX = new short[2];
    private short[] soffY = new short[2];

    public IsoFire(IsoCell cell) {
        super(cell);
    }

    public IsoFire(IsoCell cell, IsoGridSquare gridSquare) {
        super(cell);
        this.square = gridSquare;
        this.perm = true;
    }

    @Override
    public String getObjectName() {
        return "Fire";
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        ArrayList<IsoSpriteInstance> anims = this.attachedAnimSprite;
        this.attachedAnimSprite = null;
        super.save(output, IS_DEBUG_SAVE);
        this.attachedAnimSprite = anims;
        this.sprite = null;
        output.putInt(this.life);
        output.putInt(this.spreadDelay);
        output.putInt(this.lifeStage - 1);
        output.putInt(this.lifeStageTimer);
        output.putInt(this.lifeStageDuration);
        output.putInt(this.energy);
        output.putInt(this.numFlameParticles);
        output.putInt(this.spreadTimer);
        output.putInt(this.age);
        output.put((byte)(this.perm ? 1 : 0));
        output.put((byte)this.lightRadius);
        output.put((byte)(this.smoke ? 1 : 0));
    }

    @Override
    public void load(ByteBuffer b, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(b, WorldVersion, IS_DEBUG_SAVE);
        this.sprite = null;
        this.life = b.getInt();
        this.spreadDelay = b.getInt();
        this.lifeStage = b.getInt();
        this.lifeStageTimer = b.getInt();
        this.lifeStageDuration = b.getInt();
        this.energy = b.getInt();
        this.numFlameParticles = b.getInt();
        this.spreadTimer = b.getInt();
        this.age = b.getInt();
        this.perm = b.get() == 1;
        this.lightRadius = b.get() & 255;
        this.smoke = b.get() == 1;
        if (this.perm) {
            this.AttachAnim("Fire", "01", 30, 0.5F, 1 * Core.tileScale, -1 * Core.tileScale, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
        } else {
            if (this.numFlameParticles == 0) {
                this.numFlameParticles = 1;
            }

            switch (this.lifeStage) {
                case -1:
                    this.lifeStage = 0;

                    for (int i = 0; i < this.numFlameParticles; i++) {
                        this.AttachAnim("Fire", "01", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    }
                    break;
                case 0:
                    this.lifeStage = 1;
                    this.lifeStageTimer = this.lifeStageDuration;
                    this.AttachAnim("Fire", "02", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    break;
                case 1:
                    this.lifeStage = 2;
                    this.lifeStageTimer = this.lifeStageDuration;
                    this.AttachAnim("Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
                    this.AttachAnim("Fire", "03", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    break;
                case 2:
                    this.lifeStage = 3;
                    this.lifeStageTimer = this.lifeStageDuration / 3;
                    this.RemoveAttachedAnims();
                    this.AttachAnim("Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
                    this.AttachAnim("Fire", "02", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    break;
                case 3:
                    this.lifeStage = 4;
                    this.lifeStageTimer = this.lifeStageDuration / 3;
                    this.RemoveAttachedAnims();
                    if (this.smoke) {
                        this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
                    } else {
                        this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
                        this.AttachAnim("Fire", "01", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    }
                    break;
                case 4:
                    this.lifeStage = 5;
                    this.lifeStageTimer = this.lifeStageDuration / 3;
                    this.RemoveAttachedAnims();
                    this.AttachAnim("Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
            }

            if (this.square != null) {
                if (this.lifeStage < 4) {
                    this.square.getProperties().set(IsoFlagType.burning);
                } else {
                    this.square.getProperties().set(IsoFlagType.smoke);
                }
            }
        }
    }

    public IsoFire(IsoCell cell, IsoGridSquare gridSquare, boolean CanBurnAnywhere, int StartingEnergy, int SetLife, boolean isSmoke) {
        this.square = gridSquare;
        this.DirtySlice();
        this.square.getProperties().set(IsoFlagType.smoke);
        this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod);
        this.life = 800 + Rand.Next(2200);
        if (SetLife > 0) {
            this.life = SetLife;
        }

        this.lifeStage = 4;
        this.lifeStageTimer = this.lifeStageDuration = this.life / 4;
        this.energy = StartingEnergy;
        this.smoke = isSmoke;
    }

    public IsoFire(IsoCell cell, IsoGridSquare gridSquare, boolean CanBurnAnywhere, int StartingEnergy, int SetLife) {
        this.square = gridSquare;
        this.DirtySlice();
        this.numFlameParticles = 1;

        for (int i = 0; i < this.numFlameParticles; i++) {
            this.AttachAnim("Fire", "01", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
        }

        this.life = 800 + Rand.Next(2200);
        if (SetLife > 0) {
            this.life = SetLife;
        }

        if (this.square.getProperties() != null && !this.square.getProperties().has(IsoFlagType.vegitation) && this.square.getFloor() != null) {
            this.life = this.life - this.square.getFloor().getSprite().firerequirement * 100;
            if (this.life < 600) {
                this.life = Rand.Next(300, 600);
            }
        }

        this.spreadDelay = this.spreadTimer = Rand.Next(this.life - this.life / 2);
        this.lifeStage = 0;
        this.lifeStageTimer = this.lifeStageDuration = this.life / 5;
        if (TutorialManager.instance.active) {
            this.lifeStageDuration *= 2;
            this.life *= 2;
        }

        if (TutorialManager.instance.active) {
            this.spreadDelay = this.spreadTimer /= 4;
        }

        gridSquare.getProperties().set(IsoFlagType.burning);
        this.energy = StartingEnergy;
        if (this.square.getProperties().has(IsoFlagType.vegitation)) {
            this.energy += 50;
        }

        LuaEventManager.triggerEvent("OnNewFire", this);
    }

    public IsoFire(IsoCell cell, IsoGridSquare gridSquare, boolean CanBurnAnywhere, int StartingEnergy) {
        this(cell, gridSquare, CanBurnAnywhere, StartingEnergy, 0);
    }

    public static boolean CanAddSmoke(IsoGridSquare gridSquare, boolean CanBurnAnywhere) {
        return CanAddFire(gridSquare, CanBurnAnywhere, true);
    }

    public static boolean CanAddFire(IsoGridSquare gridSquare, boolean CanBurnAnywhere) {
        return CanAddFire(gridSquare, CanBurnAnywhere, false);
    }

    public static boolean CanAddFire(IsoGridSquare gridSquare, boolean CanBurnAnywhere, boolean smoke) {
        if (!smoke && (GameServer.server || GameClient.client) && ServerOptions.instance.noFire.getValue()) {
            return false;
        } else if (gridSquare == null || gridSquare.getObjects().isEmpty()) {
            return false;
        } else if (gridSquare.has(IsoFlagType.water)) {
            return false;
        } else if (!CanBurnAnywhere && gridSquare.getProperties().has(IsoFlagType.burntOut)) {
            return false;
        } else if (gridSquare.getProperties().has(IsoFlagType.burning) || gridSquare.getProperties().has(IsoFlagType.smoke)) {
            return false;
        } else {
            return !CanBurnAnywhere && !Fire_IsSquareFlamable(gridSquare)
                ? false
                : smoke
                    || !GameServer.server && !GameClient.client
                    || SafeHouse.getSafeHouse(gridSquare) == null
                    || ServerOptions.instance.safehouseAllowFire.getValue();
        }
    }

    public static boolean Fire_IsSquareFlamable(IsoGridSquare gridSquare) {
        return !gridSquare.getProperties().has(IsoFlagType.unflamable);
    }

    @Override
    public boolean HasTooltip() {
        return false;
    }

    public void Spread() {
        if (!GameClient.client) {
            if (SandboxOptions.instance.fireSpread.getValue()) {
                if (this.getCell() != null) {
                    if (this.square != null) {
                        if (this.lifeStage < 4) {
                            IsoGridSquare NewSquare = null;
                            int NumSpreads = Rand.Next(3) + 1;
                            if (Rand.Next(50) == 0) {
                                NumSpreads += 15;
                            }

                            if (TutorialManager.instance.active) {
                                NumSpreads += 15;
                            }

                            for (int i = 0; i < NumSpreads; i++) {
                                int SpreadDirection = Rand.Next(13);
                                switch (SpreadDirection) {
                                    case 0:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ());
                                        break;
                                    case 1:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX() + 1, this.square.getY() - 1, this.square.getZ());
                                        break;
                                    case 2:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX() + 1, this.square.getY(), this.square.getZ());
                                        break;
                                    case 3:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX() + 1, this.square.getY() + 1, this.square.getZ());
                                        break;
                                    case 4:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY() + 1, this.square.getZ());
                                        break;
                                    case 5:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY() + 1, this.square.getZ());
                                        break;
                                    case 6:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ());
                                        break;
                                    case 7:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY() - 1, this.square.getZ());
                                        break;
                                    case 8:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY() - 1, this.square.getZ() - 1);
                                        break;
                                    case 9:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX() - 1, this.square.getY(), this.square.getZ() - 1);
                                        break;
                                    case 10:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY() - 1, this.square.getZ() - 1);
                                        break;
                                    case 11:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ() - 1);
                                        break;
                                    case 12:
                                        NewSquare = this.getCell().getGridSquare(this.square.getX(), this.square.getY(), this.square.getZ() + 1);
                                }

                                if (CanAddFire(NewSquare, false)) {
                                    int NewSquareEnergyRequirement = this.getSquaresEnergyRequirement(NewSquare);
                                    if (this.energy >= NewSquareEnergyRequirement) {
                                        this.energy -= NewSquareEnergyRequirement;
                                        if (GameServer.server) {
                                            this.sendObjectChange("Energy");
                                        }

                                        if (RainManager.isRaining()) {
                                            return;
                                        }

                                        int energy = NewSquare.getProperties().has(IsoFlagType.exterior) ? this.energy : NewSquareEnergyRequirement * 2;
                                        IsoFireManager.StartFire(this.getCell(), NewSquare, false, energy);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean TestCollide(IsoMovingObject obj, IsoGridSquare PassedObjectSquare) {
        return this.square == PassedObjectSquare;
    }

    @Override
    public IsoObject.VisionResult TestVision(IsoGridSquare from, IsoGridSquare to) {
        return IsoObject.VisionResult.NoEffect;
    }

    @Override
    public void update() {
        if (this.getObjectIndex() != -1) {
            if (!GameServer.server) {
                IsoFireManager.updateSound(this);
            }

            if (this.lifeStage < 4) {
                this.square.getProperties().set(IsoFlagType.burning);
            } else {
                this.square.getProperties().set(IsoFlagType.smoke);
            }

            if (!this.smoke && this.lifeStage < 5) {
                this.square.BurnTick();
            }

            int n = this.attachedAnimSprite.size();

            for (int i = 0; i < n; i++) {
                IsoSpriteInstance s = this.attachedAnimSprite.get(i);
                IsoSprite sp = s.parentSprite;
                s.update();
                if (sp.hasAnimation()) {
                    float dt = GameTime.instance.getMultipliedSecondsSinceLastUpdate() * 60.0F;
                    s.frame = s.frame + s.animFrameIncrease * dt;
                    if ((int)s.frame >= sp.currentAnim.frames.size() && sp.loop && s.looped) {
                        s.frame = 0.0F;
                    }
                }
            }

            float lightR = 0.61F;
            float lightG = 0.175F;
            float lightB = 0.0F;
            if (!this.smoke && !GameServer.server && this.lightSource == null) {
                this.lightSource = new IsoLightSource(
                    this.square.getX(), this.square.getY(), this.square.getZ(), 0.61F, 0.175F, 0.0F, this.perm ? this.lightRadius : 5
                );
                IsoWorld.instance.currentCell.addLamppost(this.lightSource);
            }

            if (this.lightSource != null && this.lifeStage == 5) {
                this.lightSource.r = 0.61F * this.getAlpha(IsoCamera.frameState.playerIndex);
                this.lightSource.g = 0.175F * this.getAlpha(IsoCamera.frameState.playerIndex);
                this.lightSource.b = 0.0F * this.getAlpha(IsoCamera.frameState.playerIndex);
            }

            if (this.perm) {
                if (this.heatSource == null) {
                    this.heatSource = new IsoHeatSource(this.square.x, this.square.y, this.square.z, this.lightRadius, 35);
                    IsoWorld.instance.currentCell.addHeatSource(this.heatSource);
                } else {
                    this.heatSource.setRadius(this.lightRadius);
                }
            } else {
                this.accum = this.accum + GameTime.getInstance().getThirtyFPSMultiplier();

                while (this.accum > 1.0F) {
                    this.accum--;
                    this.age++;
                    if (this.lifeStageTimer > 0) {
                        this.lifeStageTimer--;
                        if (this.lifeStageTimer <= 0) {
                            switch (this.lifeStage) {
                                case 0:
                                    this.lifeStage = 1;
                                    this.lifeStageTimer = this.lifeStageDuration;
                                    this.square.Burn();
                                    if (this.lightSource != null) {
                                        this.setLightRadius(5);
                                    }
                                    break;
                                case 1:
                                    this.lifeStage = 2;
                                    this.lifeStageTimer = this.lifeStageDuration;
                                    this.RemoveAttachedAnims();
                                    this.AttachAnim(
                                        "Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true
                                    );
                                    this.AttachAnim("Fire", "02", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                                    this.square.Burn();
                                    if (this.lightSource != null) {
                                        this.setLightRadius(8);
                                    }
                                    break;
                                case 2:
                                    this.lifeStage = 3;
                                    this.lifeStageTimer = this.lifeStageDuration;
                                    this.RemoveAttachedAnims();
                                    this.AttachAnim(
                                        "Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true
                                    );
                                    this.AttachAnim("Fire", "03", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                                    if (this.lightSource != null) {
                                        this.setLightRadius(12);
                                    }
                                    break;
                                case 3:
                                    this.lifeStage = 4;
                                    this.lifeStageTimer = this.lifeStageDuration / 2;
                                    this.RemoveAttachedAnims();
                                    this.AttachAnim(
                                        "Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true
                                    );
                                    this.AttachAnim("Fire", "02", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                                    if (this.lightSource != null) {
                                        this.setLightRadius(8);
                                    }
                                    break;
                                case 4:
                                    this.lifeStage = 5;
                                    this.lifeStageTimer = this.lifeStageDuration / 2;
                                    this.RemoveAttachedAnims();
                                    this.AttachAnim(
                                        "Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true
                                    );
                                    if (this.lightSource != null) {
                                        this.setLightRadius(1);
                                    }
                            }
                        }
                    }

                    if (this.life > 0) {
                        this.life--;
                        if (this.lifeStage > 0 && this.spreadTimer > 0) {
                            this.spreadTimer--;
                            if (this.spreadTimer <= 0) {
                                if (this.lifeStage != 5) {
                                    this.Spread();
                                }

                                this.spreadTimer = this.spreadDelay;
                            }
                        }

                        if (this.energy > 0) {
                            continue;
                        }

                        this.extinctFire();
                        break;
                    }

                    this.extinctFire();
                    break;
                }
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        x += 0.5F;
        y += 0.5F;
        this.setAlphaToTarget(IsoCamera.frameState.playerIndex);
        this.sx = 0.0F;
        this.offsetX = 32 * Core.tileScale;
        this.offsetY = 96 * Core.tileScale;
        float SCL = Core.tileScale / 2.0F;

        for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
            IsoSprite sprite = this.attachedAnimSprite.get(i).parentSprite;
            if (sprite != null && sprite.currentAnim != null && sprite.def != null) {
                Texture tex = sprite.getTextureForCurrentFrame(this.getDir());
                if (tex != null) {
                    if (this.soffX.length <= i) {
                        this.soffX = Arrays.copyOf(this.soffX, i + 1);
                        this.soffY = Arrays.copyOf(this.soffY, i + 1);
                    }

                    this.soffX[i] = sprite.soffX;
                    this.soffY[i] = sprite.soffY;
                    sprite.soffX = (short)(sprite.soffX + (short)(this.offsetX - tex.getWidthOrig() / 2.0F * SCL));
                    sprite.soffY = (short)(sprite.soffY + (short)(this.offsetY - tex.getHeightOrig() * SCL));
                    this.attachedAnimSprite.get(i).setScale(SCL, SCL);
                }
            }
        }

        if (PerformanceSettings.fboRenderChunk) {
            bDoChild = false;
        }

        super.render(x, y, z, col, bDoChild, bWallLightingPass, shader);

        for (int ix = 0; ix < this.attachedAnimSprite.size(); ix++) {
            IsoSprite sprite = this.attachedAnimSprite.get(ix).parentSprite;
            if (sprite != null && sprite.currentAnim != null && sprite.def != null) {
                sprite.soffX = this.soffX[ix];
                sprite.soffY = this.soffY[ix];
            }
        }
    }

    public void extinctFire() {
        this.square.getProperties().unset(IsoFlagType.burning);
        this.square.getProperties().unset(IsoFlagType.smoke);
        this.RemoveAttachedAnims();
        this.square.getObjects().remove(this);
        this.square.RemoveTileObject(this);
        this.setLife(0);
        this.removeFromWorld();
    }

    int getSquaresEnergyRequirement(IsoGridSquare TestSquare) {
        int EnergyRequirementVal = 30;
        if (TestSquare.getProperties().has(IsoFlagType.vegitation)) {
            EnergyRequirementVal = 15;
        }

        if (!TestSquare.getProperties().has(IsoFlagType.exterior)) {
            EnergyRequirementVal = 40;
        }

        if (TestSquare.getFloor() != null && TestSquare.getFloor().getSprite() != null) {
            EnergyRequirementVal = TestSquare.getFloor().getSprite().firerequirement;
        }

        return TutorialManager.instance.active ? EnergyRequirementVal / 4 : EnergyRequirementVal;
    }

    /**
     * The more this number is low, the faster it's gonna spread
     */
    public void setSpreadDelay(int SpreadDelay) {
        this.spreadDelay = SpreadDelay;
    }

    /**
     * The more this number is low, the faster it's gonna spread
     */
    public int getSpreadDelay() {
        return this.spreadDelay;
    }

    /**
     * Up this number to make the fire life longer
     */
    public void setLife(int Life) {
        this.life = Life;
    }

    public int getLife() {
        return this.life;
    }

    public int getEnergy() {
        return this.energy;
    }

    public boolean isPermanent() {
        return this.perm;
    }

    public void setLifeStage(int lifeStage) {
        if (this.perm) {
            this.RemoveAttachedAnims();
            switch (lifeStage) {
                case 0:
                    this.AttachAnim("Fire", "01", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    break;
                case 1:
                    this.AttachAnim("Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
                    this.AttachAnim("Fire", "02", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    break;
                case 2:
                    this.AttachAnim("Smoke", "03", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
                    this.AttachAnim("Fire", "03", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    break;
                case 3:
                    this.AttachAnim("Smoke", "02", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
                    this.AttachAnim("Fire", "02", 30, 0.5F, 0, 0, true, 0, false, 0.7F, IsoFireManager.FIRE_TINT_MOD, true);
                    break;
                case 4:
                    this.AttachAnim("Smoke", "01", 30, IsoFireManager.smokeAnimDelay, 0, 0, true, 0, false, 0.7F, IsoFireManager.smokeTintMod, true);
            }
        }
    }

    public void setLightRadius(int radius) {
        this.lightRadius = radius;
        if (this.lightSource != null && radius != this.lightSource.getRadius()) {
            this.getCell().removeLamppost(this.lightSource);
            this.lightSource = new IsoLightSource(
                this.square.getX(), this.square.getY(), this.square.getZ(), fireColor.r, fireColor.g, fireColor.b, this.lightRadius
            );
            this.getCell().getLamppostPositions().add(this.lightSource);
            IsoGridSquare.recalcLightTime = -1.0F;
            Core.dirtyGlobalLightsCount++;
            GameTime.instance.lightSourceUpdate = 100.0F;
        }
    }

    public int getLightRadius() {
        return this.lightRadius;
    }

    @Override
    public void addToWorld() {
        if (this.perm) {
            this.getCell().addToStaticUpdaterObjectList(this);
        } else {
            IsoFireManager.Add(this);
        }
    }

    @Override
    public void removeFromWorld() {
        if (!this.perm) {
            IsoFireManager.Remove(this);
        }

        IsoFireManager.stopSound(this);
        if (this.lightSource != null) {
            this.getCell().removeLamppost(this.lightSource);
            this.lightSource = null;
        }

        if (this.heatSource != null) {
            this.getCell().removeHeatSource(this.heatSource);
            this.heatSource = null;
        }

        super.removeFromWorld();
    }

    @Override
    public void saveChange(String change, KahluaTable tbl, ByteBuffer bb) {
        super.saveChange(change, tbl, bb);
        if ("Energy".equals(change)) {
            bb.putInt(this.energy);
        } else if ("LightRadius".equals(change)) {
            bb.putInt(this.getLightRadius());
        }
    }

    @Override
    public void loadChange(String change, ByteBuffer bb) {
        super.loadChange(change, bb);
        if ("Energy".equals(change)) {
            this.energy = bb.getInt();
        }

        if ("LightRadius".equals(change)) {
            int radius = bb.getInt();
            this.setLightRadius(radius);
        }
    }

    public boolean isCampfire() {
        if (this.getSquare() == null) {
            return false;
        } else {
            IsoObject[] objects = this.getSquare().getObjects().getElements();
            int i = 1;

            for (int n = this.getSquare().getObjects().size(); i < n; i++) {
                IsoObject obj = objects[i];
                if (!(obj instanceof IsoWorldInventoryObject) && "Campfire".equalsIgnoreCase(obj.getName())) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean hasAnimatedAttachments() {
        return this.attachedAnimSprite != null && !this.attachedAnimSprite.isEmpty();
    }

    @Override
    public void renderAnimatedAttachments(float x, float y, float z, ColorInfo col) {
        if (this.attachedAnimSprite != null) {
            this.sx = 0.0F;
            float offsetX = this.offsetX;
            float offsetY = this.offsetY + 16 * Core.tileScale;
            float SCL = Core.tileScale / 2.0F;
            if (this.isCampfire()) {
                SCL *= 0.75F;
            }

            for (int i = 0; i < this.attachedAnimSprite.size(); i++) {
                IsoSpriteInstance spriteInstance = this.attachedAnimSprite.get(i);
                IsoSprite sprite = spriteInstance.parentSprite;
                if (sprite != null && sprite.def != null) {
                    Texture tex = sprite.getTextureForCurrentFrame(this.dir);
                    if (tex != null) {
                        if (sprite.name != null && sprite.name.startsWith("Fire")) {
                            tempColorInfo.set(1.0F, 1.0F, 1.0F, col.a);
                        } else {
                            if (this.lifeStage == 5) {
                                float duration = this.lifeStageDuration / 2.0F;
                                float f = 1.0F - this.lifeStageTimer / duration;
                                float alpha = f == 0.0F ? 0.0F : PZMath.pow(2.0F, 10.0F * f - 10.0F);
                                this.setAlphaAndTarget(IsoCamera.frameState.playerIndex, 1.0F - alpha);
                            }

                            tempColorInfo.set(col);
                        }

                        short soffX = sprite.soffX;
                        short soffY = sprite.soffY;
                        sprite.soffX = (short)(soffX + 32 * Core.tileScale - tex.getWidthOrig() / 2.0F * SCL);
                        sprite.soffY = (short)(soffY + 128 * Core.tileScale - tex.getHeightOrig() * SCL);
                        spriteInstance.setScale(SCL, SCL);
                        spriteInstance.getParentSprite().render(spriteInstance, this, x + 0.0F, y + 0.0F, z, this.dir, offsetX, offsetY, tempColorInfo, true);
                        sprite.soffX = soffX;
                        sprite.soffY = soffY;
                    }
                }
            }
        }
    }
}
