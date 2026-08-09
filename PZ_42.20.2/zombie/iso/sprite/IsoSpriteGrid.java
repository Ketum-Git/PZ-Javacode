// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.sprite;

import zombie.UsedFromLua;

/**
 * Turbo
 */
@UsedFromLua
public final class IsoSpriteGrid {
    private final IsoSprite[] sprites;
    private final int width;
    private final int height;
    private final int levels;

    public IsoSpriteGrid(int width, int height, int levels) {
        this.sprites = new IsoSprite[width * height * levels];
        this.width = width;
        this.height = height;
        this.levels = levels;
    }

    public IsoSpriteGrid(int width, int height) {
        this(width, height, 1);
    }

    public IsoSprite getAnchorSprite() {
        for (int i = 0; i < this.getWidth() * this.getHeight(); i++) {
            IsoSprite sprite = this.sprites[i];
            if (sprite != null) {
                return sprite;
            }
        }

        return null;
    }

    public IsoSprite getSprite(int x, int y, int z) {
        return this.isValidXYZ(x, y, z) ? this.sprites[this.getSpriteIndex(x, y, z)] : null;
    }

    public IsoSprite getSprite(int x, int y) {
        return this.getSprite(x, y, 0);
    }

    public void setSprite(int x, int y, int z, IsoSprite sprite) {
        if (this.isValidXYZ(x, y, z)) {
            this.sprites[this.getSpriteIndex(x, y, z)] = sprite;
        }
    }

    public void setSprite(int x, int y, IsoSprite sprite) {
        this.setSprite(x, y, 0, sprite);
    }

    public int getSpriteIndex(IsoSprite sprite) {
        for (int i = 0; i < this.sprites.length; i++) {
            IsoSprite sprite1 = this.sprites[i];
            if (sprite1 != null && sprite1 == sprite) {
                return i;
            }
        }

        return -1;
    }

    public int getSpriteGridPosX(IsoSprite sprite) {
        int index = this.getSpriteIndex(sprite);
        if (index >= 0) {
            index %= this.getWidth() * this.getHeight();
            return index % this.getWidth();
        } else {
            return -1;
        }
    }

    public int getSpriteGridPosY(IsoSprite sprite) {
        int index = this.getSpriteIndex(sprite);
        if (index >= 0) {
            index %= this.getWidth() * this.getHeight();
            return index / this.getWidth();
        } else {
            return -1;
        }
    }

    public int getSpriteGridPosZ(IsoSprite sprite) {
        int index = this.getSpriteIndex(sprite);
        return index >= 0 ? index / (this.getWidth() * this.getHeight()) : -1;
    }

    public IsoSprite getSpriteFromIndex(int index) {
        return index >= 0 && index < this.getSpriteCount() ? this.sprites[index] : null;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getLevels() {
        return this.levels;
    }

    public boolean validate() {
        if (this.getLevels() == 0) {
            return false;
        } else {
            for (int z = 0; z < this.getLevels(); z++) {
                int levelStartIndex = z * this.getWidth() * this.getHeight();
                boolean bLevelEmpty = true;

                for (int i = 0; i < this.getWidth() * this.getHeight(); i++) {
                    if (this.sprites[levelStartIndex + i] != null) {
                        bLevelEmpty = false;
                        break;
                    }
                }

                if (bLevelEmpty) {
                    return false;
                }
            }

            return true;
        }
    }

    public int getSpriteCount() {
        return this.sprites.length;
    }

    public IsoSprite[] getSprites() {
        return this.sprites;
    }

    public boolean isValidXYZ(int x, int y, int z) {
        return x >= 0 && x < this.getWidth() && y >= 0 && y < this.getHeight() && z >= 0 && z < this.getLevels();
    }

    public int getSpriteIndex(int x, int y, int z) {
        return this.isValidXYZ(x, y, z) ? x + y * this.getWidth() + z * this.getWidth() * this.getHeight() : -1;
    }
}
