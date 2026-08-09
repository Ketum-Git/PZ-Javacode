// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.symbols;

public final class SymbolLayout {
    float x;
    float y;
    boolean collided;
    final TextLayout textLayout = new TextLayout();
    boolean isAuthorHidden;

    SymbolLayout set(SymbolLayout other) {
        this.collided = other.collided;
        this.x = other.x;
        this.y = other.y;
        this.textLayout.set(other.textLayout);
        this.isAuthorHidden = other.isAuthorHidden;
        return this;
    }
}
