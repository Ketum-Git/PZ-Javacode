// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite.shapers;

import zombie.core.textures.TextureDraw;
import zombie.debug.DebugOptions;

public class SpritePadding {
    public static void applyPadding(
        TextureDraw ddraw, float xPadLeft, float yPadUp, float xPadRight, float yPadDown, float uPadLeft, float vPadUp, float uPadRight, float vPadDown
    ) {
        float x0 = ddraw.x0;
        float y0 = ddraw.y0;
        float x1 = ddraw.x1;
        float y1 = ddraw.y1;
        float x2 = ddraw.x2;
        float y2 = ddraw.y2;
        float x3 = ddraw.x3;
        float y3 = ddraw.y3;
        float u0 = ddraw.u0;
        float v0 = ddraw.v0;
        float u1 = ddraw.u1;
        float v1 = ddraw.v1;
        float u2 = ddraw.u2;
        float v2 = ddraw.v2;
        float u3 = ddraw.u3;
        float v3 = ddraw.v3;
        ddraw.x0 = x0 - xPadLeft;
        ddraw.y0 = y0 - yPadUp;
        ddraw.u0 = u0 - uPadLeft;
        ddraw.v0 = v0 - vPadUp;
        ddraw.x1 = x1 + xPadRight;
        ddraw.y1 = y1 - yPadUp;
        ddraw.u1 = u1 + uPadRight;
        ddraw.v1 = v1 - vPadUp;
        ddraw.x2 = x2 + xPadRight;
        ddraw.y2 = y2 + yPadDown;
        ddraw.u2 = u2 + uPadRight;
        ddraw.v2 = v2 + vPadDown;
        ddraw.x3 = x3 - xPadLeft;
        ddraw.y3 = y3 + yPadDown;
        ddraw.u3 = u3 - uPadLeft;
        ddraw.v3 = v3 + vPadDown;
    }

    public static void applyPaddingBorder(TextureDraw ddraw, float borderThickness, float uvFraction) {
        float width = ddraw.x1 - ddraw.x0;
        float height = ddraw.y2 - ddraw.y1;
        float uWidth = ddraw.u1 - ddraw.u0;
        float vHeight = ddraw.v2 - ddraw.v1;
        float uBorder = uWidth * borderThickness / width;
        float vBorder = vHeight * borderThickness / height;
        float ub = uvFraction * uBorder;
        float vb = uvFraction * vBorder;
        applyPadding(ddraw, borderThickness, borderThickness, borderThickness, borderThickness, ub, vb, ub, vb);
    }

    public static void applyIsoPadding(TextureDraw ddraw, SpritePadding.IsoPaddingSettings settings) {
        if (DebugOptions.instance.terrain.renderTiles.isoGridSquare.isoPadding.getValue()) {
            SpritePadding.IsoPaddingSettings.IsoBorderSetting setting = settings.getCurrentZoomSetting();
            float borderThickness = setting.borderThickness;
            float uvBorderFraction = setting.uvFraction;
            applyPaddingBorder(ddraw, borderThickness, uvBorderFraction);
        }
    }

    public static class IsoPaddingSettings extends SpritePaddingSettings.GenericZoomBasedSettingGroup {
        public SpritePadding.IsoPaddingSettings.IsoBorderSetting zoomedIn = new SpritePadding.IsoPaddingSettings.IsoBorderSetting(1.0F, 0.99F);
        public SpritePadding.IsoPaddingSettings.IsoBorderSetting notZoomed = new SpritePadding.IsoPaddingSettings.IsoBorderSetting(1.0F, 0.99F);
        public SpritePadding.IsoPaddingSettings.IsoBorderSetting zoomedOut = new SpritePadding.IsoPaddingSettings.IsoBorderSetting(2.0F, 0.01F);

        public SpritePadding.IsoPaddingSettings.IsoBorderSetting getCurrentZoomSetting() {
            return getCurrentZoomSetting(this.zoomedIn, this.notZoomed, this.zoomedOut);
        }

        public static class IsoBorderSetting {
            public float borderThickness;
            public float uvFraction;

            public IsoBorderSetting() {
            }

            public IsoBorderSetting(float borderThickness, float uvFraction) {
                this.borderThickness = borderThickness;
                this.uvFraction = uvFraction;
            }
        }
    }
}
