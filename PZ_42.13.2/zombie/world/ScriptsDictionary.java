// Decompiled with Zomboid Decompiler v0.3.0 using Vineflower.
package zombie.world;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import zombie.GameWindow;
import zombie.core.Core;
import zombie.core.utils.ByteBlock;
import zombie.debug.DebugLog;
import zombie.network.GameClient;
import zombie.scripting.entity.components.contextmenuconfig.ContextMenuConfigScript;
import zombie.scripting.entity.components.spriteconfig.SpriteConfigScript;
import zombie.scripting.entity.components.spriteconfig.SpriteOverlayConfigScript;
import zombie.scripting.objects.BaseScriptObject;
import zombie.world.logger.Log;
import zombie.world.logger.WorldDictionaryLogger;
import zombie.world.scripts.VersionHash;

public class ScriptsDictionary {
    private static final ArrayList<ScriptsDictionary.ScriptRegister<?>> registers = new ArrayList<>();
    public static final ScriptsDictionary.ScriptRegister<SpriteConfigScript> spriteConfigs = addScriptRegister(
        new ScriptsDictionary.ScriptRegister<SpriteConfigScript>("SpriteConfigs") {
            @Override
            protected boolean canRegister(Object obj) {
                return obj instanceof SpriteConfigScript;
            }
        }
    );
    public static final ScriptsDictionary.ScriptRegister<SpriteOverlayConfigScript> spriteOverlayConfigs = addScriptRegister(
        new ScriptsDictionary.ScriptRegister<SpriteOverlayConfigScript>("SpriteOverlayConfigs") {
            @Override
            protected boolean canRegister(Object obj) {
                return obj instanceof SpriteOverlayConfigScript;
            }
        }
    );
    public static final ScriptsDictionary.ScriptRegister<ContextMenuConfigScript> ContextMenuConfigs = addScriptRegister(
        new ScriptsDictionary.ScriptRegister<ContextMenuConfigScript>("ContextMenuConfigs") {
            @Override
            protected boolean canRegister(Object obj) {
                return obj instanceof ContextMenuConfigScript;
            }
        }
    );

    private ScriptsDictionary() {
    }

    private static <T extends ScriptsDictionary.ScriptRegister<?>> T addScriptRegister(T register) {
        if (!registers.contains(register)) {
            registers.add(register);
        }

        return register;
    }

    protected static void StartScriptLoading() {
        for (ScriptsDictionary.ScriptRegister<?> register : registers) {
            register.onStartScriptLoading();
        }
    }

    protected static void reset() {
        for (ScriptsDictionary.ScriptRegister<?> register : registers) {
            register.reset();
        }
    }

    public static void registerScript(BaseScriptObject script) {
        try {
            if (!WorldDictionary.isAllowScriptItemLoading()) {
                throw new WorldDictionaryException("Cannot register script at this time.");
            }

            boolean canAdd = false;

            for (int i = 0; i < registers.size(); i++) {
                ScriptsDictionary.ScriptRegister register = registers.get(i);
                if (register.canRegister(script)) {
                    canAdd = true;
                    register.register(script);
                    break;
                }
            }

            if (!canAdd) {
                throw new WorldDictionaryException("Missing script register.");
            }
        } catch (Exception var4) {
            var4.printStackTrace();
        }
    }

    protected static void parseRegisters() throws WorldDictionaryException {
        for (int i = 0; i < registers.size(); i++) {
            ScriptsDictionary.ScriptRegister<?> register = registers.get(i);
            if (!GameClient.client) {
                register.parseLoadList();
            } else {
                register.parseLoadListClient();
            }
        }
    }

    protected static void saveAsText(FileWriter w, String padding) throws IOException {
        for (int i = 0; i < registers.size(); i++) {
            ScriptsDictionary.ScriptRegister<?> register = registers.get(i);
            register.saveAsText(w, padding);
        }
    }

    protected static void saveToByteBuffer(ByteBuffer bb) throws IOException {
        bb.putInt(registers.size());

        for (ScriptsDictionary.ScriptRegister<?> register : registers) {
            GameWindow.WriteString(bb, register.name);
            register.save(bb);
        }
    }

    protected static void loadFromByteBuffer(ByteBuffer bb, int Version) throws IOException {
        int size = bb.getInt();

        for (int i = 0; i < size; i++) {
            String registerName = GameWindow.ReadString(bb);
            boolean found = false;

            for (ScriptsDictionary.ScriptRegister<?> register : registers) {
                if (register.name.equals(registerName)) {
                    register.load(bb, Version);
                    found = true;
                    break;
                }
            }

            if (!found) {
                ScriptsDictionary.ScriptRegister.loadEmpty(bb, Version);
                DebugLog.General.debugln("ScriptRegister not found or deprecated = " + registerName);
            }
        }
    }

    public static class ScriptRegister<T extends BaseScriptObject> {
        private final Map<String, DictionaryScriptInfo<T>> namedMap = new HashMap<>();
        private final Map<Short, DictionaryScriptInfo<T>> idMap = new HashMap<>();
        private final Map<String, DictionaryScriptInfo<T>> loadList = new HashMap<>();
        private final VersionHash hash = new VersionHash();
        private final String name;
        private short nextId;
        private final ScriptsDictionary.Tuple<T> emptyTuple = new ScriptsDictionary.Tuple<>(false);
        private final ScriptsDictionary.Tuple<T> notFoundTuple = new ScriptsDictionary.Tuple<>(true);

        protected ScriptRegister(String name) {
            if (name == null) {
                throw new RuntimeException("Name cannot be null.");
            } else {
                this.name = name;
            }
        }

        public void saveScript(ByteBuffer output, T script) {
            DictionaryScriptInfo<T> info = this.getInfoForName(script.getScriptObjectFullType());
            if (info != null) {
                output.put((byte)1);
                output.putShort(info.registryId);
            } else {
                output.put((byte)0);
                if (Core.debug) {
                    DebugLog.General.warn("[" + this.name + "] Unable to save script: '" + script.getScriptObjectFullType() + "'");
                }
            }
        }

        public T loadScript(ByteBuffer input, int WorldVersion) {
            if (input.get() == 1) {
                short id = input.getShort();
                DictionaryScriptInfo<T> info = this.getInfoForID(id);
                if (info != null) {
                    return info.script;
                }

                if (Core.debug) {
                    DebugLog.General.warn("[" + this.name + "] Unable to load script with id: " + id);
                }
            }

            return null;
        }

        public T get(String name) {
            DictionaryScriptInfo<T> info = this.namedMap.get(name);
            return info != null ? info.script : null;
        }

        public T get(short id) {
            DictionaryScriptInfo<T> info = this.idMap.get(id);
            return info != null ? info.script : null;
        }

        public short getIdFor(String name) {
            DictionaryScriptInfo<T> info = this.namedMap.get(name);
            return info != null ? info.registryId : -1;
        }

        public boolean isRegistered(T script) {
            if (WorldDictionary.isAllowScriptItemLoading()) {
                if (script != null && this.loadList.containsKey(script.getScriptObjectFullType())) {
                    if (this.loadList.get(script.getScriptObjectFullType()).script == script) {
                        return true;
                    }

                    if (Core.debug) {
                        DebugLog.General
                            .warn(
                                "["
                                    + this.name
                                    + "] A script with same full type is registered, but objects are not equal, script: '"
                                    + script.getScriptObjectFullType()
                                    + "'"
                            );
                    }
                }

                return false;
            } else {
                if (script != null) {
                    DictionaryScriptInfo<T> info = this.namedMap.get(script.getScriptObjectFullType());
                    if (info != null && info.script == script && info.isLoaded()) {
                        return true;
                    }
                }

                return false;
            }
        }

        public DictionaryScriptInfo<T> getInfoForName(String name) {
            return this.namedMap.get(name);
        }

        public DictionaryScriptInfo<T> getInfoForID(short id) {
            return this.idMap.get(id);
        }

        protected void register(T script) throws WorldDictionaryException {
            if (!this.canRegister(script)) {
                throw new WorldDictionaryException("[" + this.name + "]  Cannot register this script!");
            } else {
                DictionaryScriptInfo<T> info = new DictionaryScriptInfo<>();
                info.script = script;
                info.name = script.getScriptObjectFullType();
                this.loadList.put(info.name, info);
            }
        }

        protected boolean canRegister(Object obj) throws WorldDictionaryException {
            throw new WorldDictionaryException("[" + this.name + "]  CanRegister has not been overridden.");
        }

        public long getVersionHash(T script) throws WorldDictionaryException {
            this.hash.reset();
            script.getVersion(this.hash);
            if (this.hash.isEmpty()) {
                throw new WorldDictionaryException("[" + this.name + "] Script hash is empty: " + script.getScriptObjectFullType());
            } else if (this.hash.isCorrupted()) {
                throw new WorldDictionaryException("[" + this.name + "] Corrupted hash for script: " + script.getScriptObjectFullType());
            } else {
                return this.hash.getHash();
            }
        }

        protected void parseLoadList() throws WorldDictionaryException {
            if (GameClient.client) {
                throw new WorldDictionaryException("[" + this.name + "] Shouldn't be called on client!");
            } else {
                DebugLog.General.debugln("- Parse load list: " + this.name);

                for (Entry<String, DictionaryScriptInfo<T>> entry : this.loadList.entrySet()) {
                    entry.getValue().isLoaded = false;
                }

                for (Entry<String, DictionaryScriptInfo<T>> entry : this.loadList.entrySet()) {
                    DictionaryScriptInfo<T> info = entry.getValue();
                    DictionaryScriptInfo<T> stored = this.namedMap.get(info.name);
                    info.version = this.getVersionHash(info.script);
                    if (stored == null) {
                        if (this.nextId >= 32767) {
                            throw new WorldDictionaryException("[" + this.name + "] Max script ID value reached for " + this.name + "!");
                        }

                        info.registryId = this.nextId++;
                        this.namedMap.put(info.name, info);
                        this.idMap.put(info.registryId, info);
                        info.isLoaded = true;
                        WorldDictionaryLogger.log(new Log.RegisterScript(info.copy()));
                    } else {
                        stored.script = info.script;
                        stored.isLoaded = true;
                        if (stored.version != info.version) {
                            DebugLog.log("[" + this.name + "]  Script '" + stored.name + "' changed version.");
                            WorldDictionaryLogger.log(new Log.VersionChangedScript(info.copy(), stored.version));
                            stored.version = info.version;
                        }
                    }
                }
            }
        }

        protected void parseLoadListClient() throws WorldDictionaryException {
            if (!GameClient.client) {
                throw new WorldDictionaryException("Should only be called on client!");
            } else {
                for (Entry<String, DictionaryScriptInfo<T>> entry : this.namedMap.entrySet()) {
                    DictionaryScriptInfo<T> stored = entry.getValue();
                    if (stored.isLoaded) {
                        DictionaryScriptInfo<T> info = this.loadList.get(stored.name);
                        if (info == null) {
                            throw new WorldDictionaryException("[" + this.name + "] Missing dictionary script on client: " + stored.name);
                        }

                        long version = this.getVersionHash(info.script);
                        if (version != stored.version) {
                            throw new WorldDictionaryException("[" + this.name + "] Script version mismatch with server: " + stored.name);
                        }

                        stored.script = info.script;
                    }
                }
            }
        }

        protected void reset() {
            this.nextId = 0;
            this.namedMap.clear();
            this.idMap.clear();
        }

        protected void onStartScriptLoading() {
            this.loadList.clear();
        }

        protected void saveAsText(FileWriter w, String padding) throws IOException {
            w.write(padding + this.name + " = {" + System.lineSeparator());
            String p1 = padding + "\t";
            String p2 = p1 + "\t";

            for (Entry<String, DictionaryScriptInfo<T>> entry : this.namedMap.entrySet()) {
                w.write(p1 + "{" + System.lineSeparator());
                entry.getValue().saveAsText(w, p2);
                w.write(p1 + "}," + System.lineSeparator());
            }

            w.write(padding + "}," + System.lineSeparator());
        }

        protected void save(ByteBuffer bb) throws IOException {
            ByteBlock block = ByteBlock.Start(bb, ByteBlock.Mode.Save);
            bb.putShort(this.nextId);
            bb.putInt(this.namedMap.size());

            for (Entry<String, DictionaryScriptInfo<T>> entry : this.namedMap.entrySet()) {
                entry.getValue().save(bb);
            }

            ByteBlock.End(bb, block);
        }

        protected void load(ByteBuffer bb, int Version) throws IOException {
            ByteBlock block = ByteBlock.Start(bb, ByteBlock.Mode.Load);
            this.nextId = bb.getShort();
            int size = bb.getInt();

            for (int i = 0; i < size; i++) {
                DictionaryScriptInfo<T> info = new DictionaryScriptInfo<>();
                info.load(bb, Version);
                this.namedMap.put(info.name, info);
                this.idMap.put(info.registryId, info);
            }

            ByteBlock.End(bb, block);
        }

        private static final void loadEmpty(ByteBuffer bb, int Version) throws IOException {
            ByteBlock block = ByteBlock.Start(bb, ByteBlock.Mode.Load);
            ByteBlock.SkipAndEnd(bb, block);
        }
    }

    public static class Tuple<T extends BaseScriptObject> {
        private final T script;
        private long version;
        private long loadedVersion;
        private boolean notFound;

        public Tuple(boolean notFound) {
            this.script = null;
            this.version = 0L;
            this.loadedVersion = 0L;
            this.notFound = notFound;
        }

        public Tuple(T script, long version, long loadedVersion) {
            this.script = script;
            this.version = version;
            this.loadedVersion = loadedVersion;
        }

        public T getScript() {
            return this.script;
        }

        public long getVersion() {
            return this.version;
        }

        public long getLoadedVersion() {
            return this.loadedVersion;
        }

        public boolean isVersionValid() {
            return this.version == this.loadedVersion;
        }

        public boolean isScriptValid() {
            return this.script != null && this.isVersionValid();
        }

        public boolean isNotFound() {
            return this.notFound;
        }
    }
}
