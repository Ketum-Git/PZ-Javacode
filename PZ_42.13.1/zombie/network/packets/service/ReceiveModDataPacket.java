// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network.packets.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import zombie.GameTime;
import zombie.Lua.LuaEventManager;
import zombie.characters.Capability;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.network.PacketSetting;
import zombie.network.PacketTypes;
import zombie.network.ServerMap;
import zombie.network.packets.INetworkPacket;

@PacketSetting(ordering = 0, priority = 1, reliability = 2, requiredCapability = Capability.LoginOnServer, handlingType = 3)
public class ReceiveModDataPacket implements INetworkPacket {
    IsoGridSquare sq;

    public void set(IsoGridSquare sq) {
        this.sq = sq;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        int x = b.getInt();
        int y = b.getInt();
        byte z = b.get();
        if (GameClient.client) {
            this.sq = IsoWorld.instance.currentCell.getGridSquare(x, y, z);
            if (this.sq == null && IsoWorld.instance.isValidSquare(x, y, z) && IsoWorld.instance.currentCell.getChunkForGridSquare(x, y, z) != null) {
                this.sq = IsoGridSquare.getNew(IsoWorld.instance.getCell(), null, x, y, z);
            }

            if (this.sq == null) {
                GameClient.instance.delayPacket(x, y, z);
                return;
            }

            try {
                this.sq.getModData().load(b, 240);
            } catch (IOException var7) {
                var7.printStackTrace();
            }
        } else {
            this.sq = ServerMap.instance.getGridSquare(x, y, z);
            if (this.sq == null) {
                return;
            }

            try {
                this.sq.getModData().load(b, 240);
                if (this.sq.getModData().rawget("id") != null
                    && (this.sq.getModData().rawget("remove") == null || this.sq.getModData().rawget("remove").equals("false"))) {
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":x", (double)this.sq.getX());
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":y", (double)this.sq.getY());
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":z", (double)this.sq.getZ());
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":typeOfSeed", this.sq.getModData().rawget("typeOfSeed"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":nbOfGrow", this.sq.getModData().rawget("nbOfGrow"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":id", this.sq.getModData().rawget("id"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":waterLvl", this.sq.getModData().rawget("waterLvl"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset(
                            "planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":lastWaterHour",
                            this.sq.getModData().rawget("lastWaterHour")
                        );
                    GameTime.getInstance()
                        .getModData()
                        .rawset(
                            "planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":waterNeeded", this.sq.getModData().rawget("waterNeeded")
                        );
                    GameTime.getInstance()
                        .getModData()
                        .rawset(
                            "planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":waterNeededMax",
                            this.sq.getModData().rawget("waterNeededMax")
                        );
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":mildewLvl", this.sq.getModData().rawget("mildewLvl"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":aphidLvl", this.sq.getModData().rawget("aphidLvl"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":fliesLvl", this.sq.getModData().rawget("fliesLvl"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":fertilizer", this.sq.getModData().rawget("fertilizer"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset(
                            "planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":nextGrowing", this.sq.getModData().rawget("nextGrowing")
                        );
                    GameTime.getInstance()
                        .getModData()
                        .rawset(
                            "planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":hasVegetable", this.sq.getModData().rawget("hasVegetable")
                        );
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":hasSeed", this.sq.getModData().rawget("hasSeed"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":health", this.sq.getModData().rawget("health"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":badCare", this.sq.getModData().rawget("badCare"));
                    GameTime.getInstance()
                        .getModData()
                        .rawset("planting:" + ((Double)this.sq.getModData().rawget("id")).intValue() + ":state", this.sq.getModData().rawget("state"));
                    if (this.sq.getModData().rawget("hoursElapsed") != null) {
                        GameTime.getInstance().getModData().rawset("hoursElapsed", this.sq.getModData().rawget("hoursElapsed"));
                    }
                }
            } catch (IOException var8) {
                var8.printStackTrace();
            }
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        b.putInt(this.sq.getX());
        b.putInt(this.sq.getY());
        b.putByte((byte)this.sq.getZ());

        try {
            this.sq.getModData().save(b.bb);
        } catch (IOException var3) {
            var3.printStackTrace();
        }
    }

    @Override
    public void processClient(UdpConnection connection) {
        LuaEventManager.triggerEvent("onLoadModDataFromServer", this.sq);
    }

    @Override
    public void processServer(PacketTypes.PacketType packetType, UdpConnection connection) {
        LuaEventManager.triggerEvent("onLoadModDataFromServer", this.sq);

        for (int n = 0; n < GameServer.udpEngine.connections.size(); n++) {
            UdpConnection c = GameServer.udpEngine.connections.get(n);
            if (c.RelevantTo(this.sq.getX(), this.sq.getY()) && (connection == null || c.getConnectedGUID() != connection.getConnectedGUID())) {
                ByteBufferWriter b2 = c.startPacket();
                PacketTypes.PacketType.ReceiveModData.doPacket(b2);
                this.write(b2);
                PacketTypes.PacketType.ReceiveModData.send(c);
            }
        }
    }
}
