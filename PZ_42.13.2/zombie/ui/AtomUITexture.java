// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.ui;

import java.util.ArrayList;
import se.krka.kahlua.vm.KahluaTable;
import zombie.UsedFromLua;
import zombie.core.SpriteRenderer;
import zombie.core.textures.Texture;

@UsedFromLua
public class AtomUITexture extends AtomUI {
    Texture tex;
    double sliceLeft;
    double sliceTop;
    double sliceRight;
    double sliceDown;
    double animDelay;
    int animFrameNum;
    int animFrameRows;
    int animFrameColumns;
    boolean textureIsReady;
    boolean isSlice9;
    final ArrayList<AtomUITexture.Slice> slices = new ArrayList<>();
    boolean isAnim;
    int frameIndex;
    long frameTimer;
    long beforeTime;
    boolean isAnimPlay;

    public AtomUITexture(KahluaTable table) {
        super(table);
    }

    @Override
    public void render() {
        if (this.visible) {
            if (!this.textureIsReady) {
                this.textureIsReady = this.tex == null || this.tex.isReady();
                if (this.textureIsReady) {
                    this.updateSlices();
                }
            } else if (this.isSlice9) {
                this.drawTextureSlice9();
            } else if (this.isAnim) {
                this.drawTextureAnim();
                if (this.isAnimPlay) {
                    long time = System.currentTimeMillis();
                    this.frameTimer = this.frameTimer + (time - this.beforeTime);
                    if (this.frameTimer > this.animDelay) {
                        this.frameTimer = (long)(this.frameTimer - this.animDelay);
                        this.frameIndex++;
                        if (this.frameIndex >= this.animFrameNum) {
                            this.frameIndex = 0;
                        }
                    }

                    this.beforeTime = time;
                }
            } else {
                this.drawTexture();
            }

            super.render();
        }
    }

    public void animPlay() {
        if (this.isAnim) {
            this.beforeTime = System.currentTimeMillis();
            this.isAnimPlay = true;
        }
    }

    public void animStop() {
        if (this.isAnim) {
            this.isAnimPlay = false;
            this.frameIndex = 0;
            this.frameTimer = 0L;
        }
    }

    public void animPause() {
        if (this.isAnim) {
            this.isAnimPlay = false;
        }
    }

    void drawTextureAnim() {
        double[] leftTop = this.getAbsolutePosition(this.leftSide, this.topSide);
        double[] rightTop = this.getAbsolutePosition(this.rightSide, this.topSide);
        double[] rightDown = this.getAbsolutePosition(this.rightSide, this.downSide);
        double[] leftDown = this.getAbsolutePosition(this.leftSide, this.downSide);
        AtomUITexture.Slice slice = this.slices.get(this.frameIndex);
        SpriteRenderer.instance
            .render(
                this.tex,
                leftTop[0],
                leftTop[1],
                rightTop[0],
                rightTop[1],
                rightDown[0],
                rightDown[1],
                leftDown[0],
                leftDown[1],
                slice.uvLeft,
                slice.uvTop,
                slice.uvRight,
                slice.uvTop,
                slice.uvRight,
                slice.uvDown,
                slice.uvLeft,
                slice.uvDown,
                this.colorR,
                this.colorG,
                this.colorB,
                this.colorA
            );
    }

    void drawTexture() {
        double[] leftTop = this.getAbsolutePosition(this.leftSide, this.topSide);
        double[] rightTop = this.getAbsolutePosition(this.rightSide, this.topSide);
        double[] rightDown = this.getAbsolutePosition(this.rightSide, this.downSide);
        double[] leftDown = this.getAbsolutePosition(this.leftSide, this.downSide);
        SpriteRenderer.instance
            .render(
                this.tex,
                leftTop[0],
                leftTop[1],
                rightTop[0],
                rightTop[1],
                rightDown[0],
                rightDown[1],
                leftDown[0],
                leftDown[1],
                this.colorR,
                this.colorG,
                this.colorB,
                this.colorA,
                null
            );
    }

    void drawTextureSlice9() {
        for (int i = 0; i < this.slices.size(); i++) {
            AtomUITexture.Slice slice = this.slices.get(i);
            double[] leftTop = this.getAbsolutePosition(slice.leftSide, slice.topSide);
            double[] rightTop = this.getAbsolutePosition(slice.rightSide, slice.topSide);
            double[] rightDown = this.getAbsolutePosition(slice.rightSide, slice.downSide);
            double[] leftDown = this.getAbsolutePosition(slice.leftSide, slice.downSide);
            SpriteRenderer.instance
                .render(
                    this.tex,
                    leftTop[0],
                    leftTop[1],
                    rightTop[0],
                    rightTop[1],
                    rightDown[0],
                    rightDown[1],
                    leftDown[0],
                    leftDown[1],
                    slice.uvLeft,
                    slice.uvTop,
                    slice.uvRight,
                    slice.uvTop,
                    slice.uvRight,
                    slice.uvDown,
                    slice.uvLeft,
                    slice.uvDown,
                    this.colorR,
                    this.colorG,
                    this.colorB,
                    this.colorA
                );
        }
    }

    @Override
    public void init() {
        super.init();
        this.updateInternalValues();
    }

    @Override
    void loadFromTable() {
        super.loadFromTable();
        this.tex = this.tryGetTexture("texture");
        this.sliceTop = this.tryGetDouble("sliceTop", 0.0);
        this.sliceDown = this.tryGetDouble("sliceDown", 0.0);
        this.sliceLeft = this.tryGetDouble("sliceLeft", 0.0);
        this.sliceRight = this.tryGetDouble("sliceRight", 0.0);
        this.animDelay = this.tryGetDouble("animDelay", 0.0);
        this.animFrameNum = (int)this.tryGetDouble("animFrameNum", 0.0);
        this.animFrameRows = (int)this.tryGetDouble("animFrameRows", 0.0);
        this.animFrameColumns = (int)this.tryGetDouble("animFrameColumns", 0.0);
    }

    @Override
    void updateInternalValues() {
        super.updateInternalValues();
        this.textureIsReady = this.tex == null || this.tex.isReady();
        this.isSlice9 = this.sliceLeft != 0.0 || this.sliceRight != 0.0 || this.sliceTop != 0.0 || this.sliceDown != 0.0;
        this.isAnim = this.animDelay != 0.0 && this.animFrameNum != 0 && this.animFrameRows != 0 && this.animFrameColumns != 0;
        this.updateSlices();
    }

    private void updateSlices() {
        this.slices.clear();
        if (this.tex != null && this.tex.isReady()) {
            if (this.isSlice9) {
                this.updateSlices9();
            } else if (this.isAnim) {
                this.updateSlicesAnim();
            }
        }
    }

    private void updateSlicesAnim() {
        double texUVX = this.tex.getXStart();
        double texUVY = this.tex.getYStart();
        double texUVW = this.tex.getXEnd() - texUVX;
        double texUVH = this.tex.getYEnd() - texUVY;
        double frameUVWidth = texUVW / this.animFrameColumns;
        double frameUVHeight = texUVH / this.animFrameRows;

        for (int y = 0; y < this.animFrameRows; y++) {
            for (int x = 0; x < this.animFrameColumns; x++) {
                int frameIndex = y * this.animFrameColumns + x;
                if (frameIndex >= this.animFrameNum) {
                    return;
                }

                this.slices
                    .add(
                        new AtomUITexture.Slice(
                            0.0,
                            0.0,
                            0.0,
                            0.0,
                            texUVX + frameUVWidth * x,
                            texUVX + frameUVWidth * (x + 1),
                            texUVY + frameUVHeight * y,
                            texUVY + frameUVHeight * (y + 1)
                        )
                    );
            }
        }
    }

    private void updateSlices9() {
        double texW = this.tex.getWidth();
        double texH = this.tex.getHeight();
        double texUVX = this.tex.getXStart();
        double texUVY = this.tex.getYStart();
        double texUVW = this.tex.getXEnd() - texUVX;
        double texUVH = this.tex.getYEnd() - texUVY;
        double x0 = this.leftSide;
        double x1 = this.leftSide + this.sliceLeft;
        double x2 = this.rightSide - this.sliceRight;
        double x3 = this.rightSide;
        double y0 = this.topSide;
        double y1 = this.topSide + this.sliceTop;
        double y2 = this.downSide - this.sliceDown;
        double y3 = this.downSide;
        double u1 = texUVX + texUVW * (this.sliceLeft / texW);
        double u2 = texUVX + texUVW * ((texW - this.sliceRight) / texW);
        double u3 = texUVX + texUVW;
        double v1 = texUVY + texUVH * (this.sliceTop / texH);
        double v2 = texUVY + texUVH * ((texH - this.sliceDown) / texH);
        double v3 = texUVY + texUVH;
        if (this.sliceLeft != 0.0 && this.sliceTop != 0.0) {
            this.slices.add(new AtomUITexture.Slice(x0, x1, y0, y1, texUVX, u1, texUVY, v1));
        }

        if (this.sliceTop != 0.0) {
            this.slices.add(new AtomUITexture.Slice(x1, x2, y0, y1, u1, u2, texUVY, v1));
        }

        if (this.sliceRight != 0.0 && this.sliceTop != 0.0) {
            this.slices.add(new AtomUITexture.Slice(x2, x3, y0, y1, u2, u3, texUVY, v1));
        }

        if (this.sliceLeft != 0.0) {
            this.slices.add(new AtomUITexture.Slice(x0, x1, y1, y2, texUVX, u1, v1, v2));
        }

        this.slices.add(new AtomUITexture.Slice(x1, x2, y1, y2, u1, u2, v1, v2));
        if (this.sliceRight != 0.0) {
            this.slices.add(new AtomUITexture.Slice(x2, x3, y1, y2, u2, u3, v1, v2));
        }

        if (this.sliceLeft != 0.0 && this.sliceDown != 0.0) {
            this.slices.add(new AtomUITexture.Slice(x0, x1, y2, y3, texUVX, u1, v2, v3));
        }

        if (this.sliceDown != 0.0) {
            this.slices.add(new AtomUITexture.Slice(x1, x2, y2, y3, u1, u2, v2, v3));
        }

        if (this.sliceRight != 0.0 && this.sliceDown != 0.0) {
            this.slices.add(new AtomUITexture.Slice(x2, x3, y2, y3, u2, u3, v2, v3));
        }
    }

    public void setTexture(Texture tex) {
        this.tex = tex;
        this.updateInternalValues();
    }

    public void setSlice9(double left, double right, double top, double down) {
        this.sliceLeft = left;
        this.sliceRight = right;
        this.sliceTop = top;
        this.sliceDown = down;
        this.updateInternalValues();
    }

    public void setAnimValues(double animDelay, double animFrameNum, double animFrameRows, double animFrameColumns) {
        this.animDelay = animDelay;
        this.animFrameNum = (int)animFrameNum;
        this.animFrameRows = (int)animFrameRows;
        this.animFrameColumns = (int)animFrameColumns;
        this.updateInternalValues();
    }

    Texture tryGetTexture(String key) {
        return UIManager.tableget(this.table, key) instanceof Texture texture ? texture : null;
    }

    static class Slice {
        public double leftSide;
        public double rightSide = 256.0;
        public double topSide;
        public double downSide = 256.0;
        public double uvLeft;
        public double uvRight = 1.0;
        public double uvTop;
        public double uvDown = 1.0;

        public Slice(double leftSide, double rightSide, double topSide, double downSide, double uvLeft, double uvRight, double uvTop, double uvDown) {
            this.leftSide = leftSide;
            this.rightSide = rightSide;
            this.topSide = topSide;
            this.downSide = downSide;
            this.uvLeft = uvLeft;
            this.uvRight = uvRight;
            this.uvTop = uvTop;
            this.uvDown = uvDown;
        }
    }
}
