// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion.obj;

import zombie.erosion.ErosionMain;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;

public final class ErosionObjOverlaySprites {
    public String name;
    public int stages;
    private final ErosionObjOverlaySprites.Stage[] sprites;

    public ErosionObjOverlaySprites(int stages, String name) {
        this.name = name;
        this.stages = stages;
        this.sprites = new ErosionObjOverlaySprites.Stage[stages];

        for (int i = 0; i < this.stages; i++) {
            this.sprites[i] = new ErosionObjOverlaySprites.Stage();
        }
    }

    public IsoSprite getSprite(int stage, int season) {
        return this.sprites[stage].seasons[season].getSprite();
    }

    public IsoSpriteInstance getSpriteInstance(int stage, int season) {
        return this.sprites[stage].seasons[season].getInstance();
    }

    public void setSprite(int stage, String sprite, int season) {
        this.sprites[stage].seasons[season] = new ErosionObjOverlaySprites.Sprite(sprite);
    }

    private static final class Sprite {
        private final String sprite;

        public Sprite(String sprite) {
            this.sprite = sprite;
        }

        public IsoSprite getSprite() {
            return this.sprite != null ? ErosionMain.getInstance().getSpriteManager().getSprite(this.sprite) : null;
        }

        public IsoSpriteInstance getInstance() {
            return this.sprite != null ? ErosionMain.getInstance().getSpriteManager().getSprite(this.sprite).newInstance() : null;
        }
    }

    private static class Stage {
        public ErosionObjOverlaySprites.Sprite[] seasons = new ErosionObjOverlaySprites.Sprite[6];
    }
}
