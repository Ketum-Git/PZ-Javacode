// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import zombie.UsedFromLua;
import zombie.core.math.PZMath;
import zombie.iso.IsoPuddles;
import zombie.iso.IsoWater;
import zombie.ui.UIManager;

@UsedFromLua
public final class PerformanceSettings {
    public static int manualFrameSkips;
    private static int lockFps = 60;
    private static boolean uncappedFps;
    public static int waterQuality;
    public static int puddlesQuality;
    public static boolean newRoofHiding = true;
    public static boolean lightingThread = true;
    public static int lightingFps = 15;
    public static boolean auto3DZombies;
    public static final PerformanceSettings instance = new PerformanceSettings();
    public static boolean interpolateAnims = true;
    public static int animationSkip = 1;
    public static boolean modelLighting = true;
    public static int zombieAnimationSpeedFalloffCount = 6;
    public static int zombieBonusFullspeedFalloff = 3;
    public static int baseStaticAnimFramerate = 60;
    public static boolean useFbos;
    public static int numberZombiesBlended = 20;
    public static boolean fboRenderChunk = true;
    public static int fogQuality;
    public static int viewConeOpacity = 3;

    public static int getLockFPS() {
        return lockFps;
    }

    public static void setLockFPS(int lockFPS) {
        lockFps = lockFPS;
    }

    public boolean isFramerateUncapped() {
        return uncappedFps;
    }

    public void setFramerateUncapped(boolean uncappedFPS) {
        uncappedFps = uncappedFPS;
    }

    public int getFramerate() {
        return getLockFPS();
    }

    public void setFramerate(int framerate) {
        setLockFPS(framerate);
    }

    public void setLightingQuality(int lighting) {
    }

    public int getLightingQuality() {
        return 0;
    }

    public void setWaterQuality(int water) {
        waterQuality = water;
        IsoWater.getInstance().applyWaterQuality();
    }

    public int getWaterQuality() {
        return waterQuality;
    }

    public void setPuddlesQuality(int puddles) {
        puddlesQuality = puddles;
        if (puddles > 2 || puddles < 0) {
            puddlesQuality = 0;
        }

        IsoPuddles.getInstance().applyPuddlesQuality();
    }

    public int getPuddlesQuality() {
        return puddlesQuality;
    }

    public void setNewRoofHiding(boolean enabled) {
        newRoofHiding = enabled;
    }

    public boolean getNewRoofHiding() {
        return newRoofHiding;
    }

    public void setLightingFPS(int fps) {
        fps = Math.max(1, Math.min(120, fps));
        lightingFps = fps;
        System.out.println("LightingFPS set to " + lightingFps);
    }

    public int getLightingFPS() {
        return lightingFps;
    }

    public int getUIRenderFPS() {
        return UIManager.useUiFbo ? Core.getInstance().getOptionUIRenderFPS() : lockFps;
    }

    public int getFogQuality() {
        return fogQuality;
    }

    public void setFogQuality(int fogQuality) {
        PerformanceSettings.fogQuality = PZMath.clamp(fogQuality, 0, 2);
    }

    public int getViewConeOpacity() {
        return viewConeOpacity;
    }

    public void setViewConeOpacity(int viewConeOpacity) {
        PerformanceSettings.viewConeOpacity = PZMath.clamp(viewConeOpacity, 0, 5);
    }
}
