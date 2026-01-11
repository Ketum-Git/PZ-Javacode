// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import zombie.core.Color;
import zombie.core.PerformanceSettings;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;

public class FloorShaperDeDiamond extends FloorShaper {
    public static final FloorShaperDeDiamond instance = new FloorShaperDeDiamond();

    @Override
    public void accept(TextureDraw ddraw) {
        int colTint = this.colTint;
        this.colTint = 0;
        super.accept(ddraw);
        this.applyDeDiamondPadding(ddraw);
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.floor.lighting.getValue()) {
            if (!DebugOptions.instance.fboRenderChunk.nolighting.getValue()) {
                int col0 = this.col[0];
                int col1 = this.col[1];
                int col2 = this.col[2];
                int col3 = this.col[3];
                int rotatedCol0 = Color.lerpABGR(col0, col3, 0.5F);
                int rotatedCol1 = Color.lerpABGR(col1, col0, 0.5F);
                int rotatedCol2 = Color.lerpABGR(col2, col1, 0.5F);
                int rotatedCol3 = Color.lerpABGR(col3, col2, 0.5F);
                ddraw.col0 = Color.blendBGR(ddraw.col0, rotatedCol0);
                ddraw.col1 = Color.blendBGR(ddraw.col1, rotatedCol1);
                ddraw.col2 = Color.blendBGR(ddraw.col2, rotatedCol2);
                ddraw.col3 = Color.blendBGR(ddraw.col3, rotatedCol3);
                if (colTint != 0) {
                    ddraw.col0 = Color.tintABGR(ddraw.col0, colTint);
                    ddraw.col1 = Color.tintABGR(ddraw.col1, colTint);
                    ddraw.col2 = Color.tintABGR(ddraw.col2, colTint);
                    ddraw.col3 = Color.tintABGR(ddraw.col3, colTint);
                }
            }
        }
    }

    private void applyDeDiamondPadding(TextureDraw ddraw) {
        if (!PerformanceSettings.fboRenderChunk) {
            if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.isoPaddingDeDiamond.getValue()) {
                FloorShaperDeDiamond.Settings settings = this.getSettings();
                FloorShaperDeDiamond.Settings.BorderSetting setting = settings.getCurrentZoomSetting();
                float borderThicknessUp = setting.borderThicknessUp;
                float borderThicknessDown = setting.borderThicknessDown;
                float borderThicknessLR = setting.borderThicknessLeftRight;
                float uvFraction = setting.uvFraction;
                float width = ddraw.x1 - ddraw.x0;
                float height = ddraw.y2 - ddraw.y1;
                float uWidth = ddraw.u1 - ddraw.u0;
                float vHeight = ddraw.v2 - ddraw.v1;
                float uBorder = uWidth * borderThicknessLR / width;
                float vUp = vHeight * borderThicknessUp / height;
                float vDown = vHeight * borderThicknessDown / height;
                float ub = uvFraction * uBorder;
                float vbUp = uvFraction * vUp;
                float vbDown = uvFraction * vDown;
                SpritePadding.applyPadding(ddraw, borderThicknessLR, borderThicknessUp, borderThicknessLR, borderThicknessDown, ub, vbUp, ub, vbDown);
            }
        }
    }

    private FloorShaperDeDiamond.Settings getSettings() {
        return SpritePaddingSettings.getSettings().floorDeDiamond;
    }

    @XmlType(name = "FloorShaperDeDiamondSettings")
    public static class Settings extends SpritePaddingSettings.GenericZoomBasedSettingGroup {
        @XmlElement(name = "ZoomedIn")
        public FloorShaperDeDiamond.Settings.BorderSetting zoomedIn = new FloorShaperDeDiamond.Settings.BorderSetting(2.0F, 1.0F, 2.0F, 0.01F);
        @XmlElement(name = "NotZoomed")
        public FloorShaperDeDiamond.Settings.BorderSetting notZoomed = new FloorShaperDeDiamond.Settings.BorderSetting(2.0F, 1.0F, 2.0F, 0.01F);
        @XmlElement(name = "ZoomedOut")
        public FloorShaperDeDiamond.Settings.BorderSetting zoomedOut = new FloorShaperDeDiamond.Settings.BorderSetting(2.0F, 0.0F, 2.5F, 0.0F);

        public FloorShaperDeDiamond.Settings.BorderSetting getCurrentZoomSetting() {
            return getCurrentZoomSetting(this.zoomedIn, this.notZoomed, this.zoomedOut);
        }

        @XmlType(name = "BorderSetting")
        public static class BorderSetting {
            @XmlElement(name = "borderThicknessUp")
            public float borderThicknessUp = 3.0F;
            @XmlElement(name = "borderThicknessDown")
            public float borderThicknessDown = 3.0F;
            @XmlElement(name = "borderThicknessLR")
            public float borderThicknessLeftRight;
            @XmlElement(name = "uvFraction")
            public float uvFraction = 0.01F;

            public BorderSetting() {
            }

            public BorderSetting(float borderThicknessUp, float borderThicknessDown, float borderThicknessLeftRight, float uvFraction) {
                this.borderThicknessUp = borderThicknessUp;
                this.borderThicknessDown = borderThicknessDown;
                this.borderThicknessLeftRight = borderThicknessLeftRight;
                this.uvFraction = uvFraction;
            }
        }
    }
}
