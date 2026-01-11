// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core.textures;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import zombie.core.opengl.RenderThread;
import zombie.core.utils.BooleanGrid;
import zombie.core.utils.WrappedBuffer;
import zombie.interfaces.ITexture;

public final class Mask implements Serializable, Cloneable {
    private static final long serialVersionUID = -5679205580926696806L;
    private boolean full;
    private int height;
    BooleanGrid mask;
    private int width;
    private boolean subMask;
    private int offsetX;
    private int offsetY;

    protected Mask() {
    }

    /**
     * Creates a new instance of Mask.
     *  The Mask will be maked fully
     * 
     * @param this width of mask
     * @param width height of mask
     */
    public Mask(int width, int height) {
        this.width = width;
        this.height = height;
        this.mask = new BooleanGrid(width, height);
        this.full();
    }

    /**
     * Creates a new instance of Mask from a texture
     * 
     * @param this
     * @param from the source texture
     * @param texture
     * @param x
     * @param y
     * @param width
     */
    public Mask(Texture from, Texture texture, int x, int y, int width, int height) {
        if (from.getMask() != null) {
            width = texture.getWidth();
            height = texture.getHeight();
            texture.setMask(this);
            this.mask = new BooleanGrid(width, height);

            for (int sx = x; sx < x + width; sx++) {
                for (int sy = y; sy < y + height; sy++) {
                    this.mask.setValue(sx - x, sy - y, from.getMask().mask.getValue(sx, sy));
                }
            }
        }
    }

    public Mask(Mask other, int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        this.mask = new BooleanGrid(width, height);

        for (int x1 = 0; x1 < width; x1++) {
            for (int y1 = 0; y1 < height; y1++) {
                this.mask.setValue(x1, y1, other.mask.getValue(x + x1, y + y1));
            }
        }
    }

    public Mask(boolean[] mask1, int maskW, int maskH, int x, int y, int width, int height) {
        this.width = width;
        this.height = height;
        this.mask = new BooleanGrid(width, height);

        for (int x1 = 0; x1 < width; x1++) {
            for (int y1 = 0; y1 < height; y1++) {
                this.mask.setValue(x1, y1, mask1[x + x1 + (y + y1) * maskW]);
            }
        }
    }

    public Mask(BooleanGrid mask1, int x, int y, int width, int height) {
        this.subMask = true;
        this.offsetX = x;
        this.offsetY = y;
        this.width = width;
        this.height = height;
        this.mask = mask1;
    }

    protected Mask(Texture texture, WrappedBuffer wb) {
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        int t = texture.getWidthHW();
        int th = texture.getHeightHW();
        int xs = (int)(texture.getXStart() * t);
        int xe = (int)(texture.getXEnd() * t);
        int ys = (int)(texture.getYStart() * th);
        int ye = (int)(texture.getYEnd() * th);
        this.mask = new BooleanGrid(this.width, this.height);
        texture.setMask(this);
        ByteBuffer temp = wb.getBuffer();
        temp.rewind();

        for (int y = 0; y < texture.getHeightHW(); y++) {
            for (int x = 0; x < texture.getWidthHW(); x++) {
                temp.get();
                temp.get();
                temp.get();
                int alpha = temp.get();
                if (x >= xs && x < xe && y >= ys && y < ye) {
                    if (alpha == 0) {
                        this.mask.setValue(x - xs, y - ys, false);
                        this.full = false;
                    } else {
                        if (alpha < 127) {
                            this.mask.setValue(x - xs, y - ys, true);
                        }

                        this.mask.setValue(x - xs, y - ys, true);
                    }
                }

                if (y >= ye) {
                    break;
                }
            }
        }

        wb.dispose();
    }

    public Mask(ITexture texture, boolean[] mask) {
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        int t = texture.getWidthHW();
        int xs = (int)(texture.getXStart() * t);
        int xe = (int)(texture.getXEnd() * t);
        int ys = (int)(texture.getYStart() * (t = texture.getHeightHW()));
        int ye = (int)(texture.getYEnd() * t);
        texture.setMask(this);
        this.mask = new BooleanGrid(this.width, this.height);

        for (int y = 0; y < texture.getHeight(); y++) {
            for (int x = 0; x < texture.getWidth(); x++) {
                this.mask.setValue(x, y, mask[y * texture.getWidth() + x]);
            }
        }
    }

    public Mask(ITexture texture, BooleanGrid mask) {
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        texture.setMask(this);
        this.mask = new BooleanGrid(this.width, this.height);

        for (int y = 0; y < texture.getHeight(); y++) {
            for (int x = 0; x < texture.getWidth(); x++) {
                this.mask.setValue(x, y, mask.getValue(x, y));
            }
        }
    }

    public Mask(ITexture texture) {
        this.width = texture.getWidth();
        this.height = texture.getHeight();
        int t = texture.getWidthHW();
        int xs = (int)(texture.getXStart() * t);
        int xe = (int)(texture.getXEnd() * t);
        int ys = (int)(texture.getYStart() * (t = texture.getHeightHW()));
        int ye = (int)(texture.getYEnd() * t);
        texture.setMask(this);
        this.mask = new BooleanGrid(this.width, this.height);
        RenderThread.invokeOnRenderContext(() -> {
            WrappedBuffer wb = texture.getData();
            ByteBuffer temp = wb.getBuffer();
            temp.rewind();

            for (int y = 0; y < texture.getHeightHW(); y++) {
                for (int x = 0; x < texture.getWidthHW(); x++) {
                    temp.get();
                    temp.get();
                    temp.get();
                    int alpha = temp.get();
                    if (x >= xs && x < xe && y >= ys && y < ye) {
                        if (alpha == 0) {
                            this.mask.setValue(x - xs, y - ys, false);
                            this.full = false;
                        } else {
                            if (alpha < 127) {
                                this.mask.setValue(x - xs, y - ys, true);
                            } else {
                                boolean var11 = false;
                            }

                            this.mask.setValue(x - xs, y - ys, true);
                        }
                    }

                    if (y >= ye) {
                        break;
                    }
                }
            }

            wb.dispose();
        });
    }

    public Mask(Mask obj) {
        this.subMask = obj.subMask;
        this.offsetX = obj.offsetX;
        this.offsetY = obj.offsetY;
        this.width = obj.width;
        this.height = obj.height;
        this.full = obj.full;
        if (this.subMask) {
            this.mask = obj.mask;
        } else {
            try {
                this.mask = obj.mask.clone();
            } catch (CloneNotSupportedException var3) {
                var3.printStackTrace(System.err);
            }
        }
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public boolean isSubMask() {
        return this.subMask;
    }

    public int getOffsetX() {
        return this.isSubMask() ? this.offsetX : 0;
    }

    public int getOffsetY() {
        return this.isSubMask() ? this.offsetY : 0;
    }

    @Override
    public Object clone() {
        return new Mask(this);
    }

    /**
     * creates a full-rectangular mask
     */
    public void full() {
        this.mask.fill();
        this.full = true;
    }

    /**
     * changes the x,y value of the mask
     * 
     * @param x coordinate
     * @param y coordinate
     * @param val new value
     */
    public void set(int x, int y, boolean val) {
        if (this.isSubMask()) {
            if (x >= 0 && x < this.width) {
                if (y >= 0 && y < this.height) {
                    this.mask.setValue(this.offsetX + x, this.offsetY + y, val);
                    if (!val && this.full) {
                        this.full = false;
                    }
                }
            }
        } else {
            this.mask.setValue(x, y, val);
            if (!val && this.full) {
                this.full = false;
            }
        }
    }

    public boolean get(int x, int y) {
        if (this.full) {
            return true;
        } else if (this.isSubMask()) {
            if (x < 0 || x >= this.width) {
                return false;
            } else {
                return y >= 0 && y < this.height ? this.mask.getValue(this.offsetX + x, this.offsetY + y) : false;
            }
        } else {
            return this.mask.getValue(x, y);
        }
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        this.width = s.readInt();
        this.height = s.readInt();
        this.full = s.readBoolean();
        if (s.readBoolean()) {
            this.mask = (BooleanGrid)s.readObject();
        }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeInt(this.width);
        s.writeInt(this.height);
        s.writeBoolean(this.full);
        if (this.mask != null) {
            s.writeBoolean(true);
            s.writeObject(this.mask);
        } else {
            s.writeBoolean(false);
        }
    }

    public void save(String name) {
    }
}
