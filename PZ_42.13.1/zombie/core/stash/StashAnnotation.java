// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.stash;

import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import zombie.util.Type;

public final class StashAnnotation {
    public String symbol;
    public String text;
    public float x;
    public float y;
    public float r;
    public float g;
    public float b;
    public float anchorX = Float.NaN;
    public float anchorY = Float.NaN;
    public float rotation;

    public void fromLua(KahluaTable lua) {
        KahluaTableImpl luai = (KahluaTableImpl)lua;
        this.symbol = Type.tryCastTo(lua.rawget("symbol"), String.class);
        this.text = Type.tryCastTo(lua.rawget("text"), String.class);
        this.x = luai.rawgetFloat("x");
        this.y = luai.rawgetFloat("y");
        this.r = luai.rawgetFloat("r");
        this.g = luai.rawgetFloat("g");
        this.b = luai.rawgetFloat("b");
        this.anchorX = luai.tryGetFloat("anchorX", Float.NaN);
        this.anchorY = luai.tryGetFloat("anchorY", Float.NaN);
        this.rotation = luai.tryGetFloat("rotation", Float.NaN);
    }
}
