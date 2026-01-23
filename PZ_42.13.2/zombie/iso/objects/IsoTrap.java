// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.UsedFromLua;
import zombie.WorldSoundManager;
import zombie.Lua.LuaEventManager;
import zombie.ai.states.ZombieIdleState;
import zombie.audio.BaseSoundEmitter;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.BodyDamage.BodyPartType;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.opengl.Shader;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.model.ItemModelRenderer;
import zombie.core.skinnedmodel.model.WorldItemModelDrawer;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.Texture;
import zombie.core.utils.GameTimer;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.inventory.types.HandWeapon;
import zombie.iso.IItemProvider;
import zombie.iso.IsoCamera;
import zombie.iso.IsoCell;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoUtils;
import zombie.iso.IsoWorld;
import zombie.iso.LosUtil;
import zombie.iso.areas.NonPvpZone;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.fboRenderChunk.FBORenderItems;
import zombie.iso.fboRenderChunk.FBORenderLevels;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.util.StringUtils;
import zombie.util.Type;

@UsedFromLua
public class IsoTrap extends IsoObject implements IItemProvider {
    private static final int MaximumTrapRadius = 15;
    private static final int MaximumChance = 100;
    private static final int MaximumExplosivePower = 100;
    private static final int ExplosionDamageDivisor = 20;
    private static final float ExplosionDamageVarianceMultiplier = 2.0F;
    private int timerBeforeExplosion;
    private int sensorRange;
    private int fireRange;
    private int fireStartingChance;
    private int fireStartingEnergy;
    private int explosionPower;
    private int explosionRange;
    private int smokeRange;
    private int noiseRange;
    private int noiseDuration;
    private float noiseStartTime;
    private float lastWorldSoundTime;
    private float extraDamage;
    private int remoteControlId = -1;
    private String countDownSound;
    private String explosionSound;
    private HandWeapon weapon;
    private IsoGameCharacter attacker;
    private boolean instantExplosion;
    private final GameTimer beep = new GameTimer(1);
    private int explosionDuration;
    private float explosionStartTime;

    public IsoTrap(IsoCell cell) {
        super(cell);
    }

    public IsoTrap(HandWeapon weapon, IsoCell cell, IsoGridSquare sq) {
        this.IsoTrap(null, weapon, cell, sq);
    }

    public IsoTrap(IsoGameCharacter attacker, HandWeapon weapon, IsoCell cell, IsoGridSquare sq) {
        this.IsoTrap(attacker, weapon, cell, sq);
    }

    private void IsoTrap(IsoGameCharacter attacker, HandWeapon weapon, IsoCell cell, IsoGridSquare sq) {
        this.square = sq;
        this.initSprite(weapon);
        this.setSensorRange(weapon.getSensorRange());
        this.setFireRange(weapon.getFireRange());
        this.setFireStartingEnergy(weapon.getFireStartingEnergy());
        this.setFireStartingChance(weapon.getFireStartingChance());
        this.setExplosionPower(weapon.getExplosionPower());
        this.setExplosionRange(weapon.getExplosionRange());
        this.setSmokeRange(weapon.getSmokeRange());
        this.setNoiseRange(weapon.getNoiseRange());
        this.setNoiseDuration(weapon.getNoiseDuration());
        this.setExtraDamage(weapon.getExtraDamage());
        this.setRemoteControlID(weapon.getRemoteControlID());
        this.setCountDownSound(weapon.getCountDownSound());
        this.setExplosionSound(weapon.getExplosionSound());
        this.setTimerBeforeExplosion(weapon.getExplosionTimer());
        this.setInstantExplosion(weapon.isInstantExplosion());
        this.setExplosionDuration(weapon.getExplosionDuration());
        if (weapon.getAttackTargetSquare(null) == null) {
            weapon.setAttackTargetSquare(sq);
        }

        this.weapon = weapon;
        this.attacker = attacker;
    }

    private void initSprite(HandWeapon weapon) {
        if (weapon != null) {
            String texName;
            if (weapon.getPlacedSprite() != null && !weapon.getPlacedSprite().isEmpty()) {
                texName = weapon.getPlacedSprite();
            } else if (weapon.getTex() != null && weapon.getTex().getName() != null) {
                texName = weapon.getTex().getName();
            } else {
                texName = "media/inventory/world/WItem_Sack.png";
            }

            this.sprite = IsoSpriteManager.instance.namedMap.get(texName);
            if (this.sprite == null) {
                this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
                this.sprite.LoadSingleTexture(texName);
            }

            Texture tex = this.sprite.getTextureForCurrentFrame(this.getDir());
            if (texName.startsWith("Item_") && tex != null) {
                if (weapon.getScriptItem() == null) {
                    this.sprite.def.scaleAspect(tex.getWidthOrig(), tex.getHeightOrig(), 16 * Core.tileScale, 16 * Core.tileScale);
                } else {
                    float var10001 = Core.tileScale;
                    float scale = weapon.getScriptItem().scaleWorldIcon * (var10001 / 2.0F);
                    this.sprite.def.setScale(scale, scale);
                }
            }
        }
    }

    @Override
    public void update() {
        if (!GameClient.client && this.timerBeforeExplosion > 0 && this.beep.check()) {
            this.timerBeforeExplosion--;
            if (this.timerBeforeExplosion == 0) {
                this.triggerExplosion(this.getSensorRange() > 0);
            } else if (this.getObjectIndex() != -1 && this.weapon.getType().endsWith("Triggered")) {
                String sound = "TrapTimerLoop";
                if (!StringUtils.isNullOrWhitespace(this.getCountDownSound())) {
                    sound = this.getCountDownSound();
                } else if (this.timerBeforeExplosion == 1) {
                    sound = "TrapTimerExpired";
                }

                if (GameServer.server) {
                    GameServer.PlayWorldSoundServer(sound, false, this.square, 1.0F, 10.0F, 1.0F, true);
                } else if (!GameClient.client) {
                    this.getOrCreateEmitter();
                    this.emitter.playSound(sound);
                }
            }
        }

        if (!this.updateExplosionDuration()) {
            this.updateVictimsInSensorRange();
            this.updateSounds();
        }
    }

    private boolean updateExplosionDuration() {
        if (!this.isExploding()) {
            return false;
        } else {
            float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
            this.explosionStartTime = PZMath.min(this.explosionStartTime, worldAge);
            float dayLengthScale = 60.0F / SandboxOptions.getInstance().getDayLengthMinutes();
            float minutesPerHour = 60.0F;
            if (worldAge - this.explosionStartTime > this.getExplosionDuration() / 60.0F * dayLengthScale) {
                this.explosionStartTime = 0.0F;
                if (this.emitter != null) {
                    this.emitter.stopAll();
                }

                if (GameServer.server) {
                    GameServer.RemoveItemFromMap(this);
                } else if (!GameClient.client) {
                    this.removeFromWorld();
                    this.removeFromSquare();
                }

                return true;
            } else {
                if (this.getSmokeRange() > 0 && this.getObjectIndex() != -1) {
                    int radius = this.getSmokeRange();

                    for (int x = this.getSquare().getX() - radius; x <= this.getX() + radius; x++) {
                        for (int y = this.getSquare().getY() - radius; y <= this.getSquare().getY() + radius; y++) {
                            IsoGridSquare explodedSquare = this.getCell().getGridSquare((double)x, (double)y, (double)this.getZ());
                            this.refreshSmokeBombSmoke(explodedSquare);
                        }
                    }
                }

                return false;
            }
        }
    }

    private void refreshSmokeBombSmoke(IsoGridSquare explodedSquare) {
        if (explodedSquare != null) {
            IsoFire fire = explodedSquare.getFire();
            if (fire != null && fire.smoke) {
                int radius = this.getSmokeRange();
                if (!(IsoUtils.DistanceTo(explodedSquare.getX() + 0.5F, explodedSquare.getY() + 0.5F, this.getX() + 0.5F, this.getY() + 0.5F) > radius)) {
                    LosUtil.TestResults testResults = LosUtil.lineClear(
                        this.getCell(),
                        PZMath.fastfloor(this.getX()),
                        PZMath.fastfloor(this.getY()),
                        PZMath.fastfloor(this.getZ()),
                        explodedSquare.getX(),
                        explodedSquare.getY(),
                        explodedSquare.getZ(),
                        false
                    );
                    if (testResults != LosUtil.TestResults.Blocked && testResults != LosUtil.TestResults.ClearThroughClosedDoor) {
                        if (NonPvpZone.getNonPvpZone(explodedSquare.getX(), explodedSquare.getY()) == null) {
                            fire.energy = 40;
                            fire.life = Rand.Next(800, 3000) / 5;
                            fire.lifeStage = 4;
                            fire.lifeStageTimer = fire.lifeStageDuration = fire.life / 4;
                            this.smoke(explodedSquare);
                        }
                    }
                }
            }
        }
    }

    private void updateVictimsInSensorRange() {
        if ((GameServer.server || !GameClient.client)
            && this.getSensorRange() > 0
            && IsoWorld.instance.currentCell != null
            && IsoWorld.instance.currentCell.getObjectList() != null) {
            ArrayList<IsoMovingObject> objects = IsoWorld.instance.currentCell.getObjectList();

            for (int i = 0; i < objects.size(); i++) {
                IsoMovingObject movingObject = objects.get(i);
                IsoGameCharacter character = Type.tryCastTo(movingObject, IsoGameCharacter.class);
                if ((character == null || !character.isInvisible())
                    && IsoUtils.DistanceTo(movingObject.getX(), movingObject.getY(), this.getX(), this.getY()) <= this.getSensorRange()) {
                    if (GameServer.server) {
                        INetworkPacket.sendToAll(PacketTypes.PacketType.AddExplosiveTrap, null, this.getSquare());
                    }

                    this.explodeTrap(this.getSquare());
                }
            }
        }
    }

    private void updateSounds() {
        if (this.noiseStartTime > 0.0F) {
            float worldAge = (float)GameTime.getInstance().getWorldAgeHours();
            this.noiseStartTime = PZMath.min(this.noiseStartTime, worldAge);
            this.lastWorldSoundTime = PZMath.min(this.lastWorldSoundTime, worldAge);
            float dayLengthScale = 60.0F / SandboxOptions.getInstance().getDayLengthMinutes();
            float minutesPerHour = 60.0F;
            if (worldAge - this.noiseStartTime > this.getNoiseDuration() / 60.0F * dayLengthScale) {
                this.noiseStartTime = 0.0F;
                if (this.emitter != null) {
                    this.emitter.stopAll();
                }
            } else {
                if (!GameServer.server && (this.emitter == null || !this.emitter.isPlaying(this.getExplosionSound()))) {
                    BaseSoundEmitter emitter = this.getOrCreateEmitter();
                    if (emitter != null) {
                        emitter.playSound(this.getExplosionSound());
                    }
                }

                if (worldAge - this.lastWorldSoundTime > 0.016666668F * dayLengthScale && this.getObjectIndex() != -1) {
                    this.lastWorldSoundTime = worldAge;
                    WorldSoundManager.instance
                        .addSoundRepeating(null, this.getSquare().getX(), this.getSquare().getY(), this.getSquare().getZ(), this.getNoiseRange(), 1, true);
                }
            }
        }

        if (this.emitter != null) {
            this.emitter.tick();
        }
    }

    @Override
    public IsoGridSquare getRenderSquare() {
        if (this.getSquare() == null) {
            return null;
        } else {
            int CPW = 8;
            if (PZMath.coordmodulo(this.square.x, 8) == 0 && PZMath.coordmodulo(this.square.y, 8) == 7) {
                return this.square.getAdjacentSquare(IsoDirections.S);
            } else {
                return PZMath.coordmodulo(this.square.x, 8) == 7 && PZMath.coordmodulo(this.square.y, 8) == 0
                    ? this.square.getAdjacentSquare(IsoDirections.E)
                    : this.getSquare();
            }
        }
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoChild, boolean bWallLightingPass, Shader shader) {
        if (Core.getInstance().isOption3DGroundItem() && ItemModelRenderer.itemHasModel(this.getItem())) {
            if (PerformanceSettings.fboRenderChunk && FBORenderChunkManager.instance.isCaching()) {
                int playerIndex = IsoCamera.frameState.playerIndex;
                IsoGridSquare renderSquare = this.getRenderSquare();
                FBORenderLevels renderLevels = renderSquare.chunk.getRenderLevels(playerIndex);
                FBORenderChunk renderChunk = renderLevels.getFBOForLevel(renderSquare.z, Core.getInstance().getZoom(playerIndex));
                FBORenderItems.getInstance().render(renderChunk.index, this);
                return;
            }

            ItemModelRenderer.RenderStatus status = WorldItemModelDrawer.renderMain(
                this.getItem(), this.getSquare(), this.getRenderSquare(), this.getX() + 0.5F, this.getY() + 0.5F, this.getZ(), 0.0F, -1.0F, true
            );
            if (status == ItemModelRenderer.RenderStatus.Loading || status == ItemModelRenderer.RenderStatus.Ready) {
                return;
            }
        }

        if (!this.sprite.hasNoTextures()) {
            Texture tex = this.sprite.getTextureForCurrentFrame(this.dir);
            if (tex != null) {
                if (tex.getName().startsWith("Item_")) {
                    float dx = tex.getWidthOrig() * this.sprite.def.getScaleX() / 2.0F;
                    float dy = tex.getHeightOrig() * this.sprite.def.getScaleY() * 3.0F / 4.0F;
                    this.setAlphaAndTarget(1.0F);
                    this.offsetX = 0.0F;
                    this.offsetY = 0.0F;
                    this.sx = 0.0F;
                    this.sprite.render(this, x + 0.5F, y + 0.5F, z, this.dir, this.offsetX + dx, this.offsetY + dy, col, true);
                } else {
                    this.offsetX = 32 * Core.tileScale;
                    this.offsetY = 96 * Core.tileScale;
                    this.sx = 0.0F;
                    super.render(x, y, z, col, bDoChild, bWallLightingPass, shader);
                }
            }
        }
    }

    public void triggerExplosion(boolean sensor) {
        LuaEventManager.triggerEvent("OnThrowableExplode", this, this.square);
        this.timerBeforeExplosion = 0;
        if (sensor) {
            if (this.getSensorRange() > 0) {
                this.square.setTrapPositionX(this.square.getX());
                this.square.setTrapPositionY(this.square.getY());
                this.square.setTrapPositionZ(this.square.getZ());
                this.drawCircleExplosion(this.square, this.getSensorRange(), IsoTrap.ExplosionMode.Sensor);
            }
        } else {
            if (this.getExplosionSound() != null) {
                this.playExplosionSound();
            }

            if (this.getNoiseRange() > 0) {
                WorldSoundManager.instance.addSound(null, this.getXi(), this.getYi(), this.getZi(), this.getNoiseRange(), 1);
            } else if (this.getExplosionSound() != null) {
                WorldSoundManager.instance.addSound(null, this.getXi(), this.getYi(), this.getZi(), 50, 1);
            }

            if (this.getExplosionRange() > 0) {
                this.drawCircleExplosion(this.square, this.getExplosionRange(), IsoTrap.ExplosionMode.Explosion);
            }

            if (this.getFireRange() > 0) {
                this.drawCircleExplosion(this.square, this.getFireRange(), IsoTrap.ExplosionMode.Fire);
            }

            if (this.getSmokeRange() > 0) {
                this.drawCircleExplosion(this.square, this.getSmokeRange(), IsoTrap.ExplosionMode.Smoke);
            }

            if (this.getExplosionDuration() > 0) {
                if (this.explosionStartTime == 0.0F) {
                    this.explosionStartTime = (float)GameTime.getInstance().getWorldAgeHours();
                }

                return;
            }

            if (this.weapon == null || !this.weapon.canBeReused()) {
                if (GameServer.server) {
                    GameServer.RemoveItemFromMap(this);
                } else {
                    this.removeFromWorld();
                    this.removeFromSquare();
                }
            }
        }
    }

    private BaseSoundEmitter getOrCreateEmitter() {
        if (this.getObjectIndex() == -1) {
            return null;
        } else {
            if (this.emitter == null) {
                this.emitter = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, this.getZ());
                IsoWorld.instance.takeOwnershipOfEmitter(this.emitter);
            }

            return this.emitter;
        }
    }

    public void playExplosionSound() {
        if (!StringUtils.isNullOrWhitespace(this.getExplosionSound())) {
            if (this.getObjectIndex() == -1) {
                if (!GameServer.server && this.isInstantExplosion()) {
                    BaseSoundEmitter emitter1 = IsoWorld.instance.getFreeEmitter(this.getX() + 0.5F, this.getY() + 0.5F, this.getZ());
                    emitter1.playSoundImpl(this.getExplosionSound(), (IsoObject)null);
                }
            } else {
                if (this.getNoiseRange() > 0 && this.getNoiseDuration() > 0.0F) {
                    this.noiseStartTime = (float)GameTime.getInstance().getWorldAgeHours();
                }

                if (!GameServer.server) {
                    this.getOrCreateEmitter();
                    if (!this.emitter.isPlaying(this.getExplosionSound())) {
                        this.emitter.playSoundImpl(this.getExplosionSound(), (IsoObject)null);
                    }

                    if ("SmokeBombLoop".equalsIgnoreCase(this.getExplosionSound())) {
                        this.emitter.playSoundImpl("SmokeBombExplode", (IsoObject)null);
                    }
                } else {
                    if (this.getNoiseRange() > 0 && this.getNoiseDuration() > 0.0F) {
                        GameServer.PlayWorldSoundServer(this.getExplosionSound(), this.getSquare(), this.getNoiseRange(), this.getObjectIndex());
                    } else {
                        GameServer.PlayWorldSoundServer(this.getExplosionSound(), false, this.getSquare(), 0.0F, 50.0F, 1.0F, false);
                    }
                }
            }
        }
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion, boolean IS_DEBUG_SAVE) throws IOException {
        super.load(input, WorldVersion, IS_DEBUG_SAVE);
        this.sensorRange = input.getInt();
        this.fireStartingChance = input.getInt();
        this.fireStartingEnergy = input.getInt();
        this.fireRange = input.getInt();
        this.explosionPower = input.getInt();
        this.explosionRange = input.getInt();
        if (WorldVersion >= 219) {
            this.explosionDuration = input.getInt();
            this.explosionStartTime = input.getFloat();
        }

        this.smokeRange = input.getInt();
        this.noiseRange = input.getInt();
        this.noiseDuration = input.getInt();
        this.noiseStartTime = input.getFloat();
        this.extraDamage = input.getFloat();
        this.remoteControlId = input.getInt();
        this.timerBeforeExplosion = input.getInt();
        this.countDownSound = GameWindow.ReadStringUTF(input);
        this.explosionSound = GameWindow.ReadStringUTF(input);
        if ("bigExplosion".equals(this.explosionSound)) {
            this.explosionSound = "BigExplosion";
        }

        if ("smallExplosion".equals(this.explosionSound)) {
            this.explosionSound = "SmallExplosion";
        }

        if ("feedback".equals(this.explosionSound)) {
            this.explosionSound = "NoiseTrapExplosion";
        }

        boolean hasItem = input.get() == 1;
        if (hasItem && InventoryItem.loadItem(input, WorldVersion) instanceof HandWeapon handWeapon) {
            this.weapon = handWeapon;
            this.initSprite(this.weapon);
        }
    }

    @Override
    public void save(ByteBuffer output, boolean IS_DEBUG_SAVE) throws IOException {
        super.save(output, IS_DEBUG_SAVE);
        output.putInt(this.sensorRange);
        output.putInt(this.fireStartingChance);
        output.putInt(this.fireStartingEnergy);
        output.putInt(this.fireRange);
        output.putInt(this.explosionPower);
        output.putInt(this.explosionRange);
        output.putInt(this.explosionDuration);
        output.putFloat(this.explosionStartTime);
        output.putInt(this.smokeRange);
        output.putInt(this.noiseRange);
        output.putInt(this.noiseDuration);
        output.putFloat(this.noiseStartTime);
        output.putFloat(this.extraDamage);
        output.putInt(this.remoteControlId);
        output.putInt(this.timerBeforeExplosion);
        GameWindow.WriteStringUTF(output, this.countDownSound);
        GameWindow.WriteStringUTF(output, this.explosionSound);
        if (this.weapon != null) {
            output.put((byte)1);
            this.weapon.saveWithSize(output, false);
        } else {
            output.put((byte)0);
        }
    }

    @Override
    public void addToWorld() {
        this.getCell().addToProcessIsoObject(this);
    }

    @Override
    public void removeFromWorld() {
        if (this.emitter != null) {
            if (this.noiseStartTime > 0.0F) {
                this.emitter.stopAll();
            }

            IsoWorld.instance.returnOwnershipOfEmitter(this.emitter);
            this.emitter = null;
        }

        super.removeFromWorld();
    }

    public int getTimerBeforeExplosion() {
        return this.timerBeforeExplosion;
    }

    public void setTimerBeforeExplosion(int timerBeforeExplosion) {
        this.timerBeforeExplosion = timerBeforeExplosion;
    }

    public int getSensorRange() {
        return this.sensorRange;
    }

    public void setSensorRange(int sensorRange) {
        this.sensorRange = sensorRange;
    }

    public int getFireRange() {
        return this.fireRange;
    }

    public void setFireRange(int fireRange) {
        this.fireRange = fireRange;
    }

    public int getFireStartingEnergy() {
        return this.fireStartingEnergy;
    }

    public void setFireStartingEnergy(int fireStartingEnergy) {
        this.fireStartingEnergy = fireStartingEnergy;
    }

    public int getFireStartingChance() {
        return this.fireStartingChance;
    }

    public void setFireStartingChance(int fireStartingChance) {
        this.fireStartingChance = fireStartingChance;
    }

    public int getExplosionPower() {
        return this.explosionPower;
    }

    public void setExplosionPower(int explosionPower) {
        this.explosionPower = explosionPower;
    }

    public int getNoiseDuration() {
        return this.noiseDuration;
    }

    public void setNoiseDuration(int noiseDuration) {
        this.noiseDuration = noiseDuration;
    }

    public int getNoiseRange() {
        return this.noiseRange;
    }

    public void setNoiseRange(int noiseRange) {
        this.noiseRange = noiseRange;
    }

    public int getExplosionRange() {
        return this.explosionRange;
    }

    public void setExplosionRange(int explosionRange) {
        this.explosionRange = explosionRange;
    }

    public int getSmokeRange() {
        return this.smokeRange;
    }

    public void setSmokeRange(int smokeRange) {
        this.smokeRange = smokeRange;
    }

    public float getExtraDamage() {
        return this.extraDamage;
    }

    public void setExtraDamage(float extraDamage) {
        this.extraDamage = extraDamage;
    }

    @Override
    public String getObjectName() {
        return "IsoTrap";
    }

    public int getRemoteControlID() {
        return this.remoteControlId;
    }

    public void setRemoteControlID(int remoteControlId) {
        this.remoteControlId = remoteControlId;
    }

    public String getCountDownSound() {
        return this.countDownSound;
    }

    public void setCountDownSound(String sound) {
        this.countDownSound = sound;
    }

    public String getExplosionSound() {
        return this.explosionSound;
    }

    public void setExplosionSound(String explosionSound) {
        this.explosionSound = explosionSound;
    }

    public int getExplosionDuration() {
        return this.explosionDuration;
    }

    public void setExplosionDuration(int minutes) {
        this.explosionDuration = minutes;
    }

    public boolean isExploding() {
        return this.explosionDuration > 0 && this.explosionStartTime > 0.0F;
    }

    @Override
    public InventoryItem getItem() {
        return this.weapon;
    }

    public static void triggerRemote(IsoPlayer player, int remoteID, int range) {
        int px = player.getXi();
        int py = player.getYi();
        int pz = player.getZi();
        int minZ = Math.max(pz - range / 2, 0);
        int maxZ = Math.min(pz + range / 2, 8);
        IsoCell cell = IsoWorld.instance.currentCell;

        for (int z = minZ; z < maxZ; z++) {
            for (int y = py - range; y < py + range; y++) {
                for (int x = px - range; x < px + range; x++) {
                    IsoGridSquare sq = cell.getGridSquare(x, y, z);
                    if (sq != null) {
                        for (int i = sq.getObjects().size() - 1; i >= 0; i--) {
                            IsoObject o = sq.getObjects().get(i);
                            if (o instanceof IsoTrap isoTrap && isoTrap.getRemoteControlID() == remoteID) {
                                isoTrap.triggerExplosion(false);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean isInstantExplosion() {
        return this.instantExplosion;
    }

    public void setInstantExplosion(boolean instantExplosion) {
        this.instantExplosion = instantExplosion;
    }

    public HandWeapon getHandWeapon() {
        return this.weapon;
    }

    public IsoGameCharacter getAttacker() {
        return this.attacker;
    }

    private void drawCircleExplosion(IsoGridSquare square, int radius, IsoTrap.ExplosionMode explosionMode) {
        radius = Math.min(radius, 15);
        int fireStartingPower = this.getFireStartingEnergy();

        for (int x = square.getX() - radius; x <= square.getX() + radius; x++) {
            for (int y = square.getY() - radius; y <= square.getY() + radius; y++) {
                if (!(IsoUtils.DistanceTo(x + 0.5F, y + 0.5F, square.getX() + 0.5F, square.getY() + 0.5F) > radius)) {
                    LosUtil.TestResults testResults = LosUtil.lineClear(
                        square.getCell(),
                        PZMath.fastfloor(this.getX()),
                        PZMath.fastfloor(this.getY()),
                        PZMath.fastfloor(this.getZ()),
                        x,
                        y,
                        square.getZ(),
                        false
                    );
                    if (testResults != LosUtil.TestResults.Blocked && testResults != LosUtil.TestResults.ClearThroughClosedDoor) {
                        IsoGridSquare explodedSquare = this.getCell().getGridSquare(x, y, square.getZ());
                        if (explodedSquare != null && NonPvpZone.getNonPvpZone(explodedSquare.getX(), explodedSquare.getY()) == null) {
                            boolean startFire = Rand.Next(100) < this.getFireStartingChance();
                            if (explosionMode == IsoTrap.ExplosionMode.Smoke) {
                                if (startFire) {
                                    IsoFireManager.StartSmoke(this.getCell(), explodedSquare, true, fireStartingPower, 0);
                                }

                                this.smoke(explodedSquare);
                            }

                            if (explosionMode == IsoTrap.ExplosionMode.Explosion) {
                                boolean burnSquare = Rand.Next(100) < this.getFireStartingChance();
                                if (!GameClient.client && this.getExplosionPower() > 0 && burnSquare) {
                                    explodedSquare.Burn();
                                }

                                this.explosion(explodedSquare);
                                if (startFire) {
                                    IsoFireManager.StartFire(this.getCell(), explodedSquare, true, fireStartingPower);
                                }
                            }

                            if (explosionMode == IsoTrap.ExplosionMode.Fire && startFire) {
                                IsoFireManager.StartFire(this.getCell(), explodedSquare, true, fireStartingPower);
                            }

                            if (explosionMode == IsoTrap.ExplosionMode.Sensor) {
                                explodedSquare.setTrapPositionX(square.getX());
                                explodedSquare.setTrapPositionY(square.getY());
                                explodedSquare.setTrapPositionZ(square.getZ());
                            }
                        }
                    }
                }
            }
        }
    }

    private void explosion(IsoGridSquare isoGridSquare) {
        if (!GameServer.server || !this.isInstantExplosion()) {
            IsoGameCharacter attackingIsoGameCharacter = (IsoGameCharacter)(this.getAttacker() != null
                ? this.getAttacker()
                : IsoWorld.instance.currentCell.getFakeZombieForHit());

            for (int i = 0; i < isoGridSquare.getMovingObjects().size(); i++) {
                IsoMovingObject moving = isoGridSquare.getMovingObjects().get(i);
                if ((!(moving instanceof IsoPlayer) || ServerOptions.getInstance().pvp.getValue()) && moving instanceof IsoGameCharacter isoGameCharacter) {
                    if (!(!GameServer.server && moving instanceof IsoZombie isoZombie) || isoZombie.isLocal()) {
                        int explosionPower = Math.min(this.getExplosionPower(), 100);
                        moving.Hit(
                            this.getHandWeapon(),
                            attackingIsoGameCharacter,
                            Rand.Next(explosionPower / 20.0F, explosionPower / 20.0F * 2.0F) + this.getExtraDamage(),
                            false,
                            1.0F
                        );
                        if (!GameServer.server) {
                            CombatManager.getInstance().processInstantExplosion(isoGameCharacter, this);
                        }

                        int fireStartingChance = this.getFireStartingChance();
                        if (fireStartingChance > 0 && !(moving instanceof IsoZombie)) {
                            int burnAttempts = BodyPartType.MAX.ordinal();

                            for (int j = 0; j < burnAttempts; j++) {
                                if (Rand.Next(100) < fireStartingChance) {
                                    BodyPart bodypart = isoGameCharacter.getBodyDamage().getBodyPart(BodyPartType.FromIndex(j));
                                    bodypart.setBurned();
                                }
                            }
                        }
                    }

                    if (GameClient.client && moving instanceof IsoZombie isoZombie && isoZombie.isRemoteZombie()) {
                        moving.Hit(InventoryItemFactory.CreateItem("Base.Axe"), attackingIsoGameCharacter, 0.0F, true, 0.0F);
                    }
                }
            }
        }
    }

    private void smoke(IsoGridSquare isoGridSquare) {
        for (int i = 0; i < isoGridSquare.getMovingObjects().size(); i++) {
            IsoMovingObject moving = isoGridSquare.getMovingObjects().get(i);
            if (moving instanceof IsoZombie isoZombie) {
                isoZombie.setTarget(null);
                isoZombie.changeState(ZombieIdleState.instance());
            }
        }
    }

    private void explodeTrap(IsoGridSquare isoGridSquare) {
        IsoGridSquare squareTrapped = isoGridSquare.getCell()
            .getGridSquare(isoGridSquare.getTrapPositionX(), isoGridSquare.getTrapPositionY(), isoGridSquare.getTrapPositionZ());
        if (squareTrapped != null) {
            for (int i = 0; i < squareTrapped.getObjects().size(); i++) {
                IsoObject object = squareTrapped.getObjects().get(i);
                if (object instanceof IsoTrap trap) {
                    trap.triggerExplosion(false);
                    int radius = trap.getSensorRange();

                    for (int x = squareTrapped.getX() - radius; x <= squareTrapped.getX() + radius; x++) {
                        for (int y = squareTrapped.getY() - radius; y <= squareTrapped.getY() + radius; y++) {
                            if (IsoUtils.DistanceTo(x + 0.5F, y + 0.5F, squareTrapped.getX() + 0.5F, squareTrapped.getY() + 0.5F) <= radius) {
                                IsoGridSquare explodedSquare = isoGridSquare.getCell().getGridSquare(x, y, isoGridSquare.getZ());
                                if (explodedSquare != null) {
                                    explodedSquare.setTrapPositionX(-1);
                                    explodedSquare.setTrapPositionY(-1);
                                    explodedSquare.setTrapPositionZ(-1);
                                }
                            }
                        }
                    }

                    return;
                }
            }
        }
    }

    public static enum ExplosionMode {
        Explosion,
        Fire,
        Smoke,
        Sensor;
    }
}
