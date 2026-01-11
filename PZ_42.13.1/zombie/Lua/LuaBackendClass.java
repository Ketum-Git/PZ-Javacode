// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.Lua;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;

public class LuaBackendClass implements KahluaTable {
    KahluaTable table;
    KahluaTable typeTable;

    @Override
    public String getString(String string) {
        return (String)this.rawget(string);
    }

    public LuaBackendClass(String type) {
        this.typeTable = (KahluaTable)LuaManager.env.rawget(type);
    }

    public void callVoid(String func) {
        LuaManager.caller.pcallvoid(LuaManager.thread, this.typeTable.rawget(func), this.table);
    }

    public void callVoid(String func, Object param1) {
        LuaManager.caller.pcallvoid(LuaManager.thread, this.typeTable.rawget(func), new Object[]{this.table, param1});
    }

    public void callVoid(String func, Object param1, Object param2) {
        LuaManager.caller.pcallvoid(LuaManager.thread, this.typeTable.rawget(func), new Object[]{this.table, param1, param2});
    }

    public void callVoid(String func, Object param1, Object param2, Object param3) {
        LuaManager.caller.pcallvoid(LuaManager.thread, this.typeTable.rawget(func), new Object[]{this.table, param1, param2, param3});
    }

    public void callVoid(String func, Object param1, Object param2, Object param3, Object param4) {
        LuaManager.caller.pcallvoid(LuaManager.thread, this.typeTable.rawget(func), new Object[]{this.table, param1, param2, param3, param4});
    }

    public void callVoid(String func, Object param1, Object param2, Object param3, Object param4, Object param5) {
        LuaManager.caller.pcallvoid(LuaManager.thread, this.typeTable.rawget(func), new Object[]{this.table, param1, param2, param3, param4, param5});
    }

    public Object call(String func) {
        return LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table)[1];
    }

    public Object call(String func, Object param1) {
        return LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1)[1];
    }

    public Object call(String func, Object param1, Object param2) {
        return LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2)[1];
    }

    public Object call(String func, Object param1, Object param2, Object param3) {
        return LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3)[1];
    }

    public Object call(String func, Object param1, Object param2, Object param3, Object param4) {
        return LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3, param4)[1];
    }

    public Object call(String func, Object param1, Object param2, Object param3, Object param4, Object param5) {
        return LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3, param4, param5)[1];
    }

    public int callInt(String func) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table)[1]).intValue();
    }

    public int callInt(String func, Object param1) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1)[1]).intValue();
    }

    public int callInt(String func, Object param1, Object param2) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2)[1]).intValue();
    }

    public int callInt(String func, Object param1, Object param2, Object param3) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3)[1]).intValue();
    }

    public int callInt(String func, Object param1, Object param2, Object param3, Object param4) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3, param4)[1]).intValue();
    }

    public int callInt(String func, Object param1, Object param2, Object param3, Object param4, Object param5) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3, param4, param5)[1])
            .intValue();
    }

    public float callFloat(String func) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table)[1]).floatValue();
    }

    public float callFloat(String func, Object param1) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1)[1]).floatValue();
    }

    public float callFloat(String func, Object param1, Object param2) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2)[1]).floatValue();
    }

    public float callFloat(String func, Object param1, Object param2, Object param3) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3)[1]).floatValue();
    }

    public float callFloat(String func, Object param1, Object param2, Object param3, Object param4) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3, param4)[1]).floatValue();
    }

    public float callFloat(String func, Object param1, Object param2, Object param3, Object param4, Object param5) {
        return ((Double)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3, param4, param5)[1])
            .floatValue();
    }

    public boolean callBool(String func) {
        return (Boolean)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table)[1];
    }

    public boolean callBool(String func, Object param1) {
        return (Boolean)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1)[1];
    }

    public boolean callBool(String func, Object param1, Object param2) {
        return (Boolean)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2)[1];
    }

    public boolean callBool(String func, Object param1, Object param2, Object param3) {
        return (Boolean)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3)[1];
    }

    public boolean callBool(String func, Object param1, Object param2, Object param3, Object param4) {
        return (Boolean)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3, param4)[1];
    }

    public boolean callBool(String func, Object param1, Object param2, Object param3, Object param4, Object param5) {
        return (Boolean)LuaManager.caller.pcall(LuaManager.thread, this.typeTable.rawget(func), this.table, param1, param2, param3, param4, param5)[1];
    }

    @Override
    public void setMetatable(KahluaTable metatable) {
        this.table.setMetatable(metatable);
    }

    @Override
    public KahluaTable getMetatable() {
        return this.table.getMetatable();
    }

    @Override
    public void rawset(Object key, Object value) {
        this.table.rawset(key, value);
    }

    @Override
    public Object rawget(Object key) {
        return this.table.rawget(key);
    }

    @Override
    public void rawset(int key, Object value) {
        this.table.rawset(key, value);
    }

    @Override
    public Object rawget(int key) {
        return this.table.rawget(key);
    }

    @Override
    public int len() {
        return this.table.len();
    }

    @Override
    public int size() {
        return this.table.len();
    }

    @Override
    public KahluaTableIterator iterator() {
        return this.table.iterator();
    }

    @Override
    public boolean isEmpty() {
        return this.table.isEmpty();
    }

    @Override
    public void wipe() {
        this.table.wipe();
    }

    @Override
    public void save(ByteBuffer output) throws IOException {
        this.table.save(output);
    }

    @Override
    public void load(ByteBuffer input, int WorldVersion) throws IOException {
        this.table.load(input, WorldVersion);
    }

    @Override
    public void save(DataOutputStream output) throws IOException {
        this.table.save(output);
    }

    @Override
    public void load(DataInputStream input, int WorldVersion) throws IOException {
        this.table.load(input, WorldVersion);
    }
}
