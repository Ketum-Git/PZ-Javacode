// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream.Filter;
import java.util.ArrayList;
import java.util.Objects;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.LuaClosure;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.debug.DebugLog;
import zombie.scripting.objects.CharacterProfession;

public class SpawnRegions {
    private SpawnRegions.Region parseRegionTable(KahluaTable regionTable) {
        Object name = regionTable.rawget("name");
        Object file = regionTable.rawget("file");
        Object serverfile = regionTable.rawget("serverfile");
        if (name instanceof String nameString && file instanceof String fileString) {
            SpawnRegions.Region region = new SpawnRegions.Region();
            region.name = nameString;
            region.file = fileString;
            return region;
        } else if (name instanceof String nameString && serverfile instanceof String fileString) {
            SpawnRegions.Region region = new SpawnRegions.Region();
            region.name = nameString;
            region.serverfile = fileString;
            return region;
        } else {
            return null;
        }
    }

    private ArrayList<SpawnRegions.Profession> parseProfessionsTable(KahluaTable professionTable) {
        ArrayList<SpawnRegions.Profession> result = null;
        KahluaTableIterator itProfessions = professionTable.iterator();

        while (itProfessions.advance()) {
            Object name = itProfessions.getKey();
            Object pointsObj = itProfessions.getValue();
            if (name instanceof String s && pointsObj instanceof KahluaTable kahluaTable) {
                ArrayList<SpawnRegions.Point> points = this.parsePointsTable(kahluaTable);
                if (points != null) {
                    SpawnRegions.Profession profession = new SpawnRegions.Profession();
                    profession.name = s;
                    profession.points = points;
                    if (result == null) {
                        result = new ArrayList<>();
                    }

                    result.add(profession);
                }
            }
        }

        return result;
    }

    private ArrayList<SpawnRegions.Point> parsePointsTable(KahluaTable pointsTable) {
        ArrayList<SpawnRegions.Point> result = null;
        KahluaTableIterator itPoints = pointsTable.iterator();

        while (itPoints.advance()) {
            if (itPoints.getValue() instanceof KahluaTable kahluaTable) {
                SpawnRegions.Point point = this.parsePointTable(kahluaTable);
                if (point != null) {
                    if (result == null) {
                        result = new ArrayList<>();
                    }

                    result.add(point);
                }
            }
        }

        return result;
    }

    private SpawnRegions.Point parsePointTable(KahluaTable pointTable) {
        if (pointTable.rawget("worldX") != null
            && pointTable.rawget("worldX") instanceof Double worldX
            && pointTable.rawget("worldY") instanceof Double worldY
            && pointTable.rawget("posX") instanceof Double posX
            && pointTable.rawget("posY") instanceof Double posY) {
            SpawnRegions.Point point = new SpawnRegions.Point();
            int cellX = worldX.intValue();
            int cellY = worldY.intValue();
            point.posX = cellX * 300 + posX.intValue();
            point.posY = cellY * 300 + posY.intValue();
            point.posZ = pointTable.rawget("posZ") instanceof Double posZ ? posZ.intValue() : 0;
            return point;
        } else if (pointTable.rawget("posX") instanceof Double posX && pointTable.rawget("posY") instanceof Double posY) {
            SpawnRegions.Point point = new SpawnRegions.Point();
            point.posX = posX.intValue();
            point.posY = posY.intValue();
            point.posZ = pointTable.rawget("posZ") instanceof Double posZ ? posZ.intValue() : 0;
            return point;
        } else {
            return null;
        }
    }

    public ArrayList<SpawnRegions.Region> loadRegionsFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        } else {
            try {
                LuaManager.env.rawset("SpawnRegions", null);
                LuaManager.loaded.remove(file.getAbsolutePath().replace("\\", "/"));
                LuaManager.RunLua(file.getAbsolutePath());
                Object functionObj = LuaManager.env.rawget("SpawnRegions");
                if (functionObj instanceof LuaClosure) {
                    Object[] o = LuaManager.caller.pcall(LuaManager.thread, functionObj);
                    if (o.length > 1 && o[1] instanceof KahluaTable) {
                        ArrayList<SpawnRegions.Region> result = new ArrayList<>();
                        KahluaTableIterator itRegions = ((KahluaTable)o[1]).iterator();

                        while (itRegions.advance()) {
                            if (itRegions.getValue() instanceof KahluaTable kahluaTable) {
                                SpawnRegions.Region region = this.parseRegionTable(kahluaTable);
                                if (region != null) {
                                    result.add(region);
                                }
                            }
                        }

                        return result;
                    }
                }

                return null;
            } catch (Exception var10) {
                var10.printStackTrace();
                return null;
            }
        }
    }

    private String fmtKey(String s) {
        if (s.contains("\\")) {
            s = s.replace("\\", "\\\\");
        }

        if (s.contains("\"")) {
            s = s.replace("\"", "\\\"");
        }

        if (s.contains(" ") || s.contains("\\")) {
            s = "\"" + s + "\"";
        }

        return s.startsWith("\"") ? "[" + s + "]" : s;
    }

    private String fmtValue(String s) {
        if (s.contains("\\")) {
            s = s.replace("\\", "\\\\");
        }

        if (s.contains("\"")) {
            s = s.replace("\"", "\\\"");
        }

        return "\"" + s + "\"";
    }

    public boolean saveRegionsFile(String fileName, ArrayList<SpawnRegions.Region> regions) {
        File file = new File(fileName);
        DebugLog.log("writing " + fileName);

        try {
            boolean var15;
            try (FileWriter fw = new FileWriter(file)) {
                String lineSep = System.lineSeparator();
                fw.write("function SpawnRegions()" + lineSep);
                fw.write("\treturn {" + lineSep);

                for (SpawnRegions.Region region : regions) {
                    if (region.file != null) {
                        fw.write("\t\t{ name = " + this.fmtValue(region.name) + ", file = " + this.fmtValue(region.file) + " }," + lineSep);
                    } else if (region.serverfile != null) {
                        fw.write("\t\t{ name = " + this.fmtValue(region.name) + ", serverfile = " + this.fmtValue(region.serverfile) + " }," + lineSep);
                    } else if (region.professions != null) {
                        fw.write("\t\t{ name = " + this.fmtValue(region.name) + "," + lineSep);
                        fw.write("\t\t\tpoints = {" + lineSep);

                        for (SpawnRegions.Profession profession : region.professions) {
                            fw.write("\t\t\t\t" + this.fmtKey(profession.name) + " = {" + lineSep);

                            for (SpawnRegions.Point pt : profession.points) {
                                fw.write("\t\t\t\t\t{ posX = " + pt.posX + ", posY = " + pt.posY + ", posZ = " + pt.posZ + " }," + lineSep);
                            }

                            fw.write("\t\t\t\t}," + lineSep);
                        }

                        fw.write("\t\t\t}" + lineSep);
                        fw.write("\t\t}," + lineSep);
                    }
                }

                fw.write("\t}" + lineSep);
                fw.write("end" + System.lineSeparator());
                var15 = true;
            }

            return var15;
        } catch (Exception var14) {
            var14.printStackTrace();
            return false;
        }
    }

    public ArrayList<SpawnRegions.Profession> loadPointsFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            return null;
        } else {
            try {
                LuaManager.env.rawset("SpawnPoints", null);
                LuaManager.loaded.remove(file.getAbsolutePath().replace("\\", "/"));
                LuaManager.RunLua(file.getAbsolutePath());
                Object functionObj = LuaManager.env.rawget("SpawnPoints");
                if (functionObj instanceof LuaClosure) {
                    Object[] o = LuaManager.caller.pcall(LuaManager.thread, functionObj);
                    if (o.length > 1 && o[1] instanceof KahluaTable) {
                        return this.parseProfessionsTable((KahluaTable)o[1]);
                    }
                }

                return null;
            } catch (Exception var6) {
                var6.printStackTrace();
                return null;
            }
        }
    }

    public boolean savePointsFile(String fileName, ArrayList<SpawnRegions.Profession> professions) {
        File file = new File(fileName);
        DebugLog.log("writing " + fileName);

        try {
            boolean var13;
            try (FileWriter fw = new FileWriter(file)) {
                String lineSep = System.lineSeparator();
                fw.write("function SpawnPoints()" + lineSep);
                fw.write("\treturn {" + lineSep);

                for (SpawnRegions.Profession profession : professions) {
                    fw.write("\t\t" + this.fmtKey(profession.name) + " = {" + lineSep);

                    for (SpawnRegions.Point pt : profession.points) {
                        fw.write("\t\t\t{ posX = " + pt.posX + ", posY = " + pt.posY + ", posZ = " + pt.posZ + " }," + lineSep);
                    }

                    fw.write("\t\t}," + lineSep);
                }

                fw.write("\t}" + lineSep);
                fw.write("end" + System.lineSeparator());
                var13 = true;
            }

            return var13;
        } catch (Exception var12) {
            var12.printStackTrace();
            return false;
        }
    }

    public KahluaTable loadPointsTable(String fileName) {
        ArrayList<SpawnRegions.Profession> professions = this.loadPointsFile(fileName);
        if (professions == null) {
            return null;
        } else {
            KahluaTable professionsTable = LuaManager.platform.newTable();

            for (int profIndex = 0; profIndex < professions.size(); profIndex++) {
                SpawnRegions.Profession profession = professions.get(profIndex);
                KahluaTable pointsTable = LuaManager.platform.newTable();

                for (int ptIndex = 0; ptIndex < profession.points.size(); ptIndex++) {
                    SpawnRegions.Point pt = profession.points.get(ptIndex);
                    KahluaTable pointTable = LuaManager.platform.newTable();
                    pointTable.rawset("posX", (double)pt.posX);
                    pointTable.rawset("posY", (double)pt.posY);
                    pointTable.rawset("posZ", (double)pt.posZ);
                    pointsTable.rawset(ptIndex + 1, pointTable);
                }

                professionsTable.rawset(profession.name, pointsTable);
            }

            return professionsTable;
        }
    }

    public boolean savePointsTable(String fileName, KahluaTable professionsTable) {
        ArrayList<SpawnRegions.Profession> professions = this.parseProfessionsTable(professionsTable);
        return professions != null ? this.savePointsFile(fileName, professions) : false;
    }

    public ArrayList<SpawnRegions.Region> getDefaultServerRegions() {
        ArrayList<SpawnRegions.Region> result = new ArrayList<>();
        Filter<Path> filter = new Filter<Path>() {
            {
                Objects.requireNonNull(SpawnRegions.this);
            }

            public boolean accept(Path entry) throws IOException {
                return Files.isDirectory(entry) && Files.exists(entry.resolve("spawnpoints.lua"));
            }
        };
        String mapsPath = ZomboidFileSystem.instance.getMediaPath("maps");
        Path dir = FileSystems.getDefault().getPath(mapsPath);
        if (!Files.exists(dir)) {
            return result;
        } else {
            try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(dir, filter)) {
                for (Path path : dstrm) {
                    SpawnRegions.Region region = new SpawnRegions.Region();
                    region.name = path.getFileName().toString();
                    region.file = "media/maps/" + region.name + "/spawnpoints.lua";
                    result.add(region);
                }
            } catch (Exception var11) {
                var11.printStackTrace();
            }

            return result;
        }
    }

    public ArrayList<SpawnRegions.Profession> getDefaultServerPoints() {
        ArrayList<SpawnRegions.Profession> result = new ArrayList<>();
        SpawnRegions.Profession profession = new SpawnRegions.Profession();
        profession.name = CharacterProfession.UNEMPLOYED.getName();
        profession.points = new ArrayList<>();
        result.add(profession);
        SpawnRegions.Point point = new SpawnRegions.Point();
        int cellX = 40;
        int cellY = 22;
        point.posX = 12067;
        point.posY = 6801;
        point.posZ = 0;
        profession.points.add(point);
        return result;
    }

    public static class Point {
        public int posX;
        public int posY;
        public int posZ;
    }

    public static class Profession {
        public String name;
        public ArrayList<SpawnRegions.Point> points;
    }

    public static class Region {
        public String name;
        public String file;
        public String serverfile;
        public ArrayList<SpawnRegions.Profession> professions;
    }
}
