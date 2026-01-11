// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.objects;

import java.util.ArrayDeque;
import java.util.ArrayList;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.core.random.Rand;
import zombie.iso.IsoObject;
import zombie.iso.weather.ClimateManager;
import zombie.network.GameServer;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class ObjectRenderEffects {
    public static final boolean ENABLED = true;
    private static final ArrayDeque<ObjectRenderEffects> pool = new ArrayDeque<>();
    public double x1;
    public double y1;
    public double x2;
    public double y2;
    public double x3;
    public double y3;
    public double x4;
    public double y4;
    private double tx1;
    private double ty1;
    private double tx2;
    private double ty2;
    private double tx3;
    private double ty3;
    private double tx4;
    private double ty4;
    private double lx1;
    private double ly1;
    private double lx2;
    private double ly2;
    private double lx3;
    private double ly3;
    private double lx4;
    private double ly4;
    private double maxX;
    private double maxY;
    private float curTime;
    private float maxTime;
    private float totalTime;
    private float totalMaxTime;
    private RenderEffectType type;
    private IsoObject parent;
    private boolean finish;
    private boolean isTree;
    private boolean isBig;
    private boolean gust;
    private int windType = 1;
    private static final float T_MOD = 1.0F;
    private static int windCount;
    private static int windCountTree;
    private static final int EFFECTS_COUNT = 15;
    private static final int TYPE_COUNT = 3;
    private static final ObjectRenderEffects[][] WIND_EFFECTS = new ObjectRenderEffects[3][15];
    private static final ObjectRenderEffects[][] WIND_EFFECTS_TREES = new ObjectRenderEffects[3][15];
    private static final ArrayList<ObjectRenderEffects> DYNAMIC_EFFECTS = new ArrayList<>();
    private static ObjectRenderEffects randomRustle;
    private static float randomRustleTime;
    private static float randomRustleTotalTime;
    private static int randomRustleTarget;
    private static int randomRustleType;

    public static ObjectRenderEffects alloc() {
        return !pool.isEmpty() ? pool.pop() : new ObjectRenderEffects();
    }

    public static void release(ObjectRenderEffects o) {
        assert !pool.contains(o);

        pool.push(o.reset());
    }

    private ObjectRenderEffects() {
    }

    private ObjectRenderEffects reset() {
        this.parent = null;
        this.finish = false;
        this.isBig = false;
        this.isTree = false;
        this.curTime = 0.0F;
        this.maxTime = 0.0F;
        this.totalTime = 0.0F;
        this.totalMaxTime = 0.0F;
        this.x1 = 0.0;
        this.y1 = 0.0;
        this.x2 = 0.0;
        this.y2 = 0.0;
        this.x3 = 0.0;
        this.y3 = 0.0;
        this.x4 = 0.0;
        this.y4 = 0.0;
        this.tx1 = 0.0;
        this.ty1 = 0.0;
        this.tx2 = 0.0;
        this.ty2 = 0.0;
        this.tx3 = 0.0;
        this.ty3 = 0.0;
        this.tx4 = 0.0;
        this.ty4 = 0.0;
        this.swapTargetToLast();
        return this;
    }

    public static ObjectRenderEffects getNew(IsoObject parent, RenderEffectType t, boolean reuseEqualType) {
        return getNew(parent, t, reuseEqualType, false);
    }

    public static ObjectRenderEffects getNew(IsoObject parent, RenderEffectType t, boolean reuseEqualType, boolean dontAdd) {
        if (GameServer.server) {
            return null;
        } else if (t == RenderEffectType.Hit_Door && !Core.getInstance().getOptionDoDoorSpriteEffects()) {
            return null;
        } else {
            ObjectRenderEffects e = null;

            try {
                boolean reusing = false;
                if (reuseEqualType && parent != null && parent.getObjectRenderEffects() != null && parent.getObjectRenderEffects().type == t) {
                    e = parent.getObjectRenderEffects();
                    reusing = true;
                } else {
                    e = alloc();
                }

                e.type = t;
                e.parent = parent;
                e.finish = false;
                e.isBig = false;
                e.totalTime = 0.0F;
                switch (t) {
                    case Hit_Tree_Shudder:
                        e.totalMaxTime = Rand.Next(45.0F, 60.0F) * 1.0F;
                        break;
                    case Vegetation_Rustle:
                        e.totalMaxTime = Rand.Next(45.0F, 60.0F) * 1.0F;
                        if (parent != null && parent instanceof IsoTree isoTree) {
                            e.isTree = true;
                            e.isBig = isoTree.size > 4;
                        }
                        break;
                    case Hit_Door:
                        e.totalMaxTime = Rand.Next(15.0F, 30.0F) * 1.0F;
                }

                if (!reusing && parent != null && parent.getWindRenderEffects() != null && Core.getInstance().getOptionDoWindSpriteEffects()) {
                    e.copyMainFromOther(parent.getWindRenderEffects());
                }

                if (!reusing && !dontAdd) {
                    DYNAMIC_EFFECTS.add(e);
                }
            } catch (Exception var7) {
                var7.printStackTrace();
            }

            return e;
        }
    }

    public static ObjectRenderEffects getNextWindEffect(int windType, boolean isTreeLike) {
        int id = windType - 1;
        if (id < 0 || id >= 3) {
            return null;
        } else if (isTreeLike) {
            if (++windCountTree >= 15) {
                windCountTree = 0;
            }

            return WIND_EFFECTS_TREES[id][windCountTree];
        } else {
            if (++windCount >= 15) {
                windCount = 0;
            }

            return WIND_EFFECTS[id][windCount];
        }
    }

    public static void init() {
        if (!GameServer.server) {
            for (int id = 0; id < 3; id++) {
                for (int i = 0; i < 15; i++) {
                    ObjectRenderEffects r = new ObjectRenderEffects();
                    r.windType = id + 1;
                    WIND_EFFECTS[id][i] = r;
                }

                for (int i = 0; i < 15; i++) {
                    ObjectRenderEffects r = new ObjectRenderEffects();
                    r.isTree = true;
                    r.windType = id + 1;
                    WIND_EFFECTS_TREES[id][i] = r;
                }
            }

            DYNAMIC_EFFECTS.clear();
            windCount = 0;
            windCountTree = 0;
            randomRustle = null;
            randomRustleTime = 0.0F;
            randomRustleTotalTime = 0.0F;
            randomRustleTarget = 0;
        }
    }

    public boolean update() {
        this.curTime = this.curTime + 1.0F * GameTime.getInstance().getMultiplier();
        this.totalTime = this.totalTime + 1.0F * GameTime.getInstance().getMultiplier();
        if (this.curTime > this.maxTime) {
            if (this.finish) {
                return false;
            }

            this.curTime = 0.0F;
            this.swapTargetToLast();
            float t = ClimateManager.clamp01(this.totalTime / this.totalMaxTime);
            float ti = 1.0F - t;
            switch (this.type) {
                case Hit_Tree_Shudder:
                    if (this.totalTime > this.totalMaxTime) {
                        this.maxTime = 10.0F;
                        this.tx1 = 0.0;
                        this.tx2 = 0.0;
                        this.finish = true;
                    } else {
                        this.maxTime = (3.0F + 15.0F * t) * 1.0F;
                        double x = this.isBig ? Rand.Next(-0.01F + -0.08F * ti, 0.01F + 0.08F * ti) : Rand.Next(-0.02F + -0.16F * ti, 0.02F + 0.16F * ti);
                        this.tx1 = x;
                        this.tx2 = x;
                    }
                    break;
                case Vegetation_Rustle:
                    if (this.totalTime > this.totalMaxTime) {
                        this.maxTime = 3.0F;
                        this.tx1 = 0.0;
                        this.tx2 = 0.0;
                        this.finish = true;
                    } else {
                        this.maxTime = (2.0F + 6.0F * t) * 1.0F;
                        double x = this.isBig ? Rand.Next(-0.00625F, 0.00625F) : Rand.Next(-0.015F, 0.015F);
                        double y = this.isBig ? Rand.Next(-0.00625F, 0.00625F) : Rand.Next(-0.015F, 0.015F);
                        if (ClimateManager.getWindTickFinal() < 0.15) {
                            x *= 0.6;
                            y *= 0.6;
                        }

                        this.tx1 = x;
                        this.ty1 = y;
                        this.tx2 = x;
                        this.ty2 = y;
                    }
                    break;
                case Hit_Door:
                    if (this.totalTime > this.totalMaxTime) {
                        this.maxTime = 3.0F;
                        this.tx1 = 0.0;
                        this.tx2 = 0.0;
                        this.finish = true;
                    } else {
                        this.maxTime = (1.0F + 2.0F * t) * 1.0F;
                        double x = Rand.Next(-0.005F, 0.005F);
                        double y = Rand.Next(-0.0075F, 0.0075F);
                        this.tx1 = x;
                        this.ty1 = y;
                        this.tx2 = x;
                        this.ty2 = y;
                        this.tx3 = x;
                        this.ty3 = y;
                        this.tx4 = x;
                        this.ty4 = y;
                    }
                    break;
                default:
                    this.finish = true;
            }
        }

        this.lerpAll(this.curTime / this.maxTime);
        if (this.parent != null && this.parent.getWindRenderEffects() != null && Core.getInstance().getOptionDoWindSpriteEffects()) {
            this.add(this.parent.getWindRenderEffects());
        }

        return true;
    }

    private void update(float wind, float angle) {
        this.curTime = this.curTime + 1.0F * GameTime.getInstance().getMultiplier();
        if (this.curTime >= this.maxTime) {
            this.swapTargetToLast();
            if (this.isTree) {
                float skew = 0.0F;
                float skewY = 0.04F;
                if (this.windType == 1) {
                    skew = 0.6F;
                    wind = wind <= 0.08F ? 0.0F : (wind - 0.08F) / 0.92F;
                } else if (this.windType == 2) {
                    skew = 0.3F;
                    skewY = 0.06F;
                    wind = wind <= 0.15F ? 0.0F : (wind - 0.15F) / 0.85F;
                } else if (this.windType == 3) {
                    skew = 0.15F;
                    wind = wind <= 0.3F ? 0.0F : (wind - 0.3F) / 0.7F;
                }

                float windInv = ClimateManager.clamp01(1.0F - wind);
                this.curTime = 0.0F;
                this.maxTime = Rand.Next(20.0F + 100.0F * windInv, 70.0F + 200.0F * windInv) * 1.0F;
                if (wind <= 0.01F || !Core.getInstance().getOptionDoWindSpriteEffects()) {
                    this.tx1 = 0.0;
                    this.tx2 = 0.0;
                    this.ty1 = 0.0;
                    this.ty2 = 0.0;
                    return;
                }

                float windShakeMod = 0.6F * wind + 0.4F * (wind * wind);
                double x;
                if (this.gust) {
                    x = Rand.Next(-0.1F + 0.6F * wind, 1.0F) * angle;
                    if (Rand.Next(0.0F, 1.0F) > Rand.Next(0.0F, 0.75F * wind)) {
                        this.gust = false;
                    }
                } else {
                    x = Rand.Next(-0.1F, 0.2F) * angle;
                    this.gust = true;
                }

                x *= skew * windShakeMod;
                this.tx1 = x;
                this.tx2 = x;
                double y = Rand.Next(-1.0F, 1.0F);
                y *= 0.01 + skewY * windShakeMod;
                this.ty1 = y;
                y = Rand.Next(-1.0F, 1.0F);
                y *= 0.01 + skewY * windShakeMod;
                this.ty2 = y;
            } else {
                float skewx = 0.0F;
                if (this.windType == 1) {
                    skewx = 0.575F;
                    wind = wind <= 0.02F ? 0.0F : (wind - 0.02F) / 0.98F;
                } else if (this.windType == 2) {
                    skewx = 0.375F;
                    wind = wind <= 0.2F ? 0.0F : (wind - 0.2F) / 0.8F;
                } else if (this.windType == 3) {
                    skewx = 0.175F;
                    wind = wind <= 0.6F ? 0.0F : (wind - 0.6F) / 0.4F;
                }

                float windInvx = ClimateManager.clamp01(1.0F - wind);
                this.curTime = 0.0F;
                this.maxTime = Rand.Next(20.0F + 50.0F * windInvx, 60.0F + 100.0F * windInvx) * 1.0F;
                if (wind <= 0.05F || !Core.getInstance().getOptionDoWindSpriteEffects()) {
                    this.tx1 = 0.0;
                    this.tx2 = 0.0;
                    this.ty1 = 0.0;
                    this.ty2 = 0.0;
                    return;
                }

                float windShakeMod = 0.55F * wind + 0.45F * (wind * wind);
                double x;
                if (this.gust) {
                    x = Rand.Next(-0.1F + 0.9F * wind, 1.0F) * angle;
                    if (Rand.Next(0.0F, 1.0F) > Rand.Next(0.0F, 0.95F * wind)) {
                        this.gust = false;
                    }
                } else {
                    x = Rand.Next(-0.1F, 0.2F) * angle;
                    this.gust = true;
                }

                x *= 0.025F + skewx * windShakeMod;
                this.tx1 = x;
                this.tx2 = x;
                if (wind > 0.5F) {
                    double y = Rand.Next(-1.0F, 1.0F);
                    y *= 0.05F * windShakeMod;
                    this.ty1 = y;
                    y = Rand.Next(-1.0F, 1.0F);
                    y *= 0.05F * windShakeMod;
                    this.ty2 = y;
                } else {
                    this.ty1 = 0.0;
                    this.ty2 = 0.0;
                }
            }
        } else {
            this.lerpAll(this.curTime / this.maxTime);
        }
    }

    private void updateOLD(float wind, float angle) {
        this.curTime = this.curTime + 1.0F * GameTime.getInstance().getMultiplier();
        if (this.curTime >= this.maxTime) {
            this.curTime = 0.0F;
            float windInv = ClimateManager.clamp01(1.0F - wind);
            this.maxTime = Rand.Next(20.0F + 100.0F * windInv, 70.0F + 200.0F * windInv) * 1.0F;
            this.swapTargetToLast();
            float var9 = ClimateManager.clamp01(wind * 1.25F);
            double x = Rand.Next(-0.65F, 0.65F);
            x += wind * angle * 0.7F;
            x *= 0.4F * var9;
            this.tx1 = x;
            this.tx2 = x;
            double y = Rand.Next(-1.0F, 1.0F);
            y *= 0.05F * var9;
            this.ty1 = y;
            y = Rand.Next(-1.0F, 1.0F);
            y *= 0.05F * var9;
            this.ty2 = y;
        } else {
            this.lerpAll(this.curTime / this.maxTime);
        }
    }

    private void lerpAll(float t) {
        this.x1 = ClimateManager.clerp(t, (float)this.lx1, (float)this.tx1);
        this.y1 = ClimateManager.clerp(t, (float)this.ly1, (float)this.ty1);
        this.x2 = ClimateManager.clerp(t, (float)this.lx2, (float)this.tx2);
        this.y2 = ClimateManager.clerp(t, (float)this.ly2, (float)this.ty2);
        this.x3 = ClimateManager.clerp(t, (float)this.lx3, (float)this.tx3);
        this.y3 = ClimateManager.clerp(t, (float)this.ly3, (float)this.ty3);
        this.x4 = ClimateManager.clerp(t, (float)this.lx4, (float)this.tx4);
        this.y4 = ClimateManager.clerp(t, (float)this.ly4, (float)this.ty4);
    }

    private void swapTargetToLast() {
        this.lx1 = this.tx1;
        this.ly1 = this.ty1;
        this.lx2 = this.tx2;
        this.ly2 = this.ty2;
        this.lx3 = this.tx3;
        this.ly3 = this.ty3;
        this.lx4 = this.tx4;
        this.ly4 = this.ty4;
    }

    public void copyMainFromOther(ObjectRenderEffects other) {
        this.x1 = other.x1;
        this.y1 = other.y1;
        this.x2 = other.x2;
        this.y2 = other.y2;
        this.x3 = other.x3;
        this.y3 = other.y3;
        this.x4 = other.x4;
        this.y4 = other.y4;
    }

    public void add(ObjectRenderEffects other) {
        this.x1 = this.x1 + other.x1;
        this.y1 = this.y1 + other.y1;
        this.x2 = this.x2 + other.x2;
        this.y2 = this.y2 + other.y2;
        this.x3 = this.x3 + other.x3;
        this.y3 = this.y3 + other.y3;
        this.x4 = this.x4 + other.x4;
        this.y4 = this.y4 + other.y4;
    }

    public static void updateStatic() {
        if (!GameServer.server) {
            try {
                float wind = (float)ClimateManager.getWindTickFinal();
                float angle = ClimateManager.getInstance().getWindAngleIntensity();
                if (angle < 0.0F) {
                    angle = -1.0F;
                } else {
                    angle = 1.0F;
                }

                for (int id = 0; id < 3; id++) {
                    for (int i = 0; i < 15; i++) {
                        ObjectRenderEffects r = WIND_EFFECTS[id][i];
                        r.update(wind, angle);
                    }

                    for (int i = 0; i < 15; i++) {
                        ObjectRenderEffects r = WIND_EFFECTS_TREES[id][i];
                        r.update(wind, angle);
                    }
                }

                randomRustleTime = randomRustleTime + 1.0F * GameTime.getInstance().getMultiplier();
                if (randomRustleTime > randomRustleTotalTime && randomRustle == null) {
                    float windInv = 1.0F - wind;
                    randomRustle = getNew(null, RenderEffectType.Vegetation_Rustle, false, true);
                    randomRustle.isBig = false;
                    if (wind > 0.45F && Rand.Next(0.0F, 1.0F) < Rand.Next(0.0F, 0.8F * wind)) {
                        randomRustle.isBig = true;
                    }

                    randomRustleType = Rand.Next(3);
                    randomRustleTarget = Rand.Next(15);
                    randomRustleTime = 0.0F;
                    randomRustleTotalTime = Rand.Next(400.0F + 400.0F * windInv, 1200.0F + 3200.0F * windInv);
                }

                if (randomRustle != null) {
                    if (!randomRustle.update()) {
                        release(randomRustle);
                        randomRustle = null;
                    } else {
                        ObjectRenderEffects o = WIND_EFFECTS_TREES[randomRustleType][randomRustleTarget];
                        o.add(randomRustle);
                    }
                }

                for (int i = DYNAMIC_EFFECTS.size() - 1; i >= 0; i--) {
                    ObjectRenderEffects o = DYNAMIC_EFFECTS.get(i);
                    if (!o.update()) {
                        if (o.parent != null) {
                            o.parent.removeRenderEffect(o);
                        }

                        DYNAMIC_EFFECTS.remove(i);
                        release(o);
                    }
                }
            } catch (Exception var5) {
                ExceptionLogger.logException(var5);
            }
        }
    }
}
