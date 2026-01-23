// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.characters;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameWindow;
import zombie.UsedFromLua;
import zombie.chat.ChatSettings;
import zombie.chat.defaultChats.FactionChat;
import zombie.core.network.ByteBufferWriter;
import zombie.core.random.Rand;
import zombie.core.textures.ColorInfo;
import zombie.network.GameClient;
import zombie.network.ServerOptions;
import zombie.network.chat.ChatServer;

@UsedFromLua
public final class Faction {
    private String name;
    private String owner;
    private String tag;
    private ColorInfo tagColor;
    private final ArrayList<String> players = new ArrayList<>();
    public static ArrayList<Faction> factions = new ArrayList<>();

    public Faction() {
    }

    public Faction(String name, String owner) {
        this.setName(name);
        this.setOwner(owner);
        this.tagColor = new ColorInfo(Rand.Next(0.3F, 1.0F), Rand.Next(0.3F, 1.0F), Rand.Next(0.3F, 1.0F), 1.0F);
    }

    public static Faction createFaction(String name, String owner) {
        if (!factionExist(name)) {
            Faction newFact = new Faction(name, owner);
            factions.add(newFact);
            if (GameClient.client) {
                GameClient.sendFaction(newFact, false);
            }

            return newFact;
        } else {
            return null;
        }
    }

    public static ArrayList<Faction> getFactions() {
        return factions;
    }

    public static boolean canCreateFaction(IsoPlayer player) {
        boolean allowed = ServerOptions.instance.faction.getValue();
        if (allowed
            && ServerOptions.instance.factionDaySurvivedToCreate.getValue() > 0
            && player.getHoursSurvived() / 24.0 < ServerOptions.instance.factionDaySurvivedToCreate.getValue()) {
            allowed = false;
        }

        return allowed;
    }

    public boolean canCreateTag() {
        return this.players.size() + 1 >= ServerOptions.instance.factionPlayersRequiredForTag.getValue();
    }

    public static boolean isInSameFaction(IsoPlayer player, IsoPlayer other) {
        Faction faction = getPlayerFaction(player);
        return faction != null && (faction.isMember(other.getUsername()) || faction.isOwner(other.getUsername()));
    }

    public static boolean isAlreadyInFaction(String username) {
        for (int i = 0; i < factions.size(); i++) {
            Faction fact = factions.get(i);
            if (fact.getOwner().equals(username)) {
                return true;
            }

            for (int x = 0; x < fact.getPlayers().size(); x++) {
                if (fact.getPlayers().get(x).equals(username)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isAlreadyInFaction(IsoPlayer player) {
        return isAlreadyInFaction(player.getUsername());
    }

    public void removePlayer(String player) {
        this.getPlayers().remove(player);
        if (GameClient.client) {
            GameClient.sendFaction(this, false);
        }
    }

    public static boolean factionExist(String name) {
        for (int i = 0; i < factions.size(); i++) {
            if (factions.get(i).getName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    public static boolean tagExist(String name) {
        for (int i = 0; i < factions.size(); i++) {
            if (factions.get(i).getTag() != null && factions.get(i).getTag().equals(name)) {
                return true;
            }
        }

        return false;
    }

    public static Faction getPlayerFaction(IsoPlayer player) {
        for (int i = 0; i < factions.size(); i++) {
            Faction fact = factions.get(i);
            if (fact.getOwner().equals(player.getUsername())) {
                return fact;
            }

            for (int x = 0; x < fact.getPlayers().size(); x++) {
                if (fact.getPlayers().get(x).equals(player.getUsername())) {
                    return fact;
                }
            }
        }

        return null;
    }

    public static Faction getPlayerFaction(String username) {
        for (int i = 0; i < factions.size(); i++) {
            Faction fact = factions.get(i);
            if (fact.getOwner().equals(username)) {
                return fact;
            }

            for (int x = 0; x < fact.getPlayers().size(); x++) {
                if (fact.getPlayers().get(x).equals(username)) {
                    return fact;
                }
            }
        }

        return null;
    }

    public static Faction getFaction(String name) {
        for (int i = 0; i < factions.size(); i++) {
            if (factions.get(i).getName().equals(name)) {
                return factions.get(i);
            }
        }

        return null;
    }

    public void removeFaction() {
        getFactions().remove(this);
        if (GameClient.client) {
            GameClient.sendFaction(this, true);
        }
    }

    public void syncFaction() {
        if (GameClient.client) {
            GameClient.sendFaction(this, false);
        }
    }

    public boolean isOwner(String name) {
        return this.getOwner().equals(name);
    }

    public boolean isMember(String name) {
        for (int x = 0; x < this.getPlayers().size(); x++) {
            if (this.getPlayers().get(x).equals(name)) {
                return true;
            }
        }

        return false;
    }

    public void writeToBuffer(ByteBufferWriter bb, boolean remove) {
        bb.putUTF(this.getName());
        bb.putUTF(this.getOwner());
        bb.putInt(this.getPlayers().size());
        if (this.getTag() != null) {
            bb.putByte((byte)1);
            bb.putUTF(this.getTag());
            bb.putFloat(this.getTagColor().r);
            bb.putFloat(this.getTagColor().g);
            bb.putFloat(this.getTagColor().b);
        } else {
            bb.putByte((byte)0);
        }

        for (String player : this.getPlayers()) {
            bb.putUTF(player);
        }

        bb.putBoolean(remove);
    }

    public void save(ByteBuffer output) {
        GameWindow.WriteString(output, this.getName());
        GameWindow.WriteString(output, this.getOwner());
        output.putInt(this.getPlayers().size());
        if (this.getTag() != null) {
            output.put((byte)1);
            GameWindow.WriteString(output, this.getTag());
            output.putFloat(this.getTagColor().r);
            output.putFloat(this.getTagColor().g);
            output.putFloat(this.getTagColor().b);
        } else {
            output.put((byte)0);
        }

        for (String player : this.getPlayers()) {
            GameWindow.WriteString(output, player);
        }
    }

    public void load(ByteBuffer input, int WorldVersion) {
        this.setName(GameWindow.ReadString(input));
        this.setOwner(GameWindow.ReadString(input));
        int playerSize = input.getInt();
        if (input.get() == 1) {
            this.setTag(GameWindow.ReadString(input));
            this.setTagColor(new ColorInfo(input.getFloat(), input.getFloat(), input.getFloat(), 1.0F));
        } else {
            this.setTagColor(new ColorInfo(Rand.Next(0.3F, 1.0F), Rand.Next(0.3F, 1.0F), Rand.Next(0.3F, 1.0F), 1.0F));
        }

        for (int i = 0; i < playerSize; i++) {
            this.getPlayers().add(GameWindow.ReadString(input));
        }

        if (ChatServer.isInited()) {
            FactionChat chat = ChatServer.getInstance().createFactionChat(this.getName());
            ChatSettings settings = FactionChat.getDefaultSettings();
            settings.setFontColor(this.tagColor.r, this.tagColor.g, this.tagColor.b, this.tagColor.a);
            chat.setSettings(settings);
        }
    }

    public void addPlayer(String pName) {
        for (int i = 0; i < factions.size(); i++) {
            Faction fact = factions.get(i);
            if (fact.getOwner().equals(pName)) {
                return;
            }

            for (int x = 0; x < fact.getPlayers().size(); x++) {
                if (fact.getPlayers().get(x).equals(pName)) {
                    return;
                }
            }
        }

        this.players.add(pName);
        if (GameClient.client) {
            GameClient.sendFaction(this, false);
        }
    }

    public ArrayList<String> getPlayers() {
        return this.players;
    }

    public ColorInfo getTagColor() {
        return this.tagColor;
    }

    public void setTagColor(ColorInfo tagColor) {
        if (tagColor.r < 0.19F) {
            tagColor.r = 0.19F;
        }

        if (tagColor.g < 0.19F) {
            tagColor.g = 0.19F;
        }

        if (tagColor.b < 0.19F) {
            tagColor.b = 0.19F;
        }

        this.tagColor = tagColor;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(String owner) {
        if (this.owner == null) {
            this.owner = owner;
        } else {
            if (!this.isMember(this.owner)) {
                this.getPlayers().add(this.owner);
                this.getPlayers().remove(owner);
            }

            this.owner = owner;
        }
    }
}
