// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import zombie.core.Color;
import zombie.core.math.PZMath;
import zombie.core.textures.Texture;
import zombie.input.Mouse;

public final class ScrollBar extends UIElement {
    public final Color backgroundColour = new Color(255, 255, 255, 255);
    public final Color buttonColour = new Color(255, 255, 255, 127);
    public final Color buttonHighlightColour = new Color(255, 255, 255, 255);
    public boolean isVerticle = true;
    private int fullLength = 114;
    private int insideLength = 100;
    private final int endLength = 7;
    private float buttonInsideLength = 30.0F;
    private final int buttonEndLength = 6;
    private final int thickness = 10;
    private final int buttonThickness = 9;
    private float buttonOffset = 40.0F;
    private int mouseDragStartPos;
    private float buttonDragStartPos;
    private final Texture backVertical;
    private final Texture topVertical;
    private final Texture bottomVertical;
    private final Texture buttonBackVertical;
    private final Texture buttonTopVertical;
    private final Texture buttonBottomVertical;
    private final Texture backHorizontal;
    private final Texture leftHorizontal;
    private final Texture rightHorizontal;
    private final Texture buttonBackHorizontal;
    private final Texture buttonLeftHorizontal;
    private final Texture buttonRightHorizontal;
    private boolean mouseOver;
    private boolean beingDragged;
    private UITextBox2 parentTextBox;
    UIEventHandler messageParent;
    private final String name;

    public ScrollBar(String name, UIEventHandler messages, int x_pos, int y_pos, int Length, boolean IsVertical) {
        this.messageParent = messages;
        this.name = name;
        this.x = x_pos;
        this.y = y_pos;
        this.fullLength = Length;
        this.insideLength = Length - 14;
        this.isVerticle = true;
        this.width = 10.0F;
        this.height = Length;
        this.buttonInsideLength = this.height - 12.0F;
        this.buttonOffset = 0.0F;
        this.backVertical = Texture.getSharedTexture("media/ui/ScrollbarV_Bkg_Middle.png");
        this.topVertical = Texture.getSharedTexture("media/ui/ScrollbarV_Bkg_Top.png");
        this.bottomVertical = Texture.getSharedTexture("media/ui/ScrollbarV_Bkg_Bottom.png");
        this.buttonBackVertical = Texture.getSharedTexture("media/ui/ScrollbarV_Middle.png");
        this.buttonTopVertical = Texture.getSharedTexture("media/ui/ScrollbarV_Top.png");
        this.buttonBottomVertical = Texture.getSharedTexture("media/ui/ScrollbarV_Bottom.png");
        this.backHorizontal = Texture.getSharedTexture("media/ui/ScrollbarH_Bkg_Middle.png");
        this.leftHorizontal = Texture.getSharedTexture("media/ui/ScrollbarH_Bkg_Bottom.png");
        this.rightHorizontal = Texture.getSharedTexture("media/ui/ScrollbarH_Bkg_Top.png");
        this.buttonBackHorizontal = Texture.getSharedTexture("media/ui/ScrollbarH_Middle.png");
        this.buttonLeftHorizontal = Texture.getSharedTexture("media/ui/ScrollbarH_Bottom.png");
        this.buttonRightHorizontal = Texture.getSharedTexture("media/ui/ScrollbarH_Top.png");
    }

    public void SetParentTextBox(UITextBox2 Parent) {
        this.parentTextBox = Parent;
    }

    /**
     * 
     * @param height the height to set
     */
    @Override
    public void setHeight(double height) {
        super.setHeight(height);
        this.fullLength = (int)height;
        this.insideLength = (int)height - 14;
    }

    @Override
    public void render() {
        if (this.isVerticle) {
            this.DrawTextureScaledCol(this.topVertical, 0.0, 0.0, 10.0, 7.0, this.backgroundColour);
            this.DrawTextureScaledCol(this.backVertical, 0.0, 7.0, 10.0, this.insideLength, this.backgroundColour);
            this.DrawTextureScaledCol(this.bottomVertical, 0.0, 7 + this.insideLength, 10.0, 7.0, this.backgroundColour);
            Color DrawCol;
            if (this.mouseOver) {
                DrawCol = this.buttonHighlightColour;
            } else {
                DrawCol = this.buttonColour;
            }

            this.DrawTextureScaledCol(this.buttonTopVertical, 1.0, (int)this.buttonOffset + 1, 9.0, 6.0, DrawCol);
            this.DrawTextureScaledCol(this.buttonBackVertical, 1.0, (int)this.buttonOffset + 1 + 6, 9.0, this.buttonInsideLength, DrawCol);
            this.DrawTextureScaledCol(this.buttonBottomVertical, 1.0, (int)this.buttonOffset + 1 + 6 + this.buttonInsideLength, 9.0, 6.0, DrawCol);
        }
    }

    @Override
    public Boolean onMouseMove(double dx, double dy) {
        this.mouseOver = true;
        return Boolean.TRUE;
    }

    @Override
    public void onMouseMoveOutside(double dx, double dy) {
        this.mouseOver = false;
    }

    @Override
    public Boolean onMouseUp(double x, double y) {
        this.beingDragged = false;
        return Boolean.FALSE;
    }

    @Override
    public Boolean onMouseDown(double x, double y) {
        boolean ClickedOnButton = false;
        if (y >= this.buttonOffset && y <= this.buttonOffset + this.buttonInsideLength + 12.0F) {
            ClickedOnButton = true;
        }

        if (ClickedOnButton) {
            this.beingDragged = true;
            this.mouseDragStartPos = Mouse.getYA();
            this.buttonDragStartPos = this.buttonOffset;
        } else {
            this.buttonOffset = (float)(y - (this.buttonInsideLength + 12.0F) / 2.0F);
        }

        if (this.buttonOffset < 0.0F) {
            this.buttonOffset = 0.0F;
        }

        if (this.buttonOffset > this.getHeight().intValue() - (this.buttonInsideLength + 12.0F) - 1.0F) {
            this.buttonOffset = this.getHeight().intValue() - (this.buttonInsideLength + 12.0F) - 1.0F;
        }

        return Boolean.FALSE;
    }

    public boolean isBeingDragged() {
        return this.beingDragged;
    }

    @Override
    public void update() {
        super.update();
        if (this.beingDragged) {
            int MouseDist = this.mouseDragStartPos - Mouse.getYA();
            this.buttonOffset = this.buttonDragStartPos - MouseDist;
            if (this.buttonOffset < 0.0F) {
                this.buttonOffset = 0.0F;
            }

            if (this.buttonOffset > this.getHeight().intValue() - (this.buttonInsideLength + 12.0F) - 0.0F) {
                this.buttonOffset = this.getHeight().intValue() - (this.buttonInsideLength + 12.0F) - 0.0F;
            }

            if (!Mouse.isButtonDown(0)) {
                this.beingDragged = false;
            }
        }

        if (this.parentTextBox != null) {
            int TextHeight = TextManager.instance.getFontFromEnum(this.parentTextBox.font).getLineHeight();
            if (this.parentTextBox.lines.size() > this.parentTextBox.numVisibleLines) {
                if (!this.parentTextBox.lines.isEmpty()) {
                    int NumVisibleLines = this.parentTextBox.numVisibleLines;
                    if (NumVisibleLines * TextHeight > this.parentTextBox.getHeight().intValue() - this.parentTextBox.getInset() * 2) {
                        NumVisibleLines--;
                    }

                    float PercentShown = (float)NumVisibleLines / this.parentTextBox.lines.size();
                    this.buttonInsideLength = (int)(this.getHeight().intValue() * PercentShown) - 12;
                    this.buttonInsideLength = Math.max(this.buttonInsideLength, 0.0F);
                    float buttonHeight = this.buttonInsideLength + 12.0F;
                    if (this.buttonOffset < 0.0F) {
                        this.buttonOffset = 0.0F;
                    }

                    if (this.buttonOffset > this.getHeight().intValue() - buttonHeight - 0.0F) {
                        this.buttonOffset = this.getHeight().intValue() - buttonHeight - 0.0F;
                    }

                    float PercentDown = this.buttonOffset / this.getHeight().intValue();
                    this.parentTextBox.topLineIndex = (int)(this.parentTextBox.lines.size() * PercentDown);
                    int height = this.getHeight().intValue();
                    int freePixels = height - (int)buttonHeight;
                    int unseenPixels = TextHeight * (this.parentTextBox.lines.size() - NumVisibleLines);
                    float ratio = (float)freePixels / unseenPixels;
                    float pos = this.buttonOffset / ratio;
                    this.parentTextBox.topLineIndex = PZMath.min((int)(pos / TextHeight), this.parentTextBox.lines.size() - NumVisibleLines - 1);
                } else {
                    this.buttonOffset = 0.0F;
                    this.buttonInsideLength = this.getHeight().intValue() - 12;
                    this.parentTextBox.topLineIndex = 0;
                }
            } else {
                this.buttonOffset = 0.0F;
                this.buttonInsideLength = this.getHeight().intValue() - 12;
                this.parentTextBox.topLineIndex = 0;
            }
        }
    }

    public void scrollToBottom() {
        this.buttonOffset = this.getHeight().intValue() - (this.buttonInsideLength + 12.0F) - 0.0F;
    }
}
