// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.debug.options;

import zombie.debug.BooleanDebugOption;

public final class Terrain extends OptionGroup {
    public final Terrain.RenderTiles renderTiles = this.newOptionGroup(new Terrain.RenderTiles());

    public static final class RenderTiles extends OptionGroup {
        public final BooleanDebugOption enable = this.newDebugOnlyOption("Enable", true);
        public final BooleanDebugOption newRender = this.newDebugOnlyOption("NewRender", true);
        public final BooleanDebugOption shadows = this.newDebugOnlyOption("Shadows", true);
        public final BooleanDebugOption bloodDecals = this.newDebugOnlyOption("BloodDecals", true);
        public final BooleanDebugOption water = this.newDebugOnlyOption("Water", true);
        public final BooleanDebugOption waterShore = this.newDebugOnlyOption("WaterShore", true);
        public final BooleanDebugOption waterBody = this.newDebugOnlyOption("WaterBody", true);
        public final BooleanDebugOption lua = this.newDebugOnlyOption("Lua", true);
        public final BooleanDebugOption vegetationCorpses = this.newDebugOnlyOption("VegetationCorpses", true);
        public final BooleanDebugOption minusFloorCharacters = this.newDebugOnlyOption("MinusFloorCharacters", true);
        public final BooleanDebugOption renderGridSquares = this.newDebugOnlyOption("RenderGridSquares", true);
        public final BooleanDebugOption renderSprites = this.newDebugOnlyOption("RenderSprites", true);
        public final BooleanDebugOption overlaySprites = this.newDebugOnlyOption("OverlaySprites", true);
        public final BooleanDebugOption attachedAnimSprites = this.newDebugOnlyOption("AttachedAnimSprites", true);
        public final BooleanDebugOption attachedChildren = this.newDebugOnlyOption("AttachedChildren", true);
        public final BooleanDebugOption attachedWallBloodSplats = this.newDebugOnlyOption("AttachedWallBloodSplats", true);
        public final BooleanDebugOption useShaders = this.newOption("UseShaders", true);
        public final BooleanDebugOption highContrastBg = this.newDebugOnlyOption("HighContrastBg", false);
        public final BooleanDebugOption cutaway = this.newDebugOnlyOption("Cutaway", true);
        public final BooleanDebugOption forceFullAlpha = this.newDebugOnlyOption("ForceFullAlpha", false);
        public final BooleanDebugOption renderContainerHighlight = this.newDebugOnlyOption("RenderContainerHighlight", true);
        public final Terrain.RenderTiles.IsoGridSquare isoGridSquare = this.newOptionGroup(new Terrain.RenderTiles.IsoGridSquare());

        public static final class IsoGridSquare extends OptionGroup {
            public final BooleanDebugOption renderMinusFloor = this.newDebugOnlyOption("RenderMinusFloor", true);
            public final BooleanDebugOption doorsAndWalls = this.newDebugOnlyOption("DoorsAndWalls", true);
            public final BooleanDebugOption doorsAndWallsSimpleLighting = this.newDebugOnlyOption("DoorsAndWallsSL", true);
            public final BooleanDebugOption objects = this.newDebugOnlyOption("Objects", true);
            public final BooleanDebugOption meshCutDown = this.newDebugOnlyOption("MeshCutDown", true);
            public final BooleanDebugOption isoPadding = this.newDebugOnlyOption("IsoPadding", true);
            public final BooleanDebugOption isoPaddingDeDiamond = this.newDebugOnlyOption("IsoPaddingDeDiamond", true);
            public final BooleanDebugOption isoPaddingAttached = this.newDebugOnlyOption("IsoPaddingAttached", true);
            public final BooleanDebugOption shoreFade = this.newDebugOnlyOption("ShoreFade", true);
            public final Terrain.RenderTiles.IsoGridSquare.Walls walls = this.newOptionGroup(new Terrain.RenderTiles.IsoGridSquare.Walls());
            public final Terrain.RenderTiles.IsoGridSquare.Floor floor = this.newOptionGroup(new Terrain.RenderTiles.IsoGridSquare.Floor());

            public static final class Floor extends OptionGroup {
                public final BooleanDebugOption lighting = this.newDebugOnlyOption("Lighting", true);
                public final BooleanDebugOption lightingOld = this.newDebugOnlyOption("LightingOld", false);
                public final BooleanDebugOption lightingDebug = this.newDebugOnlyOption("LightingDebug", false);
            }

            public static final class Walls extends OptionGroup {
                public final BooleanDebugOption nw = this.newDebugOnlyOption("NW", true);
                public final BooleanDebugOption w = this.newDebugOnlyOption("W", true);
                public final BooleanDebugOption n = this.newDebugOnlyOption("N", true);
                public final BooleanDebugOption render = this.newDebugOnlyOption("Render", true);
                public final BooleanDebugOption lighting = this.newDebugOnlyOption("Lighting", true);
                public final BooleanDebugOption lightingDebug = this.newDebugOnlyOption("LightingDebug", false);
                public final BooleanDebugOption lightingOldDebug = this.newDebugOnlyOption("LightingOldDebug", false);
                public final BooleanDebugOption attachedSprites = this.newDebugOnlyOption("AttachedSprites", true);
            }
        }
    }
}
