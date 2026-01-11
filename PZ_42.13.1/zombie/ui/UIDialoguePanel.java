// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.awt.Rectangle;
import java.util.Stack;
import zombie.core.Color;
import zombie.core.textures.Texture;

public final class UIDialoguePanel extends UIElement {
    float alpha = 1.0F;
    Texture dialogBottomLeft;
    Texture dialogBottomMiddle;
    Texture dialogBottomRight;
    Texture dialogLeft;
    Texture dialogMiddle;
    Texture dialogRight;
    Texture titleLeft;
    Texture titleMiddle;
    Texture titleRight;
    public float clientH;
    public float clientW;
    public Stack<Rectangle> nestedItems = new Stack<>();

    public UIDialoguePanel(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.titleLeft = Texture.getSharedTexture("media/ui/Dialog_Titlebar_Left.png");
        this.titleMiddle = Texture.getSharedTexture("media/ui/Dialog_Titlebar_Middle.png");
        this.titleRight = Texture.getSharedTexture("media/ui/Dialog_Titlebar_Right.png");
        this.dialogLeft = Texture.getSharedTexture("media/ui/Dialog_Left.png");
        this.dialogMiddle = Texture.getSharedTexture("media/ui/Dialog_Middle.png");
        this.dialogRight = Texture.getSharedTexture("media/ui/Dialog_Right.png");
        this.dialogBottomLeft = Texture.getSharedTexture("media/ui/Dialog_Bottom_Left.png");
        this.dialogBottomMiddle = Texture.getSharedTexture("media/ui/Dialog_Bottom_Middle.png");
        this.dialogBottomRight = Texture.getSharedTexture("media/ui/Dialog_Bottom_Right.png");
        this.clientW = width;
        this.clientH = height;
    }

    public void Nest(UIElement el, int t, int r, int b, int l) {
        this.AddChild(el);
        this.nestedItems.add(new Rectangle(l, t, r, b));
        el.setX(l);
        el.setY(t);
        el.update();
    }

    @Override
    public void render() {
        this.DrawTextureScaledCol(this.titleLeft, 0.0, 0.0, 28.0, 28.0, new Color(255, 255, 255, 100));
        this.DrawTextureScaledCol(this.titleMiddle, 28.0, 0.0, this.getWidth() - 56.0, 28.0, new Color(255, 255, 255, 100));
        this.DrawTextureScaledCol(this.titleRight, 0.0 + this.getWidth() - 28.0, 0.0, 28.0, 28.0, new Color(255, 255, 255, 100));
        this.DrawTextureScaledCol(this.dialogLeft, 0.0, 28.0, 78.0, this.getHeight() - 100.0, new Color(255, 255, 255, 100));
        this.DrawTextureScaledCol(this.dialogMiddle, 78.0, 28.0, this.getWidth() - 156.0, this.getHeight() - 100.0, new Color(255, 255, 255, 100));
        this.DrawTextureScaledCol(this.dialogRight, 0.0 + this.getWidth() - 78.0, 28.0, 78.0, this.getHeight() - 100.0, new Color(255, 255, 255, 100));
        this.DrawTextureScaledCol(this.dialogBottomLeft, 0.0, 0.0 + this.getHeight() - 72.0, 78.0, 72.0, new Color(255, 255, 255, 100));
        this.DrawTextureScaledCol(this.dialogBottomMiddle, 78.0, 0.0 + this.getHeight() - 72.0, this.getWidth() - 156.0, 72.0, new Color(255, 255, 255, 100));
        this.DrawTextureScaledCol(
            this.dialogBottomRight, 0.0 + this.getWidth() - 78.0, 0.0 + this.getHeight() - 72.0, 78.0, 72.0, new Color(255, 255, 255, 100)
        );
        super.render();
    }

    @Override
    public void update() {
        super.update();
        int n = 0;

        for (Rectangle rect : this.nestedItems) {
            UIElement con = this.getControls().get(n);
            con.setX((float)rect.getX());
            con.setY((float)rect.getY());
            con.setWidth((int)(this.clientW - (rect.getX() + rect.getWidth())));
            con.setHeight((int)(this.clientH - (rect.getY() + rect.getHeight())));
            con.onresize();
            n++;
        }
    }
}
