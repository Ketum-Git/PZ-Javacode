// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.Lua.LuaManager;
import zombie.core.Color;
import zombie.core.textures.Texture;

public final class DialogButton extends UIElement {
    public boolean clicked;
    public UIElement messageTarget;
    public boolean mouseOver;
    public String name;
    public String text;
    Texture downLeft;
    Texture downMid;
    Texture downRight;
    float origX;
    Texture upLeft;
    Texture upMid;
    Texture upRight;
    private UIEventHandler messageTarget2;

    public DialogButton(UIElement messages, float x, float y, String text, String name) {
        this.x = x;
        this.y = y;
        this.origX = x;
        this.messageTarget = messages;
        this.upLeft = Texture.getSharedTexture("ButtonL_Up");
        this.upMid = Texture.getSharedTexture("ButtonM_Up");
        this.upRight = Texture.getSharedTexture("ButtonR_Up");
        this.downLeft = Texture.getSharedTexture("ButtonL_Down");
        this.downMid = Texture.getSharedTexture("ButtonM_Down");
        this.downRight = Texture.getSharedTexture("ButtonR_Down");
        this.name = name;
        this.text = text;
        this.width = TextManager.instance.MeasureStringX(UIFont.Small, text);
        this.width += 8.0F;
        if (this.width < 40.0F) {
            this.width = 40.0F;
        }

        this.height = this.downMid.getHeight();
        this.x = this.x - this.width / 2.0F;
    }

    public DialogButton(UIEventHandler messages, int x, int y, String text, String name) {
        this.x = x;
        this.y = y;
        this.origX = x;
        this.messageTarget2 = messages;
        this.upLeft = Texture.getSharedTexture("ButtonL_Up");
        this.upMid = Texture.getSharedTexture("ButtonM_Up");
        this.upRight = Texture.getSharedTexture("ButtonR_Up");
        this.downLeft = Texture.getSharedTexture("ButtonL_Down");
        this.downMid = Texture.getSharedTexture("ButtonM_Down");
        this.downRight = Texture.getSharedTexture("ButtonR_Down");
        this.name = name;
        this.text = text;
        this.width = TextManager.instance.MeasureStringX(UIFont.Small, text);
        this.width += 8.0F;
        if (this.width < 40.0F) {
            this.width = 40.0F;
        }

        this.height = this.downMid.getHeight();
        this.x = this.x - this.width / 2.0F;
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        if (!this.isVisible()) {
            return false;
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
        this.mouseOver = true;
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseMove") != null) {
            Object[] var5 = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseMove"), this.table, dx, dy);
        }

        return Boolean.TRUE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        this.clicked = false;
        if (this.getTable() != null && UIManager.tableget(this.table, "onMouseMoveOutside") != null) {
            Object[] var5 = LuaManager.caller.pcall(LuaManager.thread, UIManager.tableget(this.table, "onMouseMoveOutside"), this.table, dx, dy);
        }

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
            } else if (this.messageTarget != null) {
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
                this.DrawTexture(this.downLeft, 0.0, 0.0, 1.0);
                this.DrawTextureScaledCol(
                    this.downMid,
                    this.downLeft.getWidth(),
                    0.0,
                    (int)(this.getWidth() - this.downLeft.getWidth() * 2),
                    this.downLeft.getHeight(),
                    new Color(255, 255, 255, 255)
                );
                this.DrawTexture(this.downRight, (int)(this.getWidth() - this.downRight.getWidth()), 0.0, 1.0);
                this.DrawTextCentre(this.text, this.getWidth() / 2.0, 1.0, 1.0, 1.0, 1.0, 1.0);
            } else {
                this.DrawTexture(this.upLeft, 0.0, 0.0, 1.0);
                this.DrawTextureScaledCol(
                    this.upMid,
                    this.downLeft.getWidth(),
                    0.0,
                    (int)(this.getWidth() - this.downLeft.getWidth() * 2),
                    this.downLeft.getHeight(),
                    new Color(255, 255, 255, 255)
                );
                this.DrawTexture(this.upRight, (int)(this.getWidth() - this.downRight.getWidth()), 0.0, 1.0);
                this.DrawTextCentre(this.text, this.getWidth() / 2.0, 0.0, 1.0, 1.0, 1.0, 1.0);
            }

            super.render();
        }
    }
}
