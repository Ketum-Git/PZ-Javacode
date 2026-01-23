// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

public class ColorMixer {
    public static Color LerpLCH(Color col1, Color col2, float delta, Color dest) {
        if ((col1.r != col1.g || col1.r != col1.b) && (col2.r != col2.g || col2.r != col2.b)) {
            float[] xyz1 = new float[3];
            float[] lab1 = new float[3];
            float[] lch1 = new float[3];
            float[] xyz2 = new float[3];
            float[] lab2 = new float[3];
            float[] lch2 = new float[3];
            ColorToXYZ(col1, xyz1);
            XYZToLab(xyz1, lab1);
            LabToLCH(lab1, lch1);
            ColorToXYZ(col2, xyz2);
            XYZToLab(xyz2, lab2);
            LabToLCH(lab2, lch2);
            float[] lch = new float[3];
            float[] lab = new float[3];
            float[] xyz = new float[3];
            if (lch2[2] > lch1[2]) {
                if (lch2[2] - lch1[2] > lch1[2] + 360.0F - lch2[2]) {
                    lch1[2] += 360.0F;
                }
            } else if (lch1[2] - lch2[2] > lch2[2] + 360.0F - lch1[2]) {
                lch2[2] += 360.0F;
            }

            lch[0] = lch2[0] * delta + lch1[0] * (1.0F - delta);
            lch[1] = lch2[1] * delta + lch1[1] * (1.0F - delta);
            lch[2] = lch2[2] * delta + lch1[2] * (1.0F - delta);
            if (lch[2] > 360.0F) {
                lch[2] -= 360.0F;
            }

            LCHToLab(lch, lab);
            LabToXYZ(lab, xyz);
            return XYZToRGB(xyz[0], xyz[1], xyz[2], dest);
        } else {
            col1.interp(col2, delta, dest);
            return dest;
        }
    }

    public static void ColorToXYZ(Color color, float[] outXYZ) {
        float[] rgb = new float[3];
        float[] xyz = new float[3];
        rgb[0] = color.getRedFloat();
        rgb[1] = color.getGreenFloat();
        rgb[2] = color.getBlueFloat();
        if (rgb[0] > 0.04045F) {
            rgb[0] = (float)Math.pow((rgb[0] + 0.055) / 1.055, 2.4);
        } else {
            rgb[0] /= 12.92F;
        }

        if (rgb[1] > 0.04045F) {
            rgb[1] = (float)Math.pow((rgb[1] + 0.055) / 1.055, 2.4);
        } else {
            rgb[1] /= 12.92F;
        }

        if (rgb[2] > 0.04045F) {
            rgb[2] = (float)Math.pow((rgb[2] + 0.055) / 1.055, 2.4);
        } else {
            rgb[2] /= 12.92F;
        }

        rgb[0] *= 100.0F;
        rgb[1] *= 100.0F;
        rgb[2] *= 100.0F;
        xyz[0] = rgb[0] * 0.412453F + rgb[1] * 0.35758F + rgb[2] * 0.180423F;
        xyz[1] = rgb[0] * 0.212671F + rgb[1] * 0.71516F + rgb[2] * 0.072169F;
        xyz[2] = rgb[0] * 0.019334F + rgb[1] * 0.119193F + rgb[2] * 0.950227F;
        outXYZ[0] = xyz[0];
        outXYZ[1] = xyz[1];
        outXYZ[2] = xyz[2];
    }

    private static void XYZToLab(float[] xyz, float[] outLAB) {
        xyz[0] /= 95.047F;
        xyz[1] /= 100.0F;
        xyz[2] /= 108.883F;
        if (xyz[0] > 0.008856F) {
            xyz[0] = (float)Math.pow(xyz[0], 0.3333333333333333);
        } else {
            xyz[0] = xyz[0] * 7.787F + 0.13793103F;
        }

        if (xyz[1] > 0.008856F) {
            xyz[1] = (float)Math.pow(xyz[1], 0.3333333333333333);
        } else {
            xyz[1] = xyz[1] * 7.787F + 0.13793103F;
        }

        if (xyz[2] > 0.008856F) {
            xyz[2] = (float)Math.pow(xyz[2], 0.3333333333333333);
        } else {
            xyz[2] = xyz[2] * 7.787F + 0.13793103F;
        }

        outLAB[0] = 116.0F * xyz[1] - 16.0F;
        outLAB[1] = 500.0F * (xyz[0] - xyz[1]);
        outLAB[2] = 200.0F * (xyz[1] - xyz[2]);
    }

    private static void LabToLCH(float[] lab, float[] outLCH) {
        outLCH[0] = lab[0];
        outLCH[1] = (float)Math.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);
        outLCH[2] = (float)Math.atan2(lab[2], lab[1]);
        outLCH[2] = (float)(outLCH[2] * (180.0 / Math.PI));
        if (outLCH[2] < 0.0F) {
            outLCH[2] += 360.0F;
        }
    }

    private static void LCHToLab(float[] lch, float[] outLAB) {
        outLAB[0] = lch[0];
        outLAB[1] = lch[1] * (float)Math.cos(lch[2] * (Math.PI / 180.0));
        outLAB[2] = lch[1] * (float)Math.sin(lch[2] * (Math.PI / 180.0));
    }

    private static void LabToXYZ(float[] lab, float[] outXYZ) {
        float[] xyz = new float[]{0.0F, (lab[0] + 16.0F) / 116.0F, 0.0F};
        xyz[0] = lab[1] / 500.0F + xyz[1];
        xyz[2] = xyz[1] - lab[2] / 200.0F;

        for (int i = 0; i < 3; i++) {
            float pow = xyz[i] * xyz[i] * xyz[i];
            float ratio = 0.20689656F;
            if (xyz[i] > 0.20689656F) {
                xyz[i] = pow;
            } else {
                xyz[i] = 0.12841856F * (xyz[i] - 0.13793103F);
            }
        }

        outXYZ[0] = xyz[0] * 95.047F;
        outXYZ[1] = xyz[1] * 100.0F;
        outXYZ[2] = xyz[2] * 108.883F;
    }

    public static Color XYZToRGB(float x, float y, float z, Color target) {
        float[] xyz = new float[]{x, y, z};
        float[] rgb = new float[3];

        for (int i = 0; i < 3; i++) {
            xyz[i] /= 100.0F;
        }

        rgb[0] = xyz[0] * 3.240479F + xyz[1] * -1.53715F + xyz[2] * -0.498535F;
        rgb[1] = xyz[0] * -0.969256F + xyz[1] * 1.875992F + xyz[2] * 0.041556F;
        rgb[2] = xyz[0] * 0.055648F + xyz[1] * -0.204043F + xyz[2] * 1.057311F;

        for (int i = 0; i < 3; i++) {
            if (rgb[i] > 0.0031308F) {
                rgb[i] = 1.055F * (float)Math.pow(rgb[i], 0.41666666F) - 0.055F;
            } else {
                rgb[i] *= 12.92F;
            }
        }

        rgb[0] = Math.min(Math.max(rgb[0] * 255.0F, 0.0F), 255.0F);
        rgb[1] = Math.min(Math.max(rgb[1] * 255.0F, 0.0F), 255.0F);
        rgb[2] = Math.min(Math.max(rgb[2] * 255.0F, 0.0F), 255.0F);
        target.set(rgb[0] / 255.0F, rgb[1] / 255.0F, rgb[2] / 255.0F);
        return target;
    }
}
