// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.InventoryItem;
import zombie.inventory.types.IAlarmClock;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.fields.character.PlayerID;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncPlayerAlarmClockPacket implements INetworkPacket {
    protected final PlayerID playerId = new PlayerID();
    int itemId = -1;
    boolean stopRinging = true;
    int alarmHour = -1;
    int alarmMinutes = -1;
    int forceDontRing = -1;
    boolean alarmSet = true;
    IAlarmClock alarmClock;

    @Override
    public void setData(Object... values) {
        this.playerId.set((IsoPlayer)values[0]);
        this.itemId = (Integer)values[1];
        this.stopRinging = (Boolean)values[2];
        this.alarmHour = (Integer)values[3];
        this.alarmMinutes = (Integer)values[4];
        this.alarmSet = (Boolean)values[5];
        this.forceDontRing = (Integer)values[6];
        this.findAlarmClockPlayer((IsoPlayer)values[0]);
    }

    private <T extends IAlarmClock> void updateAlarmClock(T ac) {
        if (ac == null) {
            DebugLog.Multiplayer.noise("[SyncAlarmClock] skipping update of null AlarmClock");
        } else {
            if (this.stopRinging) {
                ac.stopRinging();
            } else {
                ac.setAlarmSet(this.alarmSet);
                ac.setHour(this.alarmHour);
                ac.setMinute(this.alarmMinutes);
                ac.setForceDontRing(this.forceDontRing);
            }
        }
    }

    private void findAlarmClockPlayer(IsoPlayer player) {
        if (player == null) {
            DebugLog.Multiplayer.warn("[SyncAlarmClock] unable to find clock for null player");
        } else {
            InventoryItem inventoryItem = player.getInventory().getItemWithID(this.itemId);
            if (inventoryItem != null && inventoryItem instanceof IAlarmClock iAlarmClock) {
                this.alarmClock = iAlarmClock;
            } else {
                DebugLog.Multiplayer
                    .noise(
                        "[SyncAlarmClock] unable to find alarmClock for getPlayerNum()="
                            + player.getPlayerNum()
                            + " getOnlineID()="
                            + player.getOnlineID()
                            + " itemID:"
                            + this.itemId
                    );
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        this.playerId.write(b);
        b.putInt(this.itemId);
        b.putByte((byte)(this.stopRinging ? 1 : 0));
        if (!this.stopRinging) {
            b.putInt(this.alarmHour);
            b.putInt(this.alarmMinutes);
            b.putInt(this.forceDontRing);
            b.putByte((byte)(this.alarmSet ? 1 : 0));
        }
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        this.playerId.parse(b, connection);
        this.itemId = b.getInt();
        this.stopRinging = b.get() == 1;
        if (!this.stopRinging) {
            this.alarmHour = b.getInt();
            this.alarmMinutes = b.getInt();
            this.forceDontRing = b.getInt();
            this.alarmSet = b.get() == 1;
        }

        this.findAlarmClockPlayer(this.playerId.getPlayer());
    }

    public boolean isRelevant(UdpConnection connection) {
        if (this.playerId.getPlayer() == null) {
            DebugLog.Multiplayer.warn("[SyncAlarmClock] not relevant null isoPlayer");
            return false;
        } else if (!connection.RelevantTo(this.playerId.getPlayer().square.x, this.playerId.getPlayer().square.y)) {
            DebugLog.Multiplayer
                .noise("[SyncAlarmClock] not relevant client isoPlayer[x,y]=" + this.playerId.getPlayer().getX() + "," + this.playerId.getPlayer().getY());
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.playerId != null && this.playerId.isConsistent(connection);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isRelevant(connection)) {
            this.updateAlarmClock(this.alarmClock);
            if (this.playerId.getPlayer() == null) {
                DebugLog.Multiplayer.warn("[SyncAlarmClock] unable to process on server null isoPlayer");
            } else {
                this.sendToRelativeClients(
                    PacketTypes.PacketType.SyncPlayerAlarmClock, null, this.playerId.getPlayer().square.x, this.playerId.getPlayer().square.y
                );
            }
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        this.updateAlarmClock(this.alarmClock);
        if (this.alarmClock != null) {
            this.alarmClock.update();
        }
    }
}
