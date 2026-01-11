// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import org.joml.Vector3f;
import zombie.CombatManager;
import zombie.GameTime;
import zombie.IndieGL;
import zombie.SoundManager;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoGameCharacter;
import zombie.config.ConfigFile;
import zombie.config.ConfigOption;
import zombie.config.DoubleConfigOption;
import zombie.core.Core;
import zombie.core.PerformanceSettings;
import zombie.core.math.PZMath;
import zombie.core.physics.BallisticsController;
import zombie.debug.LineDrawer;
import zombie.iso.IsoGridSquare;
import zombie.iso.Vector3;
import zombie.popman.ObjectPool;
import zombie.scripting.objects.AmmoType;

public final class IsoBulletTracerEffects {
    private static IsoBulletTracerEffects instance;
    private final ArrayList<IsoBulletTracerEffects.Effect> effects = new ArrayList<>();
    private final ObjectPool<IsoBulletTracerEffects.Effect> effectPool = new ObjectPool<>(IsoBulletTracerEffects.Effect::new);
    private final HashMap<AmmoType, IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions> isoBulletTracerEffectsConfigOptionsHashMap = new HashMap<>();

    public static IsoBulletTracerEffects getInstance() {
        if (instance == null) {
            instance = new IsoBulletTracerEffects();
        }

        return instance;
    }

    public HashMap<AmmoType, IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions> getIsoBulletTracerEffectsConfigOptionsHashMap() {
        return this.isoBulletTracerEffectsConfigOptionsHashMap;
    }

    public IsoBulletTracerEffects.Effect addEffect(IsoGameCharacter isoGameCharacter, float range) {
        IsoBulletTracerEffects.Effect effect = this.createEffect(isoGameCharacter);
        if (effect == null) {
            return null;
        } else {
            effect.playSound = true;
            float renderedAngle = isoGameCharacter.getLookAngleRadians();
            effect.directionVector.x = (float)Math.cos(renderedAngle);
            effect.directionVector.y = (float)Math.sin(renderedAngle);
            effect.directionVector.z = 0.0F;
            effect.directionVector.normalize(1.0F);
            effect.start.set(effect.x0, effect.y0, effect.z0);
            effect.end
                .set(effect.x0 + effect.directionVector.x * range, effect.y0 + effect.directionVector.y * range, effect.z0 + effect.directionVector.z * range);
            effect.range = range;
            effect.currentRange = effect.start.distance(effect.end);
            return effect;
        }
    }

    public IsoBulletTracerEffects.Effect addEffect(IsoGameCharacter isoGameCharacter, float range, float x, float y, float z) {
        return this.addEffect(isoGameCharacter, range, x, y, z, null);
    }

    public IsoBulletTracerEffects.Effect addEffect(IsoGameCharacter isoGameCharacter, float range, float x, float y, float z, IsoGridSquare isoGridSquare) {
        IsoBulletTracerEffects.Effect effect = this.createEffect(isoGameCharacter);
        if (effect == null) {
            return null;
        } else {
            effect.isoGridSquare = isoGridSquare;
            effect.playSound = true;
            effect.start.set(effect.x0, effect.y0, effect.z0);
            effect.end.set(x, y, z);
            directionVector(effect.directionVector, effect.start, effect.end);
            effect.directionVector.normalize(1.0F);
            effect.range = range;
            effect.currentRange = effect.start.distance(effect.end);
            return effect;
        }
    }

    public static void directionVector(Vector3f directionVector, Vector3f v1, Vector3f v2) {
        directionVector.set(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
    }

    private IsoBulletTracerEffects.Effect createEffect(IsoGameCharacter isoGameCharacter) {
        if (isoGameCharacter != null && isoGameCharacter.getAnimationPlayer().isReady()) {
            BallisticsController ballisticsController = isoGameCharacter.getBallisticsController();
            if (ballisticsController == null) {
                return null;
            } else {
                IsoBulletTracerEffects.Effect effect = this.effectPool.alloc();
                effect.ammoType = isoGameCharacter.getAttackingWeapon().getAmmoType();
                IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions isoBulletTracerEffectsConfigOption = this.isoBulletTracerEffectsConfigOptionsHashMap
                    .get(effect.ammoType);
                effect.x0 = isoGameCharacter.getX();
                effect.y0 = isoGameCharacter.getY();
                effect.z0 = isoGameCharacter.getZ();
                effect.projectileRed = (float)isoBulletTracerEffectsConfigOption.projectileRed.getValue();
                effect.projectileGreen = (float)isoBulletTracerEffectsConfigOption.projectileGreen.getValue();
                effect.projectileBlue = (float)isoBulletTracerEffectsConfigOption.projectileBlue.getValue();
                effect.projectileAlpha = (float)isoBulletTracerEffectsConfigOption.projectileAlpha.getValue();
                effect.projectileLength = (float)isoBulletTracerEffectsConfigOption.projectileLength.getValue();
                effect.projectileStartThickness = (float)isoBulletTracerEffectsConfigOption.projectileStartThickness.getValue();
                effect.projectileEndThickness = (float)isoBulletTracerEffectsConfigOption.projectileEndThickness.getValue();
                effect.projectileFadeRate = (float)isoBulletTracerEffectsConfigOption.projectileFadeRate.getValue();
                effect.projectileTrailRed = (float)isoBulletTracerEffectsConfigOption.projectileTrailRed.getValue();
                effect.projectileTrailGreen = (float)isoBulletTracerEffectsConfigOption.projectileTrailGreen.getValue();
                effect.projectileTrailBlue = (float)isoBulletTracerEffectsConfigOption.projectileTrailBlue.getValue();
                effect.projectileTrailAlpha = (float)isoBulletTracerEffectsConfigOption.projectileTrailAlpha.getValue();
                effect.projectileTrailLength = (float)isoBulletTracerEffectsConfigOption.projectileTrailLength.getValue();
                effect.projectileTrailStartThickness = (float)isoBulletTracerEffectsConfigOption.projectileTrailStartThickness.getValue();
                effect.projectileTrailEndThickness = (float)isoBulletTracerEffectsConfigOption.projectileTrailEndThickness.getValue();
                effect.projectilePathRed = (float)isoBulletTracerEffectsConfigOption.projectilePathRed.getValue();
                effect.projectilePathGreen = (float)isoBulletTracerEffectsConfigOption.projectilePathGreen.getValue();
                effect.projectilePathBlue = (float)isoBulletTracerEffectsConfigOption.projectilePathBlue.getValue();
                effect.projectilePathAlpha = (float)isoBulletTracerEffectsConfigOption.projectilePathAlpha.getValue();
                effect.projectilePathStartThickness = (float)isoBulletTracerEffectsConfigOption.projectilePathStartThickness.getValue();
                effect.projectilePathEndThickness = (float)isoBulletTracerEffectsConfigOption.projectilePathEndThickness.getValue();
                effect.projectilePathFadeRate = (float)isoBulletTracerEffectsConfigOption.projectilePathFadeRate.getValue();
                effect.projectilePathTime = (float)isoBulletTracerEffectsConfigOption.projectilePathTime.getValue();
                effect.projectileSpeed = (float)isoBulletTracerEffectsConfigOption.projectileSpeed.getValue();
                ballisticsController.update();
                Vector3 muzzlePosition = ballisticsController.getMuzzlePosition();
                effect.x0 = muzzlePosition.x;
                effect.y0 = muzzlePosition.y;
                effect.z0 = muzzlePosition.z;
                effect.timer = 0.0F;
                effect.projectilePathTimer = 0.0F;
                effect.currentProjectileAlpha = effect.projectileAlpha;
                this.effects.add(effect);
                return effect;
            }
        } else {
            return null;
        }
    }

    private void updateSettings(AmmoType ammoType, IsoBulletTracerEffects.Effect effect) {
        IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions isoBulletTracerEffectsConfigOption = this.isoBulletTracerEffectsConfigOptionsHashMap
            .get(ammoType);
        effect.projectileRed = (float)isoBulletTracerEffectsConfigOption.projectileRed.getValue();
        effect.projectileGreen = (float)isoBulletTracerEffectsConfigOption.projectileGreen.getValue();
        effect.projectileBlue = (float)isoBulletTracerEffectsConfigOption.projectileBlue.getValue();
        effect.projectileAlpha = (float)isoBulletTracerEffectsConfigOption.projectileAlpha.getValue();
        effect.projectileLength = (float)isoBulletTracerEffectsConfigOption.projectileLength.getValue();
        effect.projectileStartThickness = (float)isoBulletTracerEffectsConfigOption.projectileStartThickness.getValue();
        effect.projectileEndThickness = (float)isoBulletTracerEffectsConfigOption.projectileEndThickness.getValue();
        effect.projectileFadeRate = (float)isoBulletTracerEffectsConfigOption.projectileFadeRate.getValue();
        effect.projectileTrailRed = (float)isoBulletTracerEffectsConfigOption.projectileTrailRed.getValue();
        effect.projectileTrailGreen = (float)isoBulletTracerEffectsConfigOption.projectileTrailGreen.getValue();
        effect.projectileTrailBlue = (float)isoBulletTracerEffectsConfigOption.projectileTrailBlue.getValue();
        effect.projectileTrailAlpha = (float)isoBulletTracerEffectsConfigOption.projectileTrailAlpha.getValue();
        effect.projectileTrailLength = (float)isoBulletTracerEffectsConfigOption.projectileTrailLength.getValue();
        effect.projectileTrailStartThickness = (float)isoBulletTracerEffectsConfigOption.projectileTrailStartThickness.getValue();
        effect.projectileTrailEndThickness = (float)isoBulletTracerEffectsConfigOption.projectileTrailEndThickness.getValue();
        effect.projectilePathRed = (float)isoBulletTracerEffectsConfigOption.projectilePathRed.getValue();
        effect.projectilePathGreen = (float)isoBulletTracerEffectsConfigOption.projectilePathGreen.getValue();
        effect.projectilePathBlue = (float)isoBulletTracerEffectsConfigOption.projectilePathBlue.getValue();
        effect.projectilePathAlpha = (float)isoBulletTracerEffectsConfigOption.projectilePathAlpha.getValue();
        effect.projectilePathStartThickness = (float)isoBulletTracerEffectsConfigOption.projectilePathStartThickness.getValue();
        effect.projectilePathEndThickness = (float)isoBulletTracerEffectsConfigOption.projectilePathEndThickness.getValue();
        effect.projectilePathFadeRate = (float)isoBulletTracerEffectsConfigOption.projectilePathFadeRate.getValue();
        effect.projectilePathTime = (float)isoBulletTracerEffectsConfigOption.projectilePathTime.getValue();
        effect.projectileSpeed = (float)isoBulletTracerEffectsConfigOption.projectileSpeed.getValue();
    }

    public void render() {
        float timeDelta = GameTime.getInstance().getMultiplier() / 1.6F;
        IndieGL.glEnable(2848);
        IndieGL.glBlendFunc(770, 1);
        if (PerformanceSettings.fboRenderChunk) {
            IndieGL.disableDepthTest();
            IndieGL.StartShader(0);
        }

        for (int i = 0; i < this.effects.size(); i++) {
            IsoBulletTracerEffects.Effect effect = this.effects.get(i);
            if (Core.debug) {
                this.updateSettings(effect.ammoType, effect);
            }

            effect.render();
            if (!GameTime.isGamePaused()) {
                effect.timer += timeDelta;
                effect.projectilePathTimer += timeDelta;
            }

            float distance = effect.start.distance(effect.current);
            if (distance >= effect.currentRange - effect.projectileLength) {
                this.effects.remove(i--);
                this.effectPool.release(effect);
            }
        }
    }

    public void save(AmmoType ammoType) {
        IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions isoBulletTracerEffectsConfigOption = this.getOrCreate(ammoType);
        String ammoTypeName = ammoType.toString().replaceAll("[.:]", "_");
        String fileName = ZomboidFileSystem.instance.getMediaRootPath()
            + File.separator
            + "effects"
            + File.separator
            + "bullet_tracer_effect_"
            + ammoTypeName
            + ".txt";
        ConfigFile configFile = new ConfigFile();
        configFile.write(fileName, 1, isoBulletTracerEffectsConfigOption.options);
    }

    public void load(AmmoType ammoType) {
        if (!this.isoBulletTracerEffectsConfigOptionsHashMap.containsKey(ammoType)) {
            IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions isoBulletTracerEffectsConfigOption = this.getOrCreate(ammoType);
            String ammoTypeName = ammoType.toString().replaceAll("[.:]", "_");
            String fileName = ZomboidFileSystem.instance.getMediaRootPath()
                + File.separator
                + "effects"
                + File.separator
                + "bullet_tracer_effect_"
                + ammoTypeName
                + ".txt";
            ConfigFile configFile = new ConfigFile();
            if (configFile.read(fileName)) {
                for (int i = 0; i < configFile.getOptions().size(); i++) {
                    ConfigOption configOption = configFile.getOptions().get(i);
                    ConfigOption myOption = isoBulletTracerEffectsConfigOption.getOptionByName(configOption.getName());
                    if (myOption != null) {
                        myOption.parse(configOption.getValueAsString());
                    }
                }
            }
        }
    }

    public void reset(AmmoType ammoType) {
        IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions isoBulletTracerEffectsConfigOption = this.isoBulletTracerEffectsConfigOptionsHashMap
            .get(ammoType);
        int optionCount = isoBulletTracerEffectsConfigOption.getOptionCount();

        for (int i = 0; i < optionCount; i++) {
            IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption configOption = (IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption)isoBulletTracerEffectsConfigOption.getOptionByIndex(
                i
            );
            configOption.setValue(configOption.getDefaultValue());
        }
    }

    private IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions getOrCreate(AmmoType ammoType) {
        IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions isoBulletTracerEffectsConfigOption = this.isoBulletTracerEffectsConfigOptionsHashMap
            .get(ammoType);
        if (isoBulletTracerEffectsConfigOption == null) {
            isoBulletTracerEffectsConfigOption = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOptions();
            this.isoBulletTracerEffectsConfigOptionsHashMap.put(ammoType, isoBulletTracerEffectsConfigOption);
        }

        return isoBulletTracerEffectsConfigOption;
    }

    public static final class Effect {
        private AmmoType ammoType;
        private float x0;
        private float y0;
        private float z0;
        private float projectileRed;
        private float projectileGreen;
        private float projectileBlue;
        private float projectileAlpha;
        private float projectileLength;
        private float projectileStartThickness;
        private float projectileEndThickness;
        private float projectileFadeRate;
        private float projectileTrailRed;
        private float projectileTrailGreen;
        private float projectileTrailBlue;
        private float projectileTrailAlpha;
        private float projectileTrailLength;
        private float projectileTrailStartThickness;
        private float projectileTrailEndThickness;
        private float projectilePathRed;
        private float projectilePathGreen;
        private float projectilePathBlue;
        private float projectilePathAlpha;
        private float projectilePathStartThickness;
        private float projectilePathEndThickness;
        private float projectilePathFadeRate;
        private float projectilePathTime;
        private float projectilePathTimer;
        private float projectileSpeed;
        private float range;
        private float currentRange;
        private float currentProjectileAlpha;
        private float timer;
        private IsoGridSquare isoGridSquare;
        private boolean playSound;
        private final Vector3f start = new Vector3f();
        private final Vector3f end = new Vector3f();
        private final Vector3f current = new Vector3f();
        private final Vector3f directionVector = new Vector3f();
        private static final Vector3f gcTempVector3f = new Vector3f();
        private static final Vector3f gcTempVector3f2 = new Vector3f();

        public void setRange(float range) {
            if (range < this.currentRange) {
                this.currentRange = range;
            }
        }

        public void render() {
            float speed = this.projectileSpeed / 200.0F;
            float distance = this.timer * speed;
            this.current
                .set(this.x0 + this.directionVector.x * distance, this.y0 + this.directionVector.y * distance, this.z0 + this.directionVector.z * distance);
            float relativeDistance = 1.0F;
            if (distance < this.currentRange) {
                relativeDistance = PZMath.clamp(distance / this.currentRange, 0.0F, 1.0F);
            }

            float pathAlpha = this.projectilePathAlpha * (1.0F - relativeDistance * this.projectilePathFadeRate);
            float trailAlpha = this.projectileTrailAlpha * (1.0F - relativeDistance);
            boolean continueMoving = this.start.distance(this.current) <= this.start.distance(this.end);
            if (continueMoving) {
                LineDrawer.DrawIsoLine(
                    this.start.x,
                    this.start.y,
                    this.start.z,
                    this.current.x,
                    this.current.y,
                    this.current.z,
                    this.projectilePathRed,
                    this.projectilePathGreen,
                    this.projectilePathBlue,
                    pathAlpha,
                    this.projectilePathStartThickness,
                    this.projectilePathEndThickness
                );
            } else {
                LineDrawer.DrawIsoLine(
                    this.start.x,
                    this.start.y,
                    this.start.z,
                    this.end.x,
                    this.end.y,
                    this.end.z,
                    this.projectilePathRed,
                    this.projectilePathGreen,
                    this.projectilePathBlue,
                    pathAlpha,
                    this.projectilePathStartThickness,
                    this.projectilePathEndThickness
                );
            }

            if (this.start.distance(this.current) > this.projectileTrailLength && continueMoving) {
                gcTempVector3f.set(
                    this.x0 + this.directionVector.x * (distance - this.projectileTrailLength),
                    this.y0 + this.directionVector.y * (distance - this.projectileTrailLength),
                    this.z0 + this.directionVector.z * (distance - this.projectileTrailLength)
                );
                gcTempVector3f2.set(
                    gcTempVector3f.x + this.directionVector.x * this.projectileTrailLength,
                    gcTempVector3f.y + this.directionVector.y * this.projectileTrailLength,
                    gcTempVector3f.z + this.directionVector.z * this.projectileTrailLength
                );
                LineDrawer.DrawIsoLine(
                    gcTempVector3f.x,
                    gcTempVector3f.y,
                    gcTempVector3f.z,
                    gcTempVector3f2.x,
                    gcTempVector3f2.y,
                    gcTempVector3f2.z,
                    this.projectileTrailRed,
                    this.projectileTrailGreen,
                    this.projectileTrailBlue,
                    trailAlpha,
                    this.projectileTrailStartThickness,
                    this.projectileTrailEndThickness
                );
            }

            if (continueMoving) {
                if (this.start.distance(this.current) > this.start.distance(this.end) - this.projectileLength * (1.0F / this.projectileFadeRate)) {
                    this.currentProjectileAlpha = this.currentProjectileAlpha - this.projectileAlpha * this.projectileFadeRate;
                    if (this.isoGridSquare != null && this.playSound) {
                        this.playSound = false;
                        if (!CombatManager.getInstance().hitIsoGridSquare(this.isoGridSquare, this.current)) {
                            SoundManager.instance.playImpactSound(this.isoGridSquare, this.ammoType);
                        }
                    }
                }

                gcTempVector3f.set(
                    this.current.x + this.directionVector.x * this.projectileLength,
                    this.current.y + this.directionVector.y * this.projectileLength,
                    this.current.z + this.directionVector.z * this.projectileLength
                );
                LineDrawer.DrawIsoLine(
                    this.current.x,
                    this.current.y,
                    this.current.z,
                    gcTempVector3f.x,
                    gcTempVector3f.y,
                    gcTempVector3f.z,
                    this.projectileRed,
                    this.projectileGreen,
                    this.projectileBlue,
                    this.currentProjectileAlpha,
                    this.projectileStartThickness,
                    this.projectileEndThickness
                );
            } else if (this.isoGridSquare != null && this.playSound) {
                this.playSound = false;
                if (!CombatManager.getInstance().hitIsoGridSquare(this.isoGridSquare, this.current)) {
                    SoundManager.instance.playImpactSound(this.isoGridSquare, this.ammoType);
                }
            }
        }
    }

    public static class IsoBulletTracerEffectsConfigOption extends DoubleConfigOption {
        public IsoBulletTracerEffectsConfigOption(String name, double min, double max, double defaultValue, ArrayList<ConfigOption> options) {
            super(name, min, max, defaultValue);
            options.add(this);
        }
    }

    public static class IsoBulletTracerEffectsConfigOptions {
        private static final int VERSION = 1;
        private final ArrayList<ConfigOption> options = new ArrayList<>();
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileRed = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileRed", 0.0, 1.0, 1.0, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileGreen = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileGreen", 0.0, 1.0, 0.84F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileBlue = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileBlue", 0.0, 1.0, 0.53F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileAlpha = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileAlpha", 0.0, 1.0, 0.81F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileLength = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileLength", 0.01F, 1.0, 0.2475F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileStartThickness = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileStartThickness", 0.01F, 5.0, 0.8982F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileEndThickness = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileEndThickness", 0.01F, 5.0, 0.9481F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileFadeRate = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileFadeRate", 0.0, 1.0, 0.06F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileTrailRed = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileTrailRed", 0.0, 1.0, 1.0, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileTrailGreen = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileTrailGreen", 0.0, 1.0, 0.84F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileTrailBlue = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileTrailBlue", 0.0, 1.0, 0.61F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileTrailAlpha = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileTrailAlpha", 0.0, 1.0, 0.36F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileTrailLength = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileTrailLength", 0.01F, 5.0, 0.8982F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileTrailStartThickness = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileTrailStartThickness", 0.01F, 5.0, 0.0499F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileTrailEndThickness = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileTrailEndThickness", 0.01F, 5.0, 1.1477F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectilePathRed = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectilePathRed", 0.0, 1.0, 1.0, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectilePathGreen = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectilePathGreen", 0.0, 1.0, 1.0, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectilePathBlue = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectilePathBlue", 0.0, 1.0, 1.0, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectilePathAlpha = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectilePathAlpha", 0.0, 1.0, 0.44F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectilePathStartThickness = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectilePathStartThickness", 0.01F, 5.0, 0.01F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectilePathEndThickness = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectilePathEndThickness", 0.01F, 5.0, 0.55F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectilePathFadeRate = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectilePathFadeRate", 0.0, 2.0, 0.86F, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectilePathTime = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectilePathTime", 0.0, 10.0, 5.0, this.options
        );
        private final IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption projectileSpeed = new IsoBulletTracerEffects.IsoBulletTracerEffectsConfigOption(
            "ProjectileSpeed", 1.0, 4000.0, 1679.58F, this.options
        );

        public int getOptionCount() {
            return this.options.size();
        }

        public ConfigOption getOptionByIndex(int index) {
            return this.options.get(index);
        }

        public ConfigOption getOptionByName(String name) {
            for (ConfigOption setting : this.options) {
                if (setting.getName().equals(name)) {
                    return setting;
                }
            }

            return null;
        }
    }
}
