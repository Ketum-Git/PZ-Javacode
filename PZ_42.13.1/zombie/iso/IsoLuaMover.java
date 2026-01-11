// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso;

import se.krka.kahlua.vm.KahluaTable;
import zombie.GameTime;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.characters.IsoGameCharacter;
import zombie.core.opengl.Shader;
import zombie.core.textures.ColorInfo;
import zombie.iso.sprite.IsoSprite;
import zombie.iso.sprite.IsoSpriteInstance;
import zombie.iso.sprite.IsoSpriteManager;
import zombie.ui.UIManager;

@UsedFromLua
public class IsoLuaMover extends IsoGameCharacter {
    public KahluaTable luaMoverTable;

    public IsoLuaMover(KahluaTable table) {
        super(null, 0.0F, 0.0F, 0.0F);
        this.sprite = IsoSprite.CreateSprite(IsoSpriteManager.instance);
        this.luaMoverTable = table;
        if (this.def == null) {
            this.def = IsoSpriteInstance.get(this.sprite);
        }
    }

    public void playAnim(String name, float seconds, boolean looped, boolean playing) {
        this.sprite.PlayAnim(name);
        float numFrames = this.sprite.currentAnim.frames.size();
        float timeSplitIfOneSecond = 1000.0F / numFrames;
        float period = timeSplitIfOneSecond * seconds;
        this.def.animFrameIncrease = period * GameTime.getInstance().getMultiplier();
        this.def.finished = !playing;
        this.def.looped = looped;
    }

    @Override
    public String getObjectName() {
        return "IsoLuaMover";
    }

    @Override
    public void update() {
        try {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaMoverTable.rawget("update"), this.luaMoverTable);
        } catch (Exception var2) {
            var2.printStackTrace();
        }

        this.sprite.update(this.def);
        super.update();
    }

    @Override
    public void render(float x, float y, float z, ColorInfo col, boolean bDoAttached, boolean bWallLightingPass, Shader shader) {
        float offsetYY = this.offsetY;
        offsetYY -= 100.0F;
        float offsetXX = this.offsetX;
        offsetXX -= 34.0F;
        this.sprite.render(this.def, this, this.getX(), this.getY(), this.getZ(), this.dir, offsetXX, offsetYY, col, true);

        try {
            LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaMoverTable.rawget("postrender"), this.luaMoverTable, col, bDoAttached);
        } catch (Exception var11) {
            var11.printStackTrace();
        }
    }
}
