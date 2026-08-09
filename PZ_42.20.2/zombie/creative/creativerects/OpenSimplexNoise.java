// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.creative.creativerects;

public class OpenSimplexNoise {
    private static final double STRETCH_CONSTANT_2D = -0.211324865405187;
    private static final double SQUISH_CONSTANT_2D = 0.366025403784439;
    private static final double STRETCH_CONSTANT_3D = -0.16666666666666666;
    private static final double SQUISH_CONSTANT_3D = 0.3333333333333333;
    private static final double STRETCH_CONSTANT_4D = -0.138196601125011;
    private static final double SQUISH_CONSTANT_4D = 0.309016994374947;
    private static final double NORM_CONSTANT_2D = 47.0;
    private static final double NORM_CONSTANT_3D = 103.0;
    private static final double NORM_CONSTANT_4D = 30.0;
    private static final long DEFAULT_SEED = 0L;
    private final short[] perm;
    private final short[] permGradIndex3d;
    private static final byte[] gradients2D = new byte[]{5, 2, 2, 5, -5, 2, -2, 5, 5, -2, 2, -5, -5, -2, -2, -5};
    private static final byte[] gradients3D = new byte[]{
        -11,
        4,
        4,
        -4,
        11,
        4,
        -4,
        4,
        11,
        11,
        4,
        4,
        4,
        11,
        4,
        4,
        4,
        11,
        -11,
        -4,
        4,
        -4,
        -11,
        4,
        -4,
        -4,
        11,
        11,
        -4,
        4,
        4,
        -11,
        4,
        4,
        -4,
        11,
        -11,
        4,
        -4,
        -4,
        11,
        -4,
        -4,
        4,
        -11,
        11,
        4,
        -4,
        4,
        11,
        -4,
        4,
        4,
        -11,
        -11,
        -4,
        -4,
        -4,
        -11,
        -4,
        -4,
        -4,
        -11,
        11,
        -4,
        -4,
        4,
        -11,
        -4,
        4,
        -4,
        -11
    };
    private static final byte[] gradients4D = new byte[]{
        3,
        1,
        1,
        1,
        1,
        3,
        1,
        1,
        1,
        1,
        3,
        1,
        1,
        1,
        1,
        3,
        -3,
        1,
        1,
        1,
        -1,
        3,
        1,
        1,
        -1,
        1,
        3,
        1,
        -1,
        1,
        1,
        3,
        3,
        -1,
        1,
        1,
        1,
        -3,
        1,
        1,
        1,
        -1,
        3,
        1,
        1,
        -1,
        1,
        3,
        -3,
        -1,
        1,
        1,
        -1,
        -3,
        1,
        1,
        -1,
        -1,
        3,
        1,
        -1,
        -1,
        1,
        3,
        3,
        1,
        -1,
        1,
        1,
        3,
        -1,
        1,
        1,
        1,
        -3,
        1,
        1,
        1,
        -1,
        3,
        -3,
        1,
        -1,
        1,
        -1,
        3,
        -1,
        1,
        -1,
        1,
        -3,
        1,
        -1,
        1,
        -1,
        3,
        3,
        -1,
        -1,
        1,
        1,
        -3,
        -1,
        1,
        1,
        -1,
        -3,
        1,
        1,
        -1,
        -1,
        3,
        -3,
        -1,
        -1,
        1,
        -1,
        -3,
        -1,
        1,
        -1,
        -1,
        -3,
        1,
        -1,
        -1,
        -1,
        3,
        3,
        1,
        1,
        -1,
        1,
        3,
        1,
        -1,
        1,
        1,
        3,
        -1,
        1,
        1,
        1,
        -3,
        -3,
        1,
        1,
        -1,
        -1,
        3,
        1,
        -1,
        -1,
        1,
        3,
        -1,
        -1,
        1,
        1,
        -3,
        3,
        -1,
        1,
        -1,
        1,
        -3,
        1,
        -1,
        1,
        -1,
        3,
        -1,
        1,
        -1,
        1,
        -3,
        -3,
        -1,
        1,
        -1,
        -1,
        -3,
        1,
        -1,
        -1,
        -1,
        3,
        -1,
        -1,
        -1,
        1,
        -3,
        3,
        1,
        -1,
        -1,
        1,
        3,
        -1,
        -1,
        1,
        1,
        -3,
        -1,
        1,
        1,
        -1,
        -3,
        -3,
        1,
        -1,
        -1,
        -1,
        3,
        -1,
        -1,
        -1,
        1,
        -3,
        -1,
        -1,
        1,
        -1,
        -3,
        3,
        -1,
        -1,
        -1,
        1,
        -3,
        -1,
        -1,
        1,
        -1,
        -3,
        -1,
        1,
        -1,
        -1,
        -3,
        -3,
        -1,
        -1,
        -1,
        -1,
        -3,
        -1,
        -1,
        -1,
        -1,
        -3,
        -1,
        -1,
        -1,
        -1,
        -3
    };

    public OpenSimplexNoise() {
        this(0L);
    }

    public OpenSimplexNoise(short[] perm) {
        this.perm = perm;
        this.permGradIndex3d = new short[256];

        for (int i = 0; i < 256; i++) {
            this.permGradIndex3d[i] = (short)(perm[i] % (gradients3D.length / 3) * 3);
        }
    }

    public OpenSimplexNoise(long seed) {
        this.perm = new short[256];
        this.permGradIndex3d = new short[256];
        short[] source = new short[256];
        short i = 0;

        while (i < 256) {
            source[i] = i++;
        }

        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;

        for (int ix = 255; ix >= 0; ix--) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int r = (int)((seed + 31L) % (ix + 1));
            if (r < 0) {
                r += ix + 1;
            }

            this.perm[ix] = source[r];
            this.permGradIndex3d[ix] = (short)(this.perm[ix] % (gradients3D.length / 3) * 3);
            source[r] = source[ix];
        }
    }

    public double eval(double x, double y) {
        double stretchOffset = (x + y) * -0.211324865405187;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        double squishOffset = (xsb + ysb) * 0.366025403784439;
        double xb = xsb + squishOffset;
        double yb = ysb + squishOffset;
        double xins = xs - xsb;
        double yins = ys - ysb;
        double inSum = xins + yins;
        double dx0 = x - xb;
        double dy0 = y - yb;
        double value = 0.0;
        double dx1 = dx0 - 1.0 - 0.366025403784439;
        double dy1 = dy0 - 0.0 - 0.366025403784439;
        double attn1 = 2.0 - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0.0) {
            attn1 *= attn1;
            value += attn1 * attn1 * this.extrapolate(xsb + 1, ysb + 0, dx1, dy1);
        }

        double dx2 = dx0 - 0.0 - 0.366025403784439;
        double dy2 = dy0 - 1.0 - 0.366025403784439;
        double attn2 = 2.0 - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0.0) {
            attn2 *= attn2;
            value += attn2 * attn2 * this.extrapolate(xsb + 0, ysb + 1, dx2, dy2);
        }

        double dxExt;
        double dyExt;
        int xsvExt;
        int ysvExt;
        if (inSum <= 1.0) {
            double zins = 1.0 - inSum;
            if (!(zins > xins) && !(zins > yins)) {
                xsvExt = xsb + 1;
                ysvExt = ysb + 1;
                dxExt = dx0 - 1.0 - 0.732050807568878;
                dyExt = dy0 - 1.0 - 0.732050807568878;
            } else if (xins > yins) {
                xsvExt = xsb + 1;
                ysvExt = ysb - 1;
                dxExt = dx0 - 1.0;
                dyExt = dy0 + 1.0;
            } else {
                xsvExt = xsb - 1;
                ysvExt = ysb + 1;
                dxExt = dx0 + 1.0;
                dyExt = dy0 - 1.0;
            }
        } else {
            double zins = 2.0 - inSum;
            if (!(zins < xins) && !(zins < yins)) {
                dxExt = dx0;
                dyExt = dy0;
                xsvExt = xsb;
                ysvExt = ysb;
            } else if (xins > yins) {
                xsvExt = xsb + 2;
                ysvExt = ysb + 0;
                dxExt = dx0 - 2.0 - 0.732050807568878;
                dyExt = dy0 + 0.0 - 0.732050807568878;
            } else {
                xsvExt = xsb + 0;
                ysvExt = ysb + 2;
                dxExt = dx0 + 0.0 - 0.732050807568878;
                dyExt = dy0 - 2.0 - 0.732050807568878;
            }

            xsb++;
            ysb++;
            dx0 = dx0 - 1.0 - 0.732050807568878;
            dy0 = dy0 - 1.0 - 0.732050807568878;
        }

        double attn0 = 2.0 - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0.0) {
            attn0 *= attn0;
            value += attn0 * attn0 * this.extrapolate(xsb, ysb, dx0, dy0);
        }

        double attnExt = 2.0 - dxExt * dxExt - dyExt * dyExt;
        if (attnExt > 0.0) {
            attnExt *= attnExt;
            value += attnExt * attnExt * this.extrapolate(xsvExt, ysvExt, dxExt, dyExt);
        }

        return value / 47.0;
    }

    public double eval(double x, double y, double z) {
        double stretchOffset = (x + y + z) * -0.16666666666666666;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        double zs = z + stretchOffset;
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        int zsb = fastFloor(zs);
        double squishOffset = (xsb + ysb + zsb) * 0.3333333333333333;
        double xb = xsb + squishOffset;
        double yb = ysb + squishOffset;
        double zb = zsb + squishOffset;
        double xins = xs - xsb;
        double yins = ys - ysb;
        double zins = zs - zsb;
        double inSum = xins + yins + zins;
        double dx0 = x - xb;
        double dy0 = y - yb;
        double dz0 = z - zb;
        double value = 0.0;
        double dxExt0;
        double dyExt0;
        double dzExt0;
        double dxExt1;
        double dyExt1;
        double dzExt1;
        int xsvExt0;
        int ysvExt0;
        int zsvExt0;
        int xsvExt1;
        int ysvExt1;
        int zsvExt1;
        if (inSum <= 1.0) {
            byte aPoint = 1;
            double aScore = xins;
            byte bPoint = 2;
            double bScore = yins;
            if (xins >= yins && zins > yins) {
                bScore = zins;
                bPoint = 4;
            } else if (xins < yins && zins > xins) {
                aScore = zins;
                aPoint = 4;
            }

            double wins = 1.0 - inSum;
            if (!(wins > aScore) && !(wins > bScore)) {
                byte c = (byte)(aPoint | bPoint);
                if ((c & 1) == 0) {
                    xsvExt0 = xsb;
                    xsvExt1 = xsb - 1;
                    dxExt0 = dx0 - 0.6666666666666666;
                    dxExt1 = dx0 + 1.0 - 0.3333333333333333;
                } else {
                    xsvExt0 = xsvExt1 = xsb + 1;
                    dxExt0 = dx0 - 1.0 - 0.6666666666666666;
                    dxExt1 = dx0 - 1.0 - 0.3333333333333333;
                }

                if ((c & 2) == 0) {
                    ysvExt0 = ysb;
                    ysvExt1 = ysb - 1;
                    dyExt0 = dy0 - 0.6666666666666666;
                    dyExt1 = dy0 + 1.0 - 0.3333333333333333;
                } else {
                    ysvExt0 = ysvExt1 = ysb + 1;
                    dyExt0 = dy0 - 1.0 - 0.6666666666666666;
                    dyExt1 = dy0 - 1.0 - 0.3333333333333333;
                }

                if ((c & 4) == 0) {
                    zsvExt0 = zsb;
                    zsvExt1 = zsb - 1;
                    dzExt0 = dz0 - 0.6666666666666666;
                    dzExt1 = dz0 + 1.0 - 0.3333333333333333;
                } else {
                    zsvExt0 = zsvExt1 = zsb + 1;
                    dzExt0 = dz0 - 1.0 - 0.6666666666666666;
                    dzExt1 = dz0 - 1.0 - 0.3333333333333333;
                }
            } else {
                byte cx = bScore > aScore ? bPoint : aPoint;
                if ((cx & 1) == 0) {
                    xsvExt0 = xsb - 1;
                    xsvExt1 = xsb;
                    dxExt0 = dx0 + 1.0;
                    dxExt1 = dx0;
                } else {
                    xsvExt0 = xsvExt1 = xsb + 1;
                    dxExt0 = dxExt1 = dx0 - 1.0;
                }

                if ((cx & 2) == 0) {
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt1 = dy0;
                    dyExt0 = dy0;
                    if ((cx & 1) == 0) {
                        ysvExt1 = ysb - 1;
                        dyExt1 = dy0 + 1.0;
                    } else {
                        ysvExt0 = ysb - 1;
                        dyExt0 = dy0 + 1.0;
                    }
                } else {
                    ysvExt0 = ysvExt1 = ysb + 1;
                    dyExt0 = dyExt1 = dy0 - 1.0;
                }

                if ((cx & 4) == 0) {
                    zsvExt0 = zsb;
                    zsvExt1 = zsb - 1;
                    dzExt0 = dz0;
                    dzExt1 = dz0 + 1.0;
                } else {
                    zsvExt0 = zsvExt1 = zsb + 1;
                    dzExt0 = dzExt1 = dz0 - 1.0;
                }
            }

            double attn0 = 2.0 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
            if (attn0 > 0.0) {
                attn0 *= attn0;
                value += attn0 * attn0 * this.extrapolate(xsb + 0, ysb + 0, zsb + 0, dx0, dy0, dz0);
            }

            double dx1 = dx0 - 1.0 - 0.3333333333333333;
            double dy1 = dy0 - 0.0 - 0.3333333333333333;
            double dz1 = dz0 - 0.0 - 0.3333333333333333;
            double attn1 = 2.0 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1;
            if (attn1 > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, dx1, dy1, dz1);
            }

            double dx2 = dx0 - 0.0 - 0.3333333333333333;
            double dy2 = dy0 - 1.0 - 0.3333333333333333;
            double attn2 = 2.0 - dx2 * dx2 - dy2 * dy2 - dz1 * dz1;
            if (attn2 > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, dx2, dy2, dz1);
            }

            double dz3 = dz0 - 1.0 - 0.3333333333333333;
            double attn3 = 2.0 - dx2 * dx2 - dy1 * dy1 - dz3 * dz3;
            if (attn3 > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, dx2, dy1, dz3);
            }
        } else if (inSum >= 2.0) {
            byte aPointx = 6;
            double aScorex = xins;
            byte bPointx = 5;
            double bScorex = yins;
            if (xins <= yins && zins < yins) {
                bScorex = zins;
                bPointx = 3;
            } else if (xins > yins && zins < xins) {
                aScorex = zins;
                aPointx = 3;
            }

            double winsx = 3.0 - inSum;
            if (!(winsx < aScorex) && !(winsx < bScorex)) {
                byte cxx = (byte)(aPointx & bPointx);
                if ((cxx & 1) != 0) {
                    xsvExt0 = xsb + 1;
                    xsvExt1 = xsb + 2;
                    dxExt0 = dx0 - 1.0 - 0.3333333333333333;
                    dxExt1 = dx0 - 2.0 - 0.6666666666666666;
                } else {
                    xsvExt1 = xsb;
                    xsvExt0 = xsb;
                    dxExt0 = dx0 - 0.3333333333333333;
                    dxExt1 = dx0 - 0.6666666666666666;
                }

                if ((cxx & 2) != 0) {
                    ysvExt0 = ysb + 1;
                    ysvExt1 = ysb + 2;
                    dyExt0 = dy0 - 1.0 - 0.3333333333333333;
                    dyExt1 = dy0 - 2.0 - 0.6666666666666666;
                } else {
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt0 = dy0 - 0.3333333333333333;
                    dyExt1 = dy0 - 0.6666666666666666;
                }

                if ((cxx & 4) != 0) {
                    zsvExt0 = zsb + 1;
                    zsvExt1 = zsb + 2;
                    dzExt0 = dz0 - 1.0 - 0.3333333333333333;
                    dzExt1 = dz0 - 2.0 - 0.6666666666666666;
                } else {
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    dzExt0 = dz0 - 0.3333333333333333;
                    dzExt1 = dz0 - 0.6666666666666666;
                }
            } else {
                byte cxxx = bScorex < aScorex ? bPointx : aPointx;
                if ((cxxx & 1) != 0) {
                    xsvExt0 = xsb + 2;
                    xsvExt1 = xsb + 1;
                    dxExt0 = dx0 - 2.0 - 1.0;
                    dxExt1 = dx0 - 1.0 - 1.0;
                } else {
                    xsvExt1 = xsb;
                    xsvExt0 = xsb;
                    dxExt0 = dxExt1 = dx0 - 1.0;
                }

                if ((cxxx & 2) != 0) {
                    ysvExt0 = ysvExt1 = ysb + 1;
                    dyExt0 = dyExt1 = dy0 - 1.0 - 1.0;
                    if ((cxxx & 1) != 0) {
                        ysvExt1++;
                        dyExt1--;
                    } else {
                        ysvExt0++;
                        dyExt0--;
                    }
                } else {
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt0 = dyExt1 = dy0 - 1.0;
                }

                if ((cxxx & 4) != 0) {
                    zsvExt0 = zsb + 1;
                    zsvExt1 = zsb + 2;
                    dzExt0 = dz0 - 1.0 - 1.0;
                    dzExt1 = dz0 - 2.0 - 1.0;
                } else {
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    dzExt0 = dzExt1 = dz0 - 1.0;
                }
            }

            double dx3 = dx0 - 1.0 - 0.6666666666666666;
            double dy3 = dy0 - 1.0 - 0.6666666666666666;
            double dz3 = dz0 - 0.0 - 0.6666666666666666;
            double attn3 = 2.0 - dx3 * dx3 - dy3 * dy3 - dz3 * dz3;
            if (attn3 > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, dx3, dy3, dz3);
            }

            double dy2x = dy0 - 0.0 - 0.6666666666666666;
            double dz2 = dz0 - 1.0 - 0.6666666666666666;
            double attn2x = 2.0 - dx3 * dx3 - dy2x * dy2x - dz2 * dz2;
            if (attn2x > 0.0) {
                attn2x *= attn2x;
                value += attn2x * attn2x * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, dx3, dy2x, dz2);
            }

            double dx1x = dx0 - 0.0 - 0.6666666666666666;
            double attn1x = 2.0 - dx1x * dx1x - dy3 * dy3 - dz2 * dz2;
            if (attn1x > 0.0) {
                attn1x *= attn1x;
                value += attn1x * attn1x * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, dx1x, dy3, dz2);
            }

            dx0 = dx0 - 1.0 - 1.0;
            dy0 = dy0 - 1.0 - 1.0;
            dz0 = dz0 - 1.0 - 1.0;
            double attn0x = 2.0 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0;
            if (attn0x > 0.0) {
                attn0x *= attn0x;
                value += attn0x * attn0x * this.extrapolate(xsb + 1, ysb + 1, zsb + 1, dx0, dy0, dz0);
            }
        } else {
            double p1 = xins + yins;
            boolean aIsFurtherSide = p1 > 1.0;
            byte aPointxx;
            double aScorexx;
            if (aIsFurtherSide) {
                aScorexx = p1 - 1.0;
                aPointxx = 3;
            } else {
                aScorexx = 1.0 - p1;
                aPointxx = 4;
            }

            double p2 = xins + zins;
            byte bPointxx;
            double bScorexx;
            boolean bIsFurtherSide;
            if (p2 > 1.0) {
                bScorexx = p2 - 1.0;
                bPointxx = 5;
                bIsFurtherSide = true;
            } else {
                bScorexx = 1.0 - p2;
                bPointxx = 2;
                bIsFurtherSide = false;
            }

            double p3 = yins + zins;
            if (p3 > 1.0) {
                double score = p3 - 1.0;
                if (aScorexx <= bScorexx && aScorexx < score) {
                    aPointxx = 6;
                    aIsFurtherSide = true;
                } else if (aScorexx > bScorexx && bScorexx < score) {
                    bPointxx = 6;
                    bIsFurtherSide = true;
                }
            } else {
                double score = 1.0 - p3;
                if (aScorexx <= bScorexx && aScorexx < score) {
                    aPointxx = 1;
                    aIsFurtherSide = false;
                } else if (aScorexx > bScorexx && bScorexx < score) {
                    bPointxx = 1;
                    bIsFurtherSide = false;
                }
            }

            if (aIsFurtherSide == bIsFurtherSide) {
                if (aIsFurtherSide) {
                    dxExt0 = dx0 - 1.0 - 1.0;
                    dyExt0 = dy0 - 1.0 - 1.0;
                    dzExt0 = dz0 - 1.0 - 1.0;
                    xsvExt0 = xsb + 1;
                    ysvExt0 = ysb + 1;
                    zsvExt0 = zsb + 1;
                    byte cxxxx = (byte)(aPointxx & bPointxx);
                    if ((cxxxx & 1) != 0) {
                        dxExt1 = dx0 - 2.0 - 0.6666666666666666;
                        dyExt1 = dy0 - 0.6666666666666666;
                        dzExt1 = dz0 - 0.6666666666666666;
                        xsvExt1 = xsb + 2;
                        ysvExt1 = ysb;
                        zsvExt1 = zsb;
                    } else if ((cxxxx & 2) != 0) {
                        dxExt1 = dx0 - 0.6666666666666666;
                        dyExt1 = dy0 - 2.0 - 0.6666666666666666;
                        dzExt1 = dz0 - 0.6666666666666666;
                        xsvExt1 = xsb;
                        ysvExt1 = ysb + 2;
                        zsvExt1 = zsb;
                    } else {
                        dxExt1 = dx0 - 0.6666666666666666;
                        dyExt1 = dy0 - 0.6666666666666666;
                        dzExt1 = dz0 - 2.0 - 0.6666666666666666;
                        xsvExt1 = xsb;
                        ysvExt1 = ysb;
                        zsvExt1 = zsb + 2;
                    }
                } else {
                    dxExt0 = dx0;
                    dyExt0 = dy0;
                    dzExt0 = dz0;
                    xsvExt0 = xsb;
                    ysvExt0 = ysb;
                    zsvExt0 = zsb;
                    byte cxxxx = (byte)(aPointxx | bPointxx);
                    if ((cxxxx & 1) == 0) {
                        dxExt1 = dx0 + 1.0 - 0.3333333333333333;
                        dyExt1 = dy0 - 1.0 - 0.3333333333333333;
                        dzExt1 = dz0 - 1.0 - 0.3333333333333333;
                        xsvExt1 = xsb - 1;
                        ysvExt1 = ysb + 1;
                        zsvExt1 = zsb + 1;
                    } else if ((cxxxx & 2) == 0) {
                        dxExt1 = dx0 - 1.0 - 0.3333333333333333;
                        dyExt1 = dy0 + 1.0 - 0.3333333333333333;
                        dzExt1 = dz0 - 1.0 - 0.3333333333333333;
                        xsvExt1 = xsb + 1;
                        ysvExt1 = ysb - 1;
                        zsvExt1 = zsb + 1;
                    } else {
                        dxExt1 = dx0 - 1.0 - 0.3333333333333333;
                        dyExt1 = dy0 - 1.0 - 0.3333333333333333;
                        dzExt1 = dz0 + 1.0 - 0.3333333333333333;
                        xsvExt1 = xsb + 1;
                        ysvExt1 = ysb + 1;
                        zsvExt1 = zsb - 1;
                    }
                }
            } else {
                byte c2;
                byte c1;
                if (aIsFurtherSide) {
                    c1 = aPointxx;
                    c2 = bPointxx;
                } else {
                    c1 = bPointxx;
                    c2 = aPointxx;
                }

                if ((c1 & 1) == 0) {
                    dxExt0 = dx0 + 1.0 - 0.3333333333333333;
                    dyExt0 = dy0 - 1.0 - 0.3333333333333333;
                    dzExt0 = dz0 - 1.0 - 0.3333333333333333;
                    xsvExt0 = xsb - 1;
                    ysvExt0 = ysb + 1;
                    zsvExt0 = zsb + 1;
                } else if ((c1 & 2) == 0) {
                    dxExt0 = dx0 - 1.0 - 0.3333333333333333;
                    dyExt0 = dy0 + 1.0 - 0.3333333333333333;
                    dzExt0 = dz0 - 1.0 - 0.3333333333333333;
                    xsvExt0 = xsb + 1;
                    ysvExt0 = ysb - 1;
                    zsvExt0 = zsb + 1;
                } else {
                    dxExt0 = dx0 - 1.0 - 0.3333333333333333;
                    dyExt0 = dy0 - 1.0 - 0.3333333333333333;
                    dzExt0 = dz0 + 1.0 - 0.3333333333333333;
                    xsvExt0 = xsb + 1;
                    ysvExt0 = ysb + 1;
                    zsvExt0 = zsb - 1;
                }

                dxExt1 = dx0 - 0.6666666666666666;
                dyExt1 = dy0 - 0.6666666666666666;
                dzExt1 = dz0 - 0.6666666666666666;
                xsvExt1 = xsb;
                ysvExt1 = ysb;
                zsvExt1 = zsb;
                if ((c2 & 1) != 0) {
                    dxExt1 -= 2.0;
                    xsvExt1 = xsb + 2;
                } else if ((c2 & 2) != 0) {
                    dyExt1 -= 2.0;
                    ysvExt1 = ysb + 2;
                } else {
                    dzExt1 -= 2.0;
                    zsvExt1 = zsb + 2;
                }
            }

            double dx1xx = dx0 - 1.0 - 0.3333333333333333;
            double dy1x = dy0 - 0.0 - 0.3333333333333333;
            double dz1x = dz0 - 0.0 - 0.3333333333333333;
            double attn1xx = 2.0 - dx1xx * dx1xx - dy1x * dy1x - dz1x * dz1x;
            if (attn1xx > 0.0) {
                attn1xx *= attn1xx;
                value += attn1xx * attn1xx * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, dx1xx, dy1x, dz1x);
            }

            double dx2x = dx0 - 0.0 - 0.3333333333333333;
            double dy2xx = dy0 - 1.0 - 0.3333333333333333;
            double attn2xx = 2.0 - dx2x * dx2x - dy2xx * dy2xx - dz1x * dz1x;
            if (attn2xx > 0.0) {
                attn2xx *= attn2xx;
                value += attn2xx * attn2xx * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, dx2x, dy2xx, dz1x);
            }

            double dz3x = dz0 - 1.0 - 0.3333333333333333;
            double attn3x = 2.0 - dx2x * dx2x - dy1x * dy1x - dz3x * dz3x;
            if (attn3x > 0.0) {
                attn3x *= attn3x;
                value += attn3x * attn3x * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, dx2x, dy1x, dz3x);
            }

            double dx4 = dx0 - 1.0 - 0.6666666666666666;
            double dy4 = dy0 - 1.0 - 0.6666666666666666;
            double dz4 = dz0 - 0.0 - 0.6666666666666666;
            double attn4 = 2.0 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4;
            if (attn4 > 0.0) {
                attn4 *= attn4;
                value += attn4 * attn4 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, dx4, dy4, dz4);
            }

            double dy5 = dy0 - 0.0 - 0.6666666666666666;
            double dz5 = dz0 - 1.0 - 0.6666666666666666;
            double attn5 = 2.0 - dx4 * dx4 - dy5 * dy5 - dz5 * dz5;
            if (attn5 > 0.0) {
                attn5 *= attn5;
                value += attn5 * attn5 * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, dx4, dy5, dz5);
            }

            double dx6 = dx0 - 0.0 - 0.6666666666666666;
            double attn6 = 2.0 - dx6 * dx6 - dy4 * dy4 - dz5 * dz5;
            if (attn6 > 0.0) {
                attn6 *= attn6;
                value += attn6 * attn6 * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, dx6, dy4, dz5);
            }
        }

        double attnExt0 = 2.0 - dxExt0 * dxExt0 - dyExt0 * dyExt0 - dzExt0 * dzExt0;
        if (attnExt0 > 0.0) {
            attnExt0 *= attnExt0;
            value += attnExt0 * attnExt0 * this.extrapolate(xsvExt0, ysvExt0, zsvExt0, dxExt0, dyExt0, dzExt0);
        }

        double attnExt1 = 2.0 - dxExt1 * dxExt1 - dyExt1 * dyExt1 - dzExt1 * dzExt1;
        if (attnExt1 > 0.0) {
            attnExt1 *= attnExt1;
            value += attnExt1 * attnExt1 * this.extrapolate(xsvExt1, ysvExt1, zsvExt1, dxExt1, dyExt1, dzExt1);
        }

        return value / 103.0;
    }

    public double eval(double x, double y, double z, double w) {
        double stretchOffset = (x + y + z + w) * -0.138196601125011;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        double zs = z + stretchOffset;
        double ws = w + stretchOffset;
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        int zsb = fastFloor(zs);
        int wsb = fastFloor(ws);
        double squishOffset = (xsb + ysb + zsb + wsb) * 0.309016994374947;
        double xb = xsb + squishOffset;
        double yb = ysb + squishOffset;
        double zb = zsb + squishOffset;
        double wb = wsb + squishOffset;
        double xins = xs - xsb;
        double yins = ys - ysb;
        double zins = zs - zsb;
        double wins = ws - wsb;
        double inSum = xins + yins + zins + wins;
        double dx0 = x - xb;
        double dy0 = y - yb;
        double dz0 = z - zb;
        double dw0 = w - wb;
        double value = 0.0;
        double dxExt0;
        double dyExt0;
        double dzExt0;
        double dwExt0;
        double dxExt1;
        double dyExt1;
        double dzExt1;
        double dwExt1;
        double dxExt2;
        double dyExt2;
        double dzExt2;
        double dwExt2;
        int xsvExt0;
        int ysvExt0;
        int zsvExt0;
        int wsvExt0;
        int xsvExt1;
        int ysvExt1;
        int zsvExt1;
        int wsvExt1;
        int xsvExt2;
        int ysvExt2;
        int zsvExt2;
        int wsvExt2;
        if (inSum <= 1.0) {
            byte aPoint = 1;
            double aScore = xins;
            byte bPoint = 2;
            double bScore = yins;
            if (xins >= yins && zins > yins) {
                bScore = zins;
                bPoint = 4;
            } else if (xins < yins && zins > xins) {
                aScore = zins;
                aPoint = 4;
            }

            if (aScore >= bScore && wins > bScore) {
                bScore = wins;
                bPoint = 8;
            } else if (aScore < bScore && wins > aScore) {
                aScore = wins;
                aPoint = 8;
            }

            double uins = 1.0 - inSum;
            if (!(uins > aScore) && !(uins > bScore)) {
                byte c = (byte)(aPoint | bPoint);
                if ((c & 1) == 0) {
                    xsvExt2 = xsb;
                    xsvExt0 = xsb;
                    xsvExt1 = xsb - 1;
                    dxExt0 = dx0 - 0.618033988749894;
                    dxExt1 = dx0 + 1.0 - 0.309016994374947;
                    dxExt2 = dx0 - 0.309016994374947;
                } else {
                    xsvExt0 = xsvExt1 = xsvExt2 = xsb + 1;
                    dxExt0 = dx0 - 1.0 - 0.618033988749894;
                    dxExt1 = dxExt2 = dx0 - 1.0 - 0.309016994374947;
                }

                if ((c & 2) == 0) {
                    ysvExt2 = ysb;
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt0 = dy0 - 0.618033988749894;
                    dyExt1 = dyExt2 = dy0 - 0.309016994374947;
                    if ((c & 1) == 1) {
                        ysvExt1 = ysb - 1;
                        dyExt1++;
                    } else {
                        ysvExt2 = ysb - 1;
                        dyExt2++;
                    }
                } else {
                    ysvExt0 = ysvExt1 = ysvExt2 = ysb + 1;
                    dyExt0 = dy0 - 1.0 - 0.618033988749894;
                    dyExt1 = dyExt2 = dy0 - 1.0 - 0.309016994374947;
                }

                if ((c & 4) == 0) {
                    zsvExt2 = zsb;
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    dzExt0 = dz0 - 0.618033988749894;
                    dzExt1 = dzExt2 = dz0 - 0.309016994374947;
                    if ((c & 3) == 3) {
                        zsvExt1 = zsb - 1;
                        dzExt1++;
                    } else {
                        zsvExt2 = zsb - 1;
                        dzExt2++;
                    }
                } else {
                    zsvExt0 = zsvExt1 = zsvExt2 = zsb + 1;
                    dzExt0 = dz0 - 1.0 - 0.618033988749894;
                    dzExt1 = dzExt2 = dz0 - 1.0 - 0.309016994374947;
                }

                if ((c & 8) == 0) {
                    wsvExt1 = wsb;
                    wsvExt0 = wsb;
                    wsvExt2 = wsb - 1;
                    dwExt0 = dw0 - 0.618033988749894;
                    dwExt1 = dw0 - 0.309016994374947;
                    dwExt2 = dw0 + 1.0 - 0.309016994374947;
                } else {
                    wsvExt0 = wsvExt1 = wsvExt2 = wsb + 1;
                    dwExt0 = dw0 - 1.0 - 0.618033988749894;
                    dwExt1 = dwExt2 = dw0 - 1.0 - 0.309016994374947;
                }
            } else {
                byte cx = bScore > aScore ? bPoint : aPoint;
                if ((cx & 1) == 0) {
                    xsvExt0 = xsb - 1;
                    xsvExt2 = xsb;
                    xsvExt1 = xsb;
                    dxExt0 = dx0 + 1.0;
                    dxExt2 = dx0;
                    dxExt1 = dx0;
                } else {
                    xsvExt0 = xsvExt1 = xsvExt2 = xsb + 1;
                    dxExt0 = dxExt1 = dxExt2 = dx0 - 1.0;
                }

                if ((cx & 2) == 0) {
                    ysvExt2 = ysb;
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt2 = dy0;
                    dyExt1 = dy0;
                    dyExt0 = dy0;
                    if ((cx & 1) == 1) {
                        ysvExt0 = ysb - 1;
                        dyExt0 = dy0 + 1.0;
                    } else {
                        ysvExt1 = ysb - 1;
                        dyExt1 = dy0 + 1.0;
                    }
                } else {
                    ysvExt0 = ysvExt1 = ysvExt2 = ysb + 1;
                    dyExt0 = dyExt1 = dyExt2 = dy0 - 1.0;
                }

                if ((cx & 4) == 0) {
                    zsvExt2 = zsb;
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    dzExt2 = dz0;
                    dzExt1 = dz0;
                    dzExt0 = dz0;
                    if ((cx & 3) != 0) {
                        if ((cx & 3) == 3) {
                            zsvExt0 = zsb - 1;
                            dzExt0 = dz0 + 1.0;
                        } else {
                            zsvExt1 = zsb - 1;
                            dzExt1 = dz0 + 1.0;
                        }
                    } else {
                        zsvExt2 = zsb - 1;
                        dzExt2 = dz0 + 1.0;
                    }
                } else {
                    zsvExt0 = zsvExt1 = zsvExt2 = zsb + 1;
                    dzExt0 = dzExt1 = dzExt2 = dz0 - 1.0;
                }

                if ((cx & 8) == 0) {
                    wsvExt1 = wsb;
                    wsvExt0 = wsb;
                    wsvExt2 = wsb - 1;
                    dwExt1 = dw0;
                    dwExt0 = dw0;
                    dwExt2 = dw0 + 1.0;
                } else {
                    wsvExt0 = wsvExt1 = wsvExt2 = wsb + 1;
                    dwExt0 = dwExt1 = dwExt2 = dw0 - 1.0;
                }
            }

            double attn0 = 2.0 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0 - dw0 * dw0;
            if (attn0 > 0.0) {
                attn0 *= attn0;
                value += attn0 * attn0 * this.extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 0, dx0, dy0, dz0, dw0);
            }

            double dx1 = dx0 - 1.0 - 0.309016994374947;
            double dy1 = dy0 - 0.0 - 0.309016994374947;
            double dz1 = dz0 - 0.0 - 0.309016994374947;
            double dw1 = dw0 - 0.0 - 0.309016994374947;
            double attn1 = 2.0 - dx1 * dx1 - dy1 * dy1 - dz1 * dz1 - dw1 * dw1;
            if (attn1 > 0.0) {
                attn1 *= attn1;
                value += attn1 * attn1 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 0, dx1, dy1, dz1, dw1);
            }

            double dx2 = dx0 - 0.0 - 0.309016994374947;
            double dy2 = dy0 - 1.0 - 0.309016994374947;
            double attn2 = 2.0 - dx2 * dx2 - dy2 * dy2 - dz1 * dz1 - dw1 * dw1;
            if (attn2 > 0.0) {
                attn2 *= attn2;
                value += attn2 * attn2 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 0, dx2, dy2, dz1, dw1);
            }

            double dz3 = dz0 - 1.0 - 0.309016994374947;
            double attn3 = 2.0 - dx2 * dx2 - dy1 * dy1 - dz3 * dz3 - dw1 * dw1;
            if (attn3 > 0.0) {
                attn3 *= attn3;
                value += attn3 * attn3 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 0, dx2, dy1, dz3, dw1);
            }

            double dw4 = dw0 - 1.0 - 0.309016994374947;
            double attn4 = 2.0 - dx2 * dx2 - dy1 * dy1 - dz1 * dz1 - dw4 * dw4;
            if (attn4 > 0.0) {
                attn4 *= attn4;
                value += attn4 * attn4 * this.extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 1, dx2, dy1, dz1, dw4);
            }
        } else if (inSum >= 3.0) {
            byte aPointx = 14;
            double aScorex = xins;
            byte bPointx = 13;
            double bScorex = yins;
            if (xins <= yins && zins < yins) {
                bScorex = zins;
                bPointx = 11;
            } else if (xins > yins && zins < xins) {
                aScorex = zins;
                aPointx = 11;
            }

            if (aScorex <= bScorex && wins < bScorex) {
                bScorex = wins;
                bPointx = 7;
            } else if (aScorex > bScorex && wins < aScorex) {
                aScorex = wins;
                aPointx = 7;
            }

            double uinsx = 4.0 - inSum;
            if (!(uinsx < aScorex) && !(uinsx < bScorex)) {
                byte cxx = (byte)(aPointx & bPointx);
                if ((cxx & 1) != 0) {
                    xsvExt0 = xsvExt2 = xsb + 1;
                    xsvExt1 = xsb + 2;
                    dxExt0 = dx0 - 1.0 - 0.618033988749894;
                    dxExt1 = dx0 - 2.0 - 0.927050983124841;
                    dxExt2 = dx0 - 1.0 - 0.927050983124841;
                } else {
                    xsvExt2 = xsb;
                    xsvExt1 = xsb;
                    xsvExt0 = xsb;
                    dxExt0 = dx0 - 0.618033988749894;
                    dxExt1 = dxExt2 = dx0 - 0.927050983124841;
                }

                if ((cxx & 2) != 0) {
                    ysvExt0 = ysvExt1 = ysvExt2 = ysb + 1;
                    dyExt0 = dy0 - 1.0 - 0.618033988749894;
                    dyExt1 = dyExt2 = dy0 - 1.0 - 0.927050983124841;
                    if ((cxx & 1) != 0) {
                        ysvExt2++;
                        dyExt2--;
                    } else {
                        ysvExt1++;
                        dyExt1--;
                    }
                } else {
                    ysvExt2 = ysb;
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt0 = dy0 - 0.618033988749894;
                    dyExt1 = dyExt2 = dy0 - 0.927050983124841;
                }

                if ((cxx & 4) != 0) {
                    zsvExt0 = zsvExt1 = zsvExt2 = zsb + 1;
                    dzExt0 = dz0 - 1.0 - 0.618033988749894;
                    dzExt1 = dzExt2 = dz0 - 1.0 - 0.927050983124841;
                    if ((cxx & 3) != 0) {
                        zsvExt2++;
                        dzExt2--;
                    } else {
                        zsvExt1++;
                        dzExt1--;
                    }
                } else {
                    zsvExt2 = zsb;
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    dzExt0 = dz0 - 0.618033988749894;
                    dzExt1 = dzExt2 = dz0 - 0.927050983124841;
                }

                if ((cxx & 8) != 0) {
                    wsvExt0 = wsvExt1 = wsb + 1;
                    wsvExt2 = wsb + 2;
                    dwExt0 = dw0 - 1.0 - 0.618033988749894;
                    dwExt1 = dw0 - 1.0 - 0.927050983124841;
                    dwExt2 = dw0 - 2.0 - 0.927050983124841;
                } else {
                    wsvExt2 = wsb;
                    wsvExt1 = wsb;
                    wsvExt0 = wsb;
                    dwExt0 = dw0 - 0.618033988749894;
                    dwExt1 = dwExt2 = dw0 - 0.927050983124841;
                }
            } else {
                byte cxxx = bScorex < aScorex ? bPointx : aPointx;
                if ((cxxx & 1) != 0) {
                    xsvExt0 = xsb + 2;
                    xsvExt1 = xsvExt2 = xsb + 1;
                    dxExt0 = dx0 - 2.0 - 1.236067977499788;
                    dxExt1 = dxExt2 = dx0 - 1.0 - 1.236067977499788;
                } else {
                    xsvExt2 = xsb;
                    xsvExt1 = xsb;
                    xsvExt0 = xsb;
                    dxExt0 = dxExt1 = dxExt2 = dx0 - 1.236067977499788;
                }

                if ((cxxx & 2) != 0) {
                    ysvExt0 = ysvExt1 = ysvExt2 = ysb + 1;
                    dyExt0 = dyExt1 = dyExt2 = dy0 - 1.0 - 1.236067977499788;
                    if ((cxxx & 1) != 0) {
                        ysvExt1++;
                        dyExt1--;
                    } else {
                        ysvExt0++;
                        dyExt0--;
                    }
                } else {
                    ysvExt2 = ysb;
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt0 = dyExt1 = dyExt2 = dy0 - 1.236067977499788;
                }

                if ((cxxx & 4) != 0) {
                    zsvExt0 = zsvExt1 = zsvExt2 = zsb + 1;
                    dzExt0 = dzExt1 = dzExt2 = dz0 - 1.0 - 1.236067977499788;
                    if ((cxxx & 3) != 3) {
                        if ((cxxx & 3) == 0) {
                            zsvExt0++;
                            dzExt0--;
                        } else {
                            zsvExt1++;
                            dzExt1--;
                        }
                    } else {
                        zsvExt2++;
                        dzExt2--;
                    }
                } else {
                    zsvExt2 = zsb;
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    dzExt0 = dzExt1 = dzExt2 = dz0 - 1.236067977499788;
                }

                if ((cxxx & 8) != 0) {
                    wsvExt0 = wsvExt1 = wsb + 1;
                    wsvExt2 = wsb + 2;
                    dwExt0 = dwExt1 = dw0 - 1.0 - 1.236067977499788;
                    dwExt2 = dw0 - 2.0 - 1.236067977499788;
                } else {
                    wsvExt2 = wsb;
                    wsvExt1 = wsb;
                    wsvExt0 = wsb;
                    dwExt0 = dwExt1 = dwExt2 = dw0 - 1.236067977499788;
                }
            }

            double dx4 = dx0 - 1.0 - 0.927050983124841;
            double dy4 = dy0 - 1.0 - 0.927050983124841;
            double dz4 = dz0 - 1.0 - 0.927050983124841;
            double dw4 = dw0 - 0.927050983124841;
            double attn4 = 2.0 - dx4 * dx4 - dy4 * dy4 - dz4 * dz4 - dw4 * dw4;
            if (attn4 > 0.0) {
                attn4 *= attn4;
                value += attn4 * attn4 * this.extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 0, dx4, dy4, dz4, dw4);
            }

            double dz3x = dz0 - 0.927050983124841;
            double dw3 = dw0 - 1.0 - 0.927050983124841;
            double attn3x = 2.0 - dx4 * dx4 - dy4 * dy4 - dz3x * dz3x - dw3 * dw3;
            if (attn3x > 0.0) {
                attn3x *= attn3x;
                value += attn3x * attn3x * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 1, dx4, dy4, dz3x, dw3);
            }

            double dy2x = dy0 - 0.927050983124841;
            double attn2x = 2.0 - dx4 * dx4 - dy2x * dy2x - dz4 * dz4 - dw3 * dw3;
            if (attn2x > 0.0) {
                attn2x *= attn2x;
                value += attn2x * attn2x * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 1, dx4, dy2x, dz4, dw3);
            }

            double dx1x = dx0 - 0.927050983124841;
            double attn1x = 2.0 - dx1x * dx1x - dy4 * dy4 - dz4 * dz4 - dw3 * dw3;
            if (attn1x > 0.0) {
                attn1x *= attn1x;
                value += attn1x * attn1x * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 1, dx1x, dy4, dz4, dw3);
            }

            dx0 = dx0 - 1.0 - 1.236067977499788;
            dy0 = dy0 - 1.0 - 1.236067977499788;
            dz0 = dz0 - 1.0 - 1.236067977499788;
            dw0 = dw0 - 1.0 - 1.236067977499788;
            double attn0x = 2.0 - dx0 * dx0 - dy0 * dy0 - dz0 * dz0 - dw0 * dw0;
            if (attn0x > 0.0) {
                attn0x *= attn0x;
                value += attn0x * attn0x * this.extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 1, dx0, dy0, dz0, dw0);
            }
        } else if (inSum <= 2.0) {
            boolean aIsBiggerSide = true;
            boolean bIsBiggerSide = true;
            byte aPointxx;
            double aScorexx;
            if (xins + yins > zins + wins) {
                aScorexx = xins + yins;
                aPointxx = 3;
            } else {
                aScorexx = zins + wins;
                aPointxx = 12;
            }

            double bScorexx;
            byte bPointxx;
            if (xins + zins > yins + wins) {
                bScorexx = xins + zins;
                bPointxx = 5;
            } else {
                bScorexx = yins + wins;
                bPointxx = 10;
            }

            if (xins + wins > yins + zins) {
                double score = xins + wins;
                if (aScorexx >= bScorexx && score > bScorexx) {
                    bScorexx = score;
                    bPointxx = 9;
                } else if (aScorexx < bScorexx && score > aScorexx) {
                    aScorexx = score;
                    aPointxx = 9;
                }
            } else {
                double score = yins + zins;
                if (aScorexx >= bScorexx && score > bScorexx) {
                    bScorexx = score;
                    bPointxx = 6;
                } else if (aScorexx < bScorexx && score > aScorexx) {
                    aScorexx = score;
                    aPointxx = 6;
                }
            }

            double p1 = 2.0 - inSum + xins;
            if (aScorexx >= bScorexx && p1 > bScorexx) {
                bScorexx = p1;
                bPointxx = 1;
                bIsBiggerSide = false;
            } else if (aScorexx < bScorexx && p1 > aScorexx) {
                aScorexx = p1;
                aPointxx = 1;
                aIsBiggerSide = false;
            }

            double p2 = 2.0 - inSum + yins;
            if (aScorexx >= bScorexx && p2 > bScorexx) {
                bScorexx = p2;
                bPointxx = 2;
                bIsBiggerSide = false;
            } else if (aScorexx < bScorexx && p2 > aScorexx) {
                aScorexx = p2;
                aPointxx = 2;
                aIsBiggerSide = false;
            }

            double p3 = 2.0 - inSum + zins;
            if (aScorexx >= bScorexx && p3 > bScorexx) {
                bScorexx = p3;
                bPointxx = 4;
                bIsBiggerSide = false;
            } else if (aScorexx < bScorexx && p3 > aScorexx) {
                aScorexx = p3;
                aPointxx = 4;
                aIsBiggerSide = false;
            }

            double p4 = 2.0 - inSum + wins;
            if (aScorexx >= bScorexx && p4 > bScorexx) {
                bPointxx = 8;
                bIsBiggerSide = false;
            } else if (aScorexx < bScorexx && p4 > aScorexx) {
                aPointxx = 8;
                aIsBiggerSide = false;
            }

            if (aIsBiggerSide == bIsBiggerSide) {
                if (aIsBiggerSide) {
                    byte c1 = (byte)(aPointxx | bPointxx);
                    byte c2 = (byte)(aPointxx & bPointxx);
                    if ((c1 & 1) == 0) {
                        xsvExt0 = xsb;
                        xsvExt1 = xsb - 1;
                        dxExt0 = dx0 - 0.927050983124841;
                        dxExt1 = dx0 + 1.0 - 0.618033988749894;
                    } else {
                        xsvExt0 = xsvExt1 = xsb + 1;
                        dxExt0 = dx0 - 1.0 - 0.927050983124841;
                        dxExt1 = dx0 - 1.0 - 0.618033988749894;
                    }

                    if ((c1 & 2) == 0) {
                        ysvExt0 = ysb;
                        ysvExt1 = ysb - 1;
                        dyExt0 = dy0 - 0.927050983124841;
                        dyExt1 = dy0 + 1.0 - 0.618033988749894;
                    } else {
                        ysvExt0 = ysvExt1 = ysb + 1;
                        dyExt0 = dy0 - 1.0 - 0.927050983124841;
                        dyExt1 = dy0 - 1.0 - 0.618033988749894;
                    }

                    if ((c1 & 4) == 0) {
                        zsvExt0 = zsb;
                        zsvExt1 = zsb - 1;
                        dzExt0 = dz0 - 0.927050983124841;
                        dzExt1 = dz0 + 1.0 - 0.618033988749894;
                    } else {
                        zsvExt0 = zsvExt1 = zsb + 1;
                        dzExt0 = dz0 - 1.0 - 0.927050983124841;
                        dzExt1 = dz0 - 1.0 - 0.618033988749894;
                    }

                    if ((c1 & 8) == 0) {
                        wsvExt0 = wsb;
                        wsvExt1 = wsb - 1;
                        dwExt0 = dw0 - 0.927050983124841;
                        dwExt1 = dw0 + 1.0 - 0.618033988749894;
                    } else {
                        wsvExt0 = wsvExt1 = wsb + 1;
                        dwExt0 = dw0 - 1.0 - 0.927050983124841;
                        dwExt1 = dw0 - 1.0 - 0.618033988749894;
                    }

                    xsvExt2 = xsb;
                    ysvExt2 = ysb;
                    zsvExt2 = zsb;
                    wsvExt2 = wsb;
                    dxExt2 = dx0 - 0.618033988749894;
                    dyExt2 = dy0 - 0.618033988749894;
                    dzExt2 = dz0 - 0.618033988749894;
                    dwExt2 = dw0 - 0.618033988749894;
                    if ((c2 & 1) != 0) {
                        xsvExt2 = xsb + 2;
                        dxExt2 -= 2.0;
                    } else if ((c2 & 2) != 0) {
                        ysvExt2 = ysb + 2;
                        dyExt2 -= 2.0;
                    } else if ((c2 & 4) != 0) {
                        zsvExt2 = zsb + 2;
                        dzExt2 -= 2.0;
                    } else {
                        wsvExt2 = wsb + 2;
                        dwExt2 -= 2.0;
                    }
                } else {
                    xsvExt2 = xsb;
                    ysvExt2 = ysb;
                    zsvExt2 = zsb;
                    wsvExt2 = wsb;
                    dxExt2 = dx0;
                    dyExt2 = dy0;
                    dzExt2 = dz0;
                    dwExt2 = dw0;
                    byte cxxxx = (byte)(aPointxx | bPointxx);
                    if ((cxxxx & 1) == 0) {
                        xsvExt0 = xsb - 1;
                        xsvExt1 = xsb;
                        dxExt0 = dx0 + 1.0 - 0.309016994374947;
                        dxExt1 = dx0 - 0.309016994374947;
                    } else {
                        xsvExt0 = xsvExt1 = xsb + 1;
                        dxExt0 = dxExt1 = dx0 - 1.0 - 0.309016994374947;
                    }

                    if ((cxxxx & 2) == 0) {
                        ysvExt1 = ysb;
                        ysvExt0 = ysb;
                        dyExt0 = dyExt1 = dy0 - 0.309016994374947;
                        if ((cxxxx & 1) == 1) {
                            ysvExt0 = ysb - 1;
                            dyExt0++;
                        } else {
                            ysvExt1 = ysb - 1;
                            dyExt1++;
                        }
                    } else {
                        ysvExt0 = ysvExt1 = ysb + 1;
                        dyExt0 = dyExt1 = dy0 - 1.0 - 0.309016994374947;
                    }

                    if ((cxxxx & 4) == 0) {
                        zsvExt1 = zsb;
                        zsvExt0 = zsb;
                        dzExt0 = dzExt1 = dz0 - 0.309016994374947;
                        if ((cxxxx & 3) == 3) {
                            zsvExt0 = zsb - 1;
                            dzExt0++;
                        } else {
                            zsvExt1 = zsb - 1;
                            dzExt1++;
                        }
                    } else {
                        zsvExt0 = zsvExt1 = zsb + 1;
                        dzExt0 = dzExt1 = dz0 - 1.0 - 0.309016994374947;
                    }

                    if ((cxxxx & 8) == 0) {
                        wsvExt0 = wsb;
                        wsvExt1 = wsb - 1;
                        dwExt0 = dw0 - 0.309016994374947;
                        dwExt1 = dw0 + 1.0 - 0.309016994374947;
                    } else {
                        wsvExt0 = wsvExt1 = wsb + 1;
                        dwExt0 = dwExt1 = dw0 - 1.0 - 0.309016994374947;
                    }
                }
            } else {
                byte c1x;
                byte c2x;
                if (aIsBiggerSide) {
                    c1x = aPointxx;
                    c2x = bPointxx;
                } else {
                    c1x = bPointxx;
                    c2x = aPointxx;
                }

                if ((c1x & 1) == 0) {
                    xsvExt0 = xsb - 1;
                    xsvExt1 = xsb;
                    dxExt0 = dx0 + 1.0 - 0.309016994374947;
                    dxExt1 = dx0 - 0.309016994374947;
                } else {
                    xsvExt0 = xsvExt1 = xsb + 1;
                    dxExt0 = dxExt1 = dx0 - 1.0 - 0.309016994374947;
                }

                if ((c1x & 2) == 0) {
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt0 = dyExt1 = dy0 - 0.309016994374947;
                    if ((c1x & 1) == 1) {
                        ysvExt0 = ysb - 1;
                        dyExt0++;
                    } else {
                        ysvExt1 = ysb - 1;
                        dyExt1++;
                    }
                } else {
                    ysvExt0 = ysvExt1 = ysb + 1;
                    dyExt0 = dyExt1 = dy0 - 1.0 - 0.309016994374947;
                }

                if ((c1x & 4) == 0) {
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    dzExt0 = dzExt1 = dz0 - 0.309016994374947;
                    if ((c1x & 3) == 3) {
                        zsvExt0 = zsb - 1;
                        dzExt0++;
                    } else {
                        zsvExt1 = zsb - 1;
                        dzExt1++;
                    }
                } else {
                    zsvExt0 = zsvExt1 = zsb + 1;
                    dzExt0 = dzExt1 = dz0 - 1.0 - 0.309016994374947;
                }

                if ((c1x & 8) == 0) {
                    wsvExt0 = wsb;
                    wsvExt1 = wsb - 1;
                    dwExt0 = dw0 - 0.309016994374947;
                    dwExt1 = dw0 + 1.0 - 0.309016994374947;
                } else {
                    wsvExt0 = wsvExt1 = wsb + 1;
                    dwExt0 = dwExt1 = dw0 - 1.0 - 0.309016994374947;
                }

                xsvExt2 = xsb;
                ysvExt2 = ysb;
                zsvExt2 = zsb;
                wsvExt2 = wsb;
                dxExt2 = dx0 - 0.618033988749894;
                dyExt2 = dy0 - 0.618033988749894;
                dzExt2 = dz0 - 0.618033988749894;
                dwExt2 = dw0 - 0.618033988749894;
                if ((c2x & 1) != 0) {
                    xsvExt2 = xsb + 2;
                    dxExt2 -= 2.0;
                } else if ((c2x & 2) != 0) {
                    ysvExt2 = ysb + 2;
                    dyExt2 -= 2.0;
                } else if ((c2x & 4) != 0) {
                    zsvExt2 = zsb + 2;
                    dzExt2 -= 2.0;
                } else {
                    wsvExt2 = wsb + 2;
                    dwExt2 -= 2.0;
                }
            }

            double dx1xx = dx0 - 1.0 - 0.309016994374947;
            double dy1x = dy0 - 0.0 - 0.309016994374947;
            double dz1x = dz0 - 0.0 - 0.309016994374947;
            double dw1x = dw0 - 0.0 - 0.309016994374947;
            double attn1xx = 2.0 - dx1xx * dx1xx - dy1x * dy1x - dz1x * dz1x - dw1x * dw1x;
            if (attn1xx > 0.0) {
                attn1xx *= attn1xx;
                value += attn1xx * attn1xx * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 0, dx1xx, dy1x, dz1x, dw1x);
            }

            double dx2x = dx0 - 0.0 - 0.309016994374947;
            double dy2xx = dy0 - 1.0 - 0.309016994374947;
            double attn2xx = 2.0 - dx2x * dx2x - dy2xx * dy2xx - dz1x * dz1x - dw1x * dw1x;
            if (attn2xx > 0.0) {
                attn2xx *= attn2xx;
                value += attn2xx * attn2xx * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 0, dx2x, dy2xx, dz1x, dw1x);
            }

            double dz3xx = dz0 - 1.0 - 0.309016994374947;
            double attn3xx = 2.0 - dx2x * dx2x - dy1x * dy1x - dz3xx * dz3xx - dw1x * dw1x;
            if (attn3xx > 0.0) {
                attn3xx *= attn3xx;
                value += attn3xx * attn3xx * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 0, dx2x, dy1x, dz3xx, dw1x);
            }

            double dw4x = dw0 - 1.0 - 0.309016994374947;
            double attn4x = 2.0 - dx2x * dx2x - dy1x * dy1x - dz1x * dz1x - dw4x * dw4x;
            if (attn4x > 0.0) {
                attn4x *= attn4x;
                value += attn4x * attn4x * this.extrapolate(xsb + 0, ysb + 0, zsb + 0, wsb + 1, dx2x, dy1x, dz1x, dw4x);
            }

            double dx5 = dx0 - 1.0 - 0.618033988749894;
            double dy5 = dy0 - 1.0 - 0.618033988749894;
            double dz5 = dz0 - 0.0 - 0.618033988749894;
            double dw5 = dw0 - 0.0 - 0.618033988749894;
            double attn5 = 2.0 - dx5 * dx5 - dy5 * dy5 - dz5 * dz5 - dw5 * dw5;
            if (attn5 > 0.0) {
                attn5 *= attn5;
                value += attn5 * attn5 * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 0, dx5, dy5, dz5, dw5);
            }

            double dx6 = dx0 - 1.0 - 0.618033988749894;
            double dy6 = dy0 - 0.0 - 0.618033988749894;
            double dz6 = dz0 - 1.0 - 0.618033988749894;
            double dw6 = dw0 - 0.0 - 0.618033988749894;
            double attn6 = 2.0 - dx6 * dx6 - dy6 * dy6 - dz6 * dz6 - dw6 * dw6;
            if (attn6 > 0.0) {
                attn6 *= attn6;
                value += attn6 * attn6 * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 0, dx6, dy6, dz6, dw6);
            }

            double dx7 = dx0 - 1.0 - 0.618033988749894;
            double dy7 = dy0 - 0.0 - 0.618033988749894;
            double dz7 = dz0 - 0.0 - 0.618033988749894;
            double dw7 = dw0 - 1.0 - 0.618033988749894;
            double attn7 = 2.0 - dx7 * dx7 - dy7 * dy7 - dz7 * dz7 - dw7 * dw7;
            if (attn7 > 0.0) {
                attn7 *= attn7;
                value += attn7 * attn7 * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 1, dx7, dy7, dz7, dw7);
            }

            double dx8 = dx0 - 0.0 - 0.618033988749894;
            double dy8 = dy0 - 1.0 - 0.618033988749894;
            double dz8 = dz0 - 1.0 - 0.618033988749894;
            double dw8 = dw0 - 0.0 - 0.618033988749894;
            double attn8 = 2.0 - dx8 * dx8 - dy8 * dy8 - dz8 * dz8 - dw8 * dw8;
            if (attn8 > 0.0) {
                attn8 *= attn8;
                value += attn8 * attn8 * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 0, dx8, dy8, dz8, dw8);
            }

            double dx9 = dx0 - 0.0 - 0.618033988749894;
            double dy9 = dy0 - 1.0 - 0.618033988749894;
            double dz9 = dz0 - 0.0 - 0.618033988749894;
            double dw9 = dw0 - 1.0 - 0.618033988749894;
            double attn9 = 2.0 - dx9 * dx9 - dy9 * dy9 - dz9 * dz9 - dw9 * dw9;
            if (attn9 > 0.0) {
                attn9 *= attn9;
                value += attn9 * attn9 * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 1, dx9, dy9, dz9, dw9);
            }

            double dx10 = dx0 - 0.0 - 0.618033988749894;
            double dy10 = dy0 - 0.0 - 0.618033988749894;
            double dz10 = dz0 - 1.0 - 0.618033988749894;
            double dw10 = dw0 - 1.0 - 0.618033988749894;
            double attn10 = 2.0 - dx10 * dx10 - dy10 * dy10 - dz10 * dz10 - dw10 * dw10;
            if (attn10 > 0.0) {
                attn10 *= attn10;
                value += attn10 * attn10 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 1, dx10, dy10, dz10, dw10);
            }
        } else {
            boolean aIsBiggerSidex = true;
            boolean bIsBiggerSidex = true;
            double aScorexxx;
            byte aPointxxx;
            if (xins + yins < zins + wins) {
                aScorexxx = xins + yins;
                aPointxxx = 12;
            } else {
                aScorexxx = zins + wins;
                aPointxxx = 3;
            }

            double bScorexxx;
            byte bPointxxx;
            if (xins + zins < yins + wins) {
                bScorexxx = xins + zins;
                bPointxxx = 10;
            } else {
                bScorexxx = yins + wins;
                bPointxxx = 5;
            }

            if (xins + wins < yins + zins) {
                double score = xins + wins;
                if (aScorexxx <= bScorexxx && score < bScorexxx) {
                    bScorexxx = score;
                    bPointxxx = 6;
                } else if (aScorexxx > bScorexxx && score < aScorexxx) {
                    aScorexxx = score;
                    aPointxxx = 6;
                }
            } else {
                double score = yins + zins;
                if (aScorexxx <= bScorexxx && score < bScorexxx) {
                    bScorexxx = score;
                    bPointxxx = 9;
                } else if (aScorexxx > bScorexxx && score < aScorexxx) {
                    aScorexxx = score;
                    aPointxxx = 9;
                }
            }

            double p1x = 3.0 - inSum + xins;
            if (aScorexxx <= bScorexxx && p1x < bScorexxx) {
                bScorexxx = p1x;
                bPointxxx = 14;
                bIsBiggerSidex = false;
            } else if (aScorexxx > bScorexxx && p1x < aScorexxx) {
                aScorexxx = p1x;
                aPointxxx = 14;
                aIsBiggerSidex = false;
            }

            double p2x = 3.0 - inSum + yins;
            if (aScorexxx <= bScorexxx && p2x < bScorexxx) {
                bScorexxx = p2x;
                bPointxxx = 13;
                bIsBiggerSidex = false;
            } else if (aScorexxx > bScorexxx && p2x < aScorexxx) {
                aScorexxx = p2x;
                aPointxxx = 13;
                aIsBiggerSidex = false;
            }

            double p3x = 3.0 - inSum + zins;
            if (aScorexxx <= bScorexxx && p3x < bScorexxx) {
                bScorexxx = p3x;
                bPointxxx = 11;
                bIsBiggerSidex = false;
            } else if (aScorexxx > bScorexxx && p3x < aScorexxx) {
                aScorexxx = p3x;
                aPointxxx = 11;
                aIsBiggerSidex = false;
            }

            double p4x = 3.0 - inSum + wins;
            if (aScorexxx <= bScorexxx && p4x < bScorexxx) {
                bPointxxx = 7;
                bIsBiggerSidex = false;
            } else if (aScorexxx > bScorexxx && p4x < aScorexxx) {
                aPointxxx = 7;
                aIsBiggerSidex = false;
            }

            if (aIsBiggerSidex == bIsBiggerSidex) {
                if (aIsBiggerSidex) {
                    byte c1xx = (byte)(aPointxxx & bPointxxx);
                    byte c2xx = (byte)(aPointxxx | bPointxxx);
                    xsvExt1 = xsb;
                    xsvExt0 = xsb;
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    wsvExt1 = wsb;
                    wsvExt0 = wsb;
                    dxExt0 = dx0 - 0.309016994374947;
                    dyExt0 = dy0 - 0.309016994374947;
                    dzExt0 = dz0 - 0.309016994374947;
                    dwExt0 = dw0 - 0.309016994374947;
                    dxExt1 = dx0 - 0.618033988749894;
                    dyExt1 = dy0 - 0.618033988749894;
                    dzExt1 = dz0 - 0.618033988749894;
                    dwExt1 = dw0 - 0.618033988749894;
                    if ((c1xx & 1) != 0) {
                        xsvExt0 = xsb + 1;
                        dxExt0--;
                        xsvExt1 = xsb + 2;
                        dxExt1 -= 2.0;
                    } else if ((c1xx & 2) != 0) {
                        ysvExt0 = ysb + 1;
                        dyExt0--;
                        ysvExt1 = ysb + 2;
                        dyExt1 -= 2.0;
                    } else if ((c1xx & 4) != 0) {
                        zsvExt0 = zsb + 1;
                        dzExt0--;
                        zsvExt1 = zsb + 2;
                        dzExt1 -= 2.0;
                    } else {
                        wsvExt0 = wsb + 1;
                        dwExt0--;
                        wsvExt1 = wsb + 2;
                        dwExt1 -= 2.0;
                    }

                    xsvExt2 = xsb + 1;
                    ysvExt2 = ysb + 1;
                    zsvExt2 = zsb + 1;
                    wsvExt2 = wsb + 1;
                    dxExt2 = dx0 - 1.0 - 0.618033988749894;
                    dyExt2 = dy0 - 1.0 - 0.618033988749894;
                    dzExt2 = dz0 - 1.0 - 0.618033988749894;
                    dwExt2 = dw0 - 1.0 - 0.618033988749894;
                    if ((c2xx & 1) == 0) {
                        xsvExt2 -= 2;
                        dxExt2 += 2.0;
                    } else if ((c2xx & 2) == 0) {
                        ysvExt2 -= 2;
                        dyExt2 += 2.0;
                    } else if ((c2xx & 4) == 0) {
                        zsvExt2 -= 2;
                        dzExt2 += 2.0;
                    } else {
                        wsvExt2 -= 2;
                        dwExt2 += 2.0;
                    }
                } else {
                    xsvExt2 = xsb + 1;
                    ysvExt2 = ysb + 1;
                    zsvExt2 = zsb + 1;
                    wsvExt2 = wsb + 1;
                    dxExt2 = dx0 - 1.0 - 1.236067977499788;
                    dyExt2 = dy0 - 1.0 - 1.236067977499788;
                    dzExt2 = dz0 - 1.0 - 1.236067977499788;
                    dwExt2 = dw0 - 1.0 - 1.236067977499788;
                    byte cxxxxx = (byte)(aPointxxx & bPointxxx);
                    if ((cxxxxx & 1) != 0) {
                        xsvExt0 = xsb + 2;
                        xsvExt1 = xsb + 1;
                        dxExt0 = dx0 - 2.0 - 0.927050983124841;
                        dxExt1 = dx0 - 1.0 - 0.927050983124841;
                    } else {
                        xsvExt1 = xsb;
                        xsvExt0 = xsb;
                        dxExt0 = dxExt1 = dx0 - 0.927050983124841;
                    }

                    if ((cxxxxx & 2) != 0) {
                        ysvExt0 = ysvExt1 = ysb + 1;
                        dyExt0 = dyExt1 = dy0 - 1.0 - 0.927050983124841;
                        if ((cxxxxx & 1) == 0) {
                            ysvExt0++;
                            dyExt0--;
                        } else {
                            ysvExt1++;
                            dyExt1--;
                        }
                    } else {
                        ysvExt1 = ysb;
                        ysvExt0 = ysb;
                        dyExt0 = dyExt1 = dy0 - 0.927050983124841;
                    }

                    if ((cxxxxx & 4) != 0) {
                        zsvExt0 = zsvExt1 = zsb + 1;
                        dzExt0 = dzExt1 = dz0 - 1.0 - 0.927050983124841;
                        if ((cxxxxx & 3) == 0) {
                            zsvExt0++;
                            dzExt0--;
                        } else {
                            zsvExt1++;
                            dzExt1--;
                        }
                    } else {
                        zsvExt1 = zsb;
                        zsvExt0 = zsb;
                        dzExt0 = dzExt1 = dz0 - 0.927050983124841;
                    }

                    if ((cxxxxx & 8) != 0) {
                        wsvExt0 = wsb + 1;
                        wsvExt1 = wsb + 2;
                        dwExt0 = dw0 - 1.0 - 0.927050983124841;
                        dwExt1 = dw0 - 2.0 - 0.927050983124841;
                    } else {
                        wsvExt1 = wsb;
                        wsvExt0 = wsb;
                        dwExt0 = dwExt1 = dw0 - 0.927050983124841;
                    }
                }
            } else {
                byte c1xxx;
                byte c2xxx;
                if (aIsBiggerSidex) {
                    c1xxx = aPointxxx;
                    c2xxx = bPointxxx;
                } else {
                    c1xxx = bPointxxx;
                    c2xxx = aPointxxx;
                }

                if ((c1xxx & 1) != 0) {
                    xsvExt0 = xsb + 2;
                    xsvExt1 = xsb + 1;
                    dxExt0 = dx0 - 2.0 - 0.927050983124841;
                    dxExt1 = dx0 - 1.0 - 0.927050983124841;
                } else {
                    xsvExt1 = xsb;
                    xsvExt0 = xsb;
                    dxExt0 = dxExt1 = dx0 - 0.927050983124841;
                }

                if ((c1xxx & 2) != 0) {
                    ysvExt0 = ysvExt1 = ysb + 1;
                    dyExt0 = dyExt1 = dy0 - 1.0 - 0.927050983124841;
                    if ((c1xxx & 1) == 0) {
                        ysvExt0++;
                        dyExt0--;
                    } else {
                        ysvExt1++;
                        dyExt1--;
                    }
                } else {
                    ysvExt1 = ysb;
                    ysvExt0 = ysb;
                    dyExt0 = dyExt1 = dy0 - 0.927050983124841;
                }

                if ((c1xxx & 4) != 0) {
                    zsvExt0 = zsvExt1 = zsb + 1;
                    dzExt0 = dzExt1 = dz0 - 1.0 - 0.927050983124841;
                    if ((c1xxx & 3) == 0) {
                        zsvExt0++;
                        dzExt0--;
                    } else {
                        zsvExt1++;
                        dzExt1--;
                    }
                } else {
                    zsvExt1 = zsb;
                    zsvExt0 = zsb;
                    dzExt0 = dzExt1 = dz0 - 0.927050983124841;
                }

                if ((c1xxx & 8) != 0) {
                    wsvExt0 = wsb + 1;
                    wsvExt1 = wsb + 2;
                    dwExt0 = dw0 - 1.0 - 0.927050983124841;
                    dwExt1 = dw0 - 2.0 - 0.927050983124841;
                } else {
                    wsvExt1 = wsb;
                    wsvExt0 = wsb;
                    dwExt0 = dwExt1 = dw0 - 0.927050983124841;
                }

                xsvExt2 = xsb + 1;
                ysvExt2 = ysb + 1;
                zsvExt2 = zsb + 1;
                wsvExt2 = wsb + 1;
                dxExt2 = dx0 - 1.0 - 0.618033988749894;
                dyExt2 = dy0 - 1.0 - 0.618033988749894;
                dzExt2 = dz0 - 1.0 - 0.618033988749894;
                dwExt2 = dw0 - 1.0 - 0.618033988749894;
                if ((c2xxx & 1) == 0) {
                    xsvExt2 -= 2;
                    dxExt2 += 2.0;
                } else if ((c2xxx & 2) == 0) {
                    ysvExt2 -= 2;
                    dyExt2 += 2.0;
                } else if ((c2xxx & 4) == 0) {
                    zsvExt2 -= 2;
                    dzExt2 += 2.0;
                } else {
                    wsvExt2 -= 2;
                    dwExt2 += 2.0;
                }
            }

            double dx4x = dx0 - 1.0 - 0.927050983124841;
            double dy4x = dy0 - 1.0 - 0.927050983124841;
            double dz4x = dz0 - 1.0 - 0.927050983124841;
            double dw4xx = dw0 - 0.927050983124841;
            double attn4xx = 2.0 - dx4x * dx4x - dy4x * dy4x - dz4x * dz4x - dw4xx * dw4xx;
            if (attn4xx > 0.0) {
                attn4xx *= attn4xx;
                value += attn4xx * attn4xx * this.extrapolate(xsb + 1, ysb + 1, zsb + 1, wsb + 0, dx4x, dy4x, dz4x, dw4xx);
            }

            double dz3xxx = dz0 - 0.927050983124841;
            double dw3x = dw0 - 1.0 - 0.927050983124841;
            double attn3xxx = 2.0 - dx4x * dx4x - dy4x * dy4x - dz3xxx * dz3xxx - dw3x * dw3x;
            if (attn3xxx > 0.0) {
                attn3xxx *= attn3xxx;
                value += attn3xxx * attn3xxx * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 1, dx4x, dy4x, dz3xxx, dw3x);
            }

            double dy2xxx = dy0 - 0.927050983124841;
            double attn2xxx = 2.0 - dx4x * dx4x - dy2xxx * dy2xxx - dz4x * dz4x - dw3x * dw3x;
            if (attn2xxx > 0.0) {
                attn2xxx *= attn2xxx;
                value += attn2xxx * attn2xxx * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 1, dx4x, dy2xxx, dz4x, dw3x);
            }

            double dx1xxx = dx0 - 0.927050983124841;
            double attn1xxx = 2.0 - dx1xxx * dx1xxx - dy4x * dy4x - dz4x * dz4x - dw3x * dw3x;
            if (attn1xxx > 0.0) {
                attn1xxx *= attn1xxx;
                value += attn1xxx * attn1xxx * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 1, dx1xxx, dy4x, dz4x, dw3x);
            }

            double dx5x = dx0 - 1.0 - 0.618033988749894;
            double dy5x = dy0 - 1.0 - 0.618033988749894;
            double dz5x = dz0 - 0.0 - 0.618033988749894;
            double dw5x = dw0 - 0.0 - 0.618033988749894;
            double attn5x = 2.0 - dx5x * dx5x - dy5x * dy5x - dz5x * dz5x - dw5x * dw5x;
            if (attn5x > 0.0) {
                attn5x *= attn5x;
                value += attn5x * attn5x * this.extrapolate(xsb + 1, ysb + 1, zsb + 0, wsb + 0, dx5x, dy5x, dz5x, dw5x);
            }

            double dx6x = dx0 - 1.0 - 0.618033988749894;
            double dy6x = dy0 - 0.0 - 0.618033988749894;
            double dz6x = dz0 - 1.0 - 0.618033988749894;
            double dw6x = dw0 - 0.0 - 0.618033988749894;
            double attn6x = 2.0 - dx6x * dx6x - dy6x * dy6x - dz6x * dz6x - dw6x * dw6x;
            if (attn6x > 0.0) {
                attn6x *= attn6x;
                value += attn6x * attn6x * this.extrapolate(xsb + 1, ysb + 0, zsb + 1, wsb + 0, dx6x, dy6x, dz6x, dw6x);
            }

            double dx7x = dx0 - 1.0 - 0.618033988749894;
            double dy7x = dy0 - 0.0 - 0.618033988749894;
            double dz7x = dz0 - 0.0 - 0.618033988749894;
            double dw7x = dw0 - 1.0 - 0.618033988749894;
            double attn7x = 2.0 - dx7x * dx7x - dy7x * dy7x - dz7x * dz7x - dw7x * dw7x;
            if (attn7x > 0.0) {
                attn7x *= attn7x;
                value += attn7x * attn7x * this.extrapolate(xsb + 1, ysb + 0, zsb + 0, wsb + 1, dx7x, dy7x, dz7x, dw7x);
            }

            double dx8x = dx0 - 0.0 - 0.618033988749894;
            double dy8x = dy0 - 1.0 - 0.618033988749894;
            double dz8x = dz0 - 1.0 - 0.618033988749894;
            double dw8x = dw0 - 0.0 - 0.618033988749894;
            double attn8x = 2.0 - dx8x * dx8x - dy8x * dy8x - dz8x * dz8x - dw8x * dw8x;
            if (attn8x > 0.0) {
                attn8x *= attn8x;
                value += attn8x * attn8x * this.extrapolate(xsb + 0, ysb + 1, zsb + 1, wsb + 0, dx8x, dy8x, dz8x, dw8x);
            }

            double dx9x = dx0 - 0.0 - 0.618033988749894;
            double dy9x = dy0 - 1.0 - 0.618033988749894;
            double dz9x = dz0 - 0.0 - 0.618033988749894;
            double dw9x = dw0 - 1.0 - 0.618033988749894;
            double attn9x = 2.0 - dx9x * dx9x - dy9x * dy9x - dz9x * dz9x - dw9x * dw9x;
            if (attn9x > 0.0) {
                attn9x *= attn9x;
                value += attn9x * attn9x * this.extrapolate(xsb + 0, ysb + 1, zsb + 0, wsb + 1, dx9x, dy9x, dz9x, dw9x);
            }

            double dx10 = dx0 - 0.0 - 0.618033988749894;
            double dy10 = dy0 - 0.0 - 0.618033988749894;
            double dz10 = dz0 - 1.0 - 0.618033988749894;
            double dw10 = dw0 - 1.0 - 0.618033988749894;
            double attn10 = 2.0 - dx10 * dx10 - dy10 * dy10 - dz10 * dz10 - dw10 * dw10;
            if (attn10 > 0.0) {
                attn10 *= attn10;
                value += attn10 * attn10 * this.extrapolate(xsb + 0, ysb + 0, zsb + 1, wsb + 1, dx10, dy10, dz10, dw10);
            }
        }

        double attnExt0 = 2.0 - dxExt0 * dxExt0 - dyExt0 * dyExt0 - dzExt0 * dzExt0 - dwExt0 * dwExt0;
        if (attnExt0 > 0.0) {
            attnExt0 *= attnExt0;
            value += attnExt0 * attnExt0 * this.extrapolate(xsvExt0, ysvExt0, zsvExt0, wsvExt0, dxExt0, dyExt0, dzExt0, dwExt0);
        }

        double attnExt1 = 2.0 - dxExt1 * dxExt1 - dyExt1 * dyExt1 - dzExt1 * dzExt1 - dwExt1 * dwExt1;
        if (attnExt1 > 0.0) {
            attnExt1 *= attnExt1;
            value += attnExt1 * attnExt1 * this.extrapolate(xsvExt1, ysvExt1, zsvExt1, wsvExt1, dxExt1, dyExt1, dzExt1, dwExt1);
        }

        double attnExt2 = 2.0 - dxExt2 * dxExt2 - dyExt2 * dyExt2 - dzExt2 * dzExt2 - dwExt2 * dwExt2;
        if (attnExt2 > 0.0) {
            attnExt2 *= attnExt2;
            value += attnExt2 * attnExt2 * this.extrapolate(xsvExt2, ysvExt2, zsvExt2, wsvExt2, dxExt2, dyExt2, dzExt2, dwExt2);
        }

        return value / 30.0;
    }

    private double extrapolate(int xsb, int ysb, double dx, double dy) {
        int index = this.perm[this.perm[xsb & 0xFF] + ysb & 0xFF] & 14;
        return gradients2D[index] * dx + gradients2D[index + 1] * dy;
    }

    private double extrapolate(int xsb, int ysb, int zsb, double dx, double dy, double dz) {
        int index = this.permGradIndex3d[this.perm[this.perm[xsb & 0xFF] + ysb & 0xFF] + zsb & 0xFF];
        return gradients3D[index] * dx + gradients3D[index + 1] * dy + gradients3D[index + 2] * dz;
    }

    private double extrapolate(int xsb, int ysb, int zsb, int wsb, double dx, double dy, double dz, double dw) {
        int index = this.perm[this.perm[this.perm[this.perm[xsb & 0xFF] + ysb & 0xFF] + zsb & 0xFF] + wsb & 0xFF] & 252;
        return gradients4D[index] * dx + gradients4D[index + 1] * dy + gradients4D[index + 2] * dz + gradients4D[index + 3] * dw;
    }

    private static int fastFloor(double x) {
        int xi = (int)x;
        return x < xi ? xi - 1 : xi;
    }

    public double evalOct(float v, float v1, int i) {
        double res = this.eval(v, v1, i);

        for (int x = 2; x <= 64; x++) {
            res += this.eval(v * x * v, v1 * x * v1, i * x * i);
        }

        return res;
    }
}
