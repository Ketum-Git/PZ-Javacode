// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.globalObjects;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashSet;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.KahluaTableIterator;
import zombie.UsedFromLua;
import zombie.ZomboidFileSystem;
import zombie.Lua.LuaManager;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;
import zombie.core.Core;
import zombie.core.logger.ExceptionLogger;
import zombie.iso.IsoObject;
import zombie.iso.SliceY;
import zombie.network.GameClient;
import zombie.util.Type;

@UsedFromLua
public final class SGlobalObjectSystem extends GlobalObjectSystem {
    private static KahluaTable tempTable;
    protected int loadedWorldVersion = -1;
    protected final HashSet<String> modDataKeys = new HashSet<>();
    protected final HashSet<String> objectModDataKeys = new HashSet<>();
    protected final HashSet<String> objectSyncKeys = new HashSet<>();

    public SGlobalObjectSystem(String name) {
        super(name);
    }

    @Override
    protected GlobalObject makeObject(int x, int y, int z) {
        return new SGlobalObject(this, x, y, z);
    }

    public void setModDataKeys(KahluaTable keys) {
        this.modDataKeys.clear();
        if (keys != null) {
            KahluaTableIterator iterator = keys.iterator();

            while (iterator.advance()) {
                Object value = iterator.getValue();
                if (!(value instanceof String s)) {
                    throw new IllegalArgumentException("expected string but got \"" + value + "\"");
                }

                this.modDataKeys.add(s);
            }
        }
    }

    public void setObjectModDataKeys(KahluaTable keys) {
        this.objectModDataKeys.clear();
        if (keys != null) {
            KahluaTableIterator iterator = keys.iterator();

            while (iterator.advance()) {
                Object value = iterator.getValue();
                if (!(value instanceof String s)) {
                    throw new IllegalArgumentException("expected string but got \"" + value + "\"");
                }

                this.objectModDataKeys.add(s);
            }
        }
    }

    public void setObjectSyncKeys(KahluaTable keys) {
        this.objectSyncKeys.clear();
        if (keys != null) {
            KahluaTableIterator iterator = keys.iterator();

            while (iterator.advance()) {
                Object value = iterator.getValue();
                if (!(value instanceof String s)) {
                    throw new IllegalArgumentException("expected string but got \"" + value + "\"");
                }

                this.objectSyncKeys.add(s);
            }
        }
    }

    public void update() {
    }

    public void chunkLoaded(int wx, int wy) {
        if (this.hasObjectsInChunk(wx, wy)) {
            Object function = this.modData.rawget("OnChunkLoaded");
            if (function == null) {
                throw new IllegalStateException("OnChunkLoaded method undefined for system '" + this.name + "'");
            } else {
                Double dwx = BoxedStaticValues.toDouble(wx);
                Double dwy = BoxedStaticValues.toDouble(wy);
                LuaManager.caller.pcall(LuaManager.thread, function, this.modData, dwx, dwy);
            }
        }
    }

    public void sendCommand(String command, KahluaTable args) {
        SGlobalObjectNetwork.sendServerCommand(this.name, command, args);
    }

    public void receiveClientCommand(String command, IsoPlayer playerObj, KahluaTable args) {
        Object function = this.modData.rawget("OnClientCommand");
        if (function == null) {
            throw new IllegalStateException("OnClientCommand method undefined for system '" + this.name + "'");
        } else {
            LuaManager.caller.pcall(LuaManager.thread, function, this.modData, command, playerObj, args);
        }
    }

    public void addGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        if (globalObject == null) {
            throw new IllegalArgumentException("globalObject is null");
        } else if (globalObject.system != this) {
            throw new IllegalArgumentException("object not in this system");
        } else {
            SGlobalObjectNetwork.addGlobalObjectOnClient(globalObject);
        }
    }

    public void removeGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        if (globalObject == null) {
            throw new IllegalArgumentException("globalObject is null");
        } else if (globalObject.system != this) {
            throw new IllegalArgumentException("object not in this system");
        } else {
            SGlobalObjectNetwork.removeGlobalObjectOnClient(globalObject);
        }
    }

    public void updateGlobalObjectOnClient(SGlobalObject globalObject) throws IOException {
        if (globalObject == null) {
            throw new IllegalArgumentException("globalObject is null");
        } else if (globalObject.system != this) {
            throw new IllegalArgumentException("object not in this system");
        } else {
            SGlobalObjectNetwork.updateGlobalObjectOnClient(globalObject);
        }
    }

    private String getFileName() {
        return ZomboidFileSystem.instance.getFileNameInCurrentSave("gos_" + this.name + ".bin");
    }

    public KahluaTable getInitialStateForClient() {
        Object function = this.modData.rawget("getInitialStateForClient");
        if (function == null) {
            throw new IllegalStateException("getInitialStateForClient method undefined for system '" + this.name + "'");
        } else {
            Object[] result = LuaManager.caller.pcall(LuaManager.thread, function, this.modData);
            return result != null && result[0].equals(Boolean.TRUE) && result[1] instanceof KahluaTable ? (KahluaTable)result[1] : null;
        }
    }

    public void OnIsoObjectChangedItself(IsoObject isoObject) {
        GlobalObject globalObject = this.getObjectAt(isoObject.getSquare().x, isoObject.getSquare().y, isoObject.getSquare().z);
        if (globalObject != null) {
            Object function = this.modData.rawget("OnIsoObjectChangedItself");
            if (function == null) {
                throw new IllegalStateException("OnIsoObjectChangedItself method undefined for system '" + this.name + "'");
            } else {
                LuaManager.caller.pcall(LuaManager.thread, function, this.modData, isoObject);
            }
        }
    }

    public void OnModDataChangeItself(IsoObject isoObject) {
        GlobalObject globalObject = this.getObjectAt(isoObject.getSquare().x, isoObject.getSquare().y, isoObject.getSquare().z);
        if (globalObject != null) {
            Object function = this.modData.rawget("OnModDataChangeItself");
            if (function == null) {
                throw new IllegalStateException("OnModDataChangeItself method undefined for system '" + this.name + "'");
            } else {
                LuaManager.caller.pcall(LuaManager.thread, function, this.modData, isoObject);
            }
        }
    }

    public int loadedWorldVersion() {
        return this.loadedWorldVersion;
    }

    public void load(ByteBuffer bb, int WorldVersion) throws IOException {
        boolean empty = bb.get() == 0;
        if (!empty) {
            this.modData.load(bb, WorldVersion);
        }

        int count = bb.getInt();

        for (int i = 0; i < count; i++) {
            int x = bb.getInt();
            int y = bb.getInt();
            byte z = bb.get();
            SGlobalObject object = Type.tryCastTo(this.newObject(x, y, z), SGlobalObject.class);
            object.load(bb, WorldVersion);
        }

        this.loadedWorldVersion = WorldVersion;
    }

    public void save(ByteBuffer bb) throws IOException {
        if (tempTable == null) {
            tempTable = LuaManager.platform.newTable();
        }

        tempTable.wipe();
        KahluaTableIterator iterator = this.modData.iterator();

        while (iterator.advance()) {
            Object key = iterator.getKey();
            if (this.modDataKeys.contains(key)) {
                tempTable.rawset(key, this.modData.rawget(key));
            }
        }

        if (tempTable.isEmpty()) {
            bb.put((byte)0);
        } else {
            bb.put((byte)1);
            tempTable.save(bb);
        }

        bb.putInt(this.objects.size());

        for (int i = 0; i < this.objects.size(); i++) {
            SGlobalObject object = Type.tryCastTo(this.objects.get(i), SGlobalObject.class);
            object.save(bb);
        }
    }

    public void load() {
        File file = new File(this.getFileName());

        try (
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
        ) {
            synchronized (SliceY.SliceBufferLock) {
                ByteBuffer bb = SliceY.SliceBuffer;
                bb.clear();
                int numBytes = bis.read(bb.array());
                bb.limit(numBytes);
                byte b1 = bb.get();
                byte b2 = bb.get();
                byte b3 = bb.get();
                byte b4 = bb.get();
                if (b1 != 71 || b2 != 76 || b3 != 79 || b4 != 83) {
                    throw new IOException("doesn't appear to be a GlobalObjectSystem file:" + file.getAbsolutePath());
                }

                int WorldVersion = bb.getInt();
                if (WorldVersion > 240) {
                    throw new IOException("file is from a newer version " + WorldVersion + " of the game: " + file.getAbsolutePath());
                }

                this.load(bb, WorldVersion);
            }
        } catch (FileNotFoundException var18) {
        } catch (Throwable var19) {
            ExceptionLogger.logException(var19);
        }
    }

    public void save() {
        if (!Core.getInstance().isNoSave()) {
            if (!GameClient.client) {
                File file = new File(this.getFileName());

                try (
                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                ) {
                    synchronized (SliceY.SliceBufferLock) {
                        ByteBuffer bb = SliceY.SliceBuffer;
                        bb.clear();
                        bb.put((byte)71);
                        bb.put((byte)76);
                        bb.put((byte)79);
                        bb.put((byte)83);
                        bb.putInt(240);
                        this.save(bb);
                        bos.write(bb.array(), 0, bb.position());
                    }
                } catch (Throwable var12) {
                    ExceptionLogger.logException(var12);
                }
            }
        }
    }

    @Override
    public void Reset() {
        super.Reset();
        this.modDataKeys.clear();
        this.objectModDataKeys.clear();
        this.objectSyncKeys.clear();
    }
}
