// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.awt.Rectangle;
import java.util.Stack;
import zombie.core.Color;
import zombie.core.textures.Texture;

public final class UINineGrid extends UIElement {
    Texture gridTopLeft;
    Texture gridTop;
    Texture gridTopRight;
    Texture gridLeft;
    Texture gridCenter;
    Texture gridRight;
    Texture gridBottomLeft;
    Texture gridBottom;
    Texture gridBottomRight;
    int topWidth = 10;
    int leftWidth = 10;
    int rightWidth = 10;
    int bottomWidth = 10;
    public int clientH;
    public int clientW;
    public Stack<Rectangle> nestedItems = new Stack<>();
    public Color color = new Color(50, 50, 50, 212);

    public UINineGrid(
        int x,
        int y,
        int width,
        int height,
        int TopWidth,
        int LeftWidth,
        int RightWidth,
        int BottomWidth,
        String TL_Tex,
        String T_Tex,
        String TR_Tex,
        String L_Tex,
        String C_Tex,
        String R_Tex,
        String BL_Tex,
        String B_Tex,
        String BR_Tex
    ) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.topWidth = TopWidth;
        this.leftWidth = LeftWidth;
        this.rightWidth = RightWidth;
        this.bottomWidth = BottomWidth;
        this.gridTopLeft = Texture.getSharedTexture(TL_Tex);
        this.gridTop = Texture.getSharedTexture(T_Tex);
        this.gridTopRight = Texture.getSharedTexture(TR_Tex);
        this.gridLeft = Texture.getSharedTexture(L_Tex);
        this.gridCenter = Texture.getSharedTexture(C_Tex);
        this.gridRight = Texture.getSharedTexture(R_Tex);
        this.gridBottomLeft = Texture.getSharedTexture(BL_Tex);
        this.gridBottom = Texture.getSharedTexture(B_Tex);
        this.gridBottomRight = Texture.getSharedTexture(BR_Tex);
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
        this.DrawTextureScaledCol(this.gridTopLeft, 0.0, 0.0, this.leftWidth, this.topWidth, this.color);
        this.DrawTextureScaledCol(this.gridTop, this.leftWidth, 0.0, this.getWidth() - (this.leftWidth + this.rightWidth), this.topWidth, this.color);
        this.DrawTextureScaledCol(this.gridTopRight, this.getWidth() - this.rightWidth, 0.0, this.rightWidth, this.topWidth, this.color);
        this.DrawTextureScaledCol(this.gridLeft, 0.0, this.topWidth, this.leftWidth, this.getHeight() - (this.topWidth + this.bottomWidth), this.color);
        this.DrawTextureScaledCol(
            this.gridCenter,
            this.leftWidth,
            this.topWidth,
            this.getWidth() - (this.leftWidth + this.rightWidth),
            this.getHeight() - (this.topWidth + this.bottomWidth),
            this.color
        );
        this.DrawTextureScaledCol(
            this.gridRight,
            this.getWidth() - this.rightWidth,
            this.topWidth,
            this.rightWidth,
            this.getHeight() - (this.topWidth + this.bottomWidth),
            this.color
        );
        this.DrawTextureScaledCol(this.gridBottomLeft, 0.0, this.getHeight() - this.bottomWidth, this.leftWidth, this.bottomWidth, this.color);
        this.DrawTextureScaledCol(
            this.gridBottom,
            this.leftWidth,
            this.getHeight() - this.bottomWidth,
            this.getWidth() - (this.leftWidth + this.rightWidth),
            this.bottomWidth,
            this.color
        );
        this.DrawTextureScaledCol(
            this.gridBottomRight, this.getWidth() - this.rightWidth, this.getHeight() - this.bottomWidth, this.rightWidth, this.bottomWidth, this.color
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

    public void setAlpha(float alpha) {
        this.color.a = alpha;
    }

    public float getAlpha() {
        return this.color.a;
    }
}
