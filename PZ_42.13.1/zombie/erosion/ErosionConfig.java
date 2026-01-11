// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.erosion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.ByteBuffer;
import zombie.UsedFromLua;
import zombie.core.Core;
import zombie.debug.DebugLog;

@UsedFromLua
public final class ErosionConfig {
    public final ErosionConfig.Seeds seeds = new ErosionConfig.Seeds();
    public final ErosionConfig.Time time = new ErosionConfig.Time();
    public final ErosionConfig.Debug debug = new ErosionConfig.Debug();
    public final ErosionConfig.Season season = new ErosionConfig.Season();

    public void save(ByteBuffer bb) {
        bb.putInt(this.seeds.seedMain0);
        bb.putInt(this.seeds.seedMain1);
        bb.putInt(this.seeds.seedMain2);
        bb.putInt(this.seeds.seedMoisture0);
        bb.putInt(this.seeds.seedMoisture1);
        bb.putInt(this.seeds.seedMoisture2);
        bb.putInt(this.seeds.seedMinerals0);
        bb.putInt(this.seeds.seedMinerals1);
        bb.putInt(this.seeds.seedMinerals2);
        bb.putInt(this.seeds.seedKudzu0);
        bb.putInt(this.seeds.seedKudzu1);
        bb.putInt(this.seeds.seedKudzu2);
        bb.putInt(this.time.tickunit);
        bb.putInt(this.time.ticks);
        bb.putInt(this.time.eticks);
        bb.putInt(this.time.epoch);
        bb.putInt(this.season.lat);
        bb.putInt(this.season.tempMax);
        bb.putInt(this.season.tempMin);
        bb.putInt(this.season.tempDiff);
        bb.putInt(this.season.seasonLag);
        bb.putFloat(this.season.noon);
        bb.putInt(this.season.seedA);
        bb.putInt(this.season.seedB);
        bb.putInt(this.season.seedC);
        bb.putFloat(this.season.jan);
        bb.putFloat(this.season.feb);
        bb.putFloat(this.season.mar);
        bb.putFloat(this.season.apr);
        bb.putFloat(this.season.may);
        bb.putFloat(this.season.jun);
        bb.putFloat(this.season.jul);
        bb.putFloat(this.season.aug);
        bb.putFloat(this.season.sep);
        bb.putFloat(this.season.oct);
        bb.putFloat(this.season.nov);
        bb.putFloat(this.season.dec);
    }

    public void load(ByteBuffer bb) {
        this.seeds.seedMain0 = bb.getInt();
        this.seeds.seedMain1 = bb.getInt();
        this.seeds.seedMain2 = bb.getInt();
        this.seeds.seedMoisture0 = bb.getInt();
        this.seeds.seedMoisture1 = bb.getInt();
        this.seeds.seedMoisture2 = bb.getInt();
        this.seeds.seedMinerals0 = bb.getInt();
        this.seeds.seedMinerals1 = bb.getInt();
        this.seeds.seedMinerals2 = bb.getInt();
        this.seeds.seedKudzu0 = bb.getInt();
        this.seeds.seedKudzu1 = bb.getInt();
        this.seeds.seedKudzu2 = bb.getInt();
        this.time.tickunit = bb.getInt();
        this.time.ticks = bb.getInt();
        this.time.eticks = bb.getInt();
        this.time.epoch = bb.getInt();
        this.season.lat = bb.getInt();
        this.season.tempMax = bb.getInt();
        this.season.tempMin = bb.getInt();
        this.season.tempDiff = bb.getInt();
        this.season.seasonLag = bb.getInt();
        this.season.noon = bb.getFloat();
        this.season.seedA = bb.getInt();
        this.season.seedB = bb.getInt();
        this.season.seedC = bb.getInt();
        this.season.jan = bb.getFloat();
        this.season.feb = bb.getFloat();
        this.season.mar = bb.getFloat();
        this.season.apr = bb.getFloat();
        this.season.may = bb.getFloat();
        this.season.jun = bb.getFloat();
        this.season.jul = bb.getFloat();
        this.season.aug = bb.getFloat();
        this.season.sep = bb.getFloat();
        this.season.oct = bb.getFloat();
        this.season.nov = bb.getFloat();
        this.season.dec = bb.getFloat();
    }

    public void writeFile(String _file) {
        try {
            if (Core.getInstance().isNoSave()) {
                return;
            }

            File file = new File(_file);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter w = new FileWriter(file, false);
            w.write("seeds.seedMain_0 = " + this.seeds.seedMain0 + "\n");
            w.write("seeds.seedMain_1 = " + this.seeds.seedMain1 + "\n");
            w.write("seeds.seedMain_2 = " + this.seeds.seedMain2 + "\n");
            w.write("seeds.seedMoisture_0 = " + this.seeds.seedMoisture0 + "\n");
            w.write("seeds.seedMoisture_1 = " + this.seeds.seedMoisture1 + "\n");
            w.write("seeds.seedMoisture_2 = " + this.seeds.seedMoisture2 + "\n");
            w.write("seeds.seedMinerals_0 = " + this.seeds.seedMinerals0 + "\n");
            w.write("seeds.seedMinerals_1 = " + this.seeds.seedMinerals1 + "\n");
            w.write("seeds.seedMinerals_2 = " + this.seeds.seedMinerals2 + "\n");
            w.write("seeds.seedKudzu_0 = " + this.seeds.seedKudzu0 + "\n");
            w.write("seeds.seedKudzu_1 = " + this.seeds.seedKudzu1 + "\n");
            w.write("seeds.seedKudzu_2 = " + this.seeds.seedKudzu2 + "\n");
            w.write("\n");
            w.write("time.tickunit = " + this.time.tickunit + "\n");
            w.write("time.ticks = " + this.time.ticks + "\n");
            w.write("time.eticks = " + this.time.eticks + "\n");
            w.write("time.epoch = " + this.time.epoch + "\n");
            w.write("\n");
            w.write("season.lat = " + this.season.lat + "\n");
            w.write("season.tempMax = " + this.season.tempMax + "\n");
            w.write("season.tempMin = " + this.season.tempMin + "\n");
            w.write("season.tempDiff = " + this.season.tempDiff + "\n");
            w.write("season.seasonLag = " + this.season.seasonLag + "\n");
            w.write("season.noon = " + this.season.noon + "\n");
            w.write("season.seedA = " + this.season.seedA + "\n");
            w.write("season.seedB = " + this.season.seedB + "\n");
            w.write("season.seedC = " + this.season.seedC + "\n");
            w.write("season.jan = " + this.season.jan + "\n");
            w.write("season.feb = " + this.season.feb + "\n");
            w.write("season.mar = " + this.season.mar + "\n");
            w.write("season.apr = " + this.season.apr + "\n");
            w.write("season.may = " + this.season.may + "\n");
            w.write("season.jun = " + this.season.jun + "\n");
            w.write("season.jul = " + this.season.jul + "\n");
            w.write("season.aug = " + this.season.aug + "\n");
            w.write("season.sep = " + this.season.sep + "\n");
            w.write("season.oct = " + this.season.oct + "\n");
            w.write("season.nov = " + this.season.nov + "\n");
            w.write("season.dec = " + this.season.dec + "\n");
            w.write("\n");
            w.write("debug.enabled = " + this.debug.enabled + "\n");
            w.write("debug.startday = " + this.debug.startday + "\n");
            w.write("debug.startmonth = " + this.debug.startmonth + "\n");
            w.close();
        } catch (Exception var4) {
            var4.printStackTrace();
        }
    }

    public boolean readFile(String _file) {
        try {
            File file = new File(_file);
            if (!file.exists()) {
                return false;
            } else {
                BufferedReader r = new BufferedReader(new FileReader(file));

                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        r.close();
                        return true;
                    }

                    if (!line.trim().startsWith("--")) {
                        if (!line.contains("=")) {
                            if (!line.trim().isEmpty()) {
                                DebugLog.log("ErosionConfig: unknown \"" + line + "\"");
                            }
                        } else {
                            String[] ss = line.split("=");
                            if (ss.length != 2) {
                                DebugLog.log("ErosionConfig: unknown \"" + line + "\"");
                            } else {
                                String k = ss[0].trim();
                                String v = ss[1].trim();
                                if (k.startsWith("seeds.")) {
                                    if ("seeds.seedMain_0".equals(k)) {
                                        this.seeds.seedMain0 = Integer.parseInt(v);
                                    } else if ("seeds.seedMain_1".equals(k)) {
                                        this.seeds.seedMain1 = Integer.parseInt(v);
                                    } else if ("seeds.seedMain_2".equals(k)) {
                                        this.seeds.seedMain2 = Integer.parseInt(v);
                                    } else if ("seeds.seedMoisture_0".equals(k)) {
                                        this.seeds.seedMoisture0 = Integer.parseInt(v);
                                    } else if ("seeds.seedMoisture_1".equals(k)) {
                                        this.seeds.seedMoisture1 = Integer.parseInt(v);
                                    } else if ("seeds.seedMoisture_2".equals(k)) {
                                        this.seeds.seedMoisture2 = Integer.parseInt(v);
                                    } else if ("seeds.seedMinerals_0".equals(k)) {
                                        this.seeds.seedMinerals0 = Integer.parseInt(v);
                                    } else if ("seeds.seedMinerals_1".equals(k)) {
                                        this.seeds.seedMinerals1 = Integer.parseInt(v);
                                    } else if ("seeds.seedMinerals_2".equals(k)) {
                                        this.seeds.seedMinerals2 = Integer.parseInt(v);
                                    } else if ("seeds.seedKudzu_0".equals(k)) {
                                        this.seeds.seedKudzu0 = Integer.parseInt(v);
                                    } else if ("seeds.seedKudzu_1".equals(k)) {
                                        this.seeds.seedKudzu1 = Integer.parseInt(v);
                                    } else if ("seeds.seedKudzu_2".equals(k)) {
                                        this.seeds.seedKudzu2 = Integer.parseInt(v);
                                    } else {
                                        DebugLog.log("ErosionConfig: unknown \"" + line + "\"");
                                    }
                                } else if (k.startsWith("time.")) {
                                    if ("time.tickunit".equals(k)) {
                                        this.time.tickunit = Integer.parseInt(v);
                                    } else if ("time.ticks".equals(k)) {
                                        this.time.ticks = Integer.parseInt(v);
                                    } else if ("time.eticks".equals(k)) {
                                        this.time.eticks = Integer.parseInt(v);
                                    } else if ("time.epoch".equals(k)) {
                                        this.time.epoch = Integer.parseInt(v);
                                    } else {
                                        DebugLog.log("ErosionConfig: unknown \"" + line + "\"");
                                    }
                                } else if (k.startsWith("season.")) {
                                    if ("season.lat".equals(k)) {
                                        this.season.lat = Integer.parseInt(v);
                                    } else if ("season.tempMax".equals(k)) {
                                        this.season.tempMax = Integer.parseInt(v);
                                    } else if ("season.tempMin".equals(k)) {
                                        this.season.tempMin = Integer.parseInt(v);
                                    } else if ("season.tempDiff".equals(k)) {
                                        this.season.tempDiff = Integer.parseInt(v);
                                    } else if ("season.seasonLag".equals(k)) {
                                        this.season.seasonLag = Integer.parseInt(v);
                                    } else if ("season.noon".equals(k)) {
                                        this.season.noon = Float.parseFloat(v);
                                    } else if ("season.seedA".equals(k)) {
                                        this.season.seedA = Integer.parseInt(v);
                                    } else if ("season.seedB".equals(k)) {
                                        this.season.seedB = Integer.parseInt(v);
                                    } else if ("season.seedC".equals(k)) {
                                        this.season.seedC = Integer.parseInt(v);
                                    } else if ("season.jan".equals(k)) {
                                        this.season.jan = Float.parseFloat(v);
                                    } else if ("season.feb".equals(k)) {
                                        this.season.feb = Float.parseFloat(v);
                                    } else if ("season.mar".equals(k)) {
                                        this.season.mar = Float.parseFloat(v);
                                    } else if ("season.apr".equals(k)) {
                                        this.season.apr = Float.parseFloat(v);
                                    } else if ("season.may".equals(k)) {
                                        this.season.may = Float.parseFloat(v);
                                    } else if ("season.jun".equals(k)) {
                                        this.season.jun = Float.parseFloat(v);
                                    } else if ("season.jul".equals(k)) {
                                        this.season.jul = Float.parseFloat(v);
                                    } else if ("season.aug".equals(k)) {
                                        this.season.aug = Float.parseFloat(v);
                                    } else if ("season.sep".equals(k)) {
                                        this.season.sep = Float.parseFloat(v);
                                    } else if ("season.oct".equals(k)) {
                                        this.season.oct = Float.parseFloat(v);
                                    } else if ("season.nov".equals(k)) {
                                        this.season.nov = Float.parseFloat(v);
                                    } else if ("season.dec".equals(k)) {
                                        this.season.dec = Float.parseFloat(v);
                                    } else {
                                        DebugLog.log("ErosionConfig: unknown \"" + line + "\"");
                                    }
                                } else if (k.startsWith("debug.")) {
                                    if ("debug.enabled".equals(k)) {
                                        this.debug.enabled = Boolean.parseBoolean(v);
                                    } else if ("debug.startday".equals(k)) {
                                        this.debug.startday = Integer.parseInt(v);
                                    } else if ("debug.startmonth".equals(k)) {
                                        this.debug.startmonth = Integer.parseInt(v);
                                    }
                                } else {
                                    DebugLog.log("ErosionConfig: unknown \"" + line + "\"");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception var8) {
            var8.printStackTrace();
            return false;
        }
    }

    public ErosionConfig.Debug getDebug() {
        return this.debug;
    }

    public void consolePrint() {
    }

    @UsedFromLua
    public static final class Debug {
        boolean enabled;
        int startday = 26;
        int startmonth = 11;

        public boolean getEnabled() {
            return this.enabled;
        }

        public int getStartDay() {
            return this.startday;
        }

        public int getStartMonth() {
            return this.startmonth;
        }
    }

    @UsedFromLua
    public static final class Season {
        int lat = 38;
        int tempMax = 25;
        int tempMin;
        int tempDiff = 7;
        int seasonLag = 31;
        float noon = 12.5F;
        int seedA = 64;
        int seedB = 128;
        int seedC = 255;
        float jan = 0.39F;
        float feb = 0.35F;
        float mar = 0.39F;
        float apr = 0.4F;
        float may = 0.35F;
        float jun = 0.37F;
        float jul = 0.29F;
        float aug = 0.26F;
        float sep = 0.23F;
        float oct = 0.23F;
        float nov = 0.3F;
        float dec = 0.32F;
    }

    @UsedFromLua
    public static final class Seeds {
        int seedMain0 = 16;
        int seedMain1 = 32;
        int seedMain2 = 64;
        int seedMoisture0 = 96;
        int seedMoisture1 = 128;
        int seedMoisture2 = 144;
        int seedMinerals0 = 196;
        int seedMinerals1 = 255;
        int seedMinerals2;
        int seedKudzu0 = 200;
        int seedKudzu1 = 125;
        int seedKudzu2 = 50;
    }

    @UsedFromLua
    public static final class Time {
        int tickunit = 144;
        int ticks;
        int eticks;
        int epoch;
    }
}
