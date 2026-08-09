// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import zombie.UsedFromLua;
import zombie.debug.DebugType;

@UsedFromLua
public class PlayerCraftHistory {
    private static final int MAX_HISTORY_SIZE = 20;
    private static final int MAX_RETAINED_ENTRIES = 10;
    private final IsoPlayer player;
    private final HashMap<String, PlayerCraftHistory.CraftHistoryEntry> craftHistory;
    private static final PlayerCraftHistory.CraftHistoryEntry craftHistoryDefaultEntry = new PlayerCraftHistory.CraftHistoryEntry();

    public PlayerCraftHistory(IsoPlayer player) {
        if (player == null) {
            throw new NullPointerException();
        } else {
            this.player = player;
            this.craftHistory = new HashMap<>();
        }
    }

    public PlayerCraftHistory.CraftHistoryEntry getCraftHistoryFor(String craftType) {
        return this.craftHistory.containsKey(craftType) ? this.craftHistory.get(craftType) : craftHistoryDefaultEntry;
    }

    public void addCraftHistoryCraftedEvent(String craftType) {
        PlayerCraftHistory.CraftHistoryEntry entry = this.craftHistory.get(craftType);
        if (entry == null) {
            entry = new PlayerCraftHistory.CraftHistoryEntry();
            this.craftHistory.put(craftType, entry);
        }

        entry.craftCount++;
        entry.lastCraftTime = this.player.getHoursSurvived();
        if (this.craftHistory.size() > 20) {
            this.cleanupHistory();
        }

        DebugType.CraftLogic.println("PlayerCraftHistory updated: %s (craftCount: %d, lastCraftTime %f)", craftType, entry.craftCount, entry.lastCraftTime);
    }

    public void cleanupHistory() {
        Map<String, PlayerCraftHistory.CraftHistoryEntry> result = new HashMap<>();
        List<SimpleEntry<String, PlayerCraftHistory.CraftHistoryEntry>> entries = new ArrayList<>();

        for (String key : this.craftHistory.keySet()) {
            entries.add(new SimpleEntry<>(key, this.craftHistory.get(key)));
        }

        entries.sort(Comparator.comparing(e -> e.getValue().lastCraftTime));
        result.put(entries.getLast().getKey(), entries.getLast().getValue());
        entries.sort(Comparator.comparing(e -> e.getValue().craftCount));

        for (int i = 0; i < 10; i++) {
            result.put(entries.getLast().getKey(), entries.getLast().getValue());
            entries.removeLast();
        }

        this.craftHistory.clear();
        this.craftHistory.putAll(result);
    }

    public void save(ByteBuffer output) {
        output.putInt(this.craftHistory.size());

        for (String key : this.craftHistory.keySet()) {
            output.putInt(key.length());

            for (int i = 0; i < key.length(); i++) {
                output.putChar(key.charAt(i));
            }

            PlayerCraftHistory.CraftHistoryEntry value = this.craftHistory.get(key);
            output.putInt(value.getCraftCount());
            output.putDouble(value.getLastCraftTime());
        }
    }

    public void load(ByteBuffer input) {
        this.craftHistory.clear();
        int entryCount = input.getInt();

        for (int i = 0; i < entryCount; i++) {
            int keyCharCount = input.getInt();
            char[] keyChars = new char[keyCharCount];

            for (int j = 0; j < keyCharCount; j++) {
                keyChars[j] = input.getChar();
            }

            String key = String.valueOf(keyChars);
            PlayerCraftHistory.CraftHistoryEntry value = new PlayerCraftHistory.CraftHistoryEntry();
            value.craftCount = input.getInt();
            value.lastCraftTime = input.getDouble();
            this.craftHistory.put(key, value);
        }
    }

    public static final class CraftHistoryEntry {
        private int craftCount;
        private double lastCraftTime;

        public int getCraftCount() {
            return this.craftCount;
        }

        public double getLastCraftTime() {
            return this.lastCraftTime;
        }
    }
}
