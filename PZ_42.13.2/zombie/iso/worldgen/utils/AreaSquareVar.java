// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.iso.worldgen.utils;

public class AreaSquareVar {
    private int xmin = Integer.MAX_VALUE;
    private int xmax = -2147483647;
    private int ymin = Integer.MAX_VALUE;
    private int ymax = -2147483647;

    public int getXmin() {
        return this.xmin;
    }

    public void setXmin(int xmin) {
        this.xmin = xmin;
    }

    public int getXmax() {
        return this.xmax;
    }

    public void setXmax(int xmax) {
        this.xmax = xmax;
    }

    public int getYmin() {
        return this.ymin;
    }

    public void setYmin(int ymin) {
        this.ymin = ymin;
    }

    public int getYmax() {
        return this.ymax;
    }

    public void setYmax(int ymax) {
        this.ymax = ymax;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("CellSquare{");
        sb.append("xmin=").append(this.xmin);
        sb.append(", xmax=").append(this.xmax);
        sb.append(", ymin=").append(this.ymin);
        sb.append(", ymax=").append(this.ymax);
        sb.append('}');
        return sb.toString();
    }
}
