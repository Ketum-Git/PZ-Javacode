// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.LuaClosure;
import zombie.IndieGL;
import zombie.UsedFromLua;
import zombie.Lua.LuaManager;
import zombie.core.BoxedStaticValues;
import zombie.core.SpriteRenderer;
import zombie.input.Mouse;

@UsedFromLua
public class AtomUI implements UIElementInterface {
    static int stencilLevel;
    boolean stencil;
    AtomUI stencilNode;
    KahluaTable table;
    String uiname = "";
    final ArrayList<AtomUI> nodes = new ArrayList<>();
    AtomUI parentNode;
    boolean visible = true;
    boolean enabled = true;
    boolean alwaysOnTop;
    boolean alwaysBack;
    Double anchorLeft;
    Double anchorRight;
    Double anchorTop;
    Double anchorDown;
    double x;
    double y;
    double width;
    double height;
    double pivotX = 0.5;
    double pivotY = 0.5;
    double angle;
    double scaleX = 1.0;
    double scaleY = 1.0;
    float colorR = 1.0F;
    float colorG = 1.0F;
    float colorB = 1.0F;
    float colorA = 1.0F;
    double sinA;
    double cosA = 1.0;
    double leftSide;
    double rightSide = 256.0;
    double topSide;
    double downSide = 256.0;
    Object luaMouseButtonDown;
    Object luaMouseButtonUp;
    Object luaMouseButtonDownOutside;
    Object luaMouseButtonUpOutside;
    Object luaMouseWheel;
    Object luaMouseMove;
    Object luaMouseMoveOutside;
    Object luaUpdate;
    Object luaRenderUpdate;
    Object luaKeyPress;
    Object luaKeyRepeat;
    Object luaKeyRelease;
    Object luaResize;

    public AtomUI(KahluaTable table) {
        this.table = table;
    }

    public void init() {
        this.loadFromTable();
        this.updateInternalValues();
        this.updateSize();
    }

    @Override
    public Boolean isIgnoreLossControl() {
        return false;
    }

    @Override
    public Boolean isFollowGameWorld() {
        return false;
    }

    @Override
    public Boolean isDefaultDraw() {
        return true;
    }

    @Override
    public void render() {
        if (this.visible) {
            if (this.checkStencilCollision()) {
                if (this.luaRenderUpdate != null) {
                    LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaRenderUpdate, this.table);
                }

                if (this.stencil) {
                    this.setStencilRect();

                    for (int i = 0; i < this.nodes.size(); i++) {
                        this.nodes.get(i).stencilNode = this;
                        this.nodes.get(i).render();
                    }

                    this.clearStencilRect();
                } else {
                    for (int i = 0; i < this.nodes.size(); i++) {
                        this.nodes.get(i).stencilNode = this.stencilNode;
                        this.nodes.get(i).render();
                    }
                }
            }
        }
    }

    private boolean checkStencilCollision() {
        if (this.stencilNode == null) {
            return true;
        } else {
            double[] a1 = this.getAbsolutePosition(0.0, 0.0);
            double[] a2 = this.getAbsolutePosition(this.width, this.height);
            double[] b1 = this.stencilNode.getAbsolutePosition(0.0, 0.0);
            double[] b2 = this.stencilNode.getAbsolutePosition(this.stencilNode.width, this.stencilNode.height);
            return !(a1[0] > b2[0]) && !(a1[1] > b2[1]) && !(a2[0] < b1[0]) && !(a2[1] < b1[1]);
        }
    }

    @Override
    public Boolean isVisible() {
        return this.visible ? Boolean.TRUE : Boolean.FALSE;
    }

    public void setVisible(boolean value) {
        this.visible = value;
    }

    @Override
    public Boolean isCapture() {
        return false;
    }

    @Override
    public boolean isModalVisible() {
        return false;
    }

    @Override
    public Double getMaxDrawHeight() {
        return BoxedStaticValues.toDouble(-1.0);
    }

    @Override
    public Double getX() {
        return BoxedStaticValues.toDouble(this.x);
    }

    public void setX(double value) {
        this.x = value;
    }

    @Override
    public Double getY() {
        return BoxedStaticValues.toDouble(this.y);
    }

    public void setY(double value) {
        this.y = value;
    }

    @Override
    public Double getWidth() {
        return BoxedStaticValues.toDouble(this.width);
    }

    public void setWidth(double value) {
        this.width = value;
        this.updateInternalValues();
        this.onResize();
    }

    public void setWidthSilent(double value) {
        this.width = value;
        this.table.rawset("width", value);
        this.updateInternalValues();
    }

    @Override
    public Double getHeight() {
        return BoxedStaticValues.toDouble(this.height);
    }

    public void setHeight(double value) {
        this.height = value;
        this.updateInternalValues();
        this.onResize();
    }

    public void setHeightSilent(double value) {
        this.height = value;
        this.table.rawset("height", value);
        this.updateInternalValues();
    }

    public void bringToTop() {
        if (this.parentNode != null) {
            this.parentNode.bringToTop();
        } else {
            UIManager.pushToTop(this);
        }
    }

    @Override
    public boolean isOverElement(double mx, double my) {
        return !this.visible ? false : this.isOverElementLocal(mx - this.x, my - this.y);
    }

    @Override
    public UIElementInterface getParent() {
        return this.parentNode;
    }

    @Override
    public boolean onConsumeMouseButtonDown(int btn, double x, double y) {
        if (!this.enabled) {
            return false;
        } else {
            double[] local = this.toLocalCoordinates(x, y);
            double lx = local[0];
            double ly = local[1];
            this.bringToTop();
            boolean consumed = false;

            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                if (consumed || !ui.isOverElementLocal(lx - ui.x, ly - ui.y)) {
                    ui.onMouseButtonDownOutside(btn, lx - ui.x, ly - ui.y);
                } else if (ui.onConsumeMouseButtonDown(btn, lx - ui.x, ly - ui.y)) {
                    consumed = true;
                }
            }

            if (!consumed && lx >= this.leftSide && ly >= this.topSide && lx < this.rightSide && ly < this.downSide && this.luaMouseButtonDown != null) {
                Boolean res = LuaManager.caller
                    .protectedCallBoolean(
                        UIManager.getDefaultThread(),
                        this.luaMouseButtonDown,
                        new Object[]{this.table, BoxedStaticValues.toDouble(btn), BoxedStaticValues.toDouble(lx), BoxedStaticValues.toDouble(ly)}
                    );
                return res == Boolean.TRUE;
            } else {
                return consumed;
            }
        }
    }

    @Override
    public boolean onConsumeMouseButtonUp(int btn, double x, double y) {
        if (!this.enabled) {
            return false;
        } else {
            double[] local = this.toLocalCoordinates(x, y);
            double lx = local[0];
            double ly = local[1];
            boolean consumed = false;

            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                if (consumed || !ui.isOverElementLocal(lx - ui.x, ly - ui.y)) {
                    ui.onMouseButtonUpOutside(btn, lx - ui.x, ly - ui.y);
                } else if (ui.onConsumeMouseButtonUp(btn, lx - ui.x, ly - ui.y)) {
                    consumed = true;
                }
            }

            if (!consumed && lx >= this.leftSide && ly >= this.topSide && lx < this.rightSide && ly < this.downSide && this.luaMouseButtonUp != null) {
                Boolean res = LuaManager.caller
                    .protectedCallBoolean(
                        UIManager.getDefaultThread(),
                        this.luaMouseButtonUp,
                        new Object[]{this.table, BoxedStaticValues.toDouble(btn), BoxedStaticValues.toDouble(lx), BoxedStaticValues.toDouble(ly)}
                    );
                return res == Boolean.TRUE;
            } else {
                return consumed;
            }
        }
    }

    @Override
    public void onMouseButtonDownOutside(int btn, double x, double y) {
        if (this.enabled) {
            double[] local = this.toLocalCoordinates(x, y);

            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                ui.onMouseButtonDownOutside(btn, local[0] - ui.x, local[1] - ui.y);
            }

            if (this.luaMouseButtonDownOutside != null) {
                LuaManager.caller
                    .pcallvoid(
                        UIManager.getDefaultThread(),
                        this.luaMouseButtonDownOutside,
                        new Object[]{this.table, BoxedStaticValues.toDouble(btn), BoxedStaticValues.toDouble(local[0]), BoxedStaticValues.toDouble(local[1])}
                    );
            }
        }
    }

    @Override
    public void onMouseButtonUpOutside(int btn, double x, double y) {
        if (this.enabled) {
            double[] local = this.toLocalCoordinates(x, y);

            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                ui.onMouseButtonUpOutside(btn, local[0] - ui.x, local[1] - ui.y);
            }

            if (this.luaMouseButtonUpOutside != null) {
                LuaManager.caller
                    .pcallvoid(
                        UIManager.getDefaultThread(),
                        this.luaMouseButtonUpOutside,
                        new Object[]{this.table, BoxedStaticValues.toDouble(btn), BoxedStaticValues.toDouble(local[0]), BoxedStaticValues.toDouble(local[1])}
                    );
            }
        }
    }

    @Override
    public Boolean onConsumeMouseWheel(double del, double x, double y) {
        if (!this.enabled) {
            return false;
        } else {
            double[] local = this.toLocalCoordinates(x, y);
            double lx = local[0];
            double ly = local[1];
            boolean consumed = false;

            for (int i = this.nodes.size() - 1; i >= 0 && !consumed; i--) {
                AtomUI ui = this.nodes.get(i);
                if (ui.isOverElementLocal(lx - ui.x, ly - ui.y) && ui.onConsumeMouseWheel(del, lx - ui.x, ly - ui.y)) {
                    consumed = true;
                }
            }

            if (!consumed && lx >= this.leftSide && ly >= this.topSide && lx < this.rightSide && ly < this.downSide && this.luaMouseWheel != null) {
                Boolean res = LuaManager.caller
                    .protectedCallBoolean(
                        UIManager.getDefaultThread(),
                        this.luaMouseWheel,
                        new Object[]{this.table, BoxedStaticValues.toDouble(del), BoxedStaticValues.toDouble(lx), BoxedStaticValues.toDouble(ly)}
                    );
                return res == Boolean.TRUE;
            } else {
                return consumed;
            }
        }
    }

    @Override
    public Boolean isPointOver(double screenX, double screenY) {
        return this.isOverElement(screenX, screenY);
    }

    @Override
    public Boolean onConsumeMouseMove(double dx, double dy, double x, double y) {
        if (!this.enabled) {
            return false;
        } else {
            double[] local = this.toLocalCoordinates(x, y);
            double lx = local[0];
            double ly = local[1];
            boolean consumed = false;

            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                if (consumed || !ui.isOverElementLocal(lx - ui.x, ly - ui.y)) {
                    ui.onExtendMouseMoveOutside(dx, dy, lx - ui.x, ly - ui.y);
                } else if (ui.onConsumeMouseMove(dx, dy, lx - ui.x, ly - ui.y)) {
                    consumed = true;
                }
            }

            if (!consumed && lx >= this.leftSide && ly >= this.topSide && lx < this.rightSide && ly < this.downSide && this.luaMouseMove != null) {
                Boolean res = LuaManager.caller
                    .protectedCallBoolean(
                        UIManager.getDefaultThread(),
                        this.luaMouseMove,
                        new Object[]{this.table, BoxedStaticValues.toDouble(dx), BoxedStaticValues.toDouble(dy)}
                    );
                return res == Boolean.TRUE;
            } else {
                return consumed;
            }
        }
    }

    @Override
    public void onExtendMouseMoveOutside(double dx, double dy, double x, double y) {
        if (this.enabled) {
            double[] local = this.toLocalCoordinates(x, y);

            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                ui.onExtendMouseMoveOutside(dx, dy, local[0] - ui.x, local[1] - ui.y);
            }

            if (this.luaMouseMoveOutside != null) {
                LuaManager.caller
                    .pcallvoid(
                        UIManager.getDefaultThread(),
                        this.luaMouseMoveOutside,
                        new Object[]{this.table, BoxedStaticValues.toDouble(dx), BoxedStaticValues.toDouble(dy)}
                    );
            }
        }
    }

    @Override
    public void update() {
        if (this.enabled) {
            if (UIManager.doTick) {
                if (this.luaUpdate != null) {
                    LuaManager.caller.pcallvoid(UIManager.getDefaultThread(), this.luaUpdate, this.table);
                }

                for (int i = 0; i < this.nodes.size(); i++) {
                    this.nodes.get(i).update();
                }
            }
        }
    }

    @Override
    public Boolean isMouseOver() {
        return this.isPointOver(Mouse.getXA(), Mouse.getYA()) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public boolean isWantKeyEvents() {
        return true;
    }

    @Override
    public int getRenderThisPlayerOnly() {
        return -1;
    }

    @Override
    public boolean onConsumeKeyPress(int key) {
        if (!this.enabled) {
            return false;
        } else {
            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                if (ui.onConsumeKeyPress(key)) {
                    return true;
                }
            }

            if (this.luaKeyPress != null) {
                Boolean res = LuaManager.caller
                    .protectedCallBoolean(UIManager.getDefaultThread(), this.luaKeyPress, this.table, BoxedStaticValues.toDouble(key));
                return res == Boolean.TRUE;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean onConsumeKeyRepeat(int key) {
        if (!this.enabled) {
            return false;
        } else {
            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                if (ui.onConsumeKeyRepeat(key)) {
                    return true;
                }
            }

            if (this.luaKeyRepeat != null) {
                Boolean res = LuaManager.caller
                    .protectedCallBoolean(UIManager.getDefaultThread(), this.luaKeyRepeat, this.table, BoxedStaticValues.toDouble(key));
                return res == Boolean.TRUE;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean onConsumeKeyRelease(int key) {
        if (!this.enabled) {
            return false;
        } else {
            for (int i = this.nodes.size() - 1; i >= 0; i--) {
                AtomUI ui = this.nodes.get(i);
                if (ui.onConsumeKeyRelease(key)) {
                    return true;
                }
            }

            if (this.luaKeyRelease != null) {
                Boolean res = LuaManager.caller
                    .protectedCallBoolean(UIManager.getDefaultThread(), this.luaKeyRelease, this.table, BoxedStaticValues.toDouble(key));
                return res == Boolean.TRUE;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isForceCursorVisible() {
        return false;
    }

    public KahluaTable getLuaLocalPosition(double x, double y) {
        double[] pos = this.getLocalPosition(x, y);
        KahluaTable data = LuaManager.platform.newTable();
        data.rawset("x", pos[0]);
        data.rawset("y", pos[1]);
        return data;
    }

    public KahluaTable getLuaAbsolutePosition(double x, double y) {
        double[] pos = this.getAbsolutePosition(x, y);
        KahluaTable data = LuaManager.platform.newTable();
        data.rawset("x", pos[0]);
        data.rawset("y", pos[1]);
        return data;
    }

    public KahluaTable getLuaParentPosition(double x, double y) {
        x *= this.scaleX;
        y *= this.scaleY;
        double xx = x * this.cosA + y * this.sinA;
        double yy = -x * this.sinA + y * this.cosA;
        KahluaTable data = LuaManager.platform.newTable();
        data.rawset("x", xx);
        data.rawset("y", yy);
        return data;
    }

    public AtomUI getParentNode() {
        return this.parentNode;
    }

    public void setParentNode(AtomUI parent) {
        this.parentNode = parent;
    }

    public void addNode(AtomUI el) {
        this.nodes.add(el);
        el.setParentNode(this);
    }

    public void removeNode(AtomUI el) {
        this.nodes.remove(el);
        el.setParentNode(null);
    }

    public ArrayList<AtomUI> getNodes() {
        return this.nodes;
    }

    public void setPivotX(double x) {
        this.pivotX = x;
        this.updateInternalValues();
    }

    public Double getPivotX() {
        return this.pivotX;
    }

    public void setPivotY(double y) {
        this.pivotY = y;
        this.updateInternalValues();
    }

    public Double getPivotY() {
        return this.pivotY;
    }

    public void setAngle(double angle) {
        this.angle = angle;
        this.updateInternalValues();
    }

    public Double getAngle() {
        return this.angle;
    }

    public void setScaleX(double x) {
        this.scaleX = x;
        this.onResize();
    }

    public Double getScaleX() {
        return this.scaleX;
    }

    public void setScaleY(double y) {
        this.scaleY = y;
        this.onResize();
    }

    public Double getScaleY() {
        return this.scaleY;
    }

    public void setColor(double r, double g, double b, double a) {
        this.colorR = (float)r;
        this.colorG = (float)g;
        this.colorB = (float)b;
        this.colorA = (float)a;
    }

    public KahluaTable getColor() {
        KahluaTable data = LuaManager.platform.newTable();
        data.rawset("r", BoxedStaticValues.toDouble(this.colorR));
        data.rawset("g", BoxedStaticValues.toDouble(this.colorG));
        data.rawset("b", BoxedStaticValues.toDouble(this.colorB));
        data.rawset("a", BoxedStaticValues.toDouble(this.colorA));
        return data;
    }

    public KahluaTable getTable() {
        return this.table;
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setAlwaysOnTop(boolean value) {
        this.alwaysOnTop = value;
    }

    @Override
    public boolean isAlwaysOnTop() {
        return this.alwaysOnTop;
    }

    public void setBackMost(boolean value) {
        this.alwaysBack = value;
    }

    @Override
    public boolean isBackMost() {
        return this.alwaysBack;
    }

    public String getUIName() {
        return this.uiname;
    }

    public void setUIName(String name) {
        this.uiname = name;
    }

    void loadFromTable() {
        this.uiname = this.tryGetString("uiname", "");
        this.x = this.tryGetDouble("x", 0.0);
        this.y = this.tryGetDouble("y", 0.0);
        this.width = (float)this.tryGetDouble("width", 256.0);
        this.height = (float)this.tryGetDouble("height", 256.0);
        this.pivotX = this.tryGetDouble("pivotX", 0.0);
        this.pivotY = this.tryGetDouble("pivotY", 0.0);
        this.angle = this.tryGetDouble("angle", 0.0);
        this.scaleX = this.tryGetDouble("scaleX", 1.0);
        this.scaleY = this.tryGetDouble("scaleY", 1.0);
        this.colorR = (float)this.tryGetDouble("r", 1.0);
        this.colorG = (float)this.tryGetDouble("g", 1.0);
        this.colorB = (float)this.tryGetDouble("b", 1.0);
        this.colorA = (float)this.tryGetDouble("a", 1.0);
        this.visible = this.tryGetBoolean("visible", true);
        this.enabled = this.tryGetBoolean("enabled", true);
        this.stencil = this.tryGetBoolean("isStencil", false);
        this.anchorLeft = UIManager.tableget(this.table, "anchorLeft") instanceof Double left ? left : null;
        this.anchorRight = UIManager.tableget(this.table, "anchorRight") instanceof Double right ? right : null;
        this.anchorTop = UIManager.tableget(this.table, "anchorTop") instanceof Double top ? top : null;
        this.anchorDown = UIManager.tableget(this.table, "anchorDown") instanceof Double down ? down : null;
        this.luaMouseButtonDown = this.tryGetClosure("onMouseButtonDown");
        this.luaMouseButtonUp = this.tryGetClosure("onMouseButtonUp");
        this.luaMouseButtonDownOutside = this.tryGetClosure("onMouseButtonDownOutside");
        this.luaMouseButtonUpOutside = this.tryGetClosure("onMouseButtonUpOutside");
        this.luaMouseWheel = this.tryGetClosure("onMouseWheel");
        this.luaMouseMove = this.tryGetClosure("onMouseMove");
        this.luaMouseMoveOutside = this.tryGetClosure("onMouseMoveOutside");
        this.luaUpdate = this.tryGetClosure("update");
        this.luaRenderUpdate = this.tryGetClosure("renderUpdate");
        this.luaKeyPress = this.tryGetClosure("onKeyPress");
        this.luaKeyRepeat = this.tryGetClosure("onKeyRepeat");
        this.luaKeyRelease = this.tryGetClosure("onKeyRelease");
        this.luaResize = this.tryGetClosure("onResize");
    }

    void updateInternalValues() {
        this.leftSide = -this.pivotX * this.width;
        this.topSide = -this.pivotY * this.height;
        this.rightSide = this.leftSide + this.width;
        this.downSide = this.topSide + this.height;
        double radian = Math.toRadians(this.angle);
        this.cosA = Math.cos(radian);
        this.sinA = Math.sin(radian);
    }

    double[] toLocalCoordinates(double x, double y) {
        double lx = x * this.cosA - y * this.sinA;
        double ly = x * this.sinA + y * this.cosA;
        return new double[]{lx / this.scaleX, ly / this.scaleY};
    }

    double[] getLocalPosition(double absoluteX, double absoluteY) {
        double[] localParent = new double[]{absoluteX, absoluteY};
        if (this.parentNode != null) {
            localParent = this.parentNode.getLocalPosition(absoluteX, absoluteY);
        }

        return this.toLocalCoordinates(localParent[0] - this.x, localParent[1] - this.y);
    }

    double[] getAbsolutePosition(double localX, double localY) {
        localX *= this.scaleX;
        localY *= this.scaleY;
        double parentLocalX = localX * this.cosA + localY * this.sinA + this.x;
        double parentLocalY = -localX * this.sinA + localY * this.cosA + this.y;
        return this.parentNode != null ? this.parentNode.getAbsolutePosition(parentLocalX, parentLocalY) : new double[]{parentLocalX, parentLocalY};
    }

    boolean isOverElementLocal(double x, double y) {
        double[] local = this.toLocalCoordinates(x, y);
        double localX = local[0];
        double localY = local[1];
        if (localX >= this.leftSide && localY >= this.topSide && localX < this.rightSide && localY < this.downSide) {
            return true;
        } else if (!this.stencil || !(localX < this.leftSide) && !(localY < this.topSide) && !(localX >= this.rightSide) && !(localY >= this.downSide)) {
            for (int i = 0; i < this.nodes.size(); i++) {
                AtomUI ui = this.nodes.get(i);
                if (ui.isOverElementLocal(localX - ui.x, localY - ui.y)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    void updateSize() {
        if (this.parentNode != null) {
            double pWidth = this.parentNode.width;
            if (this.anchorLeft != null && this.anchorRight != null) {
                this.setWidthSilent((pWidth - this.anchorLeft + this.anchorRight) / this.scaleX);
                this.setX(-this.parentNode.pivotX * pWidth + this.anchorLeft + this.pivotX * this.width * this.scaleX);
            } else if (this.anchorLeft != null) {
                this.setX(-this.parentNode.pivotX * pWidth + this.anchorLeft + this.pivotX * this.width * this.scaleX);
            } else if (this.anchorRight != null) {
                this.setX((1.0 - this.parentNode.pivotX) * pWidth + this.anchorRight - (1.0 - this.pivotX) * this.width * this.scaleX);
            }

            double pHeight = this.parentNode.height;
            if (this.anchorTop != null && this.anchorDown != null) {
                this.setHeightSilent((pHeight - this.anchorTop + this.anchorDown) / this.scaleY);
                this.setY(-this.parentNode.pivotY * pHeight + this.anchorTop + this.pivotY * this.height * this.scaleY);
            } else if (this.anchorTop != null) {
                this.setY(-this.parentNode.pivotY * pHeight + this.anchorTop + this.pivotY * this.height * this.scaleY);
            } else if (this.anchorDown != null) {
                this.setY((1.0 - this.parentNode.pivotY) * pHeight + this.anchorDown - (1.0 - this.pivotY) * this.height * this.scaleY);
            }
        }
    }

    void onResize() {
        this.updateSize();
        if (this.luaResize != null) {
            LuaManager.caller
                .pcallvoid(
                    UIManager.getDefaultThread(), this.luaResize, this.table, BoxedStaticValues.toDouble(this.width), BoxedStaticValues.toDouble(this.height)
                );
        }

        for (int i = 0; i < this.nodes.size(); i++) {
            this.nodes.get(i).onResize();
        }
    }

    public void setStencilRect() {
        IndieGL.glStencilMask(255);
        IndieGL.enableStencilTest();
        IndieGL.enableAlphaTest();
        stencilLevel++;
        IndieGL.glStencilFunc(519, stencilLevel, 255);
        IndieGL.glStencilOp(7680, 7680, 7681);
        double[] leftTop = this.getAbsolutePosition(this.leftSide, this.topSide);
        double[] rightTop = this.getAbsolutePosition(this.rightSide, this.topSide);
        double[] rightDown = this.getAbsolutePosition(this.rightSide, this.downSide);
        double[] leftDown = this.getAbsolutePosition(this.leftSide, this.downSide);
        IndieGL.glColorMask(false, false, false, false);
        SpriteRenderer.instance
            .render(null, leftTop[0], leftTop[1], rightTop[0], rightTop[1], rightDown[0], rightDown[1], leftDown[0], leftDown[1], 1.0F, 0.0F, 0.0F, 1.0F, null);
        IndieGL.glColorMask(true, true, true, true);
        IndieGL.glStencilOp(7680, 7680, 7680);
        IndieGL.glStencilFunc(514, stencilLevel, 255);
    }

    public void clearStencilRect() {
        if (stencilLevel > 0) {
            stencilLevel--;
        }

        if (stencilLevel > 0) {
            IndieGL.glStencilFunc(514, stencilLevel, 255);
        } else {
            IndieGL.glAlphaFunc(519, 0.0F);
            IndieGL.disableStencilTest();
            IndieGL.disableAlphaTest();
            IndieGL.glStencilFunc(519, 255, 255);
            IndieGL.glStencilOp(7680, 7680, 7680);
            IndieGL.glClear(1280);
        }
    }

    public void repaintStencilRect() {
        if (stencilLevel > 0) {
            double[] pos0 = this.getAbsolutePosition(this.x, this.y);
            double[] pos1 = this.getAbsolutePosition(this.x + this.width, this.y + this.height);
            IndieGL.glStencilFunc(519, stencilLevel, 255);
            IndieGL.glStencilOp(7680, 7680, 7681);
            double[] leftTop = this.getAbsolutePosition(this.leftSide, this.topSide);
            double[] rightTop = this.getAbsolutePosition(this.rightSide, this.topSide);
            double[] rightDown = this.getAbsolutePosition(this.rightSide, this.downSide);
            double[] leftDown = this.getAbsolutePosition(this.leftSide, this.downSide);
            IndieGL.glColorMask(false, false, false, false);
            SpriteRenderer.instance
                .render(
                    null, leftTop[0], leftTop[1], rightTop[0], rightTop[1], rightDown[0], rightDown[1], leftDown[0], leftDown[1], 1.0F, 0.0F, 0.0F, 1.0F, null
                );
            IndieGL.glColorMask(true, true, true, true);
            IndieGL.glStencilOp(7680, 7680, 7680);
            IndieGL.glStencilFunc(514, stencilLevel, 255);
        }
    }

    double tryGetDouble(String key, double defaultValue) {
        return UIManager.tableget(this.table, key) instanceof Double d ? d : defaultValue;
    }

    boolean tryGetBoolean(String key, boolean defaultValue) {
        return UIManager.tableget(this.table, key) instanceof Boolean b ? b : defaultValue;
    }

    LuaClosure tryGetClosure(String key) {
        return UIManager.tableget(this.table, key) instanceof LuaClosure luaClosure ? luaClosure : null;
    }

    String tryGetString(String key, String defaultValue) {
        return UIManager.tableget(this.table, key) instanceof String s ? s : defaultValue;
    }
}
