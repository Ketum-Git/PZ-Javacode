// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.fboRenderChunk;

import zombie.debug.BooleanDebugOption;
import zombie.debug.options.OptionGroup;

public final class FBORenderDebugOptions extends OptionGroup {
    public final BooleanDebugOption bulletTracers = this.newDebugOnlyOption("BulletTracers", false);
    public final BooleanDebugOption combinedFbo = this.newDebugOnlyOption("CombinedFBO", false);
    public final BooleanDebugOption corpsesInChunkTexture = this.newDebugOnlyOption("CorpsesInChunkTexture", false);
    public final BooleanDebugOption depthTestAll = this.newDebugOnlyOption("DepthTestAll", true);
    public final BooleanDebugOption fixJigglyModels = this.newDebugOnlyOption("FixJigglyModels", true);
    public final BooleanDebugOption highResChunkTextures = this.newDebugOnlyOption("HighResChunkTextures", false);
    public final BooleanDebugOption forceAlphaAndTargetOne = this.newDebugOnlyOption("ForceAlphaAndTargetOne", false);
    public final BooleanDebugOption forceAlphaToTarget = this.newDebugOnlyOption("ForceAlphaToTarget", false);
    public final BooleanDebugOption forceSkyLightLevel = this.newDebugOnlyOption("ForceSkyLightLevel", false);
    public final BooleanDebugOption itemsInChunkTexture = this.newDebugOnlyOption("ItemsInChunkTexture", false);
    public final BooleanDebugOption mipMaps = this.newDebugOnlyOption("MipMaps", true);
    public final BooleanDebugOption nolighting = this.newDebugOnlyOption("NoLighting", false);
    public final BooleanDebugOption renderChunkTextures = this.newDebugOnlyOption("RenderChunkTextures", true);
    public final BooleanDebugOption renderMustSeeSquares = this.newDebugOnlyOption("RenderMustSeeSquares", false);
    public final BooleanDebugOption renderTranslucentFloor = this.newDebugOnlyOption("RenderTranslucentFloor", true);
    public final BooleanDebugOption renderTranslucentNonFloor = this.newDebugOnlyOption("RenderTranslucentNonFloor", true);
    public final BooleanDebugOption renderVisionPolygon = this.newDebugOnlyOption("RenderVisionPolygon", true);
    public final BooleanDebugOption renderWallLines = this.newDebugOnlyOption("RenderWallLines", false);
    public final BooleanDebugOption seamFix1 = this.newDebugOnlyOption("SeamFix1", false);
    public final BooleanDebugOption seamFix2 = this.newDebugOnlyOption("SeamFix2", true);
    public final BooleanDebugOption updateSquareLightInfo = this.newDebugOnlyOption("UpdateSquareLightInfo", true);
    public final BooleanDebugOption useWeatherShader = this.newDebugOnlyOption("UseWeatherShader", true);

    public FBORenderDebugOptions() {
        super(null, "FBORenderChunk");
    }
}
