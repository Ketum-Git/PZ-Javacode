// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.nio.ByteBuffer;
import java.util.HashMap;
import zombie.UsedFromLua;
import zombie.debug.DebugType;

@UsedFromLua
public class PlayerCraftHistory {
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
        DebugType.CraftLogic.println("PlayerCraftHistory updated: %s (craftCount: %d, lastCraftTime %f)", craftType, entry.craftCount, entry.lastCraftTime);
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
