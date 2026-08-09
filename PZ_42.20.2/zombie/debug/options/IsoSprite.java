// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class IsoSprite extends OptionGroup {
    public final BooleanDebugOption renderSprites = this.newDebugOnlyOption("Render.Sprites", true);
    public final BooleanDebugOption renderModels = this.newDebugOnlyOption("Render.Models", true);
    public final BooleanDebugOption movingObjectEdges = this.newDebugOnlyOption("Render.MovingObjectEdges", false);
    public final BooleanDebugOption dropShadowEdges = this.newDebugOnlyOption("Render.DropShadowEdges", false);
    public final BooleanDebugOption nearestMagFilterAtMinZoom = this.newDebugOnlyOption("Render.NearestMagFilterAtMinZoom", true);
    public final BooleanDebugOption itemHeight = this.newDebugOnlyOption("Render.ItemHeight", false);
    public final BooleanDebugOption surface = this.newDebugOnlyOption("Render.Surface", false);
    public final BooleanDebugOption textureWrapClampToEdge = this.newDebugOnlyOption("Render.TextureWrap.ClampToEdge", false);
    public final BooleanDebugOption textureWrapRepeat = this.newDebugOnlyOption("Render.TextureWrap.Repeat", false);
    public final BooleanDebugOption forceLinearMagFilter = this.newDebugOnlyOption("Render.ForceLinearMagFilter", false);
    public final BooleanDebugOption forceNearestMagFilter = this.newDebugOnlyOption("Render.ForceNearestMagFilter", false);
    public final BooleanDebugOption forceNearestMipMapping = this.newDebugOnlyOption("Render.ForceNearestMipMapping", false);
    public final BooleanDebugOption characterMipmapColors = this.newDebugOnlyOption("Render.CharacterMipmapColors", false);
    public final BooleanDebugOption worldMipmapColors = this.newDebugOnlyOption("Render.WorldMipmapColors", false);
}
