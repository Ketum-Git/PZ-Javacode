// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.core;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import se.krka.kahlua.integration.LuaReturn;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.Prototype;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.characters.BodyDamage.Metabolics;
import zombie.characters.skills.PerkFactory;
import zombie.core.network.ByteBufferWriter;
import zombie.core.raknet.UdpConnection;
import zombie.inventory.InventoryItem;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoWorld;
import zombie.network.GameClient;
import zombie.network.JSONField;
import zombie.network.PZNetKahluaTableImpl;
import zombie.network.PacketTypes;
import zombie.network.packets.INetworkPacket;
import zombie.scripting.objects.CharacterTrait;

public class BuildAction extends Action {
    @JSONField
    private float x;
    @JSONField
    private float y;
    @JSONField
    private float z;
    @JSONField
    private boolean north;
    @JSONField
    private String spriteName;
    public KahluaTable item;
    @JSONField
    private String objectType;
    private IsoGridSquare square;
    private KahluaTable argTable;

    public void set(IsoPlayer player, float x, float y, float z, boolean north, String spriteName, KahluaTable item) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.north = north;
        this.spriteName = spriteName;
        this.item = item;
        this.set(player);
    }

    @Override
    public void start() {
        this.setTimeData();
        this.square = IsoWorld.instance.currentCell.getGridSquare((double)this.x, (double)this.y, (double)this.z);
        this.argTable = LuaManager.platform.newTable();
        this.argTable.rawset("x", (double)this.x);
        this.argTable.rawset("y", (double)this.y);
        this.argTable.rawset("z", (double)this.z);
        this.argTable.rawset("north", this.north);
        this.argTable.rawset("spriteName", this.spriteName);
        this.argTable.rawset("item", this.item);
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isValid() {
        return this.playerId.isConsistent(null) && !this.spriteName.isBlank()
            ? LuaManager.caller.protectedCallBoolean(LuaManager.thread, this.item.rawget("isValid"), this.item, this.square)
            : false;
    }

    @Override
    public boolean isUsingTimeout() {
        return true;
    }

    @Override
    public float getDuration() {
        float maxTime = 200 - this.playerId.getPlayer().getPerkLevel(PerkFactory.Perks.Woodwork) * 5;
        if (this.item.getMetatable().getString("Type").equals("ISEmptyGraves")) {
            maxTime = 150.0F;
        }

        if (this.playerId.getPlayer().isTimedActionInstant()) {
            maxTime = 1.0F;
        }

        if (this.playerId.getPlayer().hasTrait(CharacterTrait.HANDY)) {
            maxTime -= 50.0F;
        }

        return maxTime * 20.0F;
    }

    @Override
    public void update() {
        if (!GameClient.client) {
            this.playerId.getPlayer().setMetabolicTarget(Metabolics.HeavyWork);
        }
    }

    @Override
    public boolean perform() {
        InventoryItem hammer = this.playerId.getPlayer().getPrimaryHandItem();
        if (hammer != null && hammer.getType().equals("HammerStone") && LuaManager.GlobalObject.ZombRand(hammer.getConditionLowerChance()) == 0.0) {
            hammer.setCondition(hammer.getCondition() - 1);
            INetworkPacket.send(PacketTypes.PacketType.SyncItemFields, this.playerId.getPlayer(), hammer);
        }

        LuaEventManager.triggerEvent("OnProcessAction", "build", this.playerId.getPlayer(), this.argTable);
        return true;
    }

    @Override
    public void parse(ByteBuffer b, UdpConnection connection) {
        super.parse(b, connection);
        if (this.state == Transaction.TransactionState.Request) {
            this.x = b.getFloat();
            this.y = b.getFloat();
            this.z = b.getFloat();
            this.north = b.get() != 0;
            this.spriteName = GameWindow.ReadString(b);
            PZNetKahluaTableImpl serializedItem = new PZNetKahluaTableImpl(new LinkedHashMap<>());
            serializedItem.load(b, connection);
            this.objectType = serializedItem.getString("Type");
            Object classObject = LuaManager.get(this.objectType);
            Object functionObject = LuaManager.getFunctionObject(this.objectType + ".new");
            PZNetKahluaTableImpl Arguments = (PZNetKahluaTableImpl)serializedItem.rawget("Arguments");
            if ("TrapBO".equals(this.objectType)) {
                Arguments.rawset("player", this.playerId.getPlayer());
            }

            byte numParams = (byte)(Arguments.size() + 1);
            Object[] arguments = new Object[numParams];
            int i = 1;
            arguments[0] = classObject;
            KahluaTableIterator it = Arguments.iterator();

            while (it.advance()) {
                arguments[i++] = it.getValue();
            }

            LuaReturn result = LuaManager.caller.protectedCall(LuaManager.thread, functionObject, arguments);
            if (result.getFirst() == null) {
                this.item = null;
                return;
            }

            this.item = (KahluaTable)result.getFirst();
            Object nameValue = serializedItem.rawget("Name");
            if (nameValue instanceof String) {
                this.item.rawset("name", nameValue);
            }

            this.item.rawset("player", this.playerId.getPlayer());
        }
    }

    @Override
    public void write(ByteBufferWriter b) {
        super.write(b);
        if (this.state == Transaction.TransactionState.Request) {
            b.putFloat(this.x);
            b.putFloat(this.y);
            b.putFloat(this.z);
            b.putByte((byte)(this.north ? 1 : 0));
            b.putUTF(this.spriteName);
            PZNetKahluaTableImpl serializedItem = new PZNetKahluaTableImpl(new LinkedHashMap<>());
            PZNetKahluaTableImpl serializedItemArgs = new PZNetKahluaTableImpl(new LinkedHashMap<>());
            Prototype itemNew = ((LuaClosure)this.item.getMetatable().rawget("new")).prototype;

            for (int i = 1; i < itemNew.numParams; i++) {
                String paramName = itemNew.locvars[i];
                serializedItemArgs.rawset(paramName, this.item.rawget(paramName));
            }

            this.objectType = this.item.getMetatable().getString("Type");
            serializedItem.rawset("Type", this.objectType);
            if (this.item.rawget("name") instanceof String) {
                serializedItem.rawset("Name", this.item.getString("name"));
            }

            serializedItem.rawset("Arguments", serializedItemArgs);
            serializedItem.save(b.bb);
        }
    }
}
