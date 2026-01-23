// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import se.krka.kahlua.j2se.KahluaTableImpl;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import se.krka.kahlua.vm.KahluaUtil;
import se.krka.kahlua.vm.LuaCallFrame;
import zombie.GameWindow;
import zombie.Lua.LuaManager;
import zombie.ai.State;
import zombie.ai.states.ClimbThroughWindowPositioningParams;
import zombie.characterTextures.BloodBodyPartType;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.BodyDamage.BodyPart;
import zombie.characters.animals.IsoAnimal;
import zombie.core.Core;
import zombie.core.raknet.UdpConnection;
import zombie.entity.Component;
import zombie.entity.ComponentType;
import zombie.entity.GameEntity;
import zombie.entity.GameEntityManager;
import zombie.entity.components.crafting.CraftBench;
import zombie.entity.components.fluids.FluidContainer;
import zombie.entity.components.resources.Resource;
import zombie.entity.components.resources.Resources;
import zombie.entity.components.spriteconfig.SpriteConfigManager;
import zombie.inventory.InventoryItem;
import zombie.inventory.ItemContainer;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.MultiStageBuilding;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoHutch;
import zombie.network.fields.ContainerID;
import zombie.network.fields.NetObject;
import zombie.network.fields.StateID;
import zombie.network.fields.Variables;
import zombie.network.fields.character.AnimalID;
import zombie.network.fields.character.CharacterID;
import zombie.network.fields.character.PlayerID;
import zombie.network.fields.character.ZombieID;
import zombie.network.id.ObjectID;
import zombie.network.id.ObjectIDManager;
import zombie.network.id.ObjectIDType;
import zombie.scripting.ScriptManager;
import zombie.scripting.entity.components.crafting.CraftRecipe;
import zombie.scripting.objects.EvolvedRecipe;
import zombie.scripting.objects.Recipe;
import zombie.ui.UIManager;
import zombie.util.Type;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.VehicleManager;
import zombie.vehicles.VehiclePart;
import zombie.vehicles.VehicleWindow;

public class PZNetKahluaTableImpl implements KahluaTable {
    @JSONField
    public final Map<Object, Object> delegate;
    private KahluaTable metatable;
    private KahluaTable reloadReplace;
    private static final byte PZNKTI_NO_SAVE = -1;
    private static final byte PZNKTI_INTEGER = 0;
    private static final byte PZNKTI_STRING = 1;
    private static final byte PZNKTI_DOUBLE = 2;
    private static final byte PZNKTI_TABLE = 3;
    private static final byte PZNKTI_PZTABLE = 4;
    private static final byte PZNKTI_BOOLEAN = 5;
    private static final byte PZNKTI_InventoryItem = 6;
    private static final byte PZNKTI_IsoObject = 7;
    private static final byte PZNKTI_IsoPlayer = 8;
    private static final byte PZNKTI_Container = 9;
    private static final byte PZNKTI_BodyPart = 10;
    private static final byte PZNKTI_VehiclePart = 11;
    private static final byte PZNKTI_BaseVehicle = 12;
    private static final byte PZNKTI_IsoGridSquare = 13;
    private static final byte PZNKTI_Recipe = 14;
    private static final byte PZNKTI_BloodBodyPartType = 15;
    private static final byte PZNKTI_DeadBody = 16;
    private static final byte PZNKTI_IsoAnimal = 17;
    private static final byte PZNKTI_ObjectInfo = 18;
    private static final byte PZNKTI_Resource = 19;
    private static final byte PZNKTI_NestBox = 20;
    private static final byte PZNKTI_MultiStageBuildingStage = 21;
    private static final byte PZNKTI_EvolvedRecipe = 22;
    private static final byte PZNKTI_FluidContainer = 23;
    private static final byte PZNKTI_VehicleWindow = 24;
    private static final byte PZNKTI_CraftRecipe = 25;
    private static final byte PZNKTI_ArrayList = 26;
    private static final byte PZNKTI_Null = 27;
    private static final byte PZNKTI_FLOAT = 28;
    private static final byte PZNKTI_LONG = 29;
    private static final byte PZNKTI_SHORT = 30;
    private static final byte PZNKTI_BYTE = 31;
    private static final byte PZNKTI_IsoDirections = 32;
    private static final byte PZNKTI_State = 33;
    private static final byte PZNKIT_ClimbThroughWindowPositioningParams = 34;
    private static final byte PZNKTI_IsoZombie = 35;
    private static final byte PZNKTI_CraftBench = 36;
    private static final byte PZNKTI_StageStage = 37;
    private static final byte PZNKTI_Variables = 38;

    public PZNetKahluaTableImpl(Map<Object, Object> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void setMetatable(KahluaTable metatable) {
        this.metatable = metatable;
    }

    @Override
    public KahluaTable getMetatable() {
        return this.metatable;
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public void rawset(Object key, Object value) {
        if (this.reloadReplace != null) {
            this.reloadReplace.rawset(key, value);
        }

        Object lastVal = null;
        if (Core.debug && LuaManager.thread != null && LuaManager.thread.hasDataBreakpoint(this, key)) {
            lastVal = this.rawget(key);
        }

        if (value == null) {
            if (Core.debug && LuaManager.thread != null && LuaManager.thread.hasDataBreakpoint(this, key) && lastVal != null) {
                UIManager.debugBreakpoint(LuaManager.thread.currentfile, LuaManager.thread.lastLine);
            }

            this.delegate.remove(key);
        } else {
            if (Core.debug && LuaManager.thread != null && LuaManager.thread.hasDataBreakpoint(this, key) && !value.equals(lastVal)) {
                int a = LuaManager.GlobalObject.getCurrentCoroutine().currentCallFrame().pc;
                if (a < 0) {
                    a = 0;
                }

                UIManager.debugBreakpoint(
                    LuaManager.thread.currentfile, LuaManager.GlobalObject.getCurrentCoroutine().currentCallFrame().closure.prototype.lines[a] - 1
                );
            }

            this.delegate.put(key, value);
        }
    }

    @Override
    public Object rawget(Object key) {
        if (this.reloadReplace != null) {
            return this.reloadReplace.rawget(key);
        } else if (key == null) {
            return null;
        } else {
            if (Core.debug && LuaManager.thread != null && LuaManager.thread.hasReadDataBreakpoint(this, key)) {
                int a = LuaManager.GlobalObject.getCurrentCoroutine().currentCallFrame().pc;
                if (a < 0) {
                    a = 0;
                }

                UIManager.debugBreakpoint(
                    LuaManager.thread.currentfile, LuaManager.GlobalObject.getCurrentCoroutine().currentCallFrame().closure.prototype.lines[a] - 1
                );
            }

            return !this.delegate.containsKey(key) && this.metatable != null ? this.metatable.rawget(key) : this.delegate.get(key);
        }
    }

    @Override
    public void rawset(int key, Object value) {
        this.rawset(KahluaUtil.toDouble((long)key), value);
    }

    public String rawgetStr(Object key) {
        return (String)this.rawget(key);
    }

    public int rawgetInt(Object key) {
        return this.rawget(key) instanceof Double ? ((Double)this.rawget(key)).intValue() : -1;
    }

    public boolean rawgetBool(Object key) {
        return this.rawget(key) instanceof Boolean ? (Boolean)this.rawget(key) : false;
    }

    public float rawgetFloat(Object key) {
        return this.rawget(key) instanceof Double ? ((Double)this.rawget(key)).floatValue() : -1.0F;
    }

    @Override
    public Object rawget(int key) {
        return this.rawget(KahluaUtil.toDouble((long)key));
    }

    @Override
    public int len() {
        return KahluaUtil.len(this, 0, 2 * this.delegate.size());
    }

    @Override
    public KahluaTableIterator iterator() {
        final Object[] keys = this.delegate.isEmpty() ? null : this.delegate.keySet().toArray();
        return new KahluaTableIterator() {
            private Object curKey;
            private Object curValue;
            private int keyIndex;

            {
                Objects.requireNonNull(PZNetKahluaTableImpl.this);
            }

            @Override
            public int call(LuaCallFrame callFrame, int nArguments) {
                return this.advance() ? callFrame.push(this.getKey(), this.getValue()) : 0;
            }

            @Override
            public boolean advance() {
                if (keys != null && this.keyIndex < keys.length) {
                    this.curKey = keys[this.keyIndex];
                    this.curValue = PZNetKahluaTableImpl.this.delegate.get(this.curKey);
                    this.keyIndex++;
                    return true;
                } else {
                    this.curKey = null;
                    this.curValue = null;
                    return false;
                }
            }

            @Override
            public Object getKey() {
                return this.curKey;
            }

            @Override
            public Object getValue() {
                return this.curValue;
            }
        };
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public void wipe() {
        this.delegate.clear();
    }

    @Override
    public String toString() {
        return "table 0x" + System.identityHashCode(this);
    }

    private void saveInventoryItem(ByteBuffer output, InventoryItem inventoryItem) {
        ContainerID container = new ContainerID();
        container.set(inventoryItem.getContainer());
        container.write(output);
        output.putInt(inventoryItem.getID());
    }

    public static void saveIsoObject(ByteBuffer output, IsoObject isoObject) {
        NetObject obj = new NetObject();
        obj.setObject(isoObject);
        obj.write(output);
    }

    public static void saveIsoGameCharacter(ByteBuffer output, IsoGameCharacter character) {
        CharacterID characterID = new CharacterID();
        characterID.set(character);
        characterID.write(output);
    }

    private void saveResource(ByteBuffer output, Resource resource) {
        output.putLong(resource.getGameEntity().getEntityNetID());
        GameWindow.WriteString(output, resource.getId());
    }

    public static void saveComponent(ByteBuffer output, Component component, short componentID) {
        output.putLong(component.getGameEntity().getEntityNetID());
        output.putShort(componentID);
    }

    @Override
    public void save(ByteBuffer output) {
        KahluaTableIterator it = this.iterator();
        int count = 0;

        while (it.advance()) {
            if (canSave(it.getKey(), it.getValue())) {
                count++;
            }
        }

        it = this.iterator();
        output.putInt(count);

        while (it.advance()) {
            byte keyByte = getKeyByte(it.getKey());
            byte valueByte = getValueByte(it.getValue());
            if (keyByte != -1 && valueByte != -1) {
                this.save(output, keyByte, it.getKey());
                this.save(output, valueByte, it.getValue());
            }
        }
    }

    private void save(ByteBuffer output, byte sbyt, Object o) throws RuntimeException {
        output.put(sbyt);
        if (sbyt == 1) {
            if ("fireplace".equals(o)) {
                byte zombie = 4;
            }

            GameWindow.WriteString(output, (String)o);
        } else if (sbyt == 2) {
            output.putDouble((Double)o);
        } else if (sbyt == 5) {
            output.put((byte)((Boolean)o ? 1 : 0));
        } else if (sbyt == 3) {
            ((KahluaTableImpl)o).save(output);
        } else if (sbyt == 4) {
            ((PZNetKahluaTableImpl)o).save(output);
        } else if (sbyt == 0) {
            output.putInt((Integer)o);
        } else if (sbyt == 6) {
            this.saveInventoryItem(output, (InventoryItem)o);
        } else if (sbyt == 8) {
            PlayerID player = new PlayerID();
            player.set((IsoPlayer)o);
            player.write(output);
        } else if (sbyt == 7) {
            saveIsoObject(output, (IsoObject)o);
        } else if (sbyt == 9) {
            ContainerID container = new ContainerID();
            container.set((ItemContainer)o);
            container.write(output);
        } else if (sbyt == 10) {
            PlayerID player = new PlayerID();
            player.set((IsoPlayer)((BodyPart)o).getParentChar());
            player.write(output);
            output.putInt(((BodyPart)o).getIndex());
        } else if (sbyt == 11) {
            VehiclePart vehiclePart = (VehiclePart)o;
            output.putShort(vehiclePart.getVehicle().vehicleId);
            GameWindow.WriteString(output, vehiclePart.getId());
        } else if (sbyt == 12) {
            BaseVehicle baseVehicle = (BaseVehicle)o;
            output.putShort(baseVehicle.vehicleId);
        } else if (sbyt == 13) {
            output.putInt(((IsoGridSquare)o).getX());
            output.putInt(((IsoGridSquare)o).getY());
            output.put((byte)((IsoGridSquare)o).getZ());
        } else if (sbyt == 14) {
            GameWindow.WriteString(output, ((Recipe)o).getFullType());
        } else if (sbyt == 22) {
            GameWindow.WriteString(output, ((EvolvedRecipe)o).getScriptObjectFullType());
        } else if (sbyt == 16) {
            ((IsoDeadBody)o).getObjectID().save(output);
        } else if (sbyt == 18) {
            GameWindow.WriteString(output, ((SpriteConfigManager.ObjectInfo)o).getName());
        } else if (sbyt == 19) {
            this.saveResource(output, (Resource)o);
        } else if (sbyt == 20) {
            output.putInt(((IsoHutch.NestBox)o).getIndex());
        } else if (sbyt != 27) {
            if (sbyt == 15) {
                output.putInt(((BloodBodyPartType)o).index());
            } else if (sbyt == 17) {
                AnimalID animalID = new AnimalID();
                animalID.set((IsoAnimal)o);
                animalID.write(output);
            } else if (sbyt == 21) {
                GameWindow.WriteString(output, ((MultiStageBuilding.Stage)o).id);
            } else if (sbyt == 23) {
                GameEntity gameEntity = ((FluidContainer)o).getGameEntity();
                if (gameEntity instanceof InventoryItem inventoryItem) {
                    output.put((byte)6);
                    this.saveInventoryItem(output, inventoryItem);
                } else if (gameEntity instanceof IsoObject isoObject) {
                    output.put((byte)7);
                    saveIsoObject(output, isoObject);
                }
            } else if (sbyt == 24) {
                VehiclePart part = ((VehicleWindow)o).getPart();
                output.putShort(part.getVehicle().vehicleId);
                GameWindow.WriteString(output, part.getId());
            } else if (sbyt == 25) {
                GameWindow.WriteString(output, ((CraftRecipe)o).getName());
            } else if (sbyt == 26) {
                ArrayList<?> list = (ArrayList<?>)o;
                output.putInt(list.size());
                if (!list.isEmpty()) {
                    byte val = getValueByte(list.get(0));
                    if (val == -1) {
                        return;
                    }

                    list.forEach(object -> this.save(output, val, object));
                }
            } else if (sbyt == 28) {
                output.putFloat((Float)o);
            } else if (sbyt == 29) {
                output.putLong((Long)o);
            } else if (sbyt == 30) {
                output.putShort((Short)o);
            } else if (sbyt == 31) {
                output.put((Byte)o);
            } else if (sbyt == 32) {
                output.putInt(((IsoDirections)o).index());
            } else if (sbyt == 33) {
                StateID stateID = new StateID();
                stateID.set((State)o);
                stateID.write(output);
            } else if (sbyt == 34) {
                ((ClimbThroughWindowPositioningParams)o).write(output);
            } else if (sbyt == 35) {
                ZombieID zombie = new ZombieID();
                zombie.set((IsoZombie)o);
                zombie.write(output);
            } else if (sbyt == 36) {
                saveComponent(output, (Component)o, ComponentType.CraftBench.GetID());
            } else if (sbyt == 37) {
                output.putInt(((State.Stage)o).ordinal());
            } else {
                if (sbyt != 38) {
                    throw new RuntimeException("invalid lua table type " + sbyt);
                }

                ((Variables)o).write(output);
            }
        }
    }

    @Override
    public void save(DataOutputStream output) throws IOException {
        throw new RuntimeException("The PZNetKahluaTableImpl.save function isn't implemented");
    }

    private InventoryItem loadInventoryItem(ByteBuffer input, UdpConnection connection) {
        ContainerID container = new ContainerID();
        container.parse(input, connection);
        int itemId = input.getInt();
        return container.getContainer() != null ? container.getContainer().getItemWithID(itemId) : null;
    }

    public static IsoObject loadIsoObject(ByteBuffer input, UdpConnection connection) {
        NetObject obj = new NetObject();
        obj.parse(input, connection);
        return obj.getObject();
    }

    public static IsoGameCharacter loadIsoGameCharacter(ByteBuffer input, UdpConnection connection) {
        CharacterID characterID = new CharacterID();
        characterID.parse(input, connection);
        return characterID.getCharacter();
    }

    private Resource loadResource(ByteBuffer input) {
        long gameEntityNetID = input.getLong();
        String id = GameWindow.ReadString(input);
        GameEntity gameEntity = GameEntityManager.GetEntity(gameEntityNetID);
        Resources resources = gameEntity.getComponent(ComponentType.Resources);
        return resources != null ? resources.getResource(id) : null;
    }

    public static Component loadComponent(ByteBuffer input, UdpConnection connection) {
        long gameEntityNetID = input.getLong();
        short componentID = input.getShort();
        GameEntity gameEntity = GameEntityManager.GetEntity(gameEntityNetID);
        return gameEntity.getComponent(ComponentType.FromId(componentID));
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) {
        throw new RuntimeException("The PZNetKahluaTableImpl.load function isn't implemented");
    }

    public void load(ByteBuffer input, UdpConnection connection) {
        int count = input.getInt();
        this.wipe();

        for (int n = 0; n < count; n++) {
            byte keyByte = input.get();
            Object key = this.load(input, connection, keyByte);
            byte valueByte = input.get();
            Object value = this.load(input, connection, valueByte);
            this.delegate.put(key, value);
        }
    }

    public Object load(ByteBuffer input, UdpConnection connection, byte sbyt) throws RuntimeException {
        if (sbyt == 1) {
            String val = GameWindow.ReadString(input);
            if ("fireplace".equals(val)) {
                byte var38 = 4;
            }

            return val;
        } else if (sbyt == 2) {
            return input.getDouble();
        } else if (sbyt == 5) {
            return input.get() == 1;
        } else if (sbyt == 3) {
            KahluaTableImpl v = (KahluaTableImpl)LuaManager.platform.newTable();
            v.load(input, 241);
            return v;
        } else if (sbyt == 4) {
            PZNetKahluaTableImpl v = new PZNetKahluaTableImpl(new LinkedHashMap<>());
            v.load(input, connection);
            return v;
        } else if (sbyt == 0) {
            return input.getInt();
        } else if (sbyt == 6) {
            return this.loadInventoryItem(input, connection);
        } else if (sbyt == 8) {
            PlayerID player = new PlayerID();
            player.parse(input, connection);
            return player.getPlayer();
        } else if (sbyt == 7) {
            return loadIsoObject(input, connection);
        } else if (sbyt == 9) {
            ContainerID container = new ContainerID();
            container.parse(input, connection);
            return container.getContainer();
        } else if (sbyt == 10) {
            PlayerID player = new PlayerID();
            player.parse(input, connection);
            int bodyPartIndex = input.getInt();
            return player.getPlayer().getBodyDamage().getBodyParts().get(bodyPartIndex);
        } else if (sbyt == 11) {
            short vehicleId = input.getShort();
            String partId = GameWindow.ReadString(input);
            return VehicleManager.instance.getVehicleByID(vehicleId).getPartById(partId);
        } else if (sbyt == 12) {
            short vehicleId = input.getShort();
            return VehicleManager.instance.getVehicleByID(vehicleId);
        } else if (sbyt == 13) {
            int x = input.getInt();
            int y = input.getInt();
            int z = input.get();
            return ServerMap.instance.getGridSquare(x, y, z);
        } else if (sbyt == 14) {
            String recipeType = GameWindow.ReadString(input);
            return ScriptManager.instance.getRecipe(recipeType);
        } else if (sbyt == 22) {
            String recipeName = GameWindow.ReadString(input);
            return ScriptManager.instance.getEvolvedRecipe(recipeName);
        } else if (sbyt == 16) {
            ObjectID objectID = ObjectIDManager.createObjectID(ObjectIDType.DeadBody);
            objectID.parse(input, null);
            return objectID.getObject();
        } else if (sbyt == 18) {
            String name = GameWindow.ReadString(input);
            return SpriteConfigManager.GetObjectInfo(name);
        } else if (sbyt == 19) {
            return this.loadResource(input);
        } else if (sbyt == 20) {
            return input.getInt();
        } else if (sbyt == 27) {
            return null;
        } else if (sbyt == 15) {
            return BloodBodyPartType.FromIndex(input.getInt());
        } else if (sbyt == 17) {
            AnimalID animalID = new AnimalID();
            animalID.parse(input, connection);
            return animalID.getAnimal();
        } else if (sbyt == 21) {
            String id = GameWindow.ReadString(input);
            return MultiStageBuilding.getStage(id);
        } else if (sbyt == 23) {
            byte type = input.get();
            if (type == 6) {
                InventoryItem inventoryItem = this.loadInventoryItem(input, connection);
                if (inventoryItem != null) {
                    return inventoryItem.getFluidContainer();
                }
            } else if (type == 7) {
                IsoObject isoObject = loadIsoObject(input, connection);
                if (isoObject != null) {
                    return isoObject.getFluidContainer();
                }
            }

            return null;
        } else if (sbyt == 24) {
            short vehicleId = input.getShort();
            String partId = GameWindow.ReadString(input);
            VehiclePart vehiclePart = VehicleManager.instance.getVehicleByID(vehicleId).getPartById(partId);
            return vehiclePart.getWindow();
        } else if (sbyt == 25) {
            String recipeName = GameWindow.ReadString(input);
            return ScriptManager.instance.getCraftRecipe(recipeName);
        } else if (sbyt != 26) {
            if (sbyt == 28) {
                return input.getFloat();
            } else if (sbyt == 29) {
                return input.getLong();
            } else if (sbyt == 30) {
                return input.getShort();
            } else if (sbyt == 31) {
                return input.get();
            } else if (sbyt == 32) {
                return IsoDirections.fromIndex(input.getInt());
            } else if (sbyt == 33) {
                StateID stateID = new StateID();
                stateID.parse(input, connection);
                return stateID.getState();
            } else if (sbyt == 34) {
                ClimbThroughWindowPositioningParams climbParams = ClimbThroughWindowPositioningParams.alloc();
                climbParams.parse(input, connection);
                return climbParams;
            } else if (sbyt == 35) {
                ZombieID zombie = new ZombieID();
                zombie.parse(input, connection);
                return zombie.getZombie();
            } else if (sbyt == 36) {
                return Type.tryCastTo(loadComponent(input, connection), CraftBench.class);
            } else if (sbyt == 37) {
                return State.Stage.values()[input.getInt()];
            } else if (sbyt == 38) {
                Variables variables = new Variables();
                variables.parse(input, connection);
                return variables;
            } else {
                throw new RuntimeException("invalid lua table type " + sbyt);
            }
        } else {
            int size = input.getInt();
            ArrayList<Object> list = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                byte val = input.get();
                if (val == -1) {
                    break;
                }

                Object object = this.load(input, connection, val);
                if (object != null) {
                    list.add(object);
                }
            }

            return list;
        }
    }

    @Override
    public void load(DataInputStream input, int WorldVersion) throws IOException {
        throw new RuntimeException("The PZNetKahluaTableImpl.load function isn't implemented");
    }

    @Override
    public String getString(String string) {
        return (String)this.rawget(string);
    }

    public KahluaTableImpl getRewriteTable() {
        return (KahluaTableImpl)this.reloadReplace;
    }

    public void setRewriteTable(Object value) {
        this.reloadReplace = (KahluaTableImpl)value;
    }

    private static byte getKeyByte(Object o) {
        if (o instanceof String) {
            return 1;
        } else if (o instanceof Double) {
            return 2;
        } else {
            return (byte)(o instanceof Integer ? 0 : -1);
        }
    }

    private static byte getValueByte(Object o) {
        if (o instanceof String) {
            return 1;
        } else if (o instanceof Double) {
            return 2;
        } else if (o instanceof Boolean) {
            return 5;
        } else if (o instanceof KahluaTableImpl) {
            return 3;
        } else if (o instanceof PZNetKahluaTableImpl) {
            return 4;
        } else if (o instanceof InventoryItem) {
            return 6;
        } else if (o instanceof IsoAnimal) {
            return 17;
        } else if (o instanceof IsoPlayer) {
            return 8;
        } else if (o instanceof IsoZombie) {
            return 35;
        } else if (o instanceof BaseVehicle) {
            return 12;
        } else if (o instanceof IsoDeadBody) {
            return 16;
        } else if (o instanceof Resource) {
            return 19;
        } else if (o instanceof SpriteConfigManager.ObjectInfo) {
            return 18;
        } else if (o instanceof IsoObject) {
            return 7;
        } else if (o instanceof Integer) {
            return 0;
        } else if (o instanceof ItemContainer) {
            return 9;
        } else if (o instanceof BodyPart) {
            return 10;
        } else if (o instanceof VehiclePart) {
            return 11;
        } else if (o instanceof IsoGridSquare) {
            return 13;
        } else if (o instanceof Recipe) {
            return 14;
        } else if (o instanceof CraftRecipe) {
            return 25;
        } else if (o instanceof EvolvedRecipe) {
            return 22;
        } else if (o instanceof BloodBodyPartType) {
            return 15;
        } else if (o instanceof IsoHutch.NestBox) {
            return 20;
        } else if (o instanceof MultiStageBuilding.Stage) {
            return 21;
        } else if (o instanceof FluidContainer) {
            return 23;
        } else if (o instanceof VehicleWindow) {
            return 24;
        } else if (o instanceof ArrayList) {
            return 26;
        } else if (o instanceof PZNetKahluaNull) {
            return 27;
        } else if (o instanceof Float) {
            return 28;
        } else if (o instanceof Long) {
            return 29;
        } else if (o instanceof Short) {
            return 30;
        } else if (o instanceof Byte) {
            return 31;
        } else if (o instanceof IsoDirections) {
            return 32;
        } else if (o instanceof State) {
            return 33;
        } else if (o instanceof ClimbThroughWindowPositioningParams) {
            return 34;
        } else if (o instanceof CraftBench) {
            return 36;
        } else if (o instanceof State.Stage) {
            return 37;
        } else {
            return (byte)(o instanceof Variables ? 38 : -1);
        }
    }

    public static boolean canSave(Object key, Object value) {
        return getKeyByte(key) != -1 && getValueByte(value) != -1;
    }
}
