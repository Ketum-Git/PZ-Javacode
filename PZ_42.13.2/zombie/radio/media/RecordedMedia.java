// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.radio.media;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaEventManager;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.world.WorldDictionary;

/**
 * TurboTuTone.
 */
@UsedFromLua
public class RecordedMedia {
    public static boolean disableLineLearning;
    private static final int SPAWN_COMMON = 0;
    private static final int SPAWN_RARE = 1;
    private static final int SPAWN_EXCEPTIONAL = 2;
    public static final int VERSION1 = 1;
    public static final int VERSION2 = 2;
    public static final int VERSION = 2;
    public static final String SAVE_FILE = "recorded_media.bin";
    private final ArrayList<String> indexes = new ArrayList<>();
    private static final ArrayList<String> indexesFromServer = new ArrayList<>();
    private final Map<String, MediaData> mediaDataMap = new HashMap<>();
    private final Map<String, ArrayList<MediaData>> categorizedMap = new HashMap<>();
    private final ArrayList<String> categories = new ArrayList<>();
    private final ArrayList<String> legacyListenedLines = new ArrayList<>();
    private final HashSet<Short> homeVhsSpawned = new HashSet<>();
    private final Map<Integer, ArrayList<MediaData>> retailVhsSpawnTable = new HashMap<>();
    private final Map<Integer, ArrayList<MediaData>> retailCdSpawnTable = new HashMap<>();
    private boolean requiresSaving = true;

    public void init() {
        try {
            this.load();
        } catch (Exception var5) {
            var5.printStackTrace();
        }

        LuaEventManager.triggerEvent("OnInitRecordedMedia", this);
        this.retailCdSpawnTable.put(0, new ArrayList<>());
        this.retailCdSpawnTable.put(1, new ArrayList<>());
        this.retailCdSpawnTable.put(2, new ArrayList<>());
        this.retailVhsSpawnTable.put(0, new ArrayList<>());
        this.retailVhsSpawnTable.put(1, new ArrayList<>());
        this.retailVhsSpawnTable.put(2, new ArrayList<>());
        ArrayList<MediaData> list = this.categorizedMap.get("CDs");
        if (list != null) {
            for (MediaData data : list) {
                if (data.getSpawning() == 1) {
                    this.retailCdSpawnTable.get(1).add(data);
                } else if (data.getSpawning() == 2) {
                    this.retailCdSpawnTable.get(2).add(data);
                } else {
                    this.retailCdSpawnTable.get(0).add(data);
                }
            }
        } else {
            DebugLog.General.error("categorizedMap with CDs is empty");
        }

        list = this.categorizedMap.get("Retail-VHS");
        if (list != null) {
            for (MediaData datax : list) {
                if (datax.getSpawning() == 1) {
                    this.retailVhsSpawnTable.get(1).add(datax);
                } else if (datax.getSpawning() == 2) {
                    this.retailVhsSpawnTable.get(2).add(datax);
                } else {
                    this.retailVhsSpawnTable.get(0).add(datax);
                }
            }
        } else {
            DebugLog.General.error("categorizedMap with Retail-VHS is empty");
        }

        try {
            this.save();
        } catch (Exception var4) {
            var4.printStackTrace();
        }
    }

    public static byte getMediaTypeForCategory(String category) {
        if (category == null) {
            return -1;
        } else {
            return (byte)(category.equalsIgnoreCase("cds") ? 0 : 1);
        }
    }

    public ArrayList<String> getCategories() {
        return this.categories;
    }

    public ArrayList<MediaData> getAllMediaForType(byte type) {
        ArrayList<MediaData> media = new ArrayList<>();

        for (Entry<String, MediaData> entry : this.mediaDataMap.entrySet()) {
            if (entry.getValue().getMediaType() == type) {
                media.add(entry.getValue());
            }
        }

        media.sort(new RecordedMedia.MediaNameSorter());
        return media;
    }

    public ArrayList<MediaData> getAllMediaForCategory(String category) {
        ArrayList<MediaData> media = new ArrayList<>();

        for (Entry<String, MediaData> entry : this.mediaDataMap.entrySet()) {
            if (category.equalsIgnoreCase(entry.getValue().getCategory())) {
                media.add(entry.getValue());
            }
        }

        media.sort(new RecordedMedia.MediaNameSorter());
        return media;
    }

    public MediaData register(String category, String id, String itemDisplayName, int spawning) {
        if (this.mediaDataMap.containsKey(id)) {
            DebugLog.log("RecordeMedia -> MediaData id already exists : " + id);
            return null;
        } else {
            if (spawning < 0) {
                spawning = 0;
            }

            MediaData mediaData = new MediaData(id, itemDisplayName, spawning);
            this.mediaDataMap.put(id, mediaData);
            mediaData.setCategory(category);
            if (!this.categorizedMap.containsKey(category)) {
                this.categorizedMap.put(category, new ArrayList<>());
                this.categories.add(category);
            }

            this.categorizedMap.get(category).add(mediaData);
            short index;
            if (this.indexes.contains(id)) {
                index = (short)this.indexes.indexOf(id);
            } else {
                index = (short)this.indexes.size();
                this.indexes.add(id);
            }

            mediaData.setIndex(index);
            this.requiresSaving = true;
            return mediaData;
        }
    }

    public MediaData getMediaDataFromIndex(short index) {
        return index >= 0 && index < this.indexes.size() ? this.getMediaData(this.indexes.get(index)) : null;
    }

    public short getIndexForMediaData(MediaData data) {
        return (short)this.indexes.indexOf(data.getId());
    }

    public MediaData getMediaData(String id) {
        return this.mediaDataMap.get(id);
    }

    public MediaData getRandomFromCategory(String cat) {
        if (this.categorizedMap.containsKey(cat)) {
            MediaData data = null;
            if (cat.equalsIgnoreCase("cds")) {
                int chance = Rand.Next(0, 1000);
                if (chance < 100) {
                    if (!this.retailCdSpawnTable.get(2).isEmpty()) {
                        data = this.retailCdSpawnTable.get(2).get(Rand.Next(0, this.retailCdSpawnTable.get(2).size()));
                    }
                } else if (chance < 400) {
                    if (!this.retailCdSpawnTable.get(1).isEmpty()) {
                        data = this.retailCdSpawnTable.get(1).get(Rand.Next(0, this.retailCdSpawnTable.get(1).size()));
                    }
                } else {
                    data = this.retailCdSpawnTable.get(0).get(Rand.Next(0, this.retailCdSpawnTable.get(0).size()));
                }

                if (data != null) {
                    return data;
                }

                return this.retailCdSpawnTable.get(0).get(Rand.Next(0, this.retailCdSpawnTable.get(0).size()));
            }

            if (cat.equalsIgnoreCase("retail-vhs")) {
                int chancex = Rand.Next(0, 1000);
                if (chancex < 100) {
                    if (!this.retailVhsSpawnTable.get(2).isEmpty()) {
                        data = this.retailVhsSpawnTable.get(2).get(Rand.Next(0, this.retailVhsSpawnTable.get(2).size()));
                    }
                } else if (chancex < 400) {
                    if (!this.retailVhsSpawnTable.get(1).isEmpty()) {
                        data = this.retailVhsSpawnTable.get(1).get(Rand.Next(0, this.retailVhsSpawnTable.get(1).size()));
                    }
                } else {
                    data = this.retailVhsSpawnTable.get(0).get(Rand.Next(0, this.retailVhsSpawnTable.get(0).size()));
                }

                if (data != null) {
                    return data;
                }

                return this.retailVhsSpawnTable.get(0).get(Rand.Next(0, this.retailVhsSpawnTable.get(0).size()));
            }

            if (cat.equalsIgnoreCase("home-vhs")) {
                int chancexx = Rand.Next(0, 1000);
                if (chancexx < 200) {
                    ArrayList<MediaData> list = this.categorizedMap.get("Home-VHS");
                    data = list.get(Rand.Next(0, list.size()));
                    if (!this.homeVhsSpawned.contains(data.getIndex())) {
                        this.homeVhsSpawned.add(data.getIndex());
                        this.requiresSaving = true;
                        return data;
                    }
                }
            }
        }

        return null;
    }

    public void load() throws IOException {
        this.indexes.clear();
        if (GameClient.client) {
            this.indexes.addAll(indexesFromServer);
            indexesFromServer.clear();
        }

        if (!Core.getInstance().isNoSave()) {
            String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("recorded_media.bin");
            File path = new File(fileName);
            if (!path.exists()) {
                if (!WorldDictionary.isIsNewGame()) {
                    DebugLog.log("RecordedMedia data file is missing from world folder.");
                }
            } else {
                try (FileInputStream inStream = new FileInputStream(path)) {
                    DebugLog.DetailedInfo.trace("Loading Recorded Media:" + fileName);
                    ByteBuffer bb = ByteBuffer.allocate((int)path.length());
                    bb.clear();
                    int len = inStream.read(bb.array());
                    bb.limit(len);
                    int version = bb.getInt();
                    int size = bb.getInt();

                    for (int i = 0; i < size; i++) {
                        String id = GameWindow.ReadString(bb);
                        if (!GameClient.client) {
                            this.indexes.add(id);
                        }
                    }

                    if (version == 1) {
                        size = bb.getInt();

                        for (int ix = 0; ix < size; ix++) {
                            String id = GameWindow.ReadString(bb);
                            this.legacyListenedLines.add(id);
                        }
                    }

                    size = bb.getInt();

                    for (int ix = 0; ix < size; ix++) {
                        this.homeVhsSpawned.add(bb.getShort());
                    }
                } catch (Exception var12) {
                    var12.printStackTrace();
                }
            }
        }
    }

    public void save() throws IOException {
        if (!Core.getInstance().isNoSave() && this.requiresSaving) {
            try {
                int size = 0;
                size += this.indexes.size() * 40;
                size += this.homeVhsSpawned.size() * 2;
                size += 512;
                byte[] bytes = new byte[size];
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                bb.putInt(2);
                bb.putInt(this.indexes.size());

                for (int i = 0; i < this.indexes.size(); i++) {
                    GameWindow.WriteString(bb, this.indexes.get(i));
                }

                bb.putInt(this.homeVhsSpawned.size());
                Short[] indexlist = this.homeVhsSpawned.toArray(new Short[0]);

                for (int i = 0; i < indexlist.length; i++) {
                    bb.putShort(indexlist[i]);
                }

                bb.flip();
                String fileName = ZomboidFileSystem.instance.getFileNameInCurrentSave("recorded_media.bin");
                File path = new File(fileName);
                DebugLog.DetailedInfo.trace("Saving Recorded Media:" + fileName);
                FileOutputStream output = new FileOutputStream(path);
                output.getChannel().truncate(0L);
                output.write(bb.array(), 0, bb.limit());
                output.flush();
                output.close();
            } catch (Exception var8) {
                var8.printStackTrace();
            }

            this.requiresSaving = false;
        }
    }

    public static String toAscii(String string) {
        StringBuilder sb = new StringBuilder(string.length());
        string = Normalizer.normalize(string, Form.NFD);

        for (char c : string.toCharArray()) {
            if (c <= 127) {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    public boolean hasListenedToLine(IsoPlayer player, String guid) {
        return player.isKnownMediaLine(guid);
    }

    public boolean hasListenedToAll(IsoPlayer player, MediaData mediaData) {
        if (player == null) {
            player = IsoPlayer.players[0];
        }

        if (player != null && mediaData != null) {
            for (int i = 0; i < mediaData.getLineCount(); i++) {
                MediaData.MediaLineData lineData = mediaData.getLine(i);
                if (!player.isKnownMediaLine(lineData.getTextGuid())) {
                    return false;
                }
            }

            return mediaData.getLineCount() > 0;
        } else {
            return false;
        }
    }

    public void sendRequestData(ByteBuffer bb) {
        bb.putInt(this.indexes.size());

        for (int i = 0; i < this.indexes.size(); i++) {
            GameWindow.WriteStringUTF(bb, this.indexes.get(i));
        }
    }

    public static void receiveRequestData(ByteBuffer bb) {
        indexesFromServer.clear();
        int count = bb.getInt();

        for (int i = 0; i < count; i++) {
            indexesFromServer.add(GameWindow.ReadStringUTF(bb));
        }
    }

    public void handleLegacyListenedLines(IsoPlayer player) {
        if (!this.legacyListenedLines.isEmpty()) {
            if (player != null) {
                for (String guid : this.legacyListenedLines) {
                    player.addKnownMediaLine(guid);
                }
            }

            this.legacyListenedLines.clear();
        }
    }

    public static class MediaNameSorter implements Comparator<MediaData> {
        public int compare(MediaData o1, MediaData o2) {
            return o1.getTranslatedItemDisplayName().compareToIgnoreCase(o2.getTranslatedItemDisplayName());
        }
    }
}
