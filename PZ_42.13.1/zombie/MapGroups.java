// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import zombie.core.Core;
import zombie.core.Translator;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.gameStates.ChooseGameInfo;
import zombie.iso.IsoWorld;
import zombie.modding.ActiveMods;
import zombie.util.list.PZArrayUtil;

@UsedFromLua
public final class MapGroups {
    private final ArrayList<MapGroups.MapGroup> groups = new ArrayList<>();
    private final ArrayList<MapGroups.MapDirectory> realDirectories = new ArrayList<>();

    private static ArrayList<String> getVanillaMapDirectories(boolean includeChallenges) {
        ArrayList<String> result = new ArrayList<>();
        File mapsFile = ZomboidFileSystem.instance.getMediaFile("maps");
        String[] internalNames = mapsFile.list();
        if (internalNames != null) {
            for (int i = 0; i < internalNames.length; i++) {
                String directoryName = internalNames[i];
                if (directoryName.equalsIgnoreCase("challengemaps")) {
                    if (includeChallenges) {
                        try (DirectoryStream<Path> dstrm = Files.newDirectoryStream(
                                Paths.get(mapsFile.getPath(), directoryName), pathx -> Files.isDirectory(pathx) && Files.exists(pathx.resolve("map.info"))
                            )) {
                            for (Path path : dstrm) {
                                result.add(directoryName + "/" + path.getFileName().toString());
                            }
                        } catch (Exception var11) {
                        }
                    }
                } else {
                    result.add(directoryName);
                }
            }
        }

        return result;
    }

    public static String addMissingVanillaDirectories(String mapName) {
        ArrayList<String> vanillaDirs = getVanillaMapDirectories(false);
        boolean usesVanilla = false;
        String[] ss = mapName.split(";");

        for (String name : ss) {
            name = name.trim();
            if (!name.isEmpty() && vanillaDirs.contains(name)) {
                usesVanilla = true;
                break;
            }
        }

        if (!usesVanilla) {
            return mapName;
        } else {
            ArrayList<String> names = new ArrayList<>();

            for (String namex : ss) {
                namex = namex.trim();
                if (!namex.isEmpty()) {
                    names.add(namex);
                }
            }

            for (String vanillaDir : vanillaDirs) {
                if (!names.contains(vanillaDir)) {
                    names.add(vanillaDir);
                }
            }

            String result = "";

            for (String namexx : names) {
                if (!result.isEmpty()) {
                    result = result + ";";
                }

                result = result + namexx;
            }

            return result;
        }
    }

    public void createGroups() {
        this.createGroups(ActiveMods.getById("currentGame"), true);
    }

    public void createGroups(ActiveMods activeMods, boolean includeVanilla) {
        this.createGroups(activeMods, includeVanilla, false);
    }

    public void createGroups(ActiveMods activeMods, boolean includeVanilla, boolean includeChallenges) {
        this.groups.clear();
        this.realDirectories.clear();

        for (String modID : activeMods.getMods()) {
            ChooseGameInfo.Mod modDetails = ChooseGameInfo.getAvailableModDetails(modID);
            if (modDetails != null) {
                File fo = new File(modDetails.getCommonDir() + "/media/maps/");
                if (fo.exists()) {
                    String[] internalNames = fo.list();
                    if (internalNames != null) {
                        for (int i = 0; i < internalNames.length; i++) {
                            String directoryName = internalNames[i];
                            if (directoryName.equalsIgnoreCase("challengemaps")) {
                                if (includeChallenges) {
                                }
                            } else {
                                this.handleMapDirectory(directoryName, modDetails.getCommonDir() + "/media/maps/" + directoryName);
                            }
                        }

                        fo = new File(modDetails.getVersionDir() + "/media/maps/");
                        if (fo.exists()) {
                            internalNames = fo.list();
                            if (internalNames != null) {
                                for (int ix = 0; ix < internalNames.length; ix++) {
                                    String directoryName = internalNames[ix];
                                    if (directoryName.equalsIgnoreCase("challengemaps")) {
                                        if (includeChallenges) {
                                        }
                                    } else {
                                        this.handleMapDirectory(directoryName, modDetails.getVersionDir() + "/media/maps/" + directoryName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (includeVanilla) {
            ArrayList<String> vanillaMaps = getVanillaMapDirectories(includeChallenges);
            String mapsPath = ZomboidFileSystem.instance.getMediaPath("maps");

            for (String directoryName : vanillaMaps) {
                this.handleMapDirectory(directoryName, mapsPath + File.separator + directoryName);
            }
        }

        for (MapGroups.MapDirectory mapDir : this.realDirectories) {
            ArrayList<MapGroups.MapDirectory> directories = new ArrayList<>();
            this.getDirsRecursively(mapDir, directories);
            MapGroups.MapGroup group = this.findGroupWithAnyOfTheseDirectories(directories);
            if (group == null) {
                group = new MapGroups.MapGroup();
                this.groups.add(group);
            }

            for (MapGroups.MapDirectory mapDir2 : directories) {
                if (!group.hasDirectory(mapDir2.name)) {
                    group.addDirectory(mapDir2);
                }
            }
        }

        for (MapGroups.MapGroup group : this.groups) {
            group.setPriority();
        }

        for (MapGroups.MapGroup group : this.groups) {
            group.setOrder(activeMods);
        }

        if (Core.debug) {
            int ixx = 1;

            for (MapGroups.MapGroup group : this.groups) {
                DebugLog.log("MapGroup " + ixx + "/" + this.groups.size());

                for (MapGroups.MapDirectory mapDir : group.directories) {
                    DebugLog.log("  " + mapDir.name);
                }

                ixx++;
            }

            DebugLog.log("-----");
        }
    }

    private void getDirsRecursively(MapGroups.MapDirectory mapDir, ArrayList<MapGroups.MapDirectory> result) {
        if (!result.contains(mapDir)) {
            result.add(mapDir);

            for (String lotDir : mapDir.lotDirs) {
                for (MapGroups.MapDirectory mapDir2 : this.realDirectories) {
                    if (mapDir2.name.equals(lotDir)) {
                        this.getDirsRecursively(mapDir2, result);
                        break;
                    }
                }
            }
        }
    }

    public int getNumberOfGroups() {
        return this.groups.size();
    }

    public ArrayList<String> getMapDirectoriesInGroup(int groupIndex) {
        if (groupIndex >= 0 && groupIndex < this.groups.size()) {
            ArrayList<String> result = new ArrayList<>();

            for (MapGroups.MapDirectory mapDir : this.groups.get(groupIndex).directories) {
                result.add(mapDir.name);
            }

            return result;
        } else {
            throw new RuntimeException("invalid MapGroups index " + groupIndex);
        }
    }

    public void setWorld(int groupIndex) {
        ArrayList<String> mapDirs = this.getMapDirectoriesInGroup(groupIndex);
        String mapName = "";

        for (int i = 0; i < mapDirs.size(); i++) {
            mapName = mapName + mapDirs.get(i);
            if (i < mapDirs.size() - 1) {
                mapName = mapName + ";";
            }
        }

        IsoWorld.instance.setMap(mapName);
    }

    private void handleMapDirectory(String directoryName, String path) {
        ArrayList<String> lotDirs = this.getLotDirectories(path);
        if (lotDirs != null) {
            MapGroups.MapDirectory mapDir = new MapGroups.MapDirectory(directoryName, path, lotDirs);
            this.realDirectories.add(mapDir);
        }
    }

    private ArrayList<String> getLotDirectories(String path) {
        File file = new File(path + "/map.info");
        if (!file.exists()) {
            return null;
        } else {
            ArrayList<String> lotDirs = new ArrayList<>();

            try {
                String inputLine;
                try (
                    FileReader reader = new FileReader(file.getAbsolutePath());
                    BufferedReader br = new BufferedReader(reader);
                ) {
                    while ((inputLine = br.readLine()) != null) {
                        inputLine = inputLine.trim();
                        if (inputLine.startsWith("lots=")) {
                            lotDirs.add(inputLine.replace("lots=", "").trim());
                        }
                    }
                }

                return lotDirs;
            } catch (Exception var12) {
                ExceptionLogger.logException(var12);
                return null;
            }
        }
    }

    private MapGroups.MapGroup findGroupWithAnyOfTheseDirectories(ArrayList<MapGroups.MapDirectory> directories) {
        for (MapGroups.MapGroup group : this.groups) {
            if (group.hasAnyOfTheseDirectories(directories)) {
                return group;
            }
        }

        return null;
    }

    public ArrayList<String> getAllMapsInOrder() {
        ArrayList<String> result = new ArrayList<>();

        for (MapGroups.MapGroup group : this.groups) {
            for (MapGroups.MapDirectory mapDir : group.directories) {
                result.add(mapDir.name);
            }
        }

        return result;
    }

    public boolean checkMapConflicts() {
        boolean hasConflicts = false;

        for (MapGroups.MapGroup group : this.groups) {
            hasConflicts |= group.checkMapConflicts();
        }

        return hasConflicts;
    }

    public ArrayList<String> getMapConflicts(String mapName) {
        for (MapGroups.MapGroup group : this.groups) {
            MapGroups.MapDirectory mapDir = group.getDirectoryByName(mapName);
            if (mapDir != null) {
                ArrayList<String> result = new ArrayList<>();
                result.addAll(mapDir.conflicts);
                return result;
            }
        }

        return null;
    }

    private class MapDirectory {
        String name;
        String path;
        ArrayList<String> lotDirs;
        ArrayList<String> conflicts;

        public MapDirectory(final String name, final String path) {
            Objects.requireNonNull(MapGroups.this);
            super();
            this.lotDirs = new ArrayList<>();
            this.conflicts = new ArrayList<>();
            this.name = name;
            this.path = path;
        }

        public MapDirectory(final String name, final String path, final ArrayList<String> lotDirs) {
            Objects.requireNonNull(MapGroups.this);
            super();
            this.lotDirs = new ArrayList<>();
            this.conflicts = new ArrayList<>();
            this.name = name;
            this.path = path;
            PZArrayUtil.addAll(this.lotDirs, lotDirs);
        }

        public void getLotHeaders(ArrayList<String> result) {
            File fo = new File(this.path);
            if (fo.isDirectory()) {
                String[] internalNames = fo.list();
                if (internalNames != null) {
                    for (int i = 0; i < internalNames.length; i++) {
                        if (internalNames[i].endsWith(".lotheader")) {
                            result.add(internalNames[i]);
                        }
                    }
                }
            }
        }
    }

    private class MapGroup {
        private final LinkedList<MapGroups.MapDirectory> directories;

        private MapGroup() {
            Objects.requireNonNull(MapGroups.this);
            super();
            this.directories = new LinkedList<>();
        }

        void addDirectory(String directoryName, String path) {
            assert !this.hasDirectory(directoryName);

            MapGroups.MapDirectory mapDir = MapGroups.this.new MapDirectory(directoryName, path);
            this.directories.add(mapDir);
        }

        void addDirectory(String directoryName, String path, ArrayList<String> lotDirs) {
            assert !this.hasDirectory(directoryName);

            MapGroups.MapDirectory mapDir = MapGroups.this.new MapDirectory(directoryName, path, lotDirs);
            this.directories.add(mapDir);
        }

        void addDirectory(MapGroups.MapDirectory mapDir) {
            assert !this.hasDirectory(mapDir.name);

            this.directories.add(mapDir);
        }

        MapGroups.MapDirectory getDirectoryByName(String name) {
            for (MapGroups.MapDirectory mapDir : this.directories) {
                if (mapDir.name.equals(name)) {
                    return mapDir;
                }
            }

            return null;
        }

        boolean hasDirectory(String name) {
            return this.getDirectoryByName(name) != null;
        }

        boolean hasAnyOfTheseDirectories(ArrayList<MapGroups.MapDirectory> anyOf) {
            for (MapGroups.MapDirectory mapDir : anyOf) {
                if (this.directories.contains(mapDir)) {
                    return true;
                }
            }

            return false;
        }

        boolean isReferencedByOtherMaps(MapGroups.MapDirectory mapDir) {
            for (MapGroups.MapDirectory mapDir2 : this.directories) {
                if (mapDir != mapDir2 && mapDir2.lotDirs.contains(mapDir.name)) {
                    return true;
                }
            }

            return false;
        }

        void getDirsRecursively(MapGroups.MapDirectory mapDir, ArrayList<String> result) {
            if (!result.contains(mapDir.name)) {
                result.add(mapDir.name);

                for (String lotDir : mapDir.lotDirs) {
                    MapGroups.MapDirectory mapDir2 = this.getDirectoryByName(lotDir);
                    if (mapDir2 != null) {
                        this.getDirsRecursively(mapDir2, result);
                    }
                }
            }
        }

        void setPriority() {
            for (MapGroups.MapDirectory mapDir : new ArrayList<>(this.directories)) {
                if (!this.isReferencedByOtherMaps(mapDir)) {
                    ArrayList<String> priorityList = new ArrayList<>();
                    this.getDirsRecursively(mapDir, priorityList);
                    this.setPriority(priorityList);
                }
            }
        }

        void setPriority(List<String> priorityList) {
            ArrayList<MapGroups.MapDirectory> sorted = new ArrayList<>(priorityList.size());

            for (String name : priorityList) {
                if (this.hasDirectory(name)) {
                    sorted.add(this.getDirectoryByName(name));
                }
            }

            for (int i = 0; i < this.directories.size(); i++) {
                MapGroups.MapDirectory mapDir1 = this.directories.get(i);
                if (priorityList.contains(mapDir1.name)) {
                    this.directories.set(i, sorted.remove(0));
                }
            }
        }

        void setOrder(ActiveMods activeMods) {
            if (!activeMods.getMapOrder().isEmpty()) {
                this.setPriority(activeMods.getMapOrder());
            }
        }

        boolean checkMapConflicts() {
            HashMap<String, ArrayList<String>> lotHeaderToDir = new HashMap<>();
            ArrayList<String> lotHeaders = new ArrayList<>();

            for (MapGroups.MapDirectory mapDir : this.directories) {
                mapDir.conflicts.clear();
                lotHeaders.clear();
                mapDir.getLotHeaders(lotHeaders);

                for (String lotHeader : lotHeaders) {
                    if (!lotHeaderToDir.containsKey(lotHeader)) {
                        lotHeaderToDir.put(lotHeader, new ArrayList<>());
                    }

                    lotHeaderToDir.get(lotHeader).add(mapDir.name);
                }
            }

            boolean hasConflicts = false;

            for (String lotHeader : lotHeaderToDir.keySet()) {
                ArrayList<String> mapNames = lotHeaderToDir.get(lotHeader);
                if (mapNames.size() > 1) {
                    for (int i = 0; i < mapNames.size(); i++) {
                        MapGroups.MapDirectory mapDir = this.getDirectoryByName(mapNames.get(i));

                        for (int j = 0; j < mapNames.size(); j++) {
                            if (i != j) {
                                String conflict = Translator.getText("UI_MapConflict", mapDir.name, mapNames.get(j), lotHeader);
                                mapDir.conflicts.add(conflict);
                                hasConflicts = true;
                            }
                        }
                    }
                }
            }

            return hasConflicts;
        }
    }
}
