// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.tileDepth;

import zombie.core.Core;
import zombie.core.textures.Texture;

public final class TileSeamManager {
    public static final TileSeamManager instance = new TileSeamManager();
    private final Texture[] textures = new Texture[TileSeamManager.Tiles.values().length];
    private final float[][] vertices = new float[TileSeamManager.Tiles.values().length][];

    public void init() {
        TileSeamManager.Tiles[] tiles = TileSeamManager.Tiles.values();
        Texture texture = Texture.getSharedTexture("media/depthmaps/SEAMS_01.png", 0);

        for (int i = 0; i < tiles.length; i++) {
            int col = i % 8;
            int row = i / 8;
            this.textures[i] = new Texture(
                texture.getTextureId(), "SEAMS_01_" + i, col * 64 * Core.tileScale, row * 128 * Core.tileScale, 64 * Core.tileScale, 128 * Core.tileScale
            );
        }

        this.vertices[TileSeamManager.Tiles.FloorSouth.ordinal()] = new float[]{5.0F, 221.0F, 69.0F, 253.0F, 63.0F, 256.0F, -1.0F, 224.0F};
        this.vertices[TileSeamManager.Tiles.FloorEast.ordinal()] = new float[]{57.0F, 253.0F, 121.0F, 221.0F, 127.0F, 224.0F, 63.0F, 256.0F};
        this.vertices[TileSeamManager.Tiles.FloorSouthOneThird.ordinal()] = new float[]{5.0F, 157.0F, 69.0F, 189.0F, 63.0F, 192.0F, -1.0F, 160.0F};
        this.vertices[TileSeamManager.Tiles.FloorEastOneThird.ordinal()] = new float[]{57.0F, 189.0F, 121.0F, 157.0F, 127.0F, 160.0F, 63.0F, 192.0F};
        this.vertices[TileSeamManager.Tiles.FloorSouthTwoThirds.ordinal()] = new float[]{5.0F, 93.0F, 69.0F, 125.0F, 63.0F, 128.0F, -1.0F, 96.0F};
        this.vertices[TileSeamManager.Tiles.FloorEastTwoThirds.ordinal()] = new float[]{57.0F, 125.0F, 121.0F, 93.0F, 127.0F, 96.0F, 63.0F, 128.0F};
    }

    public Texture getTexture(TileSeamManager.Tiles tiles) {
        return this.textures[tiles.ordinal()];
    }

    public float[] getVertices(TileSeamManager.Tiles tiles) {
        return this.vertices[tiles.ordinal()];
    }

    public static enum Tiles {
        FloorSouth,
        FloorEast,
        WallSouth,
        WallEast,
        FloorSouthOneThird,
        FloorEastOneThird,
        FloorSouthTwoThirds,
        FloorEastTwoThirds;
    }
}
