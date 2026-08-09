// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import zombie.core.textures.Texture;

public class TileDepthMapManager {
    public static TileDepthMapManager instance = new TileDepthMapManager();
    Texture[] presets;

    public Texture getTextureForPreset(TileDepthMapManager.TileDepthPreset preset) {
        return this.presets[preset.index];
    }

    public void init() {
        this.presets = new Texture[8];

        for (int x = 0; x < TileDepthMapManager.TileDepthPreset.Max.index; x++) {
            TileDepthTexture depthTexture = TileDepthTextureManager.getInstance().getPresetDepthTexture(x, 0);
            this.presets[x] = depthTexture == null ? null : depthTexture.getTexture();
        }
    }

    public static enum TileDepthPreset {
        Floor(0),
        WDoorFrame(2),
        NDoorFrame(3),
        WWall(4),
        NWall(5),
        NWWall(6),
        SEWall(7),
        Max(8);

        private static final TileDepthMapManager.TileDepthPreset[] VALUES = values();
        private final int index;

        private TileDepthPreset(final int index) {
            this.index = index;
        }

        public int index() {
            return this.index;
        }

        public static TileDepthMapManager.TileDepthPreset fromIndex(int index) {
            return VALUES[index];
        }
    }
}
