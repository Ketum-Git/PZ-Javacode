// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

public interface UIElementInterface {
    Boolean isIgnoreLossControl();

    Boolean isFollowGameWorld();

    Boolean isDefaultDraw();

    void render();

    Boolean isVisible();

    Boolean isCapture();

    boolean isModalVisible();

    Double getMaxDrawHeight();

    Double getX();

    Double getY();

    Double getWidth();

    Double getHeight();

    boolean isOverElement(double arg0, double arg1);

    UIElementInterface getParent();

    boolean onConsumeMouseButtonDown(int arg0, double arg1, double arg2);

    boolean onConsumeMouseButtonUp(int arg0, double arg1, double arg2);

    void onMouseButtonDownOutside(int arg0, double arg1, double arg2);

    void onMouseButtonUpOutside(int arg0, double arg1, double arg2);

    Boolean onConsumeMouseWheel(double arg0, double arg1, double arg2);

    Boolean isPointOver(double arg0, double arg1);

    Boolean onConsumeMouseMove(double arg0, double arg1, double arg2, double arg3);

    void onExtendMouseMoveOutside(double arg0, double arg1, double arg2, double arg3);

    void update();

    Boolean isMouseOver();

    boolean isWantKeyEvents();

    boolean onConsumeKeyPress(int arg0);

    boolean onConsumeKeyRepeat(int arg0);

    boolean onConsumeKeyRelease(int arg0);

    boolean isForceCursorVisible();

    int getRenderThisPlayerOnly();

    boolean isAlwaysOnTop();

    boolean isBackMost();
}
