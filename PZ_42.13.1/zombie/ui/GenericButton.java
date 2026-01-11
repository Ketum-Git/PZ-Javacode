// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.Lua.LuaManager;
import zombie.core.textures.Texture;

public final class GenericButton extends UIElement {
    public boolean clicked;
    public UIElement messageTarget;
    public boolean mouseOver;
    public String name;
    public String text;
    Texture upTexture;
    Texture downTexture;
    private UIEventHandler messageTarget2;

    public GenericButton(UIElement messages, float x, float y, float width, float height, String name, String text, Texture UpTex, Texture DownTex) {
        this.x = x;
        this.y = y;
        this.messageTarget = messages;
        this.name = name;
        this.text = text;
        this.width = width;
        this.height = height;
        this.upTexture = UpTex;
        this.downTexture = DownTex;
    }

    public GenericButton(UIEventHandler messages, float x, float y, float width, float height, String name, String text, Texture UpTex, Texture DownTex) {
        this.x = x;
        this.y = y;
        this.messageTarget2 = messages;
        this.name = name;
        this.text = text;
        this.width = width;
        this.height = height;
        this.upTexture = UpTex;
        this.downTexture = DownTex;
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        if (!this.isVisible()) {
            return Boolean.FALSE;
        } else {
            if (this.getTable() != null && UIManager.tableget(this.table, "onMouseDown") != null) {
                Object[] var5 = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseDown"), this.table, x, y);
            }

            this.clicked = true;
            return Boolean.TRUE;
        }
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseMove") != null) {
            Object[] var5 = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseMove"), this.table, dx, dy);
        }

        this.mouseOver = true;
        return Boolean.TRUE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseMoveOutside") != null) {
            Object[] var5 = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseMoveOutside"), this.table, dx, dy);
        }

        this.clicked = false;
        this.mouseOver = false;
    }

    @Override
    public Boolean onMouseUp(double x, double y) {
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseUp") != null) {
            Object[] var5 = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseUp"), this.table, x, y);
        }

        if (this.clicked) {
            if (this.messageTarget2 != null) {
                this.messageTarget2.Selected(this.name, 0, 0);
            } else {
                this.messageTarget.ButtonClicked(this.name);
            }
        }

        this.clicked = false;
        return Boolean.TRUE;
    }

    @Override
    public void render() {
        if (this.isVisible()) {
            int dy = 0;
            if (this.clicked) {
                this.DrawTextureScaled(this.downTexture, 0.0, 0.0, this.getWidth(), this.getHeight(), 1.0);
                this.DrawTextCentre(this.text, this.getWidth() / 2.0, 1.0, 1.0, 1.0, 1.0, 1.0);
            } else {
                this.DrawTextureScaled(this.upTexture, 0.0, 0.0, this.getWidth(), this.getHeight(), 1.0);
                this.DrawTextCentre(this.text, this.getWidth() / 2.0, 1.0, 1.0, 1.0, 1.0, 1.0);
            }

            super.render();
        }
    }
}
