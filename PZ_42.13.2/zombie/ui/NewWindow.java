// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.Stack;
import org.lwjgl.util.Rectangle;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;

public class NewWindow extends UIElement {
    public int clickX;
    public int clickY;
    public int clientH;
    public int clientW;
    public boolean movable = true;
    public boolean moving;
    public int ncclientH;
    public int ncclientW;
    public Stack<Rectangle> nestedItems = new Stack<>();
    public boolean resizeToFitY = true;
    float alpha = 1.0F;
    Texture dialogBottomLeft;
    Texture dialogBottomMiddle;
    Texture dialogBottomRight;
    Texture dialogLeft;
    Texture dialogMiddle;
    Texture dialogRight;
    Texture titleCloseIcon;
    Texture titleLeft;
    Texture titleMiddle;
    Texture titleRight;
    HUDButton closeButton;

    public NewWindow(int x, int y, int width, int height, boolean bHasClose) {
        this.x = x;
        this.y = y;
        if (width < 156) {
            width = 156;
        }

        if (height < 78) {
            height = 78;
        }

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
        if (bHasClose) {
            this.closeButton = new HUDButton(
                "close", (double)(width - 16), 2.0, "media/ui/inventoryPanes/Button_Close.png", "media/ui/inventoryPanes/Button_Close.png", this
            );
            this.closeButton.setWidth(14.0);
            this.closeButton.setHeight(14.0);
            this.AddChild(this.closeButton);
        }

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
    public void ButtonClicked(String name) {
        super.ButtonClicked(name);
        if (name.equals("close")) {
            this.setVisible(false);
        }
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        if (!this.isVisible()) {
            return Boolean.FALSE;
        } else {
            super.onMouseDown(x, y);
            if (y < 18.0) {
                this.clickX = (int)x;
                this.clickY = (int)y;
                if (this.movable) {
                    this.moving = true;
                }

                this.setCapture(true);
            }

            return Boolean.TRUE;
        }
    }

    public void setMovable(boolean bMoveable) {
        this.movable = bMoveable;
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        if (!this.isVisible()) {
            return Boolean.FALSE;
        } else {
            super.onMouseMove(dx, dy);
            if (this.moving) {
                this.setX(this.getX() + dx);
                this.setY(this.getY() + dy);
            }

            return Boolean.FALSE;
        }
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        if (this.isVisible()) {
            super.onMouseMoveOutside(dx, dy);
            if (this.moving) {
                this.setX(this.getX() + dx);
                this.setY(this.getY() + dy);
            }
        }
    }

    @Override
    public Boolean onMouseUp(double x, double y) {
        if (!this.isVisible()) {
            return Boolean.FALSE;
        } else {
            super.onMouseUp(x, y);
            this.moving = false;
            this.setCapture(false);
            return Boolean.TRUE;
        }
    }

    @Override
    public void render() {
        this.dialogRight.getTextureId().setMinFilter(9728);
        float alpha = 0.8F * this.alpha;
        int x = 0;
        int y = 0;
        int titleHeight = PZMath.max(this.titleMiddle.getHeight(), TextManager.instance.getFontHeight(UIFont.Small));
        this.DrawTexture(this.titleLeft, 0.0, y, alpha);
        this.DrawTexture(this.titleRight, this.getWidth() - this.titleRight.getWidth(), y, alpha);
        this.DrawTextureScaled(this.titleMiddle, this.titleLeft.getWidth(), y, this.getWidth() - this.titleLeft.getWidth() * 2, titleHeight, alpha);
        y += this.titleRight.getHeight();
        this.DrawTextureScaled(
            this.dialogLeft, 0.0, y, this.dialogLeft.getWidth(), this.getHeight() - this.titleLeft.getHeight() - this.dialogBottomLeft.getHeight(), alpha
        );
        this.DrawTextureScaled(
            this.dialogMiddle,
            this.dialogLeft.getWidth(),
            y,
            this.getWidth() - this.dialogRight.getWidth() * 2,
            this.getHeight() - this.titleLeft.getHeight() - this.dialogBottomLeft.getHeight(),
            alpha
        );
        this.DrawTextureScaled(
            this.dialogRight,
            this.getWidth() - this.dialogRight.getWidth(),
            y,
            this.dialogLeft.getWidth(),
            this.getHeight() - this.titleLeft.getHeight() - this.dialogBottomLeft.getHeight(),
            alpha
        );
        y = (int)(y + (this.getHeight() - this.titleLeft.getHeight() - this.dialogBottomLeft.getHeight()));
        this.DrawTextureScaled(
            this.dialogBottomMiddle,
            this.dialogBottomLeft.getWidth(),
            y,
            this.getWidth() - this.dialogBottomLeft.getWidth() * 2,
            this.dialogBottomMiddle.getHeight(),
            alpha
        );
        this.DrawTexture(this.dialogBottomLeft, 0.0, y, alpha);
        this.DrawTexture(this.dialogBottomRight, this.getWidth() - this.dialogBottomRight.getWidth(), y, alpha);
        super.render();
    }

    @Override
    public void update() {
        super.update();
        if (this.closeButton != null) {
            this.closeButton.setX(3.0);
            this.closeButton.setY(3.0);
            this.closeButton.setWidth(15.0);
            this.closeButton.setHeight(15.0);
        }

        int n = 0;
        if (!this.resizeToFitY) {
            for (Rectangle rect : this.nestedItems) {
                UIElement con = this.getControls().get(n);
                if (con != this.closeButton) {
                    con.setX(rect.getX());
                    con.setY(rect.getY());
                    con.setWidth(this.clientW - (rect.getX() + rect.getWidth()));
                    con.setHeight(this.clientH - (rect.getY() + rect.getHeight()));
                    con.onresize();
                    n++;
                }
            }
        } else {
            int x = 100000;
            int y = 100000;
            float width = 0.0F;
            float height = 0.0F;

            for (Rectangle rectx : this.nestedItems) {
                UIElement con = this.getControls().get(n);
                if (con != this.closeButton) {
                    if (x > con.getAbsoluteX()) {
                        x = con.getAbsoluteX().intValue();
                    }

                    if (y > con.getAbsoluteX()) {
                        y = con.getAbsoluteX().intValue();
                    }

                    if (width < con.getWidth()) {
                        width = con.getWidth().intValue();
                    }

                    if (height < con.getHeight()) {
                        height = con.getHeight().intValue();
                    }

                    n++;
                }
            }

            height += 50.0F;
            this.height = height;
        }
    }
}
