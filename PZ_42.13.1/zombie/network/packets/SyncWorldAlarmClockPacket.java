// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets;

import java.nio.ByteBuffer;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.inventory.types.IAlarmClock;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.iso.objects.IsoWorldInventoryObject;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class SyncWorldAlarmClockPacket implements INetworkPacket {
    int itemId = -1;
    boolean stopRinging = true;
    int alarmHour = -1;
    int alarmMinutes = -1;
    int forceDontRing = -1;
    boolean alarmSet = true;
    int x = -1;
    int y = -1;
    byte z = -1;
    IsoGridSquare gridSquare;
    IAlarmClock alarmClock;

    @Override
    public void setData(Object... values) {
        this.x = (Integer)values[0];
        this.y = (Integer)values[1];
        this.z = (Byte)values[2];
        this.itemId = (Integer)values[3];
        this.stopRinging = (Boolean)values[4];
        this.alarmHour = (Integer)values[5];
        this.alarmMinutes = (Integer)values[6];
        this.alarmSet = (Boolean)values[7];
        this.forceDontRing = (Integer)values[8];
        this.findAlarmClockWorld();
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

    private IsoWorldInventoryObject getIsoWorldInventoryObjectWithID() {
        for (int i = 0; i < this.gridSquare.getWorldObjects().size(); i++) {
            IsoWorldInventoryObject wo = this.gridSquare.getWorldObjects().get(i);
            if (wo != null && wo.getItem().id == this.itemId) {
                return wo;
            }
        }

        return null;
    }

    private void findAlarmClockWorld() {
        if (GameServer.server) {
            this.gridSquare = ServerMap.instance.getGridSquare(this.x, this.y, this.z);
        } else if (GameClient.client) {
            this.gridSquare = IsoWorld.instance.currentCell.getGridSquare(this.x, this.y, this.z);
        }

        if (this.gridSquare == null) {
            DebugLog.Multiplayer.warn("[SyncAlarmClock] parse error: gridSquare is null x,y,z=" + this.x + "," + this.y + "," + this.z);
        } else {
            IsoWorldInventoryObject wo = this.getIsoWorldInventoryObjectWithID();
            if (wo != null && wo.getItem() instanceof IAlarmClock) {
                this.alarmClock = (IAlarmClock)wo.getItem();
            } else {
                DebugLog.Multiplayer.noise("[SyncAlarmClock] unable to find alarmClock for gridSquare x,y,z=" + this.x + "," + this.y + "," + this.z);
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.x);
        b.putInt(this.y);
        b.putByte(this.z);
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
        this.x = b.getInt();
        this.y = b.getInt();
        this.z = b.get();
        this.itemId = b.getInt();
        this.stopRinging = b.get() == 1;
        if (!this.stopRinging) {
            this.alarmHour = b.getInt();
            this.alarmMinutes = b.getInt();
            this.forceDontRing = b.getInt();
            this.alarmSet = b.get() == 1;
        }

        this.findAlarmClockWorld();
    }

    public boolean isRelevant(UdpConnection connection) {
        if (this.gridSquare != null && !connection.RelevantTo(this.gridSquare.x, this.gridSquare.y)) {
            DebugLog.Multiplayer
                .noise("[SyncAlarmClock] not relevant gridSquare[x,y,z]=" + this.gridSquare.x + "," + this.gridSquare.y + "," + this.gridSquare.z);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean isConsistent(UdpConnection connection) {
        return this.gridSquare != null;
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        if (this.isRelevant(connection)) {
            this.updateAlarmClock(this.alarmClock);
            this.sendToRelativeClients(PacketTypes.PacketType.SyncWorldAlarmClock, null, this.x, this.y);
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
