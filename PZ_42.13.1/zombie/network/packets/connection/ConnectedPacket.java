// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.GameWindow;
import zombie.SandboxOptions;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.characters.Role;
import zombie.characters.SurvivorDesc;
import zombie.characters.SurvivorFactory;
import zombie.characters.AttachedItems.AttachedItem;
import zombie.chat.ChatManager;
import zombie.core.Color;
import zombie.core.logger.ExceptionLogger;
import zombie.core.math.PZMath;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.core.raknet.VoiceManager;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.znet.SteamUtils;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.inventory.InventoryItemFactory;
import zombie.iso.IsoWorld;
import zombie.iso.areas.SafeHouse;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerOptions;
import zombie.network.packets.INetworkPacket;
import zombie.savefile.AccountDBHelper;

@PacketSetting(ordering = 0, priority = 1, reliability = 3, requiredCapability = Capability.LoginOnServer, handlingType = 2)
public class ConnectedPacket implements INetworkPacket {
    @JSONField
    protected IsoPlayer player;
    @JSONField
    protected boolean reply;

    @Override
    public void setData(Object... values) {
        if (values.length == 2 && values[0] instanceof IsoPlayer && values[1] instanceof Boolean) {
            this.set((IsoPlayer)values[0], (Boolean)values[1]);
        } else {
            DebugLog.Multiplayer.warn(this.getClass().getSimpleName() + ".set get invalid arguments");
        }
    }

    private void set(IsoPlayer player, boolean reply) {
        this.player = player;
        this.reply = reply;
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.player != null;
    }

    @Override
    public void parse(ByteBuffer bb, UdpConnection connection) {
        boolean bMe = false;
        short id = bb.getShort();
        int playerIndex = -1;
        if (id == -1) {
            bMe = true;
            playerIndex = bb.get();
            id = bb.getShort();

            try {
                GameTime.getInstance().load(bb);
                GameTime.getInstance().serverTimeOfDay = GameTime.getInstance().getTimeOfDay();
                GameTime.getInstance().serverNewDays = 0;
                GameTime.getInstance().setMinutesPerDay(SandboxOptions.instance.getDayLengthMinutes());
                LuaEventManager.triggerEvent("OnGameTimeLoaded");
            } catch (IOException var23) {
                DebugLog.Multiplayer.printException(var23, "GameTime load failed", LogSeverity.Error);
                return;
            }
        } else if (GameClient.IDToPlayerMap.containsKey(id)) {
            return;
        }

        int bareHandsID = bb.getInt();
        float x = bb.getFloat();
        float y = bb.getFloat();
        float z = bb.getFloat();
        IsoPlayer player = null;
        if (bMe) {
            String username = GameWindow.ReadString(bb);

            for (int i = 0; i < IsoWorld.instance.addCoopPlayers.size(); i++) {
                IsoWorld.instance.addCoopPlayers.get(i).receivePlayerConnect(playerIndex);
            }

            player = IsoPlayer.players[playerIndex];
            player.username = username;
            player.setOnlineID(id);
        } else {
            String username = GameWindow.ReadString(bb);
            SurvivorDesc desc = SurvivorFactory.CreateSurvivor();
            if (desc == null) {
                DebugLog.Multiplayer.error("SurvivorDesc creation failed");
                return;
            }

            try {
                desc.load(bb, 240, null);
            } catch (IOException var22) {
                DebugLog.Multiplayer.printException(var22, "SurvivorDesc load failed", LogSeverity.Error);
                return;
            }

            player = new IsoPlayer(IsoWorld.instance.currentCell, desc, PZMath.fastfloor(x), PZMath.fastfloor(y), PZMath.fastfloor(z));
            player.remote = true;

            try {
                player.getHumanVisual().load(bb, 240);
            } catch (Exception var21) {
                DebugLog.Multiplayer.printException(var21, "HumanVisual load failed", LogSeverity.Error);
            }

            try {
                player.getItemVisuals().load(bb, 240);
            } catch (Exception var20) {
                DebugLog.Multiplayer.printException(var20, "ItemVisuals load failed", LogSeverity.Error);
            }

            player.username = username;
            player.updateUsername();
            player.setSceneCulled(false);
            player.setX(x);
            player.setY(y);
            player.setZ(z);
            player.networkAi.targetX = x;
            player.networkAi.targetY = y;
            player.networkAi.targetZ = PZMath.fastfloor(z);
        }

        player.setOnlineID(id);
        player.bareHands.setID(bareHandsID);
        if (SteamUtils.isSteamModeEnabled()) {
            player.setSteamID(bb.getLong());
        }

        player.getSafety().load(bb, IsoWorld.getWorldVersion());
        Role role = new Role("");
        role.parse(bb);
        if (bMe) {
            connection.role = role;
            DebugLog.General.warn("ReceivePlayerConnect: guid=%d mtu=%d", connection.getConnectedGUID(), connection.getMTUSize());
        }

        player.setRole(role);
        byte extraInfoFlags = bb.get();
        player.setExtraInfoFlags(extraInfoFlags, true);
        if (!bMe) {
            try {
                player.getXp().load(bb, 240);
            } catch (IOException var19) {
                ExceptionLogger.logException(var19);
            }
        }

        player.setTagPrefix(GameWindow.ReadString(bb));
        player.setTagColor(new ColorInfo(bb.getFloat(), bb.getFloat(), bb.getFloat(), 1.0F));
        player.setHoursSurvived(bb.getDouble());
        player.setZombieKills(bb.getInt());
        player.setDisplayName(GameWindow.ReadString(bb));
        player.setSpeakColour(new Color(bb.getFloat(), bb.getFloat(), bb.getFloat(), 1.0F));
        player.showTag = bb.get() == 1;
        player.factionPvp = bb.get() == 1;
        player.setAutoDrink(bb.get() == 1);
        int nb = bb.getInt();

        for (int i = 0; i < nb; i++) {
            String location = GameWindow.ReadString(bb);
            String itemType = GameWindow.ReadString(bb);
            int itemID = bb.getInt();
            InventoryItem attachItem = InventoryItemFactory.CreateItem(itemType);
            if (attachItem != null) {
                attachItem.setID(itemID);
                player.setAttachedItem(location, attachItem);
            }
        }

        int sneakLvl = bb.getInt();
        int strLvl = bb.getInt();
        int fitLvl = bb.getInt();
        player.remoteSneakLvl = sneakLvl;
        player.remoteStrLvl = strLvl;
        player.remoteFitLvl = fitLvl;
        DebugLog.DetailedInfo.trace("Player \"%s\" is connected me=%b items=%d", GameClient.username, bMe, player.getItemVisuals().size());
        if (!bMe) {
            GameClient.rememberPlayerPosition(player, x, y);
        }

        GameClient.IDToPlayerMap.put(id, player);
        GameClient.instance.idMapDirty = true;
        LuaEventManager.triggerEvent("OnMiniScoreboardUpdate");
        if (bMe) {
            INetworkPacket.send(PacketTypes.PacketType.GetModData);
        }

        if (!bMe && ServerOptions.instance.disableSafehouseWhenPlayerConnected.getValue()) {
            SafeHouse safe = SafeHouse.hasSafehouse(player);
            if (safe != null) {
                safe.setPlayerConnected(safe.getPlayerConnected() + 1);
            }
        }

        if (bMe) {
            String welcomeMessage = ServerOptions.getInstance().getOption("ServerWelcomeMessage");
            if (welcomeMessage != null && !welcomeMessage.equals("")) {
                ChatManager.getInstance().showServerChatMessage(welcomeMessage);
            }

            VoiceManager.getInstance().UpdateChannelsRoaming(GameClient.connection);
            connection.setConnectionTimestamp(5000L);
            AccountDBHelper.getInstance().setupLastSave();
        }

        this.player = player;
    }

    @Override
    public void write(ByteBufferWriter b) {
        if (!this.reply) {
            b.putShort(this.player.onlineId);
        } else {
            b.putShort((short)-1);
            b.putByte((byte)this.player.playerIndex);
            b.putShort(this.player.onlineId);

            try {
                GameTime.getInstance().saveToPacket(b.bb);
            } catch (IOException var6) {
                var6.printStackTrace();
            }
        }

        b.putInt(this.player.bareHands.getID());
        b.putFloat(this.player.getX());
        b.putFloat(this.player.getY());
        b.putFloat(this.player.getZ());
        b.putUTF(this.player.username);
        if (!this.reply) {
            try {
                this.player.getDescriptor().save(b.bb);
                this.player.getHumanVisual().save(b.bb);
                ItemVisuals itemVisuals = new ItemVisuals();
                this.player.getItemVisuals(itemVisuals);
                itemVisuals.save(b.bb);
            } catch (IOException var5) {
                var5.printStackTrace();
            }
        }

        if (SteamUtils.isSteamModeEnabled()) {
            b.putLong(this.player.getSteamID());
        }

        this.player.getSafety().save(b.bb);
        this.player.getRole().send(b.bb);
        b.putByte(this.player.getExtraInfoFlags());
        if (!this.reply) {
            try {
                this.player.getXp().save(b.bb);
            } catch (IOException var4) {
                var4.printStackTrace();
            }
        }

        b.putUTF(this.player.getTagPrefix());
        b.putFloat(this.player.getTagColor().r);
        b.putFloat(this.player.getTagColor().g);
        b.putFloat(this.player.getTagColor().b);
        b.putDouble(this.player.getHoursSurvived());
        b.putInt(this.player.getZombieKills());
        b.putUTF(this.player.getDisplayName());
        b.putFloat(this.player.getSpeakColour().r);
        b.putFloat(this.player.getSpeakColour().g);
        b.putFloat(this.player.getSpeakColour().b);
        b.putBoolean(this.player.showTag);
        b.putBoolean(this.player.factionPvp);
        b.putBoolean(this.player.getAutoDrink());
        b.putInt(this.player.getAttachedItems().size());

        for (int i = 0; i < this.player.getAttachedItems().size(); i++) {
            AttachedItem attachedItem = this.player.getAttachedItems().get(i);
            b.putUTF(attachedItem.getLocation());
            b.putUTF(attachedItem.getItem().getFullType());
            b.putInt(attachedItem.getItem().getID());
        }

        b.putInt(this.player.remoteSneakLvl);
        b.putInt(this.player.remoteStrLvl);
        b.putInt(this.player.remoteFitLvl);
    }
}
