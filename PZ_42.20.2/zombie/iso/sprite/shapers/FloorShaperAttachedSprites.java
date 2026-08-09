// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import zombie.core.PerformanceSettings;
import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;

public class FloorShaperAttachedSprites extends FloorShaper {
    public static final FloorShaperAttachedSprites instance = new FloorShaperAttachedSprites();

    @Override
    public void accept(TextureDraw ddraw) {
        super.accept(ddraw);
        if (!PerformanceSettings.fboRenderChunk) {
            this.applyAttachedSpritesPadding(ddraw);
        }
    }

    private void applyAttachedSpritesPadding(TextureDraw ddraw) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.isoPaddingAttached.getValue()) {
            FloorShaperAttachedSprites.Settings settings = this.getSettings();
            FloorShaperAttachedSprites.Settings.ASBorderSetting setting = settings.getCurrentZoomSetting();
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

    private FloorShaperAttachedSprites.Settings getSettings() {
        return SpritePaddingSettings.getSettings().attachedSprites;
    }

    @XmlType(name = "FloorShaperAttachedSpritesSettings")
    public static class Settings extends SpritePaddingSettings.GenericZoomBasedSettingGroup {
        @XmlElement(name = "ZoomedIn")
        public FloorShaperAttachedSprites.Settings.ASBorderSetting zoomedIn = new FloorShaperAttachedSprites.Settings.ASBorderSetting(2.0F, 1.0F, 3.0F, 0.01F);
        @XmlElement(name = "NotZoomed")
        public FloorShaperAttachedSprites.Settings.ASBorderSetting notZoomed = new FloorShaperAttachedSprites.Settings.ASBorderSetting(2.0F, 1.0F, 3.0F, 0.01F);
        @XmlElement(name = "ZoomedOut")
        public FloorShaperAttachedSprites.Settings.ASBorderSetting zoomedOut = new FloorShaperAttachedSprites.Settings.ASBorderSetting(2.0F, 0.0F, 2.5F, 0.0F);

        public FloorShaperAttachedSprites.Settings.ASBorderSetting getCurrentZoomSetting() {
            return getCurrentZoomSetting(this.zoomedIn, this.notZoomed, this.zoomedOut);
        }

        @XmlType(name = "ASBorderSetting")
        public static class ASBorderSetting {
            @XmlElement(name = "borderThicknessUp")
            public float borderThicknessUp;
            @XmlElement(name = "borderThicknessDown")
            public float borderThicknessDown;
            @XmlElement(name = "borderThicknessLR")
            public float borderThicknessLeftRight;
            @XmlElement(name = "uvFraction")
            public float uvFraction;

            public ASBorderSetting() {
            }

            public ASBorderSetting(float borderThicknessUp, float borderThicknessDown, float borderThicknessLeftRight, float uvFraction) {
                this.borderThicknessUp = borderThicknessUp;
                this.borderThicknessDown = borderThicknessDown;
                this.borderThicknessLeftRight = borderThicknessLeftRight;
                this.uvFraction = uvFraction;
            }
        }
    }
}
