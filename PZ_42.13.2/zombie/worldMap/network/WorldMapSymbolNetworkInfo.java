// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.worldMap.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Objects;
import zombie.GameWindow;
import zombie.util.StringUtils;
import zombie.util.io.BitHeader;
import zombie.util.io.BitHeaderRead;
import zombie.util.io.BitHeaderWrite;

public final class WorldMapSymbolNetworkInfo {
    private int id;
    private String author;
    private boolean everyone;
    private boolean faction;
    private boolean safehouse;
    private ArrayList<String> players;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof WorldMapSymbolNetworkInfo other) {
            if (!this.author.equals(other.author)) {
                return false;
            } else {
                return this.everyone == other.everyone && this.faction == other.faction && this.safehouse == other.safehouse
                    ? Objects.equals(this.players, other.players)
                    : false;
            }
        } else {
            return false;
        }
    }

    public void setID(int id) {
        this.id = id;
    }

    public int getID() {
        return this.id;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(String username) {
        if (StringUtils.isNullOrWhitespace(username)) {
            throw new IllegalStateException("username can't be null or empty");
        } else {
            this.author = username;
        }
    }

    public boolean isVisibleToEveryone() {
        return this.everyone;
    }

    public void setVisibleToEveryone(boolean b) {
        this.everyone = b;
    }

    public boolean isVisibleToFaction() {
        return this.faction;
    }

    public void setVisibleToFaction(boolean b) {
        this.faction = b;
    }

    public boolean isVisibleToSafehouse() {
        return this.safehouse;
    }

    public void setVisibleToSafehouse(boolean b) {
        this.safehouse = b;
    }

    public void addPlayer(String username) {
        if (!StringUtils.isNullOrWhitespace(username)) {
            if (!this.hasPlayer(username)) {
                if (this.players == null) {
                    this.players = new ArrayList<>();
                }

                this.players.add(username);
            }
        }
    }

    public int getPlayerCount() {
        return this.players == null ? 0 : this.players.size();
    }

    public String getPlayerByIndex(int index) {
        return this.players.get(index);
    }

    public boolean hasPlayer(String username) {
        return this.players == null ? false : this.players.contains(username);
    }

    public void clearPlayers() {
        if (this.players != null) {
            this.players.clear();
        }
    }

    public void save(ByteBuffer bb) throws IOException {
        bb.putInt(this.id);
        GameWindow.WriteStringUTF(bb, this.author);
        BitHeaderWrite bits = BitHeader.allocWrite(BitHeader.HeaderSize.Byte, bb);
        if (this.everyone) {
            bits.addFlags(1);
        }

        if (this.faction) {
            bits.addFlags(2);
        }

        if (this.safehouse) {
            bits.addFlags(4);
        }

        if (this.getPlayerCount() > 0) {
            bits.addFlags(8);
            bb.put((byte)this.getPlayerCount());

            for (int i = 0; i < this.players.size(); i++) {
                GameWindow.WriteStringUTF(bb, this.players.get(i));
            }
        }

        bits.write();
        bits.release();
    }

    public void load(ByteBuffer bb, int WorldVersion, int SymbolsVersion) throws IOException {
        this.id = bb.getInt();
        this.author = GameWindow.ReadStringUTF(bb);
        BitHeaderRead bits = BitHeader.allocRead(BitHeader.HeaderSize.Byte, bb);
        this.everyone = bits.hasFlags(1);
        this.faction = bits.hasFlags(2);
        this.safehouse = bits.hasFlags(4);
        this.clearPlayers();
        if (bits.hasFlags(8)) {
            int count = bb.get();

            for (int i = 0; i < count; i++) {
                this.addPlayer(GameWindow.ReadStringUTF(bb));
            }
        }

        bits.release();
    }
}
