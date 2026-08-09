// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.sprite;

public final class SpriteRenderStateUI extends GenericSpriteRenderState {
    public boolean active;

    public SpriteRenderStateUI(int index) {
        super(index);
    }

    @Override
    public void clear() {
        try {
            this.active = true;
            super.clear();
        } finally {
            this.active = false;
        }
    }
}
