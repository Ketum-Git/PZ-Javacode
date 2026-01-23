// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugLog;
import zombie.debug.LogSeverity;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.JSONField;
import zombie.network.PacketTypes;
import zombie.network.fields.Square;

public class FishingAction extends Action {
    public static byte flagStartFishing = 1;
    public static byte flagStopFishing = 2;
    public static byte flagUpdateFish = 4;
    public static byte flagUpdateBobberParameters = 8;
    public static byte flagCreateBobber = 16;
    public static byte flagDestroyBobber = 32;
    private static final HashMap<IsoPlayer, InventoryItem> fishForPickUp = new HashMap<>();
    @JSONField
    public byte contentFlag;
    @JSONField
    private int fishingRodId;
    @JSONField
    private final Square position = new Square();
    private final KahluaTable tbl = LuaManager.platform.newTable();
    private KahluaTable fishingRod;
    private KahluaTable bobber;
    private KahluaTable currentFish;
    private InventoryItem currentFishItem;
    private InventoryItem lastSentFishItem;
    private boolean catchFishStarted;

    public void setStartFishing(IsoPlayer player, InventoryItem item, IsoGridSquare sq, KahluaTable bobber) {
        this.set(player);
        this.contentFlag = (byte)(this.contentFlag | flagStartFishing);
        if (item == null) {
            this.fishingRodId = 0;
        } else {
            this.fishingRodId = item.getID();
        }

        this.position.set(sq);
        this.bobber = bobber;
    }

    @Override
    public float getDuration() {
        return 100000.0F;
    }

    @Override
    public void start() {
        fishForPickUp.put(this.playerId.getPlayer(), null);
        this.setTimeData();
        KahluaTable fishingClass = (KahluaTable)LuaManager.env.rawget("Fishing");
        KahluaTable fishingRodClass = (KahluaTable)fishingClass.rawget("FishingRod");
        this.fishingRod = (KahluaTable)LuaManager.caller.pcall(LuaManager.thread, fishingRodClass.rawget("new"), fishingRodClass, this.playerId.getPlayer())[1];
        this.fishingRod.rawset("mpAimX", (double)this.position.getX());
        this.fishingRod.rawset("mpAimY", (double)this.position.getY());
        LuaManager.caller.pcall(LuaManager.thread, this.fishingRod.rawget("cast"), this.fishingRod);
        this.bobber = (KahluaTable)this.fishingRod.rawget("bobber");
        DebugLog.Action.trace("FishingAction.start %s", this.getDescription());
    }

    @Override
    public void stop() {
        if (this.bobber != null) {
            LuaManager.caller.pcall(LuaManager.thread, this.bobber.rawget("destroy"), this.bobber);
        }

        DebugLog.Action.trace("FishingAction.stop %s", this.getDescription());
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isUsingTimeout() {
        return true;
    }

    @Override
    public void update() {
        this.setTimeData();
        boolean shouldSend = false;
        if (GameClient.client) {
            if (this.state == Transaction.TransactionState.Accept) {
                boolean catchFishStarted = (Boolean)this.bobber.rawget("catchFishStarted");
                if (catchFishStarted != this.catchFishStarted) {
                    this.catchFishStarted = catchFishStarted;
                    this.contentFlag = (byte)(this.contentFlag | flagUpdateBobberParameters);
                    shouldSend = true;
                }

                if (shouldSend) {
                    ByteBufferWriter bb = GameClient.connection.startPacket();
                    PacketTypes.PacketType.FishingAction.doPacket(bb);
                    this.write(bb);
                    PacketTypes.PacketType.FishingAction.send(GameClient.connection);
                    this.contentFlag = 0;
                }
            }
        } else {
            LuaManager.caller.pcall(LuaManager.thread, this.fishingRod.rawget("update"), this.fishingRod);
            if (this.bobber == null) {
                this.bobber = (KahluaTable)this.fishingRod.rawget("bobber");
            } else {
                this.currentFish = (KahluaTable)this.bobber.rawget("fish");
                if (this.currentFish != null) {
                    this.currentFishItem = (InventoryItem)this.currentFish.rawget("fishItem");
                } else {
                    this.currentFishItem = null;
                }

                if (this.currentFishItem != this.lastSentFishItem) {
                    this.contentFlag = (byte)(this.contentFlag | flagUpdateFish);
                    shouldSend = true;
                    fishForPickUp.put(this.playerId.getPlayer(), this.currentFishItem);
                }

                if (shouldSend) {
                    UdpConnection connection = GameServer.getConnectionFromPlayer(this.playerId.getPlayer());
                    ByteBufferWriter bb = connection.startPacket();
                    PacketTypes.PacketType.FishingAction.doPacket(bb);
                    this.write(bb);
                    PacketTypes.PacketType.FishingAction.send(connection);
                    this.contentFlag = 0;
                }
            }
        }
    }

    @Override
    public boolean perform() {
        return false;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        this.contentFlag = b.get();
        if ((this.contentFlag & flagStartFishing) != 0) {
            this.fishingRodId = b.getInt();
            this.position.parse(b, connection);
        }

        if ((this.contentFlag & flagUpdateFish) != 0) {
            boolean hasFish = b.get() != 0;
            if (hasFish) {
                try {
                    this.currentFish = LuaManager.platform.newTable();
                    this.currentFish.load(b, 241);
                    this.currentFishItem = InventoryItem.loadItem(b, 241);
                } catch (Exception var5) {
                    DebugLog.Objects.printException(var5, this.getDescription(), LogSeverity.Error);
                }
            } else {
                this.currentFish = null;
                this.currentFishItem = null;
            }
        }

        if ((this.contentFlag & flagUpdateBobberParameters) != 0) {
            this.catchFishStarted = b.getInt() == 1;
        }

        DebugLog.Action.trace("FishingAction.parse: %s", this.getDescription());
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        b.putByte(this.contentFlag);
        if ((this.contentFlag & flagStartFishing) != 0) {
            b.putInt(this.fishingRodId);
            this.position.write(b);
        }

        if ((this.contentFlag & flagUpdateFish) != 0) {
            if (this.currentFishItem != null) {
                b.putByte((byte)1);

                try {
                    this.currentFish.save(b.bb);
                    this.currentFishItem.saveWithSize(b.bb, false);
                } catch (IOException var3) {
                    DebugLog.Objects.printException(var3, this.getDescription(), LogSeverity.Error);
                }
            } else {
                b.putByte((byte)0);
            }

            this.lastSentFishItem = this.currentFishItem;
        }

        if ((this.contentFlag & flagUpdateBobberParameters) != 0) {
            b.putInt(this.catchFishStarted ? 1 : 0);
        }

        DebugLog.Action.trace("FishingAction.write: %s", this.getDescription());
    }

    public KahluaTable getLuaTable() {
        this.tbl.wipe();
        if (ActionManager.getPlayer(this.id) == null) {
            return null;
        } else {
            this.tbl.rawset("player", ActionManager.getPlayer(this.id));
            this.tbl.rawset("Reject", this.state == Transaction.TransactionState.Reject);
            this.tbl.rawset("UpdateFish", (this.contentFlag & flagUpdateFish) != 0);
            this.tbl.rawset("UpdateBobberParameters", (this.contentFlag & flagUpdateBobberParameters) != 0);
            this.tbl.rawset("CreateBobber", (this.contentFlag & flagCreateBobber) != 0);
            this.tbl.rawset("DestroyBobber", (this.contentFlag & flagDestroyBobber) != 0);
            this.tbl.rawset("CatchFishStarted", this.catchFishStarted);
            if ((this.contentFlag & flagUpdateFish) != 0) {
                this.tbl.rawset("fish", this.currentFish);
                this.tbl.rawset("fishItem", this.currentFishItem);
            }

            return this.tbl;
        }
    }

    public static InventoryItem getPickedUpFish(IsoPlayer player) {
        return fishForPickUp.get(player);
    }
}
